import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { useFiling } from "@/lib/filing-store";
import { Card, CardHeader, Info2 } from "@/components/form-primitives";
import { MonthlyReturnStatusBadge } from "@/components/monthly-return/MonthlyReturnStatusBadge";
import { MonthlyReturnLinesTable } from "@/components/monthly-return/MonthlyReturnLinesTable";
import { MonthlyReturnSchedules } from "@/components/monthly-return/MonthlyReturnSchedules";
import { MonthlyReturnSummary } from "@/components/monthly-return/MonthlyReturnSummary";
import { MonthlyReturnAttachments } from "@/components/monthly-return/MonthlyReturnAttachments";
import { MonthlyReturnWorkflowTimeline } from "@/components/monthly-return/MonthlyReturnWorkflowTimeline";

import { RouteError } from "@/components/RouteError";

export const Route = createFileRoute("/returns/monthly/$id")({
  component: MonthlyReturnDetail,
  errorComponent: RouteError,
});

function MonthlyReturnDetail() {
  const { id } = Route.useParams();
  const r = useFiling((s) => s.monthlyReturns.find((x) => x.id === id));
  if (!r) throw notFound();

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      <Link to="/returns/monthly" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="h-4 w-4" /> Back to monthly returns
      </Link>

      <div className="bg-card border border-border rounded-lg p-6">
        <div className="flex items-start justify-between flex-wrap gap-4">
          <div>
            <div className="text-xs uppercase text-muted-foreground tracking-wide">Monthly Return</div>
            <h1 className="text-xl font-semibold mt-1 mono">{r.id}</h1>
            <div className="text-sm text-muted-foreground mt-1 mono">
              {r.tin} · {r.taxpayerName} · {r.taxType} · {r.filingPeriod}
            </div>
          </div>
          <MonthlyReturnStatusBadge status={r.status} />
        </div>
        <div className="mt-5 grid grid-cols-2 md:grid-cols-4 gap-4">
          <Info2 label="Reference" value={r.reference ?? "—"} />
          <Info2 label="Submitted" value={r.submittedAt ?? "—"} />
          <Info2 label="Acknowledged" value={r.acknowledgedAt ?? "—"} tone={r.acknowledgedAt ? "success" : undefined} />
          <Info2 label="Tax Office" value={r.taxOffice} />
        </div>
        {r.amendedFrom && (
          <div className="mt-4 text-xs text-muted-foreground">
            Amendment of <span className="mono">{r.amendedFrom}</span>
          </div>
        )}
      </div>

      <Card>
        <CardHeader title="Lines" />
        <MonthlyReturnLinesTable lines={r.lines} onChange={() => undefined} readOnly />
      </Card>

      <Card>
        <CardHeader title="Schedules" />
        <MonthlyReturnSchedules schedules={r.schedules} onChange={() => undefined} readOnly />
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader title="Summary" />
          <MonthlyReturnSummary summary={r.summary} />
        </Card>
        <Card>
          <CardHeader title="Processing Pipeline" />
          <MonthlyReturnWorkflowTimeline pipelineStage={r.pipelineStage} events={r.events} />
        </Card>
      </div>

      <Card>
        <CardHeader title="Attachments" />
        <MonthlyReturnAttachments attachments={r.attachments} onChange={() => undefined} readOnly />
      </Card>
    </div>
  );
}