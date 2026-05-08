import { Home, Users, TrendingUp, ScanLine, User } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface BottomNavProps {
  activeScreen: string;
  onNavigate: (screen: string) => void;
}

export default function BottomNav({ activeScreen, onNavigate }: BottomNavProps) {
  const { t } = useTranslation();

  const navItems = [
    { id: 'home', label: t('home'), icon: Home },
    { id: 'customers', label: t('customers'), icon: Users },
    { id: 'reports', label: t('reports'), icon: TrendingUp },
    { id: 'scanner', label: t('scan'), icon: ScanLine },
    { id: 'app-profile', label: t('profile'), icon: User },
  ];

  return (
    <div className="fixed bottom-3 left-3 right-3 z-50">
      <div
        className="flex items-center justify-between gap-1 rounded-[28px] border border-white/10 bg-[#13131F]/90 backdrop-blur-xl px-2 py-2"
        style={{ boxShadow: '0 20px 60px -10px rgba(108,92,231,0.45), 0 0 0 1px rgba(255,255,255,0.04)' }}
      >
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive =
            activeScreen === item.id ||
            (activeScreen === 'ledger' && item.id === 'customers') ||
            (activeScreen === 'profile' && item.id === 'customers');

          return (
            <button
              key={item.id}
              onClick={() => onNavigate(item.id)}
              className={`relative flex items-center justify-center gap-2 h-11 rounded-[22px] transition-all duration-300 ${
                isActive
                  ? 'flex-[2] px-4 text-white'
                  : 'flex-1 px-3 text-white/60 hover:text-white/90 active:scale-95'
              }`}
              style={
                isActive
                  ? {
                      background:
                        'linear-gradient(135deg, #6C5CE7 0%, #FF4D6D 100%)',
                      boxShadow: '0 8px 24px -6px rgba(255,77,109,0.55)',
                    }
                  : undefined
              }
            >
              <Icon size={20} strokeWidth={isActive ? 2.4 : 2} />
              {isActive && (
                <span className="text-xs font-semibold whitespace-nowrap">
                  {item.label}
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
