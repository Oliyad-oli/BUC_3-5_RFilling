import { createFileRoute, Link } from "@tanstack/react-router";
import { useApp } from "@/lib/store";
import { StatusBadge } from "@/components/StatusBadge";
import { formatETB, TAXPAYER } from "@/lib/mock-data";

export const Route = createFileRoute("/returns/")({
  component: ReturnsIndex,
});

function ReturnsIndex() {
  const returns = useApp((s) => s.returns).filter((r) => r.tin === TAXPAYER.tin);
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">My Filing History</h1>
          <p className="text-sm text-muted-foreground mt-1">All tax returns filed under {TAXPAYER.tin}</p>
        </div>
        <Link to="/returns/new" className="bg-accent text-accent-foreground px-4 py-2.5 rounded-md text-sm font-medium hover:opacity-90">
          File New Return
        </Link>
      </div>

      <div className="bg-card border border-border rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs text-muted-foreground uppercase">
            <tr>
              <th className="px-5 py-3 text-left font-medium">Return ID</th>
              <th className="px-5 py-3 text-left font-medium">Period</th>
              <th className="px-5 py-3 text-left font-medium">Tax Type</th>
              <th className="px-5 py-3 text-left font-medium">Status</th>
              <th className="px-5 py-3 text-right font-medium">Net Tax</th>
              <th className="px-5 py-3 text-left font-medium">Filed</th>
              <th className="px-5 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {returns.map((r) => (
              <tr key={r.id} className="border-t border-border hover:bg-muted/30">
                <td className="px-5 py-3 mono text-xs">{r.id}</td>
                <td className="px-5 py-3">{r.period}</td>
                <td className="px-5 py-3">{r.taxType}</td>
                <td className="px-5 py-3"><StatusBadge status={r.status} /></td>
                <td className="px-5 py-3 text-right mono">{formatETB(r.netTax)}</td>
                <td className="px-5 py-3 text-muted-foreground">{r.createdAt}</td>
                <td className="px-5 py-3 text-right">
                  <Link to="/returns/$id" params={{ id: r.id }} className="text-accent hover:underline">
                    {r.status === "DRAFT" ? "Continue" : "View"}
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
