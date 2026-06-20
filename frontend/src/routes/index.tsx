import { createFileRoute, Link } from "@tanstack/react-router";
import { useApp } from "@/lib/store";
import { StatusBadge } from "@/components/StatusBadge";
import { formatETB, TAXPAYER, type FilingPeriod } from "@/lib/mock-data";
import { ArrowRight, FilePlus2, AlertTriangle, Clock, CheckCircle2, Calendar, TrendingUp, Building2, User2, MapPin, Download } from "lucide-react";
import { Card, CardHeader } from "@/components/form-primitives";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";

export const Route = createFileRoute("/")({
  component: TaxpayerDashboard,
});

function TaxpayerDashboard() {
  const returns = useApp((s) => s.returns);
  const filingPeriods = useApp((s) => s.filingPeriods);
  const myReturns = returns.filter((r) => r.tin === TAXPAYER.tin);
  const myPeriods = filingPeriods.filter((p) => p.tin === TAXPAYER.tin);

  const active = myReturns.filter((r) => r.status !== "COMPLETED").length;
  const completed = myReturns.filter((r) => r.status === "COMPLETED").length;
  const review = myReturns.filter((r) => r.status === "MANUAL_REVIEW").length;
  const pendingPayment = myReturns
    .filter((r) => ["ACCEPTED", "POSTED_TO_LEDGER", "UNDER_VALIDATION", "MANUAL_REVIEW"].includes(r.status))
    .reduce((sum, r) => sum + (r.netTax || 0), 0);

  const actionRequired = myPeriods.filter((p) => ["OPEN", "OVERDUE"].includes(p.status) && !p.taxReturnId);
  const overdue = myPeriods.filter((p) => p.status === "OVERDUE");

  // Mock data for the chart
  const chartData = [
    { name: "Jan", tax: 4000 },
    { name: "Feb", tax: 3000 },
    { name: "Mar", tax: 2000 },
    { name: "Apr", tax: 2780 },
    { name: "May", tax: 1890 },
    { name: "Jun", tax: 2390 },
  ];

  return (
    <div className="space-y-6 lg:space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-foreground">Dashboard Overview</h1>
          <p className="text-sm text-muted-foreground mt-1">Welcome back, here is your tax account summary.</p>
        </div>
        <div className="flex gap-3">
          <Link to="/returns/new" className="btn-primary shadow-lg shadow-accent/20">
            <FilePlus2 className="h-4 w-4" /> 
            <span className="hidden sm:inline">File New Return</span>
            <span className="sm:hidden">New Return</span>
          </Link>
        </div>
      </div>

      {overdue.length > 0 && (
        <div className="bg-destructive/10 border border-destructive/20 rounded-xl p-4 sm:p-5 flex gap-4 animate-in slide-in-from-top-4">
          <div className="h-10 w-10 rounded-full bg-destructive/20 flex items-center justify-center shrink-0">
            <AlertTriangle className="h-5 w-5 text-destructive" />
          </div>
          <div className="text-sm pt-0.5">
            <div className="font-semibold text-destructive text-base mb-1">{overdue.length} Action{overdue.length > 1 ? "s" : ""} Required</div>
            <div className="text-destructive/90">
              {overdue.map((p) => `${p.taxTypeLabel} ${p.periodLabel}`).join(", ")} — file immediately to avoid penalties.{" "}
              <Link to="/obligations" className="font-semibold underline underline-offset-2 ml-1">View obligations</Link>
            </div>
          </div>
        </div>
      )}

      {/* Top Grid: Profile & Stats */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Profile Card */}
        <div className="lg:col-span-4 glass-card rounded-2xl p-6 relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl -mr-10 -mt-10 transition-transform group-hover:scale-110" />
          <div className="flex items-start justify-between mb-6 relative">
            <div className="h-12 w-12 rounded-xl bg-accent/10 text-accent flex items-center justify-center">
              <Building2 className="h-6 w-6" />
            </div>
            <span className="text-[10px] font-bold uppercase tracking-widest text-accent bg-accent/10 px-2 py-1 rounded">Active</span>
          </div>
          <h2 className="text-xl font-bold text-foreground mb-1">{TAXPAYER.party}</h2>
          <p className="text-sm text-muted-foreground flex items-center gap-2 mb-6">
            <User2 className="h-4 w-4" /> TIN: <span className="mono font-medium">{TAXPAYER.tin}</span>
          </p>
          <div className="space-y-3 pt-4 border-t border-border">
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Tax Center</span>
              <span className="font-medium text-foreground">LTO Addis Ababa</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Entity Type</span>
              <span className="font-medium text-foreground">PLC</span>
            </div>
          </div>
        </div>

        {/* Stats Grid */}
        <div className="lg:col-span-8 grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-6">
          <StatCard 
            label="Pending Payments" 
            value={formatETB(pendingPayment)} 
            sub="Total amount due" 
            tone="warning" 
            trend="+2.5%"
          />
          <StatCard 
            label="Active Filings" 
            value={String(active)} 
            sub={`${review} under manual review`} 
            trend="Stable"
          />
          <StatCard 
            label="Completed This Year" 
            value={String(completed)} 
            sub="Successfully processed" 
            trend="+12%"
            trendUp
          />
          <StatCard 
            label="Next Deadline" 
            value="15 May" 
            sub="VAT Monthly Return" 
            tone={overdue.length > 0 ? "warning" : "default"}
          />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Column */}
        <div className="lg:col-span-2 space-y-6">
          {/* Chart Section */}
          <div className="glass-card border border-border rounded-2xl p-6">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-lg font-bold">Filing Trends</h2>
                <p className="text-xs text-muted-foreground mt-1">Tax liability over the last 6 months</p>
              </div>
              <button className="btn-ghost text-xs py-1.5 px-3 h-auto"><Download className="h-3 w-3 mr-1.5"/> Report</button>
            </div>
            <div className="h-[250px] w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="currentColor" className="text-border/50" />
                  <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 12 }} stroke="currentColor" className="text-muted-foreground" />
                  <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12 }} stroke="currentColor" className="text-muted-foreground" tickFormatter={(v) => `${v/1000}k`} />
                  <Tooltip 
                    cursor={{ fill: 'currentColor', className: 'text-muted/50' }}
                    contentStyle={{ borderRadius: '8px', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-card)', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                  />
                  <Bar dataKey="tax" fill="currentColor" className="text-accent" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Recent Returns */}
          <div className="glass-card border border-border rounded-2xl overflow-hidden">
            <div className="px-6 py-5 border-b border-border flex items-center justify-between bg-muted/10">
              <div>
                <h2 className="font-bold text-lg">Recent Tax Returns</h2>
              </div>
              <Link to="/returns" className="text-sm font-medium text-accent hover:text-primary transition-colors flex items-center gap-1">
                View all <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-muted/30 text-xs text-muted-foreground uppercase tracking-wider font-semibold">
                  <tr>
                    <th className="px-6 py-4 text-left">Period</th>
                    <th className="px-6 py-4 text-left">Tax Type</th>
                    <th className="px-6 py-4 text-left">Status</th>
                    <th className="px-6 py-4 text-right">Net Tax</th>
                    <th className="px-6 py-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {myReturns.slice(0, 5).map((r) => (
                    <tr key={r.id} className="hover:bg-muted/20 transition-colors">
                      <td className="px-6 py-4 font-medium">{r.period}</td>
                      <td className="px-6 py-4">
                        <span className="bg-secondary text-secondary-foreground px-2 py-1 rounded text-xs font-semibold">{r.taxType}</span>
                      </td>
                      <td className="px-6 py-4"><StatusBadge status={r.status} /></td>
                      <td className="px-6 py-4 text-right mono font-medium">{formatETB(r.netTax)}</td>
                      <td className="px-6 py-4 text-right">
                        <Link to="/returns/$id" params={{ id: r.id }} className="text-accent font-medium hover:text-primary hover:underline text-sm transition-colors">
                          {r.status === "DRAFT" || r.status === "AMENDMENT_DRAFT" ? "Continue" : "View"}
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Sidebar Column */}
        <div className="space-y-6">
          {/* Filing Obligations Summary */}
          <div className="glass-card border border-border rounded-2xl overflow-hidden">
            <div className="px-5 py-4 border-b border-border bg-muted/10">
              <h2 className="font-bold text-lg flex items-center gap-2"><Calendar className="h-5 w-5 text-accent" /> Upcoming Obligations</h2>
            </div>
            <div className="divide-y divide-border">
              {myPeriods
                .filter((p) => ["OPEN", "DUE", "OVERDUE"].includes(p.status))
                .slice(0, 4)
                .map((p) => (
                  <PeriodRow key={p.id} period={p} returns={myReturns} />
                ))}
              {actionRequired.length === 0 && myPeriods.filter(p => ["OPEN","DUE","OVERDUE"].includes(p.status)).length === 0 && (
                <div className="p-8 text-center flex flex-col items-center justify-center">
                  <div className="h-12 w-12 rounded-full bg-success/10 text-success flex items-center justify-center mb-3">
                    <CheckCircle2 className="h-6 w-6" />
                  </div>
                  <p className="font-medium">All Caught Up!</p>
                  <p className="text-xs text-muted-foreground mt-1">No pending obligations right now.</p>
                </div>
              )}
            </div>
            <div className="p-3 border-t border-border bg-muted/10 text-center">
              <Link to="/obligations" className="text-xs font-semibold text-accent hover:text-primary transition-colors">
                View Full Calendar
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function PeriodRow({ period, returns }: { period: FilingPeriod; returns: ReturnType<typeof useApp.getState>["returns"] }) {
  const linkedReturn = period.taxReturnId ? returns.find((r) => r.id === period.taxReturnId) : null;

  const statusStyleMap: Record<string, string> = {
    FUTURE: "bg-muted text-muted-foreground",
    OPEN: "bg-info/15 text-info",
    DUE: "bg-warning/15 text-warning",
    OVERDUE: "bg-destructive/15 text-destructive",
    FILED: "bg-success/15 text-success",
  };
  const statusStyle = statusStyleMap[period.status] ?? "bg-muted text-muted-foreground";

  const daysUntilDue = getDaysUntilDue(period.dueDate);
  const dueText =
    period.status === "OVERDUE"
      ? `${Math.abs(daysUntilDue)} days overdue`
      : period.status === "DUE"
      ? `${daysUntilDue} days left`
      : period.status === "OPEN"
      ? `${daysUntilDue} days left`
      : period.status === "FUTURE"
      ? `Opens ${period.coversFrom}`
      : "";

  return (
    <div className="p-5 flex flex-col gap-3 hover:bg-muted/10 transition-colors group">
      <div className="flex items-start justify-between">
        <span className={`text-[10px] uppercase font-bold px-2 py-1 rounded-md shrink-0 ${statusStyle}`}>
          {period.status}
        </span>
        {linkedReturn ? (
          <Link to="/returns/$id" params={{ id: linkedReturn.id }} className="text-xs font-semibold text-accent hover:text-primary transition-colors opacity-0 group-hover:opacity-100">
            {linkedReturn.status === "DRAFT" ? "Resume" : "View"}
          </Link>
        ) : (
          <Link
            to="/returns/new"
            search={{ filingPeriodId: period.id }}
            className={`text-[10px] font-bold uppercase tracking-wider px-2 py-1 rounded transition-colors ${
              period.status === "OVERDUE"
                ? "bg-destructive/10 text-destructive hover:bg-destructive hover:text-destructive-foreground"
                : "bg-accent/10 text-accent hover:bg-accent hover:text-accent-foreground"
            }`}
          >
            File Now
          </Link>
        )}
      </div>
      <div>
        <div className="text-sm font-bold text-foreground">
          {period.taxTypeLabel} — {period.periodLabel}
        </div>
        <div className="text-xs text-muted-foreground flex items-center gap-1.5 mt-1">
          <Clock className="h-3 w-3" />
          {dueText}
        </div>
      </div>
    </div>
  );
}

function getDaysUntilDue(dueDate: string) {
  const today = new Date();
  const due = new Date(`${dueDate}T00:00:00`);
  const diff = due.getTime() - today.getTime();
  return Math.ceil(diff / 86_400_000);
}

function StatCard({ label, value, sub, tone = "default", trend, trendUp }: { label: string; value: string; sub: string; tone?: "default" | "warning"; trend?: string; trendUp?: boolean }) {
  return (
    <div className="glass-card rounded-2xl p-6 relative overflow-hidden group hover:border-accent/30 transition-colors">
      {tone === "warning" && (
        <div className="absolute top-0 right-0 w-16 h-16 bg-warning/10 rounded-full blur-2xl -mr-4 -mt-4" />
      )}
      <div className="text-xs uppercase tracking-widest text-muted-foreground font-semibold mb-3">{label}</div>
      <div className="flex items-end justify-between">
        <div className={`text-3xl font-black tracking-tight ${tone === "warning" ? "text-warning" : "text-foreground"}`}>{value}</div>
        {trend && (
          <div className={`text-xs font-semibold flex items-center gap-0.5 ${trendUp ? "text-success" : tone === "warning" ? "text-warning" : "text-muted-foreground"}`}>
            {trendUp && <TrendingUp className="h-3 w-3" />}
            {trend}
          </div>
        )}
      </div>
      <div className="text-xs text-muted-foreground mt-2 font-medium">{sub}</div>
    </div>
  );
}
