import { createFileRoute } from "@tanstack/react-router";
import { useApp } from "@/lib/store";
import { FileSearch } from "lucide-react";

export const Route = createFileRoute("/auditor/")({
  component: AuditorDashboard,
});

function AuditorDashboard() {
  const cases = useApp((s) => s.cases);
  const assigned = cases.filter(c => c.assignedOfficer === "auditor" || c.status === "OPEN");

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Auditor Dashboard</h1>
        <p className="text-sm text-muted-foreground mt-1">Senior Auditor</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card label="Active Audits" value={String(assigned.length)} tone="warning" />
        <Card label="Pending Findings" value="4" tone="danger" />
        <Card label="Closed This Month" value="12" tone="success" />
        <Card label="Avg Duration" value="14 Days" tone="info" />
      </div>

      <div className="bg-card border border-border rounded-lg p-6">
        <div className="flex items-center gap-3 mb-4">
          <FileSearch className="h-5 w-5 text-accent" />
          <h2 className="font-semibold">Recent Assigned Cases</h2>
        </div>
        {assigned.length === 0 ? (
          <div className="text-sm text-muted-foreground">No cases currently assigned to you.</div>
        ) : (
          <div className="space-y-4">
            {assigned.slice(0, 5).map(c => (
              <div key={c.id} className="flex justify-between items-center p-3 border border-border rounded-md">
                <div>
                  <div className="font-medium text-sm">{c.taxType} - {c.period}</div>
                  <div className="text-xs text-muted-foreground">TIN: {c.tin} · {c.party}</div>
                </div>
                <div className="text-right">
                  <div className="text-xs font-semibold text-warning">{c.status}</div>
                  <div className="text-[10px] text-muted-foreground">ID: {c.id}</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function Card({ label, value, tone }: { label: string; value: string; tone: "warning" | "danger" | "success" | "info" }) {
  const t = { warning: "text-warning", danger: "text-destructive", success: "text-success", info: "text-accent" }[tone];
  return (
    <div className="bg-card border border-border rounded-lg p-5">
      <div className="text-xs uppercase tracking-wide text-muted-foreground font-medium">{label}</div>
      <div className={`mt-2 text-2xl font-semibold ${t}`}>{value}</div>
    </div>
  );
}
