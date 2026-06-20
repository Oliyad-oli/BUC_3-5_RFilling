import { useMemo } from "react";
import { Field } from "@/components/form-primitives";
import type { DailyReturn } from "@/lib/filing-store";

const TAX_TYPES: DailyReturn["taxType"][] = ["VAT", "Excise Tax", "Withholding Tax"];
const SECTORS = [
  "Wholesale & Retail Trade",
  "Manufacturing",
  "Construction",
  "Hospitality",
  "Transport & Logistics",
  "Financial Services",
  "Agriculture",
];

export interface DailyReturnFormState {
  tin: string;
  taxpayerName: string;
  taxType: DailyReturn["taxType"];
  filingPeriod: string;
  businessSector: string;
  submissionDate: string;
}

export function DailyReturnForm({
  value,
  onChange,
  errors,
  readOnly,
}: {
  value: DailyReturnFormState;
  onChange: (next: DailyReturnFormState) => void;
  errors?: Partial<Record<keyof DailyReturnFormState, string>>;
  readOnly?: boolean;
}) {
  const set = (patch: Partial<DailyReturnFormState>) => onChange({ ...value, ...patch });
  const periodLabel = useMemo(() => {
    if (!value.submissionDate) return value.filingPeriod;
    const d = new Date(value.submissionDate);
    const months = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];
    return `${String(d.getDate()).padStart(2, "0")} ${months[d.getMonth()]} ${d.getFullYear()}`;
  }, [value.submissionDate, value.filingPeriod]);

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
      <Field label="Taxpayer TIN" hint={errors?.tin}>
        <input className="input mono" value={value.tin} onChange={(e) => set({ tin: e.target.value })} disabled={readOnly} />
      </Field>
      <Field label="Taxpayer Name" hint={errors?.taxpayerName}>
        <input className="input" value={value.taxpayerName} onChange={(e) => set({ taxpayerName: e.target.value })} disabled={readOnly} />
      </Field>
      <Field label="Tax Type">
        <select className="input" value={value.taxType} onChange={(e) => set({ taxType: e.target.value as DailyReturn["taxType"] })} disabled={readOnly}>
          {TAX_TYPES.map((t) => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>
      </Field>
      <Field label="Filing Period">
        <input className="input" value={periodLabel} onChange={(e) => set({ filingPeriod: e.target.value })} disabled={readOnly} />
      </Field>
      <Field label="Business Sector">
        <select className="input" value={value.businessSector} onChange={(e) => set({ businessSector: e.target.value })} disabled={readOnly}>
          {SECTORS.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </Field>
      <Field label="Submission Date" hint={errors?.submissionDate}>
        <input type="date" className="input" value={value.submissionDate} onChange={(e) => set({ submissionDate: e.target.value })} disabled={readOnly} />
      </Field>
    </div>
  );
}