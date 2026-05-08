import { useState, useEffect } from 'react';
import { searchCustomers, addCustomer, addTransaction } from '../../lib/db-operations';
import { Customer } from '../../types';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Mic, Check } from 'lucide-react';
import { parseVoiceCommand, startVoiceRecognition } from '../../lib/voice-parser';
import { toast } from 'sonner';

interface QuickEntryProps {
  onBack: () => void;
}

export default function QuickEntry({ onBack }: QuickEntryProps) {
  const [amount, setAmount] = useState('');
  const [type, setType] = useState<'credit' | 'payment'>('credit');
  const [searchQuery, setSearchQuery] = useState('');
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [isListening, setIsListening] = useState(false);

  useEffect(() => {
    if (searchQuery.length >= 2) {
      searchCustomers(searchQuery).then(setCustomers);
    } else {
      setCustomers([]);
    }
  }, [searchQuery]);

  const handleNumberClick = (num: string) => {
    setAmount((prev) => prev + num);
  };

  const handleBackspace = () => {
    setAmount((prev) => prev.slice(0, -1));
  };

  const handleClear = () => {
    setAmount('');
  };

  const handleSave = async () => {
    const amt = parseFloat(amount);
    if (!amt || amt <= 0) {
      toast.error('Enter valid amount');
      return;
    }

    let custId: number;

    if (selectedCustomer) {
      custId = selectedCustomer.id!;
    } else if (searchQuery.trim()) {
      // Create new customer
      custId = await addCustomer({
        name: searchQuery.trim(),
        phone: ''
      });
    } else {
      toast.error('Select or enter customer name');
      return;
    }

    await addTransaction({
      customerId: custId,
      type,
      amount: amt,
      date: Date.now()
    });

    toast.success(`₹${amt} ${type} saved!`);
    setAmount('');
    setSearchQuery('');
    setSelectedCustomer(null);
    setCustomers([]);
  };

  const handleVoiceInput = () => {
    setIsListening(true);
    const stop = startVoiceRecognition(
      (text) => {
        setIsListening(false);
        const parsed = parseVoiceCommand(text);
        if (parsed) {
          setSearchQuery(parsed.name);
          setAmount(parsed.amount.toString());
          setType(parsed.type);
          toast.success(`Parsed: ${parsed.name} - ₹${parsed.amount} ${parsed.type}`);
        } else {
          toast.error('Could not parse voice input');
        }
      },
      (error) => {
        setIsListening(false);
        toast.error(error);
      }
    );
  };

  const numberPad = [
    ['1', '2', '3'],
    ['4', '5', '6'],
    ['7', '8', '9'],
    ['.', '0', '⌫']
  ];

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="p-4 border-b sticky top-0 bg-background z-10">
        <div className="flex items-center justify-between mb-4">
          <Button variant="ghost" onClick={onBack}>
            ← Back
          </Button>
          <h1 className="text-xl font-bold">Quick Entry</h1>
          <Button
            variant="outline"
            size="icon"
            onClick={handleVoiceInput}
            disabled={isListening}
          >
            <Mic size={20} className={isListening ? 'animate-pulse text-red-500' : ''} />
          </Button>
        </div>

        {/* Customer Search */}
        <div className="relative">
          <Input
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setSelectedCustomer(null);
            }}
            placeholder="Customer name..."
            className="mb-2"
          />
          {customers.length > 0 && !selectedCustomer && (
            <Card className="absolute top-full left-0 right-0 max-h-40 overflow-auto z-20">
              <CardContent className="p-2">
                {customers.map((c) => (
                  <div
                    key={c.id}
                    className="p-2 hover:bg-accent rounded cursor-pointer"
                    onClick={() => {
                      setSelectedCustomer(c);
                      setSearchQuery(c.name);
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

        {/* Type Toggle */}
        <div className="grid grid-cols-2 gap-2">
          <Button
            variant={type === 'credit' ? 'default' : 'outline'}
            onClick={() => setType('credit')}
          >
            Credit (Udari)
          </Button>
          <Button
            variant={type === 'payment' ? 'default' : 'outline'}
            onClick={() => setType('payment')}
          >
            Payment
          </Button>
        </div>
      </div>

      {/* Amount Display */}
      <div className="p-6 bg-muted">
        <p className="text-center text-muted-foreground">Amount</p>
        <p className="text-center text-5xl font-bold">
          ₹{amount || '0'}
        </p>
      </div>

      {/* Number Pad */}
      <div className="flex-1 p-4">
        <div className="grid grid-cols-3 gap-3 max-w-md mx-auto">
          {numberPad.map((row, i) => (
            row.map((key) => (
              <Button
                key={key}
                variant="outline"
                className="h-16 text-2xl"
                onClick={() => {
                  if (key === '⌫') handleBackspace();
                  else handleNumberClick(key);
                }}
              >
                {key}
              </Button>
            ))
          ))}
        </div>

        <div className="grid grid-cols-2 gap-3 mt-4 max-w-md mx-auto">
          <Button variant="outline" onClick={handleClear}>
            Clear
          </Button>
          <Button onClick={handleSave} className="bg-green-600 hover:bg-green-700">
            <Check size={20} className="mr-2" />
            Save
          </Button>
        </div>
      </div>
    </div>
  );
}
