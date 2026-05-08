import { useState } from 'react';
import { processImageForOCR, ExtractedInvoiceData } from '../../lib/ocr-processor';
import { searchCustomers, addCustomer, addTransaction } from '../../lib/db-operations';
import { Customer } from '../../types';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Progress } from './ui/progress';
import { Camera, Upload, Check } from 'lucide-react';
import { toast } from 'sonner';

interface OCRScannerProps {
  onBack: () => void;
}

export default function OCRScanner({ onBack }: OCRScannerProps) {
  const [isProcessing, setIsProcessing] = useState(false);
  const [progress, setProgress] = useState(0);
  const [extractedData, setExtractedData] = useState<ExtractedInvoiceData | null>(null);
  const [customerSearch, setCustomerSearch] = useState('');
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [editedTotal, setEditedTotal] = useState('');

  const handleImageCapture = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsProcessing(true);
    setProgress(0);

    try {
      const data = await processImageForOCR(file, setProgress);
      setExtractedData(data);
      setEditedTotal(data.total.toString());
      toast.success('OCR completed!');
    } catch (error) {
      toast.error('Failed to process image. Please try again or enter manually.');
      console.error(error);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleCustomerSearch = async (query: string) => {
    setCustomerSearch(query);
    setSelectedCustomer(null);

    if (query.length >= 2) {
      const results = await searchCustomers(query);
      setCustomers(results);
    } else {
      setCustomers([]);
    }
  };

  const handleSave = async () => {
    const amount = parseFloat(editedTotal);
    if (!amount || amount <= 0) {
      toast.error('Enter valid amount');
      return;
    }

    let custId: number;

    if (selectedCustomer) {
      custId = selectedCustomer.id!;
    } else if (customerSearch.trim()) {
      custId = await addCustomer({
        name: customerSearch.trim(),
        phone: ''
      });
    } else {
      toast.error('Select or enter customer name');
      return;
    }

    await addTransaction({
      customerId: custId,
      type: 'credit',
      amount,
      note: 'From OCR scan',
      date: Date.now()
    });

    toast.success(`₹${amount} credit saved!`);
    setExtractedData(null);
    setCustomerSearch('');
    setSelectedCustomer(null);
    setEditedTotal('');
  };

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="p-4 border-b sticky top-0 bg-background z-10">
        <div className="flex items-center justify-between mb-4">
          <Button variant="ghost" onClick={onBack}>
            ← Back
          </Button>
          <h1 className="text-xl font-bold">Scan Invoice</h1>
          <div />
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-4 space-y-4">
        {!extractedData && !isProcessing && (
          <div className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Capture Bill</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-sm text-muted-foreground">
                  Take a photo of a handwritten bill or receipt. The app will extract items and total amount.
                </p>

                <div className="grid grid-cols-2 gap-4">
                  <div className="relative">
                    <input
                      type="file"
                      accept="image/*"
                      capture="environment"
                      className="hidden"
                      id="camera-input"
                      onChange={handleImageCapture}
                    />
                    <Button className="w-full" onClick={() => document.getElementById('camera-input')?.click()}>
                      <Camera size={20} className="mr-2" />
                      Camera
                    </Button>
                  </div>

                  <div className="relative">
                    <input
                      type="file"
                      accept="image/*"
                      className="hidden"
                      id="gallery-input"
                      onChange={handleImageCapture}
                    />
                    <Button variant="outline" className="w-full" onClick={() => document.getElementById('gallery-input')?.click()}>
                      <Upload size={20} className="mr-2" />
                      Gallery
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-4">
                <h3 className="font-semibold mb-2">How it works:</h3>
                <ul className="text-sm text-muted-foreground space-y-1 list-disc list-inside">
                  <li>Capture a clear photo of the bill</li>
                  <li>OCR will extract text automatically</li>
                  <li>Review and edit the extracted data</li>
                  <li>Select customer and save</li>
                </ul>
              </CardContent>
            </Card>
          </div>
        )}

        {isProcessing && (
          <Card>
            <CardContent className="p-8">
              <div className="text-center space-y-4">
                <p className="font-medium">Processing image...</p>
                <Progress value={progress} />
                <p className="text-sm text-muted-foreground">{progress}%</p>
              </div>
            </CardContent>
          </Card>
        )}

        {extractedData && !isProcessing && (
          <div className="space-y-4">
            {/* Extracted Data */}
            <Card>
              <CardHeader>
                <CardTitle>Extracted Data</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {extractedData.items.length > 0 && (
                  <div>
                    <p className="font-medium mb-2">Items:</p>
                    <div className="space-y-1">
                      {extractedData.items.map((item, i) => (
                        <div key={i} className="flex justify-between text-sm">
                          <span>{item.name}</span>
                          <span>₹{item.amount}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div>
                  <p className="font-medium mb-2">Total Amount:</p>
                  <Input
                    type="number"
                    value={editedTotal}
                    onChange={(e) => setEditedTotal(e.target.value)}
                    placeholder="Edit if needed"
                    className="text-xl font-bold"
                  />
                </div>

                <details>
                  <summary className="text-sm text-muted-foreground cursor-pointer">
                    View raw OCR text
                  </summary>
                  <pre className="text-xs mt-2 p-2 bg-muted rounded overflow-auto max-h-40">
                    {extractedData.rawText}
                  </pre>
                </details>
              </CardContent>
            </Card>

            {/* Customer Selection */}
            <Card>
              <CardHeader>
                <CardTitle>Select Customer</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="relative">
                  <Input
                    value={customerSearch}
                    onChange={(e) => handleCustomerSearch(e.target.value)}
                    placeholder="Search or enter customer name..."
                  />
                  {customers.length > 0 && !selectedCustomer && (
                    <Card className="absolute top-full left-0 right-0 max-h-40 overflow-auto z-20 mt-1">
                      <CardContent className="p-2">
                        {customers.map((c) => (
                          <div
                            key={c.id}
                            className="p-2 hover:bg-accent rounded cursor-pointer"
                            onClick={() => {
                              setSelectedCustomer(c);
                              setCustomerSearch(c.name);
                              setCustomers([]);
                            }}
                          >
                            <p className="font-medium">{c.name}</p>
                            {c.phone && <p className="text-xs text-muted-foreground">{c.phone}</p>}
                          </div>
                        ))}
                      </CardContent>
                    </Card>
                  )}
                </div>

                <div className="flex gap-2">
                  <Button variant="outline" className="flex-1" onClick={() => setExtractedData(null)}>
                    Cancel
                  </Button>
                  <Button className="flex-1 bg-green-600 hover:bg-green-700" onClick={handleSave}>
                    <Check size={20} className="mr-2" />
                    Save Credit
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>
        )}
      </div>
    </div>
  );
}
