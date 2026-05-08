import { useEffect, useState } from 'react';
import { initializeSettings } from '../lib/database';
import { seedDummyData } from '../lib/seed-data';
import { registerServiceWorker, requestNotificationPermission } from '../lib/pwa-install';
import '../lib/i18n';
import HomeDashboard from './components/HomeDashboard';
import CustomerList from './components/CustomerList';
import CustomerLedger from './components/CustomerLedger';
import CustomerProfile from './components/CustomerProfile';
import AppProfile from './components/AppProfile';
import QuickEntry from './components/QuickEntry';
import Reports from './components/Reports';
import OCRScanner from './components/OCRScanner';
import BottomNav from './components/BottomNav';
import { Toaster } from './components/ui/sonner';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState('home');
  const [selectedCustomerId, setSelectedCustomerId] = useState<number | null>(null);
  const [isInitialized, setIsInitialized] = useState(false);
  const [previousScreen, setPreviousScreen] = useState<string>('customers');

  useEffect(() => {
    const init = async () => {
      await initializeSettings();
      await seedDummyData();
      await registerServiceWorker();
      await requestNotificationPermission();
      setIsInitialized(true);
    };
    init();
  }, []);

  const handleNavigate = (screen: string) => {
    setCurrentScreen(screen);
    setSelectedCustomerId(null);
  };

  const handleSelectCustomer = (customerId: number, fromScreen: string = 'customers') => {
    setSelectedCustomerId(customerId);
    setPreviousScreen(fromScreen);
    setCurrentScreen('profile');
  };

  const handleViewLedger = () => {
    setCurrentScreen('ledger');
  };

  if (!isInitialized) {
    return (
      <div className="relative size-full flex items-center justify-center bg-[#0B0B14] overflow-hidden">
        <div className="pointer-events-none absolute -top-32 -left-24 w-96 h-96 rounded-full bg-[#6C5CE7] opacity-30 blur-[100px]" />
        <div className="pointer-events-none absolute -bottom-32 -right-24 w-96 h-96 rounded-full bg-[#FF4D6D] opacity-30 blur-[100px]" />
        <div className="relative text-center">
          <div className="relative w-24 h-24 mx-auto mb-6">
            <div
              className="absolute inset-0 rounded-full animate-pulse"
              style={{ background: 'linear-gradient(135deg,#6C5CE7,#FF4D6D)' }}
            />
            <div className="absolute inset-2 rounded-full bg-[#13131F] border border-white/10 flex items-center justify-center">
              <span className="text-3xl">🛒</span>
            </div>
            <div className="absolute -inset-1 rounded-full border-4 border-transparent border-t-[#6C5CE7] border-r-[#FF4D6D] animate-spin" />
          </div>
          <h1
            className="text-2xl font-bold mb-1 bg-clip-text text-transparent"
            style={{ backgroundImage: 'linear-gradient(135deg,#FFFFFF,#FF4D6D)' }}
          >
            Namma Santhe Ledger
          </h1>
          <p className="text-sm text-white/55">Setting up your khata…</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen flex flex-col bg-[#0B0B14]">
      <div className="flex-1 overflow-hidden pb-20">
        {currentScreen === 'home' && (
          <HomeDashboard
            onNavigate={handleNavigate}
            onSelectCustomer={(id) => handleSelectCustomer(id, 'home')}
          />
        )}
        {currentScreen === 'customers' && (
          <CustomerList
            onSelectCustomer={(id) => handleSelectCustomer(id, 'customers')}
            onBack={() => handleNavigate('home')}
          />
        )}
        {currentScreen === 'profile' && selectedCustomerId && (
          <CustomerProfile
            customerId={selectedCustomerId}
            onBack={() => handleNavigate(previousScreen)}
            onEdit={handleViewLedger}
          />
        )}
        {currentScreen === 'ledger' && selectedCustomerId && (
          <CustomerLedger
            customerId={selectedCustomerId}
            onBack={() => handleNavigate('profile')}
          />
        )}
        {currentScreen === 'quick-entry' && (
          <QuickEntry onBack={() => handleNavigate('home')} />
        )}
        {currentScreen === 'reports' && <Reports onBack={() => handleNavigate('home')} />}
        {currentScreen === 'scanner' && <OCRScanner onBack={() => handleNavigate('home')} />}
        {currentScreen === 'app-profile' && <AppProfile onBack={() => handleNavigate('home')} />}
      </div>

      <BottomNav activeScreen={currentScreen} onNavigate={handleNavigate} />
      <Toaster />
    </div>
  );
}