import { useEffect, useState } from 'react';
import { getAllCustomerBalances, getCustomerPhoto } from '../../lib/db-operations';
import { CustomerBalance } from '../../types';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { MessageCircle, Phone, AlertCircle, Crown } from 'lucide-react';
import { toast } from 'sonner';

interface TopDebtorsProps {
  onSelectCustomer?: (id: number) => void;
}

export default function TopDebtors({ onSelectCustomer }: TopDebtorsProps) {
  const [debtors, setDebtors] = useState<CustomerBalance[]>([]);

  useEffect(() => {
    (async () => {
      const balances = await getAllCustomerBalances();
      const top = balances.filter((b) => b.balance > 0).sort((a, b) => b.balance - a.balance).slice(0, 3);
      setDebtors(top);
    })();
  }, []);

  const sendReminder = (d: CustomerBalance) => {
    const msg = `Namaste ${d.customerName}, this is a friendly reminder about your pending balance of ₹${d.balance.toFixed(2)}. Please clear it at your convenience. Dhanyavaad!`;
    const phone = d.customerPhone?.replace(/\D/g, '');
    if (!phone) {
      toast.error('No phone number on file');
      return;
    }
    window.open(`https://wa.me/91${phone}?text=${encodeURIComponent(msg)}`, '_blank');
    toast.success(`Reminder sent to ${d.customerName}`);
  };

  const callCustomer = (phone: string) => {
    if (!phone) return toast.error('No phone number');
    window.open(`tel:${phone}`);
  };

  if (debtors.length === 0) {
    return (
      <Card className="bg-gradient-to-br from-emerald-50 to-teal-50 border-emerald-200 shadow-md">
        <CardContent className="p-6 text-center">
          <div className="text-4xl mb-2">🎉</div>
          <p className="font-semibold text-emerald-700">All clear! No pending dues.</p>
        </CardContent>
      </Card>
    );
  }

  const medals = ['🥇', '🥈', '🥉'];

  return (
    <Card className="shadow-md border-l-4 border-l-amber-500 bg-gradient-to-br from-amber-50 via-white to-yellow-50">
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2">
          <Crown className="text-amber-500" size={20} />
          <span className="bg-gradient-to-r from-amber-600 to-orange-600 bg-clip-text text-transparent font-bold">
            Top Pending Customers
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {debtors.map((d, idx) => {
          const photo = getCustomerPhoto(d.customerPhoto);
          return (
            <div
              key={d.customerId}
              className="flex items-center gap-3 p-3 rounded-xl bg-white/70 backdrop-blur border border-amber-100 hover:border-amber-300 hover:shadow-md transition-all"
            >
              <div className="relative">
                {photo ? (
                  <img src={photo} alt={d.customerName} className="w-12 h-12 rounded-full object-cover border-2 border-amber-300" />
                ) : (
                  <div className="w-12 h-12 rounded-full bg-gradient-to-br from-amber-400 to-orange-500 flex items-center justify-center text-white font-bold">
                    {d.customerName.charAt(0).toUpperCase()}
                  </div>
                )}
                <span className="absolute -top-1 -right-1 text-lg">{medals[idx]}</span>
              </div>
              <button
                className="flex-1 text-left"
                onClick={() => onSelectCustomer?.(d.customerId)}
              >
                <p className="font-semibold truncate">{d.customerName}</p>
                <p className="text-xs text-red-600 flex items-center gap-1">
                  <AlertCircle size={12} />₹{d.balance.toFixed(0)} pending
                </p>
              </button>
              <div className="flex gap-1">
                <Button
                  size="sm"
                  variant="outline"
                  className="h-9 w-9 p-0 border-green-300 hover:bg-green-50"
                  onClick={() => sendReminder(d)}
                  title="WhatsApp Reminder"
                >
                  <MessageCircle size={16} className="text-green-600" />
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="h-9 w-9 p-0 border-blue-300 hover:bg-blue-50"
                  onClick={() => callCustomer(d.customerPhone)}
                  title="Call"
                >
                  <Phone size={16} className="text-blue-600" />
                </Button>
              </div>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
