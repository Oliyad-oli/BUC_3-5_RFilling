import { Row } from "@/components/form-primitives";
import { formatETB } from "@/lib/mock-data";
import type { MonthlyReturn } from "@/lib/filing-store";

export function MonthlyReturnSummary({ summary }: { summary: MonthlyReturn["summary"] }) {
  return (
    <div className="bg-card border border-border rounded-lg overflow-hidden">
      <div className="px-5 py-3 bg-muted/40 border-b border-border text-sm font-semibold">Summary</div>
      <div className="divide-y divide-border">
        <Row label="Gross Amount" value={formatETB(summary.grossAmount)} />
        <Row label="Taxable Amount" value={formatETB(summary.taxableAmount)} />
        <Row label="Total Tax" value={formatETB(summary.totalTax)} />
        <Row label="Credits" value={`(${formatETB(summary.credits)})`} negative={summary.credits > 0} />
        <Row label="Penalty" value={formatETB(summary.penalty)} negative={summary.penalty > 0} />
        <Row label="Interest" value={formatETB(summary.interest)} negative={summary.interest > 0} />
        <Row label="NET PAYABLE" value={formatETB(summary.netPayable)} bold />
      </div>
    </div>
  );
}