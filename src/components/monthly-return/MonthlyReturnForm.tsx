import { Field } from "@/components/form-primitives";
import type { MonthlyReturn } from "@/lib/filing-store";

const TAX_TYPES: MonthlyReturn["taxType"][] = ["VAT", "Income Tax", "Excise Tax"];
const CATEGORIES = ["Large Taxpayer", "Medium Taxpayer", "Small Taxpayer", "Micro Enterprise"];
const OFFICES = ["Addis Ababa LTO", "Addis Ababa MTO 1", "Addis Ababa MTO 2", "Dire Dawa Branch", "Hawassa Branch"];
const PERIODS = [
  "Jan 2026", "Feb 2026", "Mar 2026", "Apr 2026", "May 2026", "Jun 2026",
  "Jul 2026", "Aug 2026", "Sep 2026", "Oct 2026", "Nov 2026", "Dec 2026",
];

export interface MonthlyReturnFormState {
  tin: string;
  taxpayerName: string;
  taxType: MonthlyReturn["taxType"];
  filingPeriod: string;
  businessCategory: string;
  taxOffice: string;
}

export function MonthlyReturnForm({
  value,
  onChange,
  readOnly,
}: {
  value: MonthlyReturnFormState;
  onChange: (v: MonthlyReturnFormState) => void;
  readOnly?: boolean;
}) {
  const set = (patch: Partial<MonthlyReturnFormState>) => onChange({ ...value, ...patch });
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
      <Field label="TIN">
        <input className="input mono" value={value.tin} onChange={(e) => set({ tin: e.target.value })} disabled={readOnly} />
      </Field>
      <Field label="Taxpayer Name">
        <input className="input" value={value.taxpayerName} onChange={(e) => set({ taxpayerName: e.target.value })} disabled={readOnly} />
      </Field>
      <Field label="Tax Type">
        <select className="input" value={value.taxType} onChange={(e) => set({ taxType: e.target.value as MonthlyReturn["taxType"] })} disabled={readOnly}>
          {TAX_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
      </Field>
      <Field label="Filing Period">
        <select className="input" value={value.filingPeriod} onChange={(e) => set({ filingPeriod: e.target.value })} disabled={readOnly}>
          {PERIODS.map((p) => <option key={p} value={p}>{p}</option>)}
        </select>
      </Field>
      <Field label="Business Category">
        <select className="input" value={value.businessCategory} onChange={(e) => set({ businessCategory: e.target.value })} disabled={readOnly}>
          {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
      </Field>
      <Field label="Tax Office">
        <select className="input" value={value.taxOffice} onChange={(e) => set({ taxOffice: e.target.value })} disabled={readOnly}>
          {OFFICES.map((o) => <option key={o} value={o}>{o}</option>)}
        </select>
      </Field>
    </div>
  );
}