import { useEffect, useState } from 'react';
import { getAllCustomerBalances, getCustomerPhoto, addCustomer } from '../../lib/db-operations';
import { CustomerBalance } from '../../types';
import { Input } from './ui/input';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';
import { Search, UserPlus, ChevronRight, ArrowLeft, X, Phone, MessageCircle, Bell } from 'lucide-react';
import { Label } from './ui/label';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';

interface CustomerListProps {
  onSelectCustomer: (customerId: number) => void;
  onBack: () => void;
}

type Filter = 'all' | 'pending' | 'advance' | 'clear';

export default function CustomerList({ onSelectCustomer, onBack }: CustomerListProps) {
  const { t } = useTranslation();
  const [customers, setCustomers] = useState<CustomerBalance[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [filter, setFilter] = useState<Filter>('all');
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [newCustomer, setNewCustomer] = useState({ name: '', phone: '' });

  useEffect(() => {
    loadCustomers();
  }, []);

  const loadCustomers = async () => {
    setCustomers(await getAllCustomerBalances());
  };

  const filtered = customers
    .filter((c) => {
      if (!searchQuery) return true;
      const q = searchQuery.toLowerCase();
      return c.customerName.toLowerCase().includes(q) || c.customerPhone.includes(q);
    })
    .filter((c) => {
      if (filter === 'pending') return c.balance > 0;
      if (filter === 'advance') return c.balance < 0;
      if (filter === 'clear') return c.balance === 0;
      return true;
    });

  const handleAddCustomer = async () => {
    if (!newCustomer.name.trim()) return;
    await addCustomer({
      name: newCustomer.name.trim(),
      phone: newCustomer.phone.trim(),
    });
    toast.success(t('customerAdded'));
    setNewCustomer({ name: '', phone: '' });
    setShowAddDialog(false);
    loadCustomers();
  };

  const getInitials = (name: string) =>
    name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);

  const pendingCount = customers.filter((c) => c.balance > 0).length;

  const cleanPhone = (p: string) => p.replace(/[^\d+]/g, '');

  const handleCall = (e: React.MouseEvent, phone: string) => {
    e.stopPropagation();
    if (!phone) return toast.error('No phone number');
    window.location.href = `tel:${cleanPhone(phone)}`;
  };

  const handleWhatsApp = (e: React.MouseEvent, name: string, phone: string, balance: number) => {
    e.stopPropagation();
    if (!phone) return toast.error('No phone number');
    const num = cleanPhone(phone).replace(/^\+/, '');
    const msg =
      balance > 0
        ? `Hi ${name}, this is a friendly reminder — your pending balance is ₹${balance.toFixed(0)}. Please settle at your convenience. Thank you! 🙏`
        : `Hi ${name}, hope you're doing well!`;
    window.open(
      `https://wa.me/${num}?text=${encodeURIComponent(msg)}`,
      '_blank',
      'noopener'
    );
  };

  const handleRemind = (e: React.MouseEvent, name: string, phone: string, balance: number) => {
    e.stopPropagation();
    if (balance <= 0) return toast.info('No pending dues');
    handleWhatsApp(e, name, phone, balance);
    toast.success(`Reminder sent to ${name}`);
  };

  const filterChips: { id: Filter; label: string }[] = [
    { id: 'all', label: 'All' },
    { id: 'pending', label: 'Pending' },
    { id: 'advance', label: 'Advance' },
    { id: 'clear', label: 'Clear' },
  ];

  return (
    <div className="relative flex flex-col h-full bg-[#0B0B14] overflow-hidden">
      {/* ambient glow */}
      <div className="pointer-events-none absolute -top-40 left-1/2 -translate-x-1/2 w-96 h-96 rounded-full bg-[#6C5CE7] opacity-20 blur-[100px]" />

      {/* Header */}
      <div className="relative px-5 pt-6 pb-3">
        <div className="flex items-center justify-between mb-4">
          <button
            onClick={onBack}
            className="w-10 h-10 rounded-full border border-white/10 bg-white/5 text-white flex items-center justify-center hover:bg-white/10 active:scale-95 transition"
          >
            <ArrowLeft size={18} />
          </button>
          <h1 className="text-white font-bold text-xl">{t('customers')}</h1>
          <div className="w-10 h-10" />
        </div>
        <p className="text-sm text-white/55">
          {customers.length} contacts • {pendingCount} pending
        </p>

        {/* Search */}
        <div className="mt-4 flex items-center gap-2 rounded-2xl border border-white/10 bg-white/[0.05] backdrop-blur px-3 py-2">
          <Search size={18} className="text-white/40" />
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={t('searchCustomers') || 'Search by name or phone…'}
            className="border-0 bg-transparent text-white placeholder:text-white/40 focus-visible:ring-0 px-1 h-8"
          />
        </div>

        {/* Filter chips */}
        <div className="mt-3 flex gap-2 overflow-x-auto no-scrollbar">
          {filterChips.map((chip) => {
            const active = filter === chip.id;
            return (
              <button
                key={chip.id}
                onClick={() => setFilter(chip.id)}
                className={`shrink-0 rounded-full border px-4 py-1.5 text-xs font-semibold transition ${
                  active
                    ? 'text-white border-transparent'
                    : 'text-white/65 border-white/10 bg-white/[0.04] hover:bg-white/[0.08]'
                }`}
                style={
                  active
                    ? {
                        background: 'linear-gradient(135deg,#6C5CE7,#FF4D6D)',
                        boxShadow: '0 6px 18px -6px rgba(255,77,109,0.55)',
                      }
                    : undefined
                }
              >
                {chip.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* List */}
      <div className="relative flex-1 overflow-auto px-5 pt-2 pb-32 space-y-2.5">
        {filtered.length === 0 ? (
          <div className="text-center py-16 text-white/45 text-sm">
            {searchQuery ? t('noCustomersFound') : t('noCustomers')}
          </div>
        ) : (
          filtered.map((customer) => {
            const status =
              customer.balance > 0 ? 'pending' : customer.balance < 0 ? 'advance' : 'clear';
            const accent =
              status === 'pending' ? '#FF4D6D' : status === 'advance' ? '#22C55E' : '#6E7280';
            return (
              <div
                key={customer.customerId}
                onClick={() => onSelectCustomer(customer.customerId)}
                className="group relative w-full overflow-hidden rounded-2xl border border-white/[0.06] bg-white/[0.04] backdrop-blur p-3 pl-5 cursor-pointer hover:bg-white/[0.08] active:scale-[0.99] transition"
              >
                {/* status bar */}
                <span
                  className="absolute left-0 top-3 bottom-3 w-1 rounded-full"
                  style={{ background: accent, boxShadow: `0 0 12px ${accent}99` }}
                />
                <div className="flex items-center gap-3">
                  <Avatar className="w-12 h-12 border border-white/15">
                    <AvatarImage src={getCustomerPhoto(customer.customerPhoto) || undefined} />
                    <AvatarFallback
                      className="text-white font-bold border-0"
                      style={{ background: 'linear-gradient(135deg,#6C5CE7,#FF4D6D)' }}
                    >
                      {getInitials(customer.customerName)}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1 min-w-0">
                    <p className="text-white font-semibold truncate">{customer.customerName}</p>
                    {customer.customerPhone && (
                      <p className="text-xs text-white/50 truncate">{customer.customerPhone}</p>
                    )}
                  </div>
                  <div className="flex flex-col items-end shrink-0">
                    <span
                      className="rounded-full px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wide"
                      style={{ background: `${accent}22`, color: accent }}
                    >
                      {status === 'pending' ? t('pending') : status === 'advance' ? t('advance') : t('clear')}
                    </span>
                    <p className="mt-1 font-bold" style={{ color: accent }}>
                      ₹{Math.abs(customer.balance).toFixed(0)}
                    </p>
                  </div>
                  <ChevronRight size={18} className="text-white/30 ml-1 shrink-0" />
                </div>

                {/* Action row */}
                {customer.customerPhone && (
                  <div className="mt-3 flex items-center gap-2 pl-0">
                    <button
                      onClick={(e) => handleCall(e, customer.customerPhone)}
                      className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-xl border border-white/10 bg-white/[0.04] py-2 text-xs font-semibold text-white/85 hover:bg-white/[0.08] active:scale-95 transition"
                    >
                      <Phone size={14} className="text-[#00D4FF]" />
                      Call
                    </button>
                    <button
                      onClick={(e) =>
                        handleWhatsApp(e, customer.customerName, customer.customerPhone, customer.balance)
                      }
                      className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-xl border border-white/10 bg-white/[0.04] py-2 text-xs font-semibold text-white/85 hover:bg-white/[0.08] active:scale-95 transition"
                    >
                      <MessageCircle size={14} className="text-[#22C55E]" />
                      WhatsApp
                    </button>
                    {status === 'pending' && (
                      <button
                        onClick={(e) =>
                          handleRemind(e, customer.customerName, customer.customerPhone, customer.balance)
                        }
                        className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-xl py-2 text-xs font-semibold text-white active:scale-95 transition"
                        style={{
                          background: 'linear-gradient(135deg,#6C5CE7,#FF4D6D)',
                          boxShadow: '0 8px 20px -8px rgba(255,77,109,0.55)',
                        }}
                      >
                        <Bell size={14} />
                        Remind
                      </button>
                    )}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* Floating Add */}
      <button
        onClick={() => setShowAddDialog(true)}
        className="absolute right-5 bottom-24 z-30 inline-flex items-center gap-2 rounded-full px-5 py-3 text-white font-semibold active:scale-95 transition"
        style={{
          background: 'linear-gradient(135deg,#6C5CE7,#FF4D6D)',
          boxShadow: '0 18px 40px -10px rgba(255,77,109,0.6)',
        }}
      >
        <UserPlus size={18} />
        {t('add')}
      </button>

      {/* Add dialog */}
      {showAddDialog && (
        <div className="absolute inset-0 z-40 flex items-end sm:items-center justify-center bg-black/60 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-t-3xl sm:rounded-3xl border border-white/10 bg-[#13131F] p-6 m-0 sm:m-4">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-white font-bold text-lg">{t('addCustomer')}</h3>
              <button
                onClick={() => setShowAddDialog(false)}
                className="w-9 h-9 rounded-full border border-white/10 bg-white/5 text-white/70 flex items-center justify-center hover:bg-white/10"
              >
                <X size={16} />
              </button>
            </div>
            <div className="space-y-3">
              <div>
                <Label className="text-white/70 text-xs">{t('customerName')} *</Label>
                <Input
                  value={newCustomer.name}
                  onChange={(e) => setNewCustomer({ ...newCustomer, name: e.target.value })}
                  placeholder={t('customerName')}
                  className="mt-1 bg-white/[0.04] border-white/10 text-white placeholder:text-white/40"
                />
              </div>
              <div>
                <Label className="text-white/70 text-xs">{t('phone')}</Label>
                <Input
                  value={newCustomer.phone}
                  onChange={(e) => setNewCustomer({ ...newCustomer, phone: e.target.value })}
                  placeholder={t('phone')}
                  type="tel"
                  className="mt-1 bg-white/[0.04] border-white/10 text-white placeholder:text-white/40"
                />
              </div>
              <button
                onClick={handleAddCustomer}
                className="w-full mt-2 py-3 rounded-2xl text-white font-semibold active:scale-[0.98] transition"
                style={{
                  background: 'linear-gradient(135deg,#6C5CE7,#FF4D6D)',
                  boxShadow: '0 14px 30px -10px rgba(255,77,109,0.55)',
                }}
              >
                {t('addCustomer')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
