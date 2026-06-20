import { createFileRoute, Link } from "@tanstack/react-router";
import { useFiling } from "@/lib/filing-store";
import { DailyReturnStatusBadge } from "@/components/daily-return/DailyReturnStatusBadge";
import { formatETB, TAXPAYER } from "@/lib/mock-data";
import { Empty } from "@/components/form-primitives";
import { FilePlus2 } from "lucide-react";

export const Route = createFileRoute("/returns/daily/")({
  component: DailyReturnsIndex,
});

function DailyReturnsIndex() {
  const returns = useFiling((s) => s.dailyReturns).filter((r) => r.tin === TAXPAYER.tin);
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-semibold">Daily Returns</h1>
          <p className="text-sm text-muted-foreground mt-1">Register and track daily tax returns filed under {TAXPAYER.tin}</p>
        </div>
        <Link to="/returns/daily/new" className="btn-primary">
          <FilePlus2 className="h-4 w-4" /> Register Daily Return
        </Link>
      </div>

      <div className="bg-card border border-border rounded-lg overflow-hidden">
        {returns.length === 0 ? (
          <div className="p-6">
            <Empty text="No daily returns yet. Register one to get started." />
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-muted/50 text-xs text-muted-foreground uppercase">
              <tr>
                <th className="px-5 py-3 text-left font-medium">Return ID</th>
                <th className="px-5 py-3 text-left font-medium">Period</th>
                <th className="px-5 py-3 text-left font-medium">Tax Type</th>
                <th className="px-5 py-3 text-left font-medium">Status</th>
                <th className="px-5 py-3 text-right font-medium">Net Payable</th>
                <th className="px-5 py-3 text-left font-medium">Submitted</th>
                <th className="px-5 py-3" />
              </tr>
            </thead>
            <tbody>
              {returns.map((r) => (
                <tr key={r.id} className="border-t border-border hover:bg-muted/30">
                  <td className="px-5 py-3 mono text-xs">{r.id}</td>
                  <td className="px-5 py-3">{r.filingPeriod}</td>
                  <td className="px-5 py-3">{r.taxType}</td>
                  <td className="px-5 py-3"><DailyReturnStatusBadge status={r.status} /></td>
                  <td className="px-5 py-3 text-right mono">{formatETB(r.summary.netPayable)}</td>
                  <td className="px-5 py-3 text-muted-foreground">{r.submittedAt ?? "—"}</td>
                  <td className="px-5 py-3 text-right">
                    <Link to="/returns/daily/$id" params={{ id: r.id }} className="text-accent hover:underline">
                      {r.status === "DRAFT" ? "Continue" : "View"}
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}