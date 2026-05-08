import { useEffect, useState } from "react";
import {
  getDailySummaries,
  getTotalOutstanding,
  getAllCustomerBalances,
} from "../../lib/db-operations";
import { DailySummary, CustomerBalance } from "../../types";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  AreaChart,
  Area,
  PieChart,
  Pie,
  Cell,
  RadialBarChart,
  RadialBar,
  ComposedChart,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

interface ReportsProps {
  onBack: () => void;
}

// ─── colour palette ───────────────────────────────────────────────────────────
const CREDIT_COLOR = "#ef4444"; // red
const PAYMENT_COLOR = "#22c55e"; // green
const NET_COLOR = "#3b82f6"; // blue
const ACCENT_COLOR = "#f59e0b"; // amber
const MUTED_COLOR = "#94a3b8"; // slate

// ─── Custom Tooltip ───────────────────────────────────────────────────────────
const RupeeTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-background border rounded-lg shadow-lg p-3 text-sm">
      <p className="font-semibold mb-1 text-foreground">{label}</p>
      {payload.map((p: any) => (
        <p
          key={p.name}
          style={{ color: p.color }}
          className="flex justify-between gap-4"
        >
          <span>{p.name}</span>
          <span className="font-bold">
            ₹
            {Number(p.value).toLocaleString("en-IN", {
              maximumFractionDigits: 0,
            })}
          </span>
        </p>
      ))}
    </div>
  );
};

// ─── KPI Card ─────────────────────────────────────────────────────────────────
interface KPICardProps {
  label: string;
  value: string;
  sub?: string;
  color?: string;
  icon: string;
}
const KPICard = ({
  label,
  value,
  sub,
  color = "text-foreground",
  icon,
}: KPICardProps) => (
  <Card className="flex-1 min-w-0">
    <CardContent className="pt-4 pb-3 px-4">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-xs text-muted-foreground truncate">{label}</p>
          <p className={`text-lg font-bold truncate ${color}`}>{value}</p>
          {sub && <p className="text-xs text-muted-foreground mt-0.5">{sub}</p>}
        </div>
        <span className="text-2xl shrink-0">{icon}</span>
      </div>
    </CardContent>
  </Card>
);

export default function Reports({ onBack }: ReportsProps) {
  const [dailySummaries, setDailySummaries] = useState<DailySummary[]>([]);
  const [customerBalances, setCustomerBalances] = useState<CustomerBalance[]>(
    [],
  );
  const [totalOutstanding, setTotalOutstanding] = useState(0);
  const [totalCollected, setTotalCollected] = useState(0);
  const [totalCreditGiven, setTotalCreditGiven] = useState(0);
  const [days, setDays] = useState(7);

  useEffect(() => {
    loadData();
  }, [days]);

  const loadData = async () => {
    const [summaries, outstanding, balances] = await Promise.all([
      getDailySummaries(days),
      getTotalOutstanding(),
      getAllCustomerBalances(),
    ]);

    setDailySummaries(summaries);
    setTotalOutstanding(outstanding);
    setCustomerBalances(balances);

    const collected = summaries.reduce((s, d) => s + d.totalPayment, 0);
    const creditGiven = summaries.reduce((s, d) => s + d.totalCredit, 0);
    setTotalCollected(collected);
    setTotalCreditGiven(creditGiven);
  };

  // ── derived datasets ────────────────────────────────────────────────────────

  /** Bar + Line composed: credit / payment bars, net line */
  const composedData = dailySummaries.map((s: DailySummary) => ({
    date: new Date(s.date).toLocaleDateString("en-IN", {
      month: "short",
      day: "numeric",
    }),
    Credit: s.totalCredit,
    Payment: s.totalPayment,
    Net: s.totalCredit - s.totalPayment,
  }));

  /** Cumulative area chart */
  let runningCredit = 0,
    runningPayment = 0;
  const areaData = dailySummaries.map((s: DailySummary) => {
    runningCredit += s.totalCredit;
    runningPayment += s.totalPayment;
    return {
      date: new Date(s.date).toLocaleDateString("en-IN", {
        month: "short",
        day: "numeric",
      }),
      Credit: runningCredit,
      Payment: runningPayment,
    };
  });

  /** Pie: outstanding vs collected */
  const pieData = [
    { name: "Outstanding", value: totalOutstanding, color: CREDIT_COLOR },
    { name: "Collected", value: totalCollected, color: PAYMENT_COLOR },
  ].filter((d: { name: string; value: number; color: string }) => d.value > 0);

  /** Top 6 debtors – horizontal bar */
  const topDebtors = customerBalances
    .filter((b: CustomerBalance) => b.balance > 0)
    .slice(0, 6)
    .map((b: CustomerBalance) => ({
      name:
        b.customerName.length > 12
          ? b.customerName.slice(0, 12) + "…"
          : b.customerName,
      Balance: b.balance,
    }))
    .reverse(); // largest at top

  /** Radial: collection efficiency % */
  const totalBusiness = totalOutstanding + totalCollected;
  const collectionRate =
    totalBusiness > 0 ? (totalCollected / totalBusiness) * 100 : 0;
  const radialData = [
    {
      name: "Collected",
      value: Math.round(collectionRate),
      fill: PAYMENT_COLOR,
    },
  ];

  /** Transaction count KPIs */
  const txnCount = dailySummaries.reduce(
    (s: number, d: DailySummary) => s + d.transactionCount,
    0,
  );

  // ── Pie label ────────────────────────────────────────────────────────────────
  const renderPieLabel = ({
    cx,
    cy,
    midAngle,
    innerRadius,
    outerRadius,
    percent,
  }: any) => {
    if (percent < 0.05) return null;
    const RADIAN = Math.PI / 180;
    const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
    const x = cx + radius * Math.cos(-midAngle * RADIAN);
    const y = cy + radius * Math.sin(-midAngle * RADIAN);
    return (
      <text
        x={x}
        y={y}
        fill="white"
        textAnchor="middle"
        dominantBaseline="central"
        fontSize={12}
        fontWeight="bold"
      >
        {`${(percent * 100).toFixed(0)}%`}
      </text>
    );
  };

  return (
    <div className="flex flex-col h-full bg-background">
      {/* ── Header ──────────────────────────────────────────────────────────── */}
      <div className="p-4 border-b sticky top-0 bg-background z-10">
        <div className="flex items-center justify-between mb-3">
          <Button variant="ghost" size="sm" onClick={onBack}>
            ← Back
          </Button>
          <h1 className="text-xl font-bold">📊 Reports</h1>
          <div />
        </div>
        {/* Period Selector */}
        <div className="flex gap-2">
          {[7, 15, 30].map((d) => (
            <Button
              key={d}
              variant={days === d ? "default" : "outline"}
              size="sm"
              onClick={() => setDays(d)}
            >
              {d}d
            </Button>
          ))}
        </div>
      </div>

      <div className="flex-1 overflow-auto p-4 space-y-5 pb-8">
        {/* ── KPI Cards ──────────────────────────────────────────────────────── */}
        <div className="flex gap-3">
          <KPICard
            label="Total Outstanding"
            value={`₹${totalOutstanding.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`}
            color="text-red-500"
            icon="🔴"
          />
          <KPICard
            label="Collected (period)"
            value={`₹${totalCollected.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`}
            color="text-green-600"
            icon="✅"
          />
        </div>
        <div className="flex gap-3">
          <KPICard
            label="Credit Given"
            value={`₹${totalCreditGiven.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`}
            color="text-blue-600"
            icon="📦"
            sub={`Last ${days} days`}
          />
          <KPICard
            label="Transactions"
            value={String(txnCount)}
            color="text-amber-600"
            icon="🧾"
            sub={`Last ${days} days`}
          />
        </div>

        {/* ── 1. Composed Bar + Net Line ──────────────────────────────────────── */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Daily Credit vs Payment</CardTitle>
            <p className="text-xs text-muted-foreground">
              Bars = daily amounts · Line = net position
            </p>
          </CardHeader>
          <CardContent>
            {composedData.length === 0 ? (
              <p className="text-center text-muted-foreground py-10 text-sm">
                No data for this period
              </p>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <ComposedChart
                  data={composedData}
                  margin={{ top: 5, right: 10, left: -10, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                  <YAxis
                    tick={{ fontSize: 11 }}
                    tickFormatter={(v: number) =>
                      `₹${v >= 1000 ? (v / 1000).toFixed(0) + "k" : v}`
                    }
                  />
                  <Tooltip content={<RupeeTooltip />} />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar
                    dataKey="Credit"
                    fill={CREDIT_COLOR}
                    radius={[3, 3, 0, 0]}
                    maxBarSize={28}
                  />
                  <Bar
                    dataKey="Payment"
                    fill={PAYMENT_COLOR}
                    radius={[3, 3, 0, 0]}
                    maxBarSize={28}
                  />
                  <Line
                    type="monotone"
                    dataKey="Net"
                    stroke={NET_COLOR}
                    strokeWidth={2}
                    dot={{ r: 3 }}
                  />
                </ComposedChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        {/* ── 2. Cumulative Area Chart ───────────────────────────────────────── */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">
              Cumulative Sales & Collections
            </CardTitle>
            <p className="text-xs text-muted-foreground">
              Running totals over the period
            </p>
          </CardHeader>
          <CardContent>
            {areaData.length === 0 ? (
              <p className="text-center text-muted-foreground py-10 text-sm">
                No data for this period
              </p>
            ) : (
              <ResponsiveContainer width="100%" height={240}>
                <AreaChart
                  data={areaData}
                  margin={{ top: 5, right: 10, left: -10, bottom: 5 }}
                >
                  <defs>
                    <linearGradient id="gradCredit" x1="0" y1="0" x2="0" y2="1">
                      <stop
                        offset="5%"
                        stopColor={CREDIT_COLOR}
                        stopOpacity={0.25}
                      />
                      <stop
                        offset="95%"
                        stopColor={CREDIT_COLOR}
                        stopOpacity={0.02}
                      />
                    </linearGradient>
                    <linearGradient
                      id="gradPayment"
                      x1="0"
                      y1="0"
                      x2="0"
                      y2="1"
                    >
                      <stop
                        offset="5%"
                        stopColor={PAYMENT_COLOR}
                        stopOpacity={0.3}
                      />
                      <stop
                        offset="95%"
                        stopColor={PAYMENT_COLOR}
                        stopOpacity={0.02}
                      />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                  <YAxis
                    tick={{ fontSize: 11 }}
                    tickFormatter={(v: number) =>
                      `₹${v >= 1000 ? (v / 1000).toFixed(0) + "k" : v}`
                    }
                  />
                  <Tooltip content={<RupeeTooltip />} />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Area
                    type="monotone"
                    dataKey="Credit"
                    stroke={CREDIT_COLOR}
                    fill="url(#gradCredit)"
                    strokeWidth={2}
                    dot={false}
                  />
                  <Area
                    type="monotone"
                    dataKey="Payment"
                    stroke={PAYMENT_COLOR}
                    fill="url(#gradPayment)"
                    strokeWidth={2}
                    dot={false}
                  />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        {/* ── 3. Pie Donut + Radial side-by-side ────────────────────────────── */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">
              Outstanding vs Collected
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              {/* Donut pie */}
              <div className="flex-1">
                {pieData.length === 0 ? (
                  <p className="text-center text-muted-foreground py-6 text-sm">
                    No transactions yet
                  </p>
                ) : (
                  <ResponsiveContainer width="100%" height={200}>
                    <PieChart>
                      <Pie
                        isAnimationActive
                        data={pieData}
                        cx="50%"
                        cy="50%"
                        innerRadius={52}
                        outerRadius={80}
                        paddingAngle={3}
                        dataKey="value"
                        labelLine={false}
                        label={renderPieLabel}
                      >
                        {pieData.map((entry) => (
                          <Cell key={entry.name} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip
                        formatter={(value: number) =>
                          `₹${value.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`
                        }
                      />
                      <Legend wrapperStyle={{ fontSize: 12 }} />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </div>

              {/* Radial collection rate */}
              <div
                className="flex flex-col items-center"
                style={{ width: 110 }}
              >
                <ResponsiveContainer width={110} height={110}>
                  <RadialBarChart
                    cx="50%"
                    cy="50%"
                    innerRadius="60%"
                    outerRadius="90%"
                    startAngle={90}
                    endAngle={-270}
                    data={radialData}
                  >
                    <RadialBar
                      dataKey="value"
                      background={{ fill: "#e2e8f0" }}
                      cornerRadius={8}
                    />
                  </RadialBarChart>
                </ResponsiveContainer>
                <p className="text-2xl font-bold text-green-600 -mt-2">
                  {Math.round(collectionRate)}%
                </p>
                <p className="text-xs text-muted-foreground text-center">
                  Collection Rate
                </p>
              </div>
            </div>

            {/* Amount labels */}
            <div className="grid grid-cols-2 gap-3 mt-3 border-t pt-3">
              <div className="text-center">
                <p className="text-xs text-muted-foreground">Outstanding</p>
                <p className="text-xl font-bold text-red-500">
                  ₹
                  {totalOutstanding.toLocaleString("en-IN", {
                    maximumFractionDigits: 0,
                  })}
                </p>
              </div>
              <div className="text-center">
                <p className="text-xs text-muted-foreground">
                  Collected ({days}d)
                </p>
                <p className="text-xl font-bold text-green-600">
                  ₹
                  {totalCollected.toLocaleString("en-IN", {
                    maximumFractionDigits: 0,
                  })}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* ── 4. Top Debtors Horizontal Bar ─────────────────────────────────── */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Top Debtors</CardTitle>
            <p className="text-xs text-muted-foreground">
              Customers with highest pending balance
            </p>
          </CardHeader>
          <CardContent>
            {topDebtors.length === 0 ? (
              <p className="text-center text-muted-foreground py-10 text-sm">
                🎉 No pending dues!
              </p>
            ) : (
              <ResponsiveContainer
                width="100%"
                height={Math.max(topDebtors.length * 46, 140)}
              >
                <BarChart
                  data={topDebtors}
                  layout="vertical"
                  margin={{ top: 4, right: 16, left: 4, bottom: 4 }}
                >
                  <CartesianGrid
                    strokeDasharray="3 3"
                    horizontal={false}
                    stroke="#e2e8f0"
                  />
                  <XAxis
                    type="number"
                    tick={{ fontSize: 11 }}
                    tickFormatter={(v: number) =>
                      `₹${v >= 1000 ? (v / 1000).toFixed(0) + "k" : v}`
                    }
                  />
                  <YAxis
                    type="category"
                    dataKey="name"
                    tick={{ fontSize: 12 }}
                    width={80}
                  />
                  <Tooltip
                    formatter={(value: number) => [
                      `₹${value.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`,
                      "Balance",
                    ]}
                  />
                  <Bar dataKey="Balance" radius={[0, 4, 4, 0]} maxBarSize={22}>
                    {topDebtors.map(
                      (_: { name: string; Balance: number }, i: number) => (
                        <Cell
                          key={i}
                          fill={`hsl(${0 + i * 18}, 80%, ${55 - i * 2}%)`}
                        />
                      ),
                    )}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        {/* ── 5. Daily Net Position Line ────────────────────────────────────── */}
        {composedData.length > 0 && (
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">Daily Net Position</CardTitle>
              <p className="text-xs text-muted-foreground">
                Credit given minus payment received each day
              </p>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={200}>
                <LineChart
                  data={composedData}
                  margin={{ top: 5, right: 10, left: -10, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                  <YAxis
                    tick={{ fontSize: 11 }}
                    tickFormatter={(v: number) =>
                      `₹${v >= 1000 ? (v / 1000).toFixed(0) + "k" : v}`
                    }
                  />
                  <Tooltip content={<RupeeTooltip />} />
                  <Line
                    type="monotone"
                    dataKey="Net"
                    stroke={NET_COLOR}
                    strokeWidth={2.5}
                    dot={{ r: 4, fill: NET_COLOR }}
                    activeDot={{ r: 6 }}
                  />
                  {/* zero reference */}
                  <Line
                    type="monotone"
                    dataKey={() => 0}
                    stroke={MUTED_COLOR}
                    strokeDasharray="4 4"
                    dot={false}
                    legendType="none"
                  />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        )}

        {/* ── 6. Detailed Summary Table ─────────────────────────────────────── */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">
              Period Summary — Last {days} Days
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {[
                {
                  label: "Total Transactions",
                  value: txnCount,
                  fmt: (v: number) => String(v),
                  color: "",
                },
                {
                  label: "Total Credit Given",
                  value: totalCreditGiven,
                  fmt: (v: number) =>
                    `₹${v.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`,
                  color: "text-red-500",
                },
                {
                  label: "Total Payments Received",
                  value: totalCollected,
                  fmt: (v: number) =>
                    `₹${v.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`,
                  color: "text-green-600",
                },
                {
                  label: "Net Change",
                  value: totalCollected - totalCreditGiven,
                  fmt: (v: number) =>
                    `₹${v.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`,
                  color:
                    totalCollected - totalCreditGiven >= 0
                      ? "text-green-600"
                      : "text-red-500",
                },
                {
                  label: "Avg Daily Credit",
                  value: dailySummaries.length
                    ? totalCreditGiven / dailySummaries.length
                    : 0,
                  fmt: (v: number) =>
                    `₹${v.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`,
                  color: "",
                },
                {
                  label: "Avg Daily Payment",
                  value: dailySummaries.length
                    ? totalCollected / dailySummaries.length
                    : 0,
                  fmt: (v: number) =>
                    `₹${v.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`,
                  color: "",
                },
              ].map(({ label, value, fmt, color }) => (
                <div
                  key={label}
                  className="flex items-center justify-between border-b pb-2 last:border-0 last:pb-0"
                >
                  <span className="text-sm text-muted-foreground">{label}</span>
                  <span className={`text-sm font-semibold ${color}`}>
                    {fmt(value)}
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
