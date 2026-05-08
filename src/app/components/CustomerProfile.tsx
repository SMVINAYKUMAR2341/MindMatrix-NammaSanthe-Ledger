import { useEffect, useState } from 'react';
import {
  getCustomer,
  getCustomerTransactions,
  getCustomerBalance,
  getCustomerPhoto,
  saveCustomerPhoto,
  updateCustomer,
  deleteCustomer
} from '../../lib/db-operations';
import { Customer, Transaction } from '../../types';
import { Card, CardContent } from './ui/card';
import { Button } from './ui/button';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';
import { Badge } from './ui/badge';
import { Camera, Edit, Trash2, Phone, Calendar, TrendingUp, TrendingDown } from 'lucide-react';
import { compressImage } from '../../lib/ocr-processor';
import { useTranslation } from 'react-i18next';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from './ui/alert-dialog';

interface CustomerProfileProps {
  customerId: number;
  onBack: () => void;
  onEdit: () => void;
}

export default function CustomerProfile({ customerId, onBack, onEdit }: CustomerProfileProps) {
  const { t } = useTranslation();
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [balance, setBalance] = useState(0);
  const [stats, setStats] = useState({
    totalCredit: 0,
    totalPayment: 0,
    transactionCount: 0
  });

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

    const totalCredit = txns.filter(t => t.type === 'credit').reduce((sum, t) => sum + t.amount, 0);
    const totalPayment = txns.filter(t => t.type === 'payment').reduce((sum, t) => sum + t.amount, 0);

    setStats({
      totalCredit,
      totalPayment,
      transactionCount: txns.length
    });
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

  const handleDelete = async () => {
    await deleteCustomer(customerId);
    onBack();
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
    <div className="flex flex-col h-full bg-gradient-to-b from-primary/5 to-background">
      {/* Header */}
      <div className="bg-gradient-to-br from-orange-500 via-red-500 to-pink-500 text-white p-6 rounded-b-3xl shadow-lg">
        <Button variant="ghost" onClick={onBack} className="text-white hover:bg-white/20 mb-4">
          ← {t('back')}
        </Button>

        <div className="flex items-start gap-4">
          <div className="relative">
            <Avatar className="w-24 h-24 border-4 border-white shadow-xl">
              <AvatarImage src={getCustomerPhoto(customer.photoPath) || undefined} />
              <AvatarFallback className="text-2xl bg-white text-primary">
                {getInitials(customer.name)}
              </AvatarFallback>
            </Avatar>
            <input
              type="file"
              accept="image/*"
              capture="environment"
              className="hidden"
              id="profile-photo-input"
              onChange={handlePhotoCapture}
            />
            <button
              onClick={() => document.getElementById('profile-photo-input')?.click()}
              className="absolute bottom-0 right-0 bg-white text-primary rounded-full p-2 shadow-lg hover:scale-110 transition-transform"
              type="button"
            >
              <Camera size={16} />
            </button>
          </div>

          <div className="flex-1">
            <h1 className="text-2xl font-bold mb-1">{customer.name}</h1>
            {customer.phone && (
              <div className="flex items-center gap-2 text-white/90">
                <Phone size={14} />
                <span>{customer.phone}</span>
              </div>
            )}
            <div className="flex items-center gap-2 mt-2 text-white/80 text-sm">
              <Calendar size={14} />
              <span>Since {new Date(customer.createdAt).toLocaleDateString('en-IN')}</span>
            </div>
          </div>
        </div>

        {/* Balance Card */}
        <Card className="mt-4 bg-white/95 backdrop-blur border-0 shadow-xl">
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-sm text-muted-foreground mb-1">{t('balance')}</p>
              <p className={`text-4xl font-bold ${balance > 0 ? 'text-red-600' : balance < 0 ? 'text-green-600' : 'text-gray-600'}`}>
                ₹{Math.abs(balance).toFixed(2)}
              </p>
              <Badge variant={balance > 0 ? 'destructive' : balance < 0 ? 'default' : 'secondary'} className="mt-2">
                {balance > 0 ? t('pending') : balance < 0 ? t('advance') : t('clear')}
              </Badge>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Stats Section */}
      <div className="p-4 space-y-4">
        <div className="grid grid-cols-3 gap-3">
          <Card className="bg-gradient-to-br from-red-50 to-red-100 border-red-200">
            <CardContent className="p-4 text-center">
              <TrendingUp className="mx-auto text-red-600 mb-2" size={24} />
              <p className="text-2xl font-bold text-red-600">₹{stats.totalCredit.toFixed(0)}</p>
              <p className="text-xs text-red-700 mt-1">{t('totalCreditGiven')}</p>
            </CardContent>
          </Card>

          <Card className="bg-gradient-to-br from-green-50 to-green-100 border-green-200">
            <CardContent className="p-4 text-center">
              <TrendingDown className="mx-auto text-green-600 mb-2" size={24} />
              <p className="text-2xl font-bold text-green-600">₹{stats.totalPayment.toFixed(0)}</p>
              <p className="text-xs text-green-700 mt-1">{t('totalPaymentsReceived')}</p>
            </CardContent>
          </Card>

          <Card className="bg-gradient-to-br from-blue-50 to-blue-100 border-blue-200">
            <CardContent className="p-4 text-center">
              <div className="text-3xl font-bold text-blue-600 mb-2">{stats.transactionCount}</div>
              <p className="text-xs text-blue-700">{t('transactions')}</p>
            </CardContent>
          </Card>
        </div>

        {/* Last Transaction */}
        {transactions.length > 0 && (
          <Card className="border-l-4 border-l-primary">
            <CardContent className="p-4">
              <p className="text-sm font-semibold text-muted-foreground mb-2">{t('lastTransaction')}</p>
              <div className="flex justify-between items-center">
                <div>
                  <p className="font-semibold">
                    {transactions[0].type === 'credit' ? t('creditGiven') : t('paymentReceived')}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {new Date(transactions[0].date).toLocaleDateString('en-IN')}
                  </p>
                </div>
                <p className={`text-2xl font-bold ${transactions[0].type === 'credit' ? 'text-red-600' : 'text-green-600'}`}>
                  {transactions[0].type === 'credit' ? '+' : '-'}₹{transactions[0].amount}
                </p>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Actions */}
        <div className="grid grid-cols-2 gap-3">
          <Button
            onClick={onEdit}
            className="bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700"
          >
            <Edit size={16} className="mr-2" />
            {t('edit')}
          </Button>

          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button variant="destructive">
                <Trash2 size={16} className="mr-2" />
                {t('delete')}
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Delete Customer?</AlertDialogTitle>
                <AlertDialogDescription>
                  This will permanently delete {customer.name} and all their transactions. This action cannot be undone.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>{t('cancel')}</AlertDialogCancel>
                <AlertDialogAction onClick={handleDelete} className="bg-destructive text-destructive-foreground">
                  {t('delete')}
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      </div>
    </div>
  );
}
