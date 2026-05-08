import { useEffect, useState } from 'react';
import {
  getCustomer,
  getCustomerTransactions,
  getCustomerBalance,
  addTransaction,
  getCustomerPhoto,
  saveCustomerPhoto
} from '../../lib/db-operations';
import { Customer, Transaction } from '../../types';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogDescription } from './ui/dialog';
import { Label } from './ui/label';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Download, Plus, Camera } from 'lucide-react';
import { generateInvoicePDF, sharePDFViaWhatsApp } from '../../lib/pdf-generator';
import { compressImage } from '../../lib/ocr-processor';

interface CustomerLedgerProps {
  customerId: number;
  onBack: () => void;
}

export default function CustomerLedger({ customerId, onBack }: CustomerLedgerProps) {
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [balance, setBalance] = useState(0);
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [newTxn, setNewTxn] = useState({ type: 'credit' as 'credit' | 'payment', amount: '', note: '' });

  useEffect(() => {
    loadData();
  }, [customerId]);

  const loadData = async () => {
    const cust = await getCustomer(customerId);
    setCustomer(cust || null);

    const txns = await getCustomerTransactions(customerId);
    setTransactions(txns);

    const bal = await getCustomerBalance(customerId);
    setBalance(bal);
  };

  const handleAddTransaction = async () => {
    const amount = parseFloat(newTxn.amount);
    if (!amount || amount <= 0) return;

    await addTransaction({
      customerId,
      type: newTxn.type,
      amount,
      note: newTxn.note.trim() || undefined,
      date: Date.now()
    });

    setNewTxn({ type: 'credit', amount: '', note: '' });
    setShowAddDialog(false);
    loadData();
  };

  const handleGenerateInvoice = async () => {
    if (!customer) return;
    const blob = generateInvoicePDF(customer, transactions, balance);
    await sharePDFViaWhatsApp(blob, customer.name);
  };

  const handlePhotoCapture = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !customer) return;

    try {
      const compressed = await compressImage(file);
      await saveCustomerPhoto(customerId, compressed);
      loadData();
    } catch (error) {
      console.error('Photo error:', error);
    }
  };

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  if (!customer) {
    return <div className="p-4">Loading...</div>;
  }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="p-4 border-b sticky top-0 bg-background z-10">
        <Button variant="ghost" onClick={onBack} className="mb-4">
          ← Back
        </Button>

        <div className="flex items-start gap-4">
          <div className="relative">
            <Avatar className="w-16 h-16">
              <AvatarImage src={getCustomerPhoto(customer.photoPath) || undefined} />
              <AvatarFallback>{getInitials(customer.name)}</AvatarFallback>
            </Avatar>
            <input
              type="file"
              accept="image/*"
              capture="environment"
              className="hidden"
              id="customer-photo-input"
              onChange={handlePhotoCapture}
            />
            <button
              onClick={() => document.getElementById('customer-photo-input')?.click()}
              className="absolute bottom-0 right-0 bg-primary text-primary-foreground rounded-full p-1 cursor-pointer"
              type="button"
            >
              <Camera size={14} />
            </button>
          </div>

          <div className="flex-1">
            <h1 className="text-xl font-bold">{customer.name}</h1>
            {customer.phone && <p className="text-sm text-muted-foreground">{customer.phone}</p>}
            <p className={`text-2xl font-bold mt-2 ${balance > 0 ? 'text-red-600' : 'text-green-600'}`}>
              ₹{Math.abs(balance).toFixed(2)} {balance > 0 ? 'pending' : balance < 0 ? 'advance' : ''}
            </p>
          </div>
        </div>

        <div className="flex gap-2 mt-4">
          <Dialog open={showAddDialog} onOpenChange={setShowAddDialog}>
            <DialogTrigger asChild>
              <Button className="flex-1">
                <Plus size={16} className="mr-2" />
                Add Transaction
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Add Transaction</DialogTitle>
                <DialogDescription>Record a new credit or payment transaction</DialogDescription>
              </DialogHeader>
              <div className="space-y-4">
                <div>
                  <Label>Type</Label>
                  <div className="grid grid-cols-2 gap-2 mt-2">
                    <Button
                      variant={newTxn.type === 'credit' ? 'default' : 'outline'}
                      onClick={() => setNewTxn({ ...newTxn, type: 'credit' })}
                    >
                      Credit (Udari)
                    </Button>
                    <Button
                      variant={newTxn.type === 'payment' ? 'default' : 'outline'}
                      onClick={() => setNewTxn({ ...newTxn, type: 'payment' })}
                    >
                      Payment
                    </Button>
                  </div>
                </div>
                <div>
                  <Label>Amount *</Label>
                  <Input
                    type="number"
                    value={newTxn.amount}
                    onChange={(e) => setNewTxn({ ...newTxn, amount: e.target.value })}
                    placeholder="Enter amount"
                  />
                </div>
                <div>
                  <Label>Note (optional)</Label>
                  <Textarea
                    value={newTxn.note}
                    onChange={(e) => setNewTxn({ ...newTxn, note: e.target.value })}
                    placeholder="Add a note"
                    rows={2}
                  />
                </div>
                <Button onClick={handleAddTransaction} className="w-full">
                  Add Transaction
                </Button>
              </div>
            </DialogContent>
          </Dialog>

          <Button variant="outline" onClick={handleGenerateInvoice}>
            <Download size={16} />
          </Button>
        </div>
      </div>

      {/* Transactions */}
      <div className="flex-1 overflow-auto p-4">
        {transactions.length === 0 ? (
          <div className="text-center py-12 text-muted-foreground">No transactions yet</div>
        ) : (
          <div className="space-y-2">
            {transactions.map((txn) => (
              <Card key={txn.id}>
                <CardContent className="p-4">
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="font-semibold">
                        {txn.type === 'credit' ? 'Credit Given' : 'Payment Received'}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {new Date(txn.date).toLocaleDateString('en-IN')} at{' '}
                        {new Date(txn.date).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
                      </p>
                      {txn.note && <p className="text-sm mt-1 text-muted-foreground italic">{txn.note}</p>}
                    </div>
                    <p className={`text-xl font-bold ${txn.type === 'credit' ? 'text-red-600' : 'text-green-600'}`}>
                      {txn.type === 'credit' ? '+' : '-'}₹{txn.amount}
                    </p>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
