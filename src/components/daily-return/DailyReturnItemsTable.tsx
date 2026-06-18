import { Plus, Trash2 } from "lucide-react";
import type { DailyReturnItem } from "@/lib/filing-store";
import { formatETB } from "@/lib/mock-data";

interface Props {
  items: DailyReturnItem[];
  onChange: (items: DailyReturnItem[]) => void;
  readOnly?: boolean;
}

export function DailyReturnItemsTable({ items, onChange, readOnly }: Props) {
  const recalc = (it: DailyReturnItem): DailyReturnItem => {
    const amount = +(it.quantity * it.unitPrice).toFixed(2);
    const taxAmount = it.taxable ? +(amount * (it.taxRate / 100)).toFixed(2) : 0;
    return { ...it, amount, taxAmount };
  };
  const update = (i: number, patch: Partial<DailyReturnItem>) =>
    onChange(items.map((it, idx) => (idx === i ? recalc({ ...it, ...patch }) : it)));
  const remove = (i: number) => onChange(items.filter((_, idx) => idx !== i));
  const add = () =>
    onChange([
      ...items,
      { id: "I" + Math.random().toString(36).slice(2, 6), description: "", quantity: 1, unitPrice: 0, amount: 0, taxable: true, taxRate: 15, taxAmount: 0 },
    ]);

  return (
    <div className="border border-border rounded-md overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[820px] text-sm">
          <thead className="bg-muted/40 text-xs text-muted-foreground uppercase">
            <tr>
              <th className="text-left px-4 py-2 font-medium">Description</th>
              <th className="text-right px-4 py-2 font-medium w-24">Qty</th>
              <th className="text-right px-4 py-2 font-medium w-32">Unit Price</th>
              <th className="text-right px-4 py-2 font-medium w-32">Amount</th>
              <th className="text-center px-4 py-2 font-medium w-20">Taxable</th>
              <th className="text-right px-4 py-2 font-medium w-20">Rate %</th>
              <th className="text-right px-4 py-2 font-medium w-32">Tax Amount</th>
              {!readOnly && <th className="w-10" />}
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && (
              <tr>
                <td colSpan={readOnly ? 7 : 8} className="px-4 py-6 text-center text-muted-foreground">
                  No return items yet.
                </td>
              </tr>
            )}
            {items.map((it, i) => (
              <tr key={it.id} className="border-t border-border">
                <td className="px-4 py-2">
                  {readOnly ? (
                    it.description || "—"
                  ) : (
                    <input className="input-ghost w-full" value={it.description} onChange={(e) => update(i, { description: e.target.value })} />
                  )}
                </td>
                <td className="px-4 py-2 text-right">
                  {readOnly ? (
                    <span className="mono">{it.quantity}</span>
                  ) : (
                    <input type="number" className="input-ghost w-full text-right mono" value={it.quantity} onChange={(e) => update(i, { quantity: +e.target.value })} />
                  )}
                </td>
                <td className="px-4 py-2 text-right">
                  {readOnly ? (
                    <span className="mono">{formatETB(it.unitPrice)}</span>
                  ) : (
                    <input type="number" className="input-ghost w-full text-right mono" value={it.unitPrice} onChange={(e) => update(i, { unitPrice: +e.target.value })} />
                  )}
                </td>
                <td className="px-4 py-2 text-right mono">{formatETB(it.amount)}</td>
                <td className="px-4 py-2 text-center">
                  {readOnly ? (
                    <span className="text-xs">{it.taxable ? "Yes" : "No"}</span>
                  ) : (
                    <input type="checkbox" className="accent-accent" checked={it.taxable} onChange={(e) => update(i, { taxable: e.target.checked })} />
                  )}
                </td>
                <td className="px-4 py-2 text-right">
                  {readOnly ? (
                    <span className="mono">{it.taxRate}</span>
                  ) : (
                    <input type="number" className="input-ghost w-full text-right mono" value={it.taxRate} disabled={!it.taxable} onChange={(e) => update(i, { taxRate: +e.target.value })} />
                  )}
                </td>
                <td className="px-4 py-2 text-right mono">{formatETB(it.taxAmount)}</td>
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
            <Plus className="h-3.5 w-3.5" /> Add return item
          </button>
        </div>
      )}
    </div>
  );
}