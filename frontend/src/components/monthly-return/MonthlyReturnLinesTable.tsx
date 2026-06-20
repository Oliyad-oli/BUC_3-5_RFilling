import { Plus, Trash2 } from "lucide-react";
import type { MonthlyReturnLine } from "@/lib/filing-store";
import { formatETB } from "@/lib/mock-data";

function recalc(l: MonthlyReturnLine): MonthlyReturnLine {
  const netAmount = +(l.grossAmount + l.vatAmount - l.creditAmount).toFixed(2);
  return { ...l, netAmount };
}

export function MonthlyReturnLinesTable({
  lines,
  onChange,
  readOnly,
}: {
  lines: MonthlyReturnLine[];
  onChange: (l: MonthlyReturnLine[]) => void;
  readOnly?: boolean;
}) {
  const update = (i: number, patch: Partial<MonthlyReturnLine>) =>
    onChange(lines.map((l, idx) => (idx === i ? recalc({ ...l, ...patch }) : l)));
  const remove = (i: number) => onChange(lines.filter((_, idx) => idx !== i));
  const add = () =>
    onChange([
      ...lines,
      { id: "ML" + Math.random().toString(36).slice(2, 6), description: "", grossAmount: 0, taxableAmount: 0, exemptAmount: 0, vatAmount: 0, creditAmount: 0, netAmount: 0 },
    ]);

  const NumCell = ({ value, onChange: oc }: { value: number; onChange: (v: number) => void }) =>
    readOnly ? <span className="mono">{formatETB(value)}</span> : (
      <input type="number" className="input-ghost w-full text-right mono" value={value} onChange={(e) => oc(+e.target.value)} />
    );

  return (
    <div className="border border-border rounded-md overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[960px] text-sm">
          <thead className="bg-muted/40 text-xs text-muted-foreground uppercase">
            <tr>
              <th className="text-left px-3 py-2 font-medium">Description</th>
              <th className="text-right px-3 py-2 font-medium w-28">Gross</th>
              <th className="text-right px-3 py-2 font-medium w-28">Taxable</th>
              <th className="text-right px-3 py-2 font-medium w-28">Exempt</th>
              <th className="text-right px-3 py-2 font-medium w-28">VAT</th>
              <th className="text-right px-3 py-2 font-medium w-28">Credit</th>
              <th className="text-right px-3 py-2 font-medium w-32">Net</th>
              {!readOnly && <th className="w-10" />}
            </tr>
          </thead>
          <tbody>
            {lines.length === 0 && (
              <tr>
                <td colSpan={readOnly ? 7 : 8} className="px-3 py-6 text-center text-muted-foreground">
                  No lines yet.
                </td>
              </tr>
            )}
            {lines.map((l, i) => (
              <tr key={l.id} className="border-t border-border">
                <td className="px-3 py-2">
                  {readOnly ? (l.description || "—") : (
                    <input className="input-ghost w-full" value={l.description} onChange={(e) => update(i, { description: e.target.value })} />
                  )}
                </td>
                <td className="px-3 py-2 text-right"><NumCell value={l.grossAmount} onChange={(v) => update(i, { grossAmount: v })} /></td>
                <td className="px-3 py-2 text-right"><NumCell value={l.taxableAmount} onChange={(v) => update(i, { taxableAmount: v })} /></td>
                <td className="px-3 py-2 text-right"><NumCell value={l.exemptAmount} onChange={(v) => update(i, { exemptAmount: v })} /></td>
                <td className="px-3 py-2 text-right"><NumCell value={l.vatAmount} onChange={(v) => update(i, { vatAmount: v })} /></td>
                <td className="px-3 py-2 text-right"><NumCell value={l.creditAmount} onChange={(v) => update(i, { creditAmount: v })} /></td>
                <td className="px-3 py-2 text-right mono font-medium">{formatETB(l.netAmount)}</td>
                {!readOnly && (
                  <td className="px-2 py-2 text-right">
                    <button onClick={() => remove(i)} className="text-muted-foreground hover:text-destructive" aria-label="Remove">
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!readOnly && (
        <div className="border-t border-border bg-muted/20 px-4 py-2">
          <button onClick={add} className="inline-flex items-center gap-1 text-sm text-accent hover:underline">
            <Plus className="h-3.5 w-3.5" /> Add line
          </button>
        </div>
      )}
    </div>
  );
}