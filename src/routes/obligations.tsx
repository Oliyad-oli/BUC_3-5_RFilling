import { createFileRoute, Link } from "@tanstack/react-router";
import { useApp } from "@/lib/store";
import { TAXPAYER, formatETB, type FilingPeriod } from "@/lib/mock-data";
import { AlertTriangle, CheckCircle2, Clock, Calendar, ArrowRight, Info, CalendarClock } from "lucide-react";

export const Route = createFileRoute("/obligations")({
  component: ObligationsCalendar,
});

const PERIOD_STATUS_CONFIG: Record<string, { label: string; bg: string; text: string; border: string }> = {
  FUTURE:  { label: "FUTURE",  bg: "bg-muted",            text: "text-muted-foreground", border: "border-l-muted-foreground" },
  OPEN:    { label: "OPEN",    bg: "bg-info/15",          text: "text-info",             border: "border-l-info" },
  DUE:     { label: "DUE",     bg: "bg-warning/15",       text: "text-warning",          border: "border-l-warning" },
  OVERDUE: { label: "OVERDUE", bg: "bg-destructive/15",   text: "text-destructive",      border: "border-l-destructive" },
  FILED:   { label: "FILED",   bg: "bg-success/15",       text: "text-success",          border: "border-l-success" },
};

function ObligationsCalendar() {
  const filingPeriods = useApp((s) => s.filingPeriods);
  const returns = useApp((s) => s.returns);
  const penalties = useApp((s) => s.penalties);
  const myPeriods = filingPeriods.filter((p) => p.tin === TAXPAYER.tin);

  const overdue   = myPeriods.filter((p) => p.status === "OVERDUE");
  const due       = myPeriods.filter((p) => p.status === "DUE");
  const open      = myPeriods.filter((p) => p.status === "OPEN");
  const future    = myPeriods.filter((p) => p.status === "FUTURE");
  const filed     = myPeriods.filter((p) => p.status === "FILED");

  const actionNeeded = [...overdue, ...due, ...open].filter((p) => !p.taxReturnId).length;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Filing Obligations Calendar</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Derived from tax account and filing period projections — taxpayer <span className="mono">{TAXPAYER.tin}</span>
        </p>
      </div>

      {/* How this works banner */}
      <div className="bg-accent/5 border border-accent/30 rounded-md p-4 flex gap-3">
        <Info className="h-5 w-5 text-accent shrink-0 mt-0.5" />
        <div className="text-sm">
          <span className="font-medium text-accent">How the obligation engine works: </span>
          <span className="text-muted-foreground">
            Registration sends <span className="mono text-foreground">HandleTaxAccountOpenedWebhook</span> to create the tax account projection.
            Calendar sends <span className="mono text-foreground"> HandleFilingWindowOpenedWebhook</span> to create filing periods.
            <span className="mono text-foreground"> FilingPeriodOverdueJob</span> runs daily at midnight, while period status is derived as FUTURE, OPEN,
            DUE, OVERDUE, or FILED from deadline and return state.
          </span>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
        {[
          { label: "Overdue", count: overdue.length, cls: "text-destructive" },
          { label: "Due", count: due.length, cls: "text-warning" },
          { label: "Open", count: open.length, cls: "text-info" },
          { label: "Future", count: future.length, cls: "text-muted-foreground" },
          { label: "Filed", count: filed.length, cls: "text-success" },
        ].map(({ label, count, cls }) => (
          <div key={label} className="bg-card border border-border rounded-lg p-4 text-center">
            <div className={`text-2xl font-semibold ${cls}`}>{count}</div>
            <div className="text-xs text-muted-foreground mt-1 uppercase tracking-wide">{label}</div>
          </div>
        ))}
      </div>

      {actionNeeded > 0 && (
        <div className="bg-warning/10 border border-warning/40 rounded-md p-4 flex gap-3">
          <AlertTriangle className="h-5 w-5 text-warning shrink-0 mt-0.5" />
          <div className="text-sm">
            <span className="font-semibold text-warning">{actionNeeded} obligation{actionNeeded > 1 ? "s require" : " requires"} action.</span>
            <span className="text-muted-foreground ml-1">Click "Start Filing" on any open or overdue period below.</span>
          </div>
        </div>
      )}

      {/* OVERDUE */}
      {overdue.length > 0 && (
        <Section title="Overdue" icon={<AlertTriangle className="h-4 w-4 text-destructive" />}>
          {overdue.map((p) => <PeriodCard key={p.id} period={p} returns={returns} penalty={penalties.find((pen) => pen.filingPeriodId === p.id)} />)}
        </Section>
      )}

      {/* DUE */}
      {due.length > 0 && (
        <Section title="Due — Deadline Approaching" icon={<Clock className="h-4 w-4 text-warning" />}>
          {due.map((p) => <PeriodCard key={p.id} period={p} returns={returns} />)}
        </Section>
      )}

      {/* OPEN */}
      {open.length > 0 && (
        <Section title="Open — Filing Period Active" icon={<Calendar className="h-4 w-4 text-info" />}>
          {open.map((p) => <PeriodCard key={p.id} period={p} returns={returns} />)}
        </Section>
      )}

      {/* FUTURE */}
      {future.length > 0 && (
        <Section title="Future — Upcoming Periods" icon={<CalendarClock className="h-4 w-4 text-muted-foreground" />}>
          {future.map((p) => <PeriodCard key={p.id} period={p} returns={returns} />)}
        </Section>
      )}

      {/* FILED */}
      {filed.length > 0 && (
        <Section title="Filed" icon={<CheckCircle2 className="h-4 w-4 text-success" />}>
          {filed.map((p) => <PeriodCard key={p.id} period={p} returns={returns} />)}
        </Section>
      )}
    </div>
  );
}

function Section({ title, icon, children }: { title: string; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <div>
      <div className="flex items-center gap-2 mb-3">
        {icon}
        <h2 className="font-semibold text-sm uppercase tracking-wide text-muted-foreground">{title}</h2>
      </div>
      <div className="space-y-2">{children}</div>
    </div>
  );
}

function PeriodCard({ period, returns, penalty }: { period: FilingPeriod; returns: ReturnType<typeof useApp.getState>["returns"]; penalty?: import("@/lib/mock-data").PenaltyExposure }) {
  const cfg = PERIOD_STATUS_CONFIG[period.status] ?? PERIOD_STATUS_CONFIG.OPEN;
  const linkedReturn = period.taxReturnId ? returns.find((r) => r.id === period.taxReturnId) : null;

  const dueLabel =
    period.status === "OVERDUE"
      ? `${Math.abs(getDaysUntilDue(period.dueDate))} days overdue`
      : period.status === "DUE"
      ? `${getDaysUntilDue(period.dueDate)} days until deadline`
      : period.status === "OPEN"
      ? `${getDaysUntilDue(period.dueDate)} days until deadline`
      : period.status === "FUTURE"
      ? `Opens ${period.coversFrom}`
      : "Filed";

  return (
    <div className={`bg-card border border-border border-l-4 ${cfg.border} rounded-lg p-4 flex items-center justify-between gap-4`}>
      <div className="flex items-center gap-4 min-w-0 flex-1">
        <span className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded shrink-0 ${cfg.bg} ${cfg.text}`}>
          {cfg.label}
        </span>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-3 flex-wrap">
            <span className="font-medium text-sm">{period.taxTypeLabel}</span>
            <span className="text-sm text-muted-foreground">{period.periodLabel}</span>
            <span className="mono text-xs text-muted-foreground">{period.id}</span>
          </div>
          <div className="text-xs text-muted-foreground mt-1 flex items-center gap-4 flex-wrap">
            <span><span className="text-foreground/60">Covers:</span> {period.coversFrom} → {period.coversTo}</span>
            <span><span className="text-foreground/60">Due:</span> {period.dueDate}</span>
            <span className={period.status === "OVERDUE" ? "text-destructive font-medium" : period.status === "DUE" ? "text-warning" : ""}>
              {dueLabel}
            </span>
            {linkedReturn && (
              <span>
                <span className="text-foreground/60">Return:</span>{" "}
                <span className="mono">{linkedReturn.id}</span>{" "}
                <span className={`font-medium ${
                  linkedReturn.status === "COMPLETED" ? "text-success"
                  : linkedReturn.status === "MANUAL_REVIEW" ? "text-warning"
                  : linkedReturn.status === "CALCULATION_FAILED" ? "text-destructive"
                  : "text-info"
                }`}>({linkedReturn.status})</span>
              </span>
            )}
          </div>
        </div>
      </div>

      {penalty && (
        <div className="shrink-0 bg-destructive/10 border border-destructive/30 rounded px-3 py-1.5 text-xs text-destructive">
          <div className="font-semibold">Penalty Exposure</div>
          <div>{penalty.daysLate} days late · Est. {formatETB(penalty.estimatedPenalty)}</div>
        </div>
      )}

      <div className="shrink-0">
        {period.status === "FILED" && linkedReturn ? (
          <Link to="/returns/$id" params={{ id: linkedReturn.id }} className="inline-flex items-center gap-1 text-sm text-accent hover:underline">
            View Return <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        ) : linkedReturn ? (
          <Link
            to="/returns/$id"
            params={{ id: linkedReturn.id }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium bg-accent/10 text-accent hover:bg-accent/20"
          >
            {linkedReturn.status === "DRAFT" ? "Resume Filing" : "View Return"} <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        ) : (
          <Link
            to="/returns/new"
            search={{ filingPeriodId: period.id }}
            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium ${
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
