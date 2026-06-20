import { CheckCircle2 } from "lucide-react";
import { Link } from "@tanstack/react-router";
import type { MonthlyReturn } from "@/lib/filing-store";
import { formatETB } from "@/lib/mock-data";

export function MonthlyReturnAcknowledgement({
  open,
  onClose,
  ret,
}: {
  open: boolean;
  onClose: () => void;
  ret: MonthlyReturn | null;
}) {
  if (!open || !ret) return null;
  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4 fade-in" onClick={onClose}>
      <div className="bg-card border border-border rounded-lg max-w-md w-full shadow-lg" onClick={(e) => e.stopPropagation()}>
        <div className="p-6 text-center">
          <div className="mx-auto h-14 w-14 rounded-full bg-success/15 text-success flex items-center justify-center mb-4">
            <CheckCircle2 className="h-7 w-7" />
          </div>
          <h2 className="text-lg font-semibold">Monthly Return Acknowledged</h2>
          <p className="text-sm text-muted-foreground mt-1">Your monthly return has cleared the processing pipeline.</p>
          <div className="mt-5 text-left bg-muted/40 border border-border rounded-md p-4 text-sm space-y-2">
            <div className="flex justify-between"><span className="text-muted-foreground">Reference</span><span className="mono">{ret.reference}</span></div>
            <div className="flex justify-between"><span className="text-muted-foreground">Return ID</span><span className="mono">{ret.id}</span></div>
            <div className="flex justify-between"><span className="text-muted-foreground">Period</span><span>{ret.filingPeriod}</span></div>
            <div className="flex justify-between"><span className="text-muted-foreground">Net Payable</span><span className="mono font-semibold">{formatETB(ret.summary.netPayable)}</span></div>
          </div>
        </div>
        <div className="p-4 border-t border-border flex justify-end gap-2">
          <Link to="/returns/monthly" className="btn-ghost" onClick={onClose}>View All</Link>
          <Link to="/returns/monthly/$id" params={{ id: ret.id }} className="btn-primary" onClick={onClose}>Open Return</Link>
        </div>
      </div>
    </div>
  );
}