import { useEffect, useState } from 'react';
import { getTotalOutstanding, getAllTransactions } from '../../lib/db-operations';
import { Transaction } from '../../types';
import {
  ArrowDownLeft,
  ArrowUpRight,
  Bell,
  ScanLine,
  Settings,
  Sparkles,
  TrendingUp,
  Users,
  User as UserIcon,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import LanguageSelector from './LanguageSelector';

interface HomeDashboardProps {
  onNavigate: (screen: string) => void;
  onSelectCustomer?: (id: number) => void;
}

export default function HomeDashboard({ onNavigate }: HomeDashboardProps) {
  const { t } = useTranslation();
  const [totalOutstanding, setTotalOutstanding] = useState(0);
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([]);
  const [todayStats, setTodayStats] = useState({ credit: 0, payment: 0, count: 0 });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    const outstanding = await getTotalOutstanding();
    setTotalOutstanding(outstanding);

    const allTxns = await getAllTransactions();
    setRecentTransactions(allTxns.slice(0, 5));

    const today = new Date().setHours(0, 0, 0, 0);
    const todayTxns = allTxns.filter(
      (tx) => new Date(tx.date).setHours(0, 0, 0, 0) === today
    );
    setTodayStats({
      credit: todayTxns.filter((t) => t.type === 'credit').reduce((s, t) => s + t.amount, 0),
      payment: todayTxns.filter((t) => t.type === 'payment').reduce((s, t) => s + t.amount, 0),
      count: todayTxns.length,
    });
  };

  const greeting = (() => {
    const h = new Date().getHours();
    if (h < 12) return 'Good Morning';
    if (h < 17) return 'Good Afternoon';
    return 'Good Evening';
  })();

  return (
    <div className="min-h-full overflow-y-auto bg-[#0B0B14]">
      {/* Ambient gradient blobs */}
      <div className="relative">
        <div className="pointer-events-none absolute -top-32 -left-24 w-80 h-80 rounded-full bg-[#6C5CE7] opacity-30 blur-[100px]" />
        <div className="pointer-events-none absolute -top-20 right-0 w-72 h-72 rounded-full bg-[#FF4D6D] opacity-25 blur-[100px]" />

        <div className="relative px-5 pt-6 pb-28 space-y-5">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div
                className="w-11 h-11 rounded-full flex items-center justify-center text-white font-bold border border-white/10"
                style={{ background: 'linear-gradient(135deg,#6C5CE7,#FF4D6D)' }}
              >
                V
              </div>
              <div>
                <p className="text-[11px] uppercase tracking-wider text-white/50">
                  {greeting}
                </p>
                <h1 className="text-white font-bold text-lg leading-tight">
                  Vinay <span className="ml-0.5">👋</span>
                </h1>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => onNavigate('app-profile')}
                className="w-10 h-10 rounded-full border border-white/10 bg-white/5 text-white/80 flex items-center justify-center hover:bg-white/10 active:scale-95 transition"
              >
                <Bell size={17} />
              </button>
              <button
                onClick={() => onNavigate('app-profile')}
                className="w-10 h-10 rounded-full border border-white/10 bg-white/5 text-white/80 flex items-center justify-center hover:bg-white/10 active:scale-95 transition"
              >
                <Settings size={17} />
              </button>
              <LanguageSelector />
            </div>
          </div>

          {/* Hero balance card */}
          <div
            className="relative overflow-hidden rounded-[28px] p-6 text-white border border-white/10"
            style={{
              background:
                'linear-gradient(135deg,#7C5CFF 0%,#FF4D6D 55%,#FFB020 100%)',
              boxShadow:
                '0 30px 60px -20px rgba(255,77,109,0.55), 0 10px 30px -10px rgba(108,92,231,0.45)',
            }}
          >
            <div className="absolute -right-10 -top-10 w-40 h-40 rounded-full bg-white/15 blur-2xl" />
            <div className="absolute -left-8 bottom-0 w-32 h-32 rounded-full bg-white/10 blur-xl" />
            <div
              className="absolute inset-0 opacity-40 mix-blend-overlay"
              style={{
                background:
                  'linear-gradient(180deg,rgba(255,255,255,0.35) 0%,rgba(255,255,255,0) 60%)',
              }}
            />
            <div className="relative">
              <div className="flex items-center gap-2 text-white/85 text-xs uppercase tracking-wider">
                <Sparkles size={14} />
                {t('totalOutstanding') || 'Total Outstanding'}
              </div>
              <p className="mt-3 text-5xl font-bold tracking-tight drop-shadow-sm">
                ₹{totalOutstanding.toFixed(2)}
              </p>
              <div className="mt-4 inline-flex items-center gap-1.5 rounded-full bg-white/20 backdrop-blur px-3 py-1 text-xs font-medium">
                <TrendingUp size={12} />
                {todayStats.count} txns today
              </div>
            </div>
          </div>

          {/* Today's stats */}
          <div className="grid grid-cols-2 gap-3">
            <StatCard
              label={t('todayCredit') || "Today's Credit"}
              value={`₹${todayStats.credit}`}
              icon={<ArrowUpRight size={16} />}
              accent="#FF4D6D"
            />
            <StatCard
              label={t('todayPayment') || "Today's Payment"}
              value={`₹${todayStats.payment}`}
              icon={<ArrowDownLeft size={16} />}
              accent="#22C55E"
            />
          </div>

          {/* Quick actions */}
          <div>
            <h2 className="text-white/90 font-semibold mb-3">Quick Actions</h2>
            <div className="grid grid-cols-4 gap-3">
              <QuickAction
                label={t('customers') || 'Customers'}
                icon={<Users size={20} />}
                gradient="linear-gradient(135deg,#6C5CE7,#FF4D6D)"
                onClick={() => onNavigate('customers')}
              />
              <QuickAction
                label={t('reports') || 'Reports'}
                icon={<TrendingUp size={20} />}
                gradient="linear-gradient(135deg,#00D4FF,#6C5CE7)"
                onClick={() => onNavigate('reports')}
              />
              <QuickAction
                label="Scan"
                icon={<ScanLine size={20} />}
                gradient="linear-gradient(135deg,#FFB020,#FF4D6D)"
                onClick={() => onNavigate('scanner')}
              />
              <QuickAction
                label="Profile"
                icon={<UserIcon size={20} />}
                gradient="linear-gradient(135deg,#FF4D6D,#6C5CE7)"
                onClick={() => onNavigate('app-profile')}
              />
            </div>
          </div>

          {/* Recent transactions */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-white/90 font-semibold">
                {t('recentTransactions') || 'Recent Transactions'}
              </h2>
              <button
                onClick={() => onNavigate('customers')}
                className="text-xs text-white/50 hover:text-white/80"
              >
                See all
              </button>
            </div>

            <div className="space-y-2">
              {recentTransactions.length === 0 && (
                <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-6 text-center text-white/50 text-sm">
                  No transactions yet
                </div>
              )}
              {recentTransactions.map((txn) => {
                const isCredit = txn.type === 'credit';
                return (
                  <div
                    key={txn.id}
                    className="flex items-center gap-3 rounded-2xl border border-white/[0.06] bg-white/[0.04] backdrop-blur p-3 hover:bg-white/[0.07] transition"
                  >
                    <div
                      className="w-10 h-10 rounded-full flex items-center justify-center"
                      style={{
                        background: isCredit
                          ? 'linear-gradient(135deg,#FF4D6D33,#FF4D6D11)'
                          : 'linear-gradient(135deg,#22C55E33,#22C55E11)',
                        color: isCredit ? '#FF4D6D' : '#22C55E',
                      }}
                    >
                      {isCredit ? <ArrowUpRight size={18} /> : <ArrowDownLeft size={18} />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-white text-sm font-semibold truncate">
                        {isCredit ? t('creditGiven') || 'Credit Given' : t('paymentReceived') || 'Payment Received'}
                      </p>
                      <p className="text-[11px] text-white/45">
                        {new Date(txn.date).toLocaleDateString('en-IN', {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </p>
                    </div>
                    <p
                      className="font-bold"
                      style={{ color: isCredit ? '#FF4D6D' : '#22C55E' }}
                    >
                      {isCredit ? '+' : '-'}₹{txn.amount}
                    </p>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function StatCard({
  label,
  value,
  icon,
  accent,
}: {
  label: string;
  value: string;
  icon: React.ReactNode;
  accent: string;
}) {
  return (
    <div className="rounded-2xl border border-white/[0.06] bg-white/[0.04] backdrop-blur p-4">
      <div className="flex items-center gap-2 text-white/70 text-xs">
        <span
          className="w-7 h-7 rounded-full flex items-center justify-center"
          style={{ background: `${accent}22`, color: accent }}
        >
          {icon}
        </span>
        {label}
      </div>
      <p className="mt-2 text-2xl font-bold" style={{ color: accent }}>
        {value}
      </p>
    </div>
  );
}

function QuickAction({
  label,
  icon,
  gradient,
  onClick,
}: {
  label: string;
  icon: React.ReactNode;
  gradient: string;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className="flex flex-col items-center gap-2 rounded-2xl border border-white/[0.06] bg-white/[0.04] backdrop-blur p-3 hover:bg-white/[0.07] active:scale-95 transition"
    >
      <span
        className="w-11 h-11 rounded-full flex items-center justify-center text-white"
        style={{ background: gradient, boxShadow: '0 6px 18px -6px rgba(108,92,231,0.6)' }}
      >
        {icon}
      </span>
      <span className="text-[11px] text-white/80 font-medium">{label}</span>
    </button>
  );
}
