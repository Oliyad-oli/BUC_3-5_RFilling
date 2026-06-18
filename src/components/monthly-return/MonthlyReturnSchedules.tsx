import { Plus, Trash2 } from "lucide-react";
import type { MonthlySchedule } from "@/lib/filing-store";
import { formatETB } from "@/lib/mock-data";

const CODES: MonthlySchedule["code"][] = ["SCHEDULE_A", "SCHEDULE_B", "SCHEDULE_C"];
const CODE_LABEL: Record<MonthlySchedule["code"], string> = {
  SCHEDULE_A: "Schedule A — Sales register",
  SCHEDULE_B: "Schedule B — Purchase register",
  SCHEDULE_C: "Schedule C — Adjustments",
};

export function MonthlyReturnSchedules({
  schedules,
  onChange,
  readOnly,
}: {
  schedules: MonthlySchedule[];
  onChange: (s: MonthlySchedule[]) => void;
  readOnly?: boolean;
}) {
  const add = () => {
    const used = new Set(schedules.map((s) => s.code));
    const available = CODES.find((c) => !used.has(c)) ?? "SCHEDULE_C";
    onChange([
      ...schedules,
      { id: "S" + Math.random().toString(36).slice(2, 6), code: available, label: CODE_LABEL[available], description: "", amount: 0 },
    ]);
  };
  const update = (i: number, patch: Partial<MonthlySchedule>) =>
    onChange(schedules.map((s, idx) => (idx === i ? { ...s, ...patch, label: patch.code ? CODE_LABEL[patch.code] : s.label } : s)));
  const remove = (i: number) => onChange(schedules.filter((_, idx) => idx !== i));

  return (
    <div className="space-y-3">
      {schedules.length === 0 && (
        <div className="text-sm text-muted-foreground border border-dashed border-border rounded-md p-6 text-center">
          No schedules added. Use the button below to add Schedule A, B or C.
        </div>
      )}
      {schedules.map((s, i) => (
        <div key={s.id} className="border border-border rounded-md p-4 grid grid-cols-1 md:grid-cols-4 gap-3 items-start">
          <div>
            <div className="text-[11px] uppercase text-muted-foreground tracking-wide mb-1">Schedule</div>
            {readOnly ? (
              <div className="text-sm font-medium">{s.label}</div>
            ) : (
              <select className="input" value={s.code} onChange={(e) => update(i, { code: e.target.value as MonthlySchedule["code"] })}>
                {CODES.map((c) => <option key={c} value={c}>{CODE_LABEL[c]}</option>)}
              </select>
            )}
          </div>
          <div className="md:col-span-2">
            <div className="text-[11px] uppercase text-muted-foreground tracking-wide mb-1">Description</div>
            {readOnly ? (
              <div className="text-sm">{s.description || "—"}</div>
            ) : (
              <input className="input" value={s.description} onChange={(e) => update(i, { description: e.target.value })} />
            )}
          </div>
          <div>
            <div className="text-[11px] uppercase text-muted-foreground tracking-wide mb-1">Amount</div>
            {readOnly ? (
              <div className="mono text-sm">{formatETB(s.amount)}</div>
            ) : (
              <div className="flex gap-2">
                <input type="number" className="input text-right mono" value={s.amount} onChange={(e) => update(i, { amount: +e.target.value })} />
                <button onClick={() => remove(i)} className="btn-ghost px-2" aria-label="Remove schedule">
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
            )}
          </div>
        </div>
      ))}
      {!readOnly && (
        <button onClick={add} className="inline-flex items-center gap-1 text-sm text-accent hover:underline">
          <Plus className="h-3.5 w-3.5" /> Add schedule
        </button>
      )}
    </div>
  );
}