import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { useFiling } from "@/lib/filing-store";
import { Card, CardHeader, Info2 } from "@/components/form-primitives";
import { DailyReturnStatusBadge } from "@/components/daily-return/DailyReturnStatusBadge";
import { DailyReturnItemsTable } from "@/components/daily-return/DailyReturnItemsTable";
import { DailyReturnSummaryCard } from "@/components/daily-return/DailyReturnSummaryCard";
import { DailyReturnAttachments } from "@/components/daily-return/DailyReturnAttachments";
import { DailyReturnTimeline } from "@/components/daily-return/DailyReturnTimeline";

import { RouteError } from "@/components/RouteError";

export const Route = createFileRoute("/returns/daily/$id")({
  component: DailyReturnDetail,
  errorComponent: RouteError,
});

function DailyReturnDetail() {
  const { id } = Route.useParams();
  const r = useFiling((s) => s.dailyReturns.find((x) => x.id === id));
  if (!r) throw notFound();

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      <Link to="/returns/daily" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="h-4 w-4" /> Back to daily returns
      </Link>

      <div className="bg-card border border-border rounded-lg p-6">
        <div className="flex items-start justify-between flex-wrap gap-4">
          <div>
            <div className="text-xs uppercase text-muted-foreground tracking-wide">Daily Return</div>
            <h1 className="text-xl font-semibold mt-1 mono">{r.id}</h1>
            <div className="text-sm text-muted-foreground mt-1 mono">
              {r.tin} · {r.taxpayerName} · {r.taxType} · {r.filingPeriod}
            </div>
          </div>
          <DailyReturnStatusBadge status={r.status} />
        </div>
        <div className="mt-5 grid grid-cols-2 md:grid-cols-4 gap-4">
          <Info2 label="Reference" value={r.reference ?? "—"} />
          <Info2 label="Submitted" value={r.submittedAt ?? "—"} />
          <Info2 label="Acknowledged" value={r.acknowledgedAt ?? "—"} tone={r.acknowledgedAt ? "success" : undefined} />
          <Info2 label="Business Sector" value={r.businessSector} />
        </div>
      </div>

      <Card>
        <CardHeader title="Return Items" />
        <DailyReturnItemsTable items={r.items} onChange={() => undefined} readOnly />
        <div className="mt-5">
          <DailyReturnSummaryCard summary={r.summary} />
        </div>
      </Card>

      <Card>
        <CardHeader title="Attachments" />
        <DailyReturnAttachments attachments={r.attachments} onChange={() => undefined} readOnly />
      </Card>

      <Card>
        <CardHeader title="Timeline" />
        <DailyReturnTimeline status={r.status} events={r.events} />
      </Card>
    </div>
  );
}