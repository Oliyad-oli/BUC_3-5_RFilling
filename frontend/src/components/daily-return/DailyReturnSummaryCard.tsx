import { formatETB } from "@/lib/mock-data";
import { Row } from "@/components/form-primitives";
import type { DailyReturn } from "@/lib/filing-store";

export function DailyReturnSummaryCard({ summary }: { summary: DailyReturn["summary"] }) {
  return (
    <div className="bg-card border border-border rounded-lg overflow-hidden">
      <div className="px-5 py-3 bg-muted/40 border-b border-border text-sm font-semibold">Return Summary</div>
      <div className="divide-y divide-border">
        <Row label="Gross Sales" value={formatETB(summary.grossSales)} />
        <Row label="Taxable Sales" value={formatETB(summary.taxableSales)} />
        <Row label="Exempt Sales" value={formatETB(summary.exemptSales)} />
        <Row label="Tax Amount" value={formatETB(summary.taxAmount)} />
        <Row label="Penalty" value={formatETB(summary.penalty)} negative={summary.penalty > 0} />
        <Row label="NET PAYABLE" value={formatETB(summary.netPayable)} bold />
      </div>
    </div>
  );
}