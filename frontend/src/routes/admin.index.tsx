import { createFileRoute } from "@tanstack/react-router";
import { Users, Activity, Server, ShieldAlert, ArrowUpRight, ArrowDownRight, UserPlus, Database } from "lucide-react";
import { LineChart, Line, BarChart, Bar, AreaChart, Area, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from "recharts";

export const Route = createFileRoute("/admin/")({
  component: AdminDashboard,
});

const revenueData = [
  { name: 'Jan', revenue: 4000 },
  { name: 'Feb', revenue: 3000 },
  { name: 'Mar', revenue: 5000 },
  { name: 'Apr', revenue: 4500 },
  { name: 'May', revenue: 6000 },
  { name: 'Jun', revenue: 5500 },
  { name: 'Jul', revenue: 7000 },
];

const filingData = [
  { name: 'Week 1', approved: 400, processing: 240, rejected: 20 },
  { name: 'Week 2', approved: 300, processing: 139, rejected: 10 },
  { name: 'Week 3', approved: 200, processing: 980, rejected: 40 },
  { name: 'Week 4', approved: 278, processing: 390, rejected: 15 },
];

const roleData = [
  { name: 'Taxpayers', value: 8500 },
  { name: 'Officers', value: 300 },
  { name: 'Auditors', value: 150 },
  { name: 'Admins', value: 20 },
];

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444'];

function AdminDashboard() {
  return (
    <div className="space-y-6 lg:space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-foreground">System Administrator Dashboard</h1>
          <p className="text-sm text-muted-foreground mt-1">Global Configuration, Monitoring & Analytics</p>
        </div>
        <div className="flex gap-3">
          <button className="btn-primary shadow-lg shadow-accent/20">
            Generate Full Report
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        <KpiCard label="Total Taxpayers" value="8,542" trend="+12%" icon={Users} trendUp />
        <KpiCard label="Total Revenue (ETB)" value="45.2M" trend="+5.4%" icon={Database} trendUp />
        <KpiCard label="Active Sessions" value="1,204" trend="-2%" icon={Activity} />
        <KpiCard label="System Alerts" value="3" trend="+3" icon={ShieldAlert} tone="danger" />
      </div>

      {/* Main Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Revenue Trend Area Chart */}
        <div className="lg:col-span-2 glass-card border border-border rounded-2xl p-6">
          <div className="mb-6">
            <h2 className="text-lg font-bold">Revenue Trend</h2>
            <p className="text-xs text-muted-foreground mt-1">Monthly aggregated tax revenue</p>
          </div>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={revenueData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--color-accent)" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="var(--color-accent)" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-border)" opacity={0.5} />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: 'var(--color-muted-foreground)' }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: 'var(--color-muted-foreground)' }} tickFormatter={(v) => `${v/1000}k`} />
                <Tooltip 
                  contentStyle={{ borderRadius: '12px', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-card)', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                  itemStyle={{ color: 'var(--color-foreground)' }}
                />
                <Area type="monotone" dataKey="revenue" stroke="var(--color-accent)" strokeWidth={3} fillOpacity={1} fill="url(#colorRevenue)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Role Distribution Pie Chart */}
        <div className="glass-card border border-border rounded-2xl p-6 flex flex-col">
          <div className="mb-2">
            <h2 className="text-lg font-bold">User Roles</h2>
            <p className="text-xs text-muted-foreground mt-1">Distribution across platform</p>
          </div>
          <div className="flex-1 h-[250px] w-full min-h-[250px]">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={roleData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                  stroke="none"
                >
                  {roleData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ borderRadius: '8px', border: 'none', backgroundColor: 'var(--color-card)', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                />
                <Legend verticalAlign="bottom" height={36} iconType="circle" wrapperStyle={{ fontSize: '12px' }}/>
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Filing Processing Status Bar Chart */}
        <div className="glass-card border border-border rounded-2xl p-6">
          <div className="mb-6">
            <h2 className="text-lg font-bold">Return Processing Status</h2>
            <p className="text-xs text-muted-foreground mt-1">Weekly breakdown of return volumes</p>
          </div>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={filingData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-border)" opacity={0.5} />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: 'var(--color-muted-foreground)' }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: 'var(--color-muted-foreground)' }} />
                <Tooltip 
                  contentStyle={{ borderRadius: '12px', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-card)' }}
                  cursor={{ fill: 'var(--color-muted)', opacity: 0.2 }}
                />
                <Legend iconType="circle" wrapperStyle={{ fontSize: '12px', paddingTop: '10px' }} />
                <Bar dataKey="approved" stackId="a" fill="var(--color-success)" radius={[0, 0, 4, 4]} />
                <Bar dataKey="processing" stackId="a" fill="var(--color-accent)" />
                <Bar dataKey="rejected" stackId="a" fill="var(--color-destructive)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* System Health & Recent Activity */}
        <div className="space-y-6 flex flex-col">
          <div className="glass-card border border-border rounded-2xl p-6 flex-1">
             <div className="flex items-center gap-3 mb-6 border-b border-border pb-4">
               <Server className="h-5 w-5 text-accent" />
               <h2 className="font-bold text-lg">System Health Core</h2>
             </div>
             <div className="space-y-4 text-sm">
                <HealthRow label="Database Cluster (Primary)" status="Operational" />
                <HealthRow label="Authentication Service" status="Operational" />
                <HealthRow label="Payment Gateway Link" status="Degraded" tone="warning" />
                <HealthRow label="Integration Bus API" status="Operational" />
             </div>
          </div>

          <div className="glass-card border border-border rounded-2xl p-6 flex-1">
             <div className="flex items-center gap-3 mb-6 border-b border-border pb-4">
               <Activity className="h-5 w-5 text-accent" />
               <h2 className="font-bold text-lg">Recent Administrative Actions</h2>
             </div>
             <div className="space-y-5">
               <ActionRow title="System Update Applied v2.4" time="2 hours ago" actor="Admin-001" />
               <ActionRow title="New Auditor Account Provisioned" time="5 hours ago" actor="Admin-002" />
               <ActionRow title="Taxpayer Records Sync Completed" time="12 hours ago" actor="System Service" />
             </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function KpiCard({ label, value, trend, icon: Icon, tone = "default", trendUp }: any) {
  const isDanger = tone === "danger";
  return (
    <div className={`glass-card rounded-2xl p-6 relative overflow-hidden group transition-all duration-300 hover:shadow-lg ${isDanger ? 'border-destructive/30 bg-destructive/5' : ''}`}>
      {isDanger && <div className="absolute top-0 right-0 w-24 h-24 bg-destructive/10 rounded-full blur-2xl -mr-6 -mt-6" />}
      <div className="flex items-start justify-between mb-4">
        <div className={`h-10 w-10 rounded-xl flex items-center justify-center ${isDanger ? 'bg-destructive/20 text-destructive' : 'bg-accent/10 text-accent'}`}>
          <Icon className="h-5 w-5" />
        </div>
        {trend && (
          <span className={`text-xs font-semibold flex items-center gap-0.5 px-2 py-1 rounded-full ${trendUp ? "bg-success/15 text-success" : isDanger ? "bg-destructive/15 text-destructive" : "bg-muted text-muted-foreground"}`}>
            {trendUp ? <ArrowUpRight className="h-3 w-3" /> : !isDanger ? <ArrowDownRight className="h-3 w-3" /> : null}
            {trend}
          </span>
        )}
      </div>
      <div className="text-xs uppercase tracking-widest text-muted-foreground font-semibold mb-1">{label}</div>
      <div className={`text-3xl font-black tracking-tight ${isDanger ? "text-destructive" : "text-foreground"}`}>{value}</div>
    </div>
  );
}

function HealthRow({ label, status, tone = "success" }: { label: string; status: string; tone?: "success" | "warning" | "danger" }) {
  const colorMap = { success: "text-success", warning: "text-warning", danger: "text-destructive" };
  const bgMap = { success: "bg-success", warning: "bg-warning", danger: "bg-destructive" };
  const color = colorMap[tone];
  const bg = bgMap[tone];
  
  return (
    <div className="flex justify-between items-center group">
      <span className="text-muted-foreground group-hover:text-foreground transition-colors">{label}</span>
      <div className="flex items-center gap-2">
        <span className={`h-2 w-2 rounded-full ${bg} ${tone === "success" ? "animate-pulse" : ""}`} />
        <span className={`${color} font-medium text-xs uppercase tracking-wider`}>{status}</span>
      </div>
    </div>
  );
}

function ActionRow({ title, time, actor }: { title: string; time: string; actor: string }) {
  return (
    <div className="flex items-start gap-3">
      <div className="h-8 w-8 rounded-full bg-muted flex items-center justify-center shrink-0 mt-0.5">
        <UserPlus className="h-4 w-4 text-muted-foreground" />
      </div>
      <div>
        <div className="text-sm font-medium text-foreground">{title}</div>
        <div className="text-xs text-muted-foreground mt-0.5">{actor} &bull; {time}</div>
      </div>
    </div>
  );
}
