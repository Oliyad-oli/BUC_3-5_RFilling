import { createFileRoute, Link } from "@tanstack/react-router";
import { useApp } from "@/lib/store";
import { Clock, CheckCircle2, AlertTriangle, AlertCircle, TrendingUp, BarChart2, Briefcase, FileText } from "lucide-react";
import { LineChart, Line, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from "recharts";

export const Route = createFileRoute("/officer/")({
  component: OfficerDashboard,
});

const processingTrend = [
  { name: 'Mon', reviewed: 12, approved: 10, rejected: 2 },
  { name: 'Tue', reviewed: 19, approved: 15, rejected: 4 },
  { name: 'Wed', reviewed: 15, approved: 12, rejected: 3 },
  { name: 'Thu', reviewed: 22, approved: 18, rejected: 4 },
  { name: 'Fri', reviewed: 28, approved: 25, rejected: 3 },
];

const taxTypeData = [
  { name: 'VAT', value: 45 },
  { name: 'Income Tax', value: 30 },
  { name: 'Excise Tax', value: 15 },
  { name: 'Withholding', value: 10 },
];

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'];

function OfficerDashboard() {
  const cases = useApp((s) => s.cases);
  const decisions = useApp((s) => s.decisions);
  const counts = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 };
  cases.forEach((c) => counts[c.severity]++);

  const totalPending = cases.length;
  const reviewedToday = decisions.length + 3; // mock baseline

  return (
    <div className="space-y-6 lg:space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-foreground">Officer Operations Dashboard</h1>
          <p className="text-sm text-muted-foreground mt-1">Welcome back, Officer Abebe Girma (ID: officer-001)</p>
        </div>
        <Link to="/officer/queue" className="btn-primary shadow-lg shadow-accent/20">
          <Briefcase className="h-4 w-4 mr-2" /> Start Reviewing Queue
        </Link>
      </div>

      {counts.CRITICAL > 0 && (
        <div className="bg-destructive/10 border border-destructive/30 rounded-xl p-5 flex items-start sm:items-center gap-4 animate-in slide-in-from-top-4">
          <div className="h-10 w-10 rounded-full bg-destructive/20 flex items-center justify-center shrink-0">
            <AlertCircle className="h-5 w-5 text-destructive animate-pulse" />
          </div>
          <div className="flex-1">
            <div className="font-bold text-destructive text-base mb-0.5">Critical SLA Alert</div>
            <div className="text-sm text-destructive/90">
              You have <b>{counts.CRITICAL} case{counts.CRITICAL > 1 ? "s" : ""}</b> approaching SLA breach in less than 2 hours.
            </div>
          </div>
          <Link to="/officer/queue" className="hidden sm:flex btn-primary bg-destructive text-destructive-foreground hover:bg-destructive/90 shrink-0">
            Review Urgent
          </Link>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        <KpiCard label="Assigned Returns" value={String(totalPending)} trend="12 due today" icon={FileText} tone="warning" />
        <KpiCard label="Reviewed Today" value={String(reviewedToday)} trend="+15% vs yesterday" trendUp icon={CheckCircle2} tone="success" />
        <KpiCard label="Escalated Cases" value={String(counts.CRITICAL + counts.HIGH)} icon={AlertTriangle} tone="danger" />
        <KpiCard label="Avg Processing Time" value="1.8 hrs" trend="-10 min improvement" trendUp icon={Clock} />
      </div>

      {/* Main Charts Area */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Processing Trend */}
        <div className="lg:col-span-2 glass-card border border-border rounded-2xl p-6">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h2 className="text-lg font-bold">Weekly Processing Trend</h2>
              <p className="text-xs text-muted-foreground mt-1">Returns reviewed, approved, and rejected</p>
            </div>
            <div className="h-10 w-10 rounded-lg bg-accent/10 flex items-center justify-center">
              <TrendingUp className="h-5 w-5 text-accent" />
            </div>
          </div>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={processingTrend} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-border)" opacity={0.5} />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: 'var(--color-muted-foreground)' }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: 'var(--color-muted-foreground)' }} />
                <Tooltip 
                  contentStyle={{ borderRadius: '12px', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-card)' }}
                />
                <Legend iconType="circle" wrapperStyle={{ fontSize: '12px', paddingTop: '10px' }} />
                <Line type="monotone" dataKey="reviewed" stroke="var(--color-accent)" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
                <Line type="monotone" dataKey="approved" stroke="var(--color-success)" strokeWidth={3} dot={{ r: 4 }} />
                <Line type="monotone" dataKey="rejected" stroke="var(--color-destructive)" strokeWidth={3} dot={{ r: 4 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Workload by Tax Type (Pie Chart) */}
        <div className="glass-card border border-border rounded-2xl p-6 flex flex-col">
          <div className="flex justify-between items-start mb-2">
            <div>
              <h2 className="text-lg font-bold">Queue by Tax Type</h2>
              <p className="text-xs text-muted-foreground mt-1">Current pending workload</p>
            </div>
            <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center">
              <BarChart2 className="h-5 w-5 text-muted-foreground" />
            </div>
          </div>
          <div className="flex-1 h-[250px] w-full min-h-[250px]">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={taxTypeData}
                  cx="50%"
                  cy="50%"
                  innerRadius={65}
                  outerRadius={85}
                  paddingAngle={5}
                  dataKey="value"
                  stroke="none"
                >
                  {taxTypeData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ borderRadius: '8px', border: 'none', backgroundColor: 'var(--color-card)', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                  formatter={(value) => [`${value}%`, 'Share']}
                />
                <Legend verticalAlign="bottom" height={36} iconType="circle" wrapperStyle={{ fontSize: '12px' }}/>
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Severity Queue Progress Bars */}
        <div className="glass-card border border-border rounded-2xl p-6">
          <h2 className="font-bold text-lg mb-6">Severity Queue Summary</h2>
          <div className="space-y-6">
            {(["CRITICAL", "HIGH", "MEDIUM", "LOW"] as const).map((p) => {
              const max = Math.max(1, ...Object.values(counts));
              const w = (counts[p] / max) * 100;
              const styles = {
                CRITICAL: { bg: "bg-destructive", text: "text-destructive", label: "Critical Priority" },
                HIGH: { bg: "bg-warning", text: "text-warning", label: "High Priority" },
                MEDIUM: { bg: "bg-info", text: "text-info", label: "Medium Priority" },
                LOW: { bg: "bg-muted-foreground", text: "text-muted-foreground", label: "Standard Priority" },
              };
              const s = styles[p];
              return (
                <div key={p}>
                  <div className="flex justify-between text-sm mb-2">
                    <span className={`font-semibold ${s.text}`}>{s.label}</span>
                    <span className="font-bold">{counts[p]} cases</span>
                  </div>
                  <div className="h-3 w-full bg-muted rounded-full overflow-hidden">
                    <div className={`h-full ${s.bg} transition-all duration-1000 ease-out`} style={{ width: `${w}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Today's Tasks */}
        <div className="glass-card border border-border rounded-2xl p-6">
          <h2 className="font-bold text-lg mb-6">Today's Focus Tasks</h2>
          <div className="space-y-4">
            <TaskRow title="Review ACME Corp VAT Return" time="Due in 1 hr" status="urgent" />
            <TaskRow title="Audit Escalation: PLC Alpha" time="Due in 3 hrs" status="urgent" />
            <TaskRow title="Process Batch Income Returns (10)" time="Due today" status="normal" />
            <TaskRow title="Update Monthly Performance Log" time="By EOD" status="normal" />
          </div>
          <button className="w-full mt-6 btn-ghost text-sm">View All Tasks</button>
        </div>
      </div>
    </div>
  );
}

function KpiCard({ label, value, trend, icon: Icon, tone = "default", trendUp }: any) {
  const tones = {
    default: "bg-accent/10 text-accent",
    warning: "bg-warning/15 text-warning",
    danger: "bg-destructive/15 text-destructive",
    success: "bg-success/15 text-success"
  };
  const iconColor = tones[tone as keyof typeof tones];

  return (
    <div className="glass-card rounded-2xl p-6 hover:shadow-lg transition-all duration-300">
      <div className="flex justify-between items-start mb-4">
        <div className={`h-12 w-12 rounded-xl flex items-center justify-center shrink-0 ${iconColor}`}>
          <Icon className="h-6 w-6" />
        </div>
        {trend && (
          <div className={`text-xs font-semibold px-2 py-1 rounded-full ${trendUp ? "bg-success/10 text-success" : tone === "warning" ? "bg-warning/10 text-warning" : "bg-muted text-muted-foreground"}`}>
            {trend}
          </div>
        )}
      </div>
      <div className="text-xs uppercase tracking-widest text-muted-foreground font-semibold mb-1">{label}</div>
      <div className="text-3xl font-black text-foreground">{value}</div>
    </div>
  );
}

function TaskRow({ title, time, status }: { title: string; time: string; status: "urgent" | "normal" }) {
  const isUrgent = status === "urgent";
  return (
    <div className={`flex items-center justify-between p-3 rounded-xl border ${isUrgent ? 'bg-destructive/5 border-destructive/20' : 'bg-muted/10 border-border'} hover:bg-muted/20 transition-colors cursor-pointer`}>
      <div className="flex items-center gap-3">
        <div className={`h-2 w-2 rounded-full ${isUrgent ? 'bg-destructive animate-pulse' : 'bg-accent'}`} />
        <div>
          <div className="text-sm font-semibold text-foreground">{title}</div>
          <div className={`text-xs mt-0.5 ${isUrgent ? 'text-destructive font-medium' : 'text-muted-foreground'}`}>{time}</div>
        </div>
      </div>
      <button className="text-xs font-semibold text-accent hover:text-primary px-3 py-1.5 rounded-lg bg-accent/10 hover:bg-accent/20 transition-colors">
        Action
      </button>
    </div>
  );
}
