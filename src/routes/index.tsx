import { createFileRoute, Link } from "@tanstack/react-router";
import { useApp } from "@/lib/store";
import { StatusBadge } from "@/components/StatusBadge";
import { formatETB, TAXPAYER, type FilingPeriod } from "@/lib/mock-data";
import { ArrowRight, FilePlus2, AlertTriangle, Clock, CheckCircle2, Calendar } from "lucide-react";

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

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Welcome back, {TAXPAYER.party}</h1>
          <p className="text-sm text-muted-foreground mt-1 mono">{TAXPAYER.tin}</p>
        </div>
        <Link to="/returns/new" className="inline-flex items-center gap-2 bg-accent text-accent-foreground px-4 py-2.5 rounded-md text-sm font-medium hover:opacity-90">
          <FilePlus2 className="h-4 w-4" /> File New Return
        </Link>
      </div>

      {overdue.length > 0 && (
        <div className="bg-destructive/10 border border-destructive/40 rounded-md p-4 flex gap-3">
          <AlertTriangle className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
          <div className="text-sm">
            <div className="font-semibold text-destructive">{overdue.length} filing obligation{overdue.length > 1 ? "s are" : " is"} overdue</div>
            <div className="text-muted-foreground mt-1">
              {overdue.map((p) => `${p.taxTypeLabel} ${p.periodLabel}`).join(", ")} — file immediately to avoid penalties.{" "}
              <Link to="/obligations" className="text-destructive underline">View obligations →</Link>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Active Filings" value={String(active)} sub={`${review} pending action`} />
        <StatCard label="Completed This Year" value={String(completed)} sub="All periods" />
        <StatCard label="Pending Payments" value={formatETB(pendingPayment)} sub="Due 15 May 2026" tone="warning" />
        <StatCard label="Under Review" value={String(review)} sub="Officer assigned" tone={review > 0 ? "warning" : "default"} />
      </div>

      {/* Filing Obligations Summary */}
      <div className="bg-card border border-border rounded-lg overflow-hidden">
        <div className="px-5 py-4 border-b border-border flex items-center justify-between">
          <div>
            <h2 className="font-semibold flex items-center gap-2"><Calendar className="h-4 w-4 text-muted-foreground" /> Filing Obligations</h2>
            <p className="text-xs text-muted-foreground mt-0.5">Derived from tax account and filing period projections</p>
          </div>
          <Link to="/obligations" className="text-sm text-accent hover:underline flex items-center gap-1">
            Full calendar <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
        <div className="divide-y divide-border">
          {myPeriods
            .filter((p) => ["OPEN", "DUE", "OVERDUE"].includes(p.status))
            .slice(0, 5)
            .map((p) => (
              <PeriodRow key={p.id} period={p} returns={myReturns} />
            ))}
          {actionRequired.length === 0 && myPeriods.filter(p => ["OPEN","DUE","OVERDUE"].includes(p.status)).length === 0 && (
            <div className="px-5 py-4 text-sm text-muted-foreground text-center">
              <CheckCircle2 className="h-4 w-4 text-success inline mr-2" />All current obligations are filed.
            </div>
          )}
        </div>
      </div>

      {/* Recent Tax Returns */}
      <div className="bg-card border border-border rounded-lg overflow-hidden">
        <div className="px-5 py-4 border-b border-border flex items-center justify-between">
          <div>
            <h2 className="font-semibold">Recent Tax Returns</h2>
            <p className="text-xs text-muted-foreground mt-0.5">Most recent filings across all tax types</p>
          </div>
          <Link to="/returns" className="text-sm text-accent hover:underline flex items-center gap-1">
            View all <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs text-muted-foreground uppercase tracking-wide">
            <tr>
              <th className="px-5 py-3 text-left font-medium">TIN</th>
              <th className="px-5 py-3 text-left font-medium">Period</th>
              <th className="px-5 py-3 text-left font-medium">Tax Type</th>
              <th className="px-5 py-3 text-left font-medium">Status</th>
              <th className="px-5 py-3 text-right font-medium">Net Tax</th>
              <th className="px-5 py-3 text-right font-medium">Action</th>
            </tr>
          </thead>
          <tbody>
            {myReturns.map((r) => (
              <tr key={r.id} className="border-t border-border hover:bg-muted/30">
                <td className="px-5 py-3 mono text-xs">{r.tin}</td>
                <td className="px-5 py-3">{r.period}</td>
                <td className="px-5 py-3">{r.taxType}</td>
                <td className="px-5 py-3"><StatusBadge status={r.status} /></td>
                <td className="px-5 py-3 text-right mono">{formatETB(r.netTax)}</td>
                <td className="px-5 py-3 text-right">
                  <Link to="/returns/$id" params={{ id: r.id }} className="text-accent hover:underline text-sm">
                    {r.status === "DRAFT" || r.status === "AMENDMENT_DRAFT" ? "Continue" : "View"}
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
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
      ? `${daysUntilDue} days until deadline`
      : period.status === "OPEN"
      ? `${daysUntilDue} days until deadline`
      : period.status === "FUTURE"
      ? `Opens ${period.coversFrom}`
      : "";

  return (
    <div className="px-5 py-3 flex items-center justify-between gap-4">
      <div className="flex items-center gap-3 min-w-0">
        <span className={`text-[10px] uppercase font-semibold px-2 py-0.5 rounded shrink-0 ${statusStyle}`}>
          {period.status}
        </span>
        <div className="min-w-0">
          <div className="text-sm font-medium truncate">
            {period.taxTypeLabel} — {period.periodLabel}
          </div>
          <div className="text-xs text-muted-foreground flex items-center gap-1">
            <Clock className="h-3 w-3" />
            {dueText}
            {linkedReturn && (
              <span className="ml-2">
                · Return <span className="mono">{linkedReturn.id}</span> ({linkedReturn.status})
              </span>
            )}
          </div>
        </div>
      </div>
      <div className="shrink-0">
        {linkedReturn ? (
          <Link to="/returns/$id" params={{ id: linkedReturn.id }} className="text-sm text-accent hover:underline">
            {linkedReturn.status === "DRAFT" ? "Resume" : "View Return"}
          </Link>
        ) : (
          <Link
            to="/returns/new"
            search={{ filingPeriodId: period.id }}
            className={`text-sm px-3 py-1.5 rounded-md font-medium ${
              period.status === "OVERDUE"
                ? "bg-destructive text-destructive-foreground hover:opacity-90"
                : "bg-accent text-accent-foreground hover:opacity-90"
            }`}
          >
            {period.status === "OVERDUE" ? "File Now (Late)" : "Start Filing"}
          </Link>
        )}
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

function StatCard({ label, value, sub, tone = "default" }: { label: string; value: string; sub: string; tone?: "default" | "warning" }) {
  return (
    <div className="bg-card border border-border rounded-lg p-5">
      <div className="text-xs uppercase tracking-wide text-muted-foreground font-medium">{label}</div>
      <div className={`mt-2 text-2xl font-semibold ${tone === "warning" ? "text-warning" : "text-foreground"}`}>{value}</div>
      <div className="text-xs text-muted-foreground mt-1">{sub}</div>
    </div>
  );
}
