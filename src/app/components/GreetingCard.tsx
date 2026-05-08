import { useEffect, useState } from 'react';
import { Sun, Moon, Sunrise, Sunset, Flame } from 'lucide-react';

export default function GreetingCard() {
  const [businessName, setBusinessName] = useState('Vendor');
  const [streak, setStreak] = useState(0);

  useEffect(() => {
    try {
      const profile = JSON.parse(localStorage.getItem('appProfile') || '{}');
      if (profile.businessName) setBusinessName(profile.businessName);
      else if (profile.ownerName) setBusinessName(profile.ownerName);
    } catch {}

    const today = new Date().toDateString();
    const last = localStorage.getItem('lastVisitDate');
    let s = parseInt(localStorage.getItem('visitStreak') || '0', 10);
    if (last !== today) {
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      s = last === yesterday.toDateString() ? s + 1 : 1;
      localStorage.setItem('visitStreak', String(s));
      localStorage.setItem('lastVisitDate', today);
    }
    setStreak(s);
  }, []);

  const hour = new Date().getHours();
  let greeting = 'Good Morning';
  let Icon = Sunrise;
  let gradient = 'from-amber-400 via-orange-400 to-pink-400';
  if (hour >= 12 && hour < 17) {
    greeting = 'Good Afternoon';
    Icon = Sun;
    gradient = 'from-yellow-400 via-orange-500 to-red-500';
  } else if (hour >= 17 && hour < 20) {
    greeting = 'Good Evening';
    Icon = Sunset;
    gradient = 'from-orange-500 via-pink-500 to-purple-500';
  } else if (hour >= 20 || hour < 5) {
    greeting = 'Good Night';
    Icon = Moon;
    gradient = 'from-indigo-500 via-purple-600 to-pink-600';
  }

  return (
    <div className={`relative overflow-hidden rounded-2xl bg-gradient-to-br ${gradient} text-white p-4 shadow-lg`}>
      <div className="absolute -top-6 -right-6 w-24 h-24 bg-white/20 rounded-full blur-xl" />
      <div className="absolute bottom-0 left-1/2 w-32 h-32 bg-white/10 rounded-full blur-2xl" />
      <div className="relative flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2 text-white/90 text-sm">
            <Icon size={18} />
            <span>{greeting}</span>
          </div>
          <h2 className="text-xl font-bold mt-1 drop-shadow">{businessName} Ji 🙏</h2>
          <p className="text-xs text-white/80 mt-0.5">
            {new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' })}
          </p>
        </div>
        {streak > 0 && (
          <div className="flex flex-col items-center bg-white/20 backdrop-blur rounded-xl px-3 py-2 border border-white/30">
            <Flame className="text-yellow-200" size={20} />
            <span className="font-bold text-lg leading-none">{streak}</span>
            <span className="text-[10px] text-white/80 leading-none mt-0.5">day streak</span>
          </div>
        )}
      </div>
    </div>
  );
}
