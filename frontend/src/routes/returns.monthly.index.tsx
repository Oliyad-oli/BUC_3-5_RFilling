import { createFileRoute, Link } from "@tanstack/react-router";
import { useFiling } from "@/lib/filing-store";
import { MonthlyReturnStatusBadge } from "@/components/monthly-return/MonthlyReturnStatusBadge";
import { formatETB, TAXPAYER } from "@/lib/mock-data";
import { Empty } from "@/components/form-primitives";
import { FilePlus2, Download, Search } from "lucide-react";
import { Input } from "@/components/ui/input";

export const Route = createFileRoute("/returns/monthly/")({
  component: MonthlyReturnsIndex,
});

function MonthlyReturnsIndex() {
  const returns = useFiling((s) => s.monthlyReturns).filter((r) => r.tin === TAXPAYER.tin);
  return (
    <div className="space-y-6 lg:space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-foreground">Monthly Returns</h1>
          <p className="text-sm text-muted-foreground mt-1">Submit and track consolidated monthly returns under <span className="mono font-medium text-foreground">{TAXPAYER.tin}</span></p>
        </div>
        <Link to="/returns/monthly/new" className="btn-primary shadow-lg shadow-accent/20">
          <FilePlus2 className="h-4 w-4" /> 
          <span className="hidden sm:inline">Submit Monthly Return</span>
          <span className="sm:hidden">New Return</span>
        </Link>
      </div>

      <div className="glass-card border border-border rounded-2xl overflow-hidden">
        {/* Table Toolbar */}
        <div className="p-4 sm:p-5 border-b border-border flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-muted/10">
          <div className="relative max-w-sm w-full">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input 
              placeholder="Search by ID or Period..." 
              className="pl-9 bg-card h-10 border-border"
            />
          </div>
          <button className="btn-ghost h-10 text-xs font-semibold px-4"><Download className="h-4 w-4 mr-2"/> Export CSV</button>
        </div>

        {returns.length === 0 ? (
          <div className="p-12">
            <Empty text="No monthly returns found." />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-muted/30 text-xs text-muted-foreground uppercase tracking-wider font-semibold">
                <tr>
                  <th className="px-6 py-4 text-left">Return ID</th>
                  <th className="px-6 py-4 text-left">Period</th>
                  <th className="px-6 py-4 text-left">Tax Type</th>
                  <th className="px-6 py-4 text-left">Status</th>
                  <th className="px-6 py-4 text-right">Net Payable</th>
                  <th className="px-6 py-4 text-left">Submitted At</th>
                  <th className="px-6 py-4" />
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {returns.map((r) => (
                  <tr key={r.id} className="hover:bg-muted/20 transition-colors group">
                    <td className="px-6 py-4 mono font-medium text-xs">{r.id}</td>
                    <td className="px-6 py-4 font-semibold">{r.filingPeriod}</td>
                    <td className="px-6 py-4">
                      <span className="bg-secondary text-secondary-foreground px-2 py-1 rounded text-xs font-semibold">{r.taxType}</span>
                    </td>
                    <td className="px-6 py-4"><MonthlyReturnStatusBadge status={r.status} /></td>
                    <td className="px-6 py-4 text-right mono font-medium text-foreground">{formatETB(r.summary.netPayable)}</td>
                    <td className="px-6 py-4 text-muted-foreground text-xs">{r.submittedAt ?? "—"}</td>
                    <td className="px-6 py-4 text-right">
                      <Link to="/returns/monthly/$id" params={{ id: r.id }} className="text-accent font-medium hover:text-primary hover:underline transition-colors opacity-0 group-hover:opacity-100">
                        {r.status === "DRAFT" ? "Continue" : "View Details"}
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}