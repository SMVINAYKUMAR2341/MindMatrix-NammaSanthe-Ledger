import { useEffect, useState } from 'react';
import { getAllTransactions } from '../../lib/db-operations';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { TrendingUp, TrendingDown } from 'lucide-react';

interface DayBucket {
  label: string;
  credit: number;
  payment: number;
}

export default function WeeklyTrend() {
  const [days, setDays] = useState<DayBucket[]>([]);
  const [weekTotal, setWeekTotal] = useState({ credit: 0, payment: 0 });

  useEffect(() => {
    (async () => {
      const txns = await getAllTransactions();
      const buckets: DayBucket[] = [];
      const now = new Date();
      for (let i = 6; i >= 0; i--) {
        const d = new Date(now);
        d.setDate(d.getDate() - i);
        d.setHours(0, 0, 0, 0);
        const next = d.getTime() + 86400000;
        const dayTxns = txns.filter((t) => t.date >= d.getTime() && t.date < next);
        buckets.push({
          label: d.toLocaleDateString('en-IN', { weekday: 'short' }).charAt(0),
          credit: dayTxns.filter((t) => t.type === 'credit').reduce((s, t) => s + t.amount, 0),
          payment: dayTxns.filter((t) => t.type === 'payment').reduce((s, t) => s + t.amount, 0),
        });
      }
      setDays(buckets);
      setWeekTotal({
        credit: buckets.reduce((s, b) => s + b.credit, 0),
        payment: buckets.reduce((s, b) => s + b.payment, 0),
      });
    })();
  }, []);

  const max = Math.max(1, ...days.flatMap((d) => [d.credit, d.payment]));

  return (
    <Card className="shadow-md border-l-4 border-l-indigo-500 bg-gradient-to-br from-indigo-50 via-white to-purple-50">
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center justify-between">
          <span className="bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent font-bold">
            7-Day Trend
          </span>
          <div className="flex gap-3 text-xs">
            <span className="flex items-center gap-1 text-red-600">
              <TrendingUp size={14} />₹{weekTotal.credit.toFixed(0)}
            </span>
            <span className="flex items-center gap-1 text-green-600">
              <TrendingDown size={14} />₹{weekTotal.payment.toFixed(0)}
            </span>
          </div>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-end justify-between gap-2 h-28">
          {days.map((d, i) => (
            <div key={i} className="flex-1 flex flex-col items-center gap-1">
              <div className="w-full flex items-end justify-center gap-0.5 h-24">
                <div
                  className="w-2 bg-gradient-to-t from-red-400 to-red-500 rounded-t transition-all hover:from-red-500 hover:to-red-600"
                  style={{ height: `${(d.credit / max) * 100}%`, minHeight: d.credit > 0 ? '4px' : '0' }}
                  title={`Credit: ₹${d.credit}`}
                />
                <div
                  className="w-2 bg-gradient-to-t from-green-400 to-green-500 rounded-t transition-all hover:from-green-500 hover:to-green-600"
                  style={{ height: `${(d.payment / max) * 100}%`, minHeight: d.payment > 0 ? '4px' : '0' }}
                  title={`Payment: ₹${d.payment}`}
                />
              </div>
              <span className="text-xs text-muted-foreground font-semibold">{d.label}</span>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
