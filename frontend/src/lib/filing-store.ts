import { create } from "zustand";

// ── Shared types ──
export type FilingStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "VALIDATED"
  | "ACKNOWLEDGED"
  | "CROSS_MATCH"
  | "RISK_SCORING"
  | "VALIDATION"
  | "WORKFLOW_ROUTING"
  | "AMENDED"
  | "CANCELLED";

export interface Attachment {
  id: string;
  name: string;
  size: number;
  mime: string;
}

export interface TimelineEvent {
  time: string;
  label: string;
  actor: "TAXPAYER" | "SYSTEM" | "OFFICER" | "PORTAL";
  detail?: string;
}

// ── Daily Return ──
export interface DailyReturnItem {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  taxable: boolean;
  taxRate: number;
  taxAmount: number;
}

export interface DailyReturn {
  id: string;
  reference?: string;
  tin: string;
  taxpayerName: string;
  taxType: "VAT" | "Excise Tax" | "Withholding Tax";
  filingPeriod: string;
  businessSector: string;
  submissionDate: string;
  status: FilingStatus;
  items: DailyReturnItem[];
  summary: {
    grossSales: number;
    taxableSales: number;
    exemptSales: number;
    taxAmount: number;
    penalty: number;
    netPayable: number;
  };
  attachments: Attachment[];
  events: TimelineEvent[];
  createdAt: string;
  submittedAt?: string;
  acknowledgedAt?: string;
}

// ── Monthly Return ──
export interface MonthlyReturnLine {
  id: string;
  description: string;
  grossAmount: number;
  taxableAmount: number;
  exemptAmount: number;
  vatAmount: number;
  creditAmount: number;
  netAmount: number;
}

export interface MonthlySchedule {
  id: string;
  code: "SCHEDULE_A" | "SCHEDULE_B" | "SCHEDULE_C";
  label: string;
  description: string;
  amount: number;
}

export interface MonthlyReturn {
  id: string;
  reference?: string;
  tin: string;
  taxpayerName: string;
  taxType: "VAT" | "Income Tax" | "Excise Tax";
  filingPeriod: string;
  businessCategory: string;
  taxOffice: string;
  status: FilingStatus;
  pipelineStage:
    | "NONE"
    | "SUBMITTED"
    | "CROSS_MATCH"
    | "RISK_SCORING"
    | "VALIDATION"
    | "WORKFLOW_ROUTING"
    | "ACKNOWLEDGED";
  lines: MonthlyReturnLine[];
  schedules: MonthlySchedule[];
  attachments: Attachment[];
  summary: {
    grossAmount: number;
    taxableAmount: number;
    totalTax: number;
    credits: number;
    penalty: number;
    interest: number;
    netPayable: number;
  };
  events: TimelineEvent[];
  createdAt: string;
  submittedAt?: string;
  acknowledgedAt?: string;
  amendedFrom?: string;
}

// ── Helpers ──
const now = () => {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
};
const today = () => new Date().toISOString().slice(0, 10);
const rand = (prefix: string) =>
  `${prefix}-${new Date().getFullYear()}-${String(Math.floor(Math.random() * 90000) + 10000)}`;

// ── Seed ──
const seedDaily: DailyReturn[] = [
  {
    id: "DR-2026-10001",
    reference: "ACK-DR-2026-10001",
    tin: "TIN-2024-001842",
    taxpayerName: "Addis Trading PLC",
    taxType: "VAT",
    filingPeriod: "12 Jun 2026",
    businessSector: "Wholesale & Retail Trade",
    submissionDate: "2026-06-12",
    status: "ACKNOWLEDGED",
    items: [
      { id: "I1", description: "Imported textile (rolls)", quantity: 80, unitPrice: 1250, amount: 100000, taxable: true, taxRate: 15, taxAmount: 15000 },
      { id: "I2", description: "Office supplies — exempt", quantity: 40, unitPrice: 350, amount: 14000, taxable: false, taxRate: 0, taxAmount: 0 },
    ],
    summary: { grossSales: 114000, taxableSales: 100000, exemptSales: 14000, taxAmount: 15000, penalty: 0, netPayable: 15000 },
    attachments: [{ id: "A1", name: "invoices-12jun.pdf", size: 184320, mime: "application/pdf" }],
    events: [
      { time: "08:12", label: "Draft created", actor: "PORTAL" },
      { time: "08:34", label: "Submitted", actor: "TAXPAYER" },
      { time: "08:34", label: "Validated by rule engine", actor: "SYSTEM" },
      { time: "08:35", label: "Acknowledgement issued", actor: "SYSTEM", detail: "ACK-DR-2026-10001" },
    ],
    createdAt: "12 Jun 2026",
    submittedAt: "12 Jun 2026 08:34",
    acknowledgedAt: "12 Jun 2026 08:35",
  },
];

const seedMonthly: MonthlyReturn[] = [
  {
    id: "MR-2026-20001",
    reference: "ACK-MR-2026-20001",
    tin: "TIN-2024-001842",
    taxpayerName: "Addis Trading PLC",
    taxType: "VAT",
    filingPeriod: "May 2026",
    businessCategory: "Wholesale & Retail Trade",
    taxOffice: "Addis Ababa LTO",
    status: "ACKNOWLEDGED",
    pipelineStage: "ACKNOWLEDGED",
    lines: [
      { id: "ML1", description: "Standard sales", grossAmount: 1450000, taxableAmount: 1340000, exemptAmount: 110000, vatAmount: 201000, creditAmount: 0, netAmount: 1541000 },
      { id: "ML2", description: "Input VAT on purchases", grossAmount: 620000, taxableAmount: 620000, exemptAmount: 0, vatAmount: 0, creditAmount: 93000, netAmount: 527000 },
    ],
    schedules: [
      { id: "S1", code: "SCHEDULE_A", label: "Schedule A — Sales register", description: "Aggregated taxable supplies", amount: 1340000 },
      { id: "S2", code: "SCHEDULE_B", label: "Schedule B — Purchase register", description: "Allowable input VAT detail", amount: 620000 },
    ],
    attachments: [
      { id: "MA1", name: "sales-may-2026.xlsx", size: 412000, mime: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
      { id: "MA2", name: "purchases-may-2026.pdf", size: 218000, mime: "application/pdf" },
    ],
    summary: { grossAmount: 1450000, taxableAmount: 1340000, totalTax: 201000, credits: 93000, penalty: 0, interest: 0, netPayable: 108000 },
    events: [
      { time: "09:01", label: "Draft created", actor: "PORTAL" },
      { time: "09:45", label: "Submitted", actor: "TAXPAYER" },
      { time: "09:45", label: "Cross-match against Sales registry", actor: "SYSTEM" },
      { time: "09:46", label: "Risk scoring — LOW (0.18)", actor: "SYSTEM" },
      { time: "09:46", label: "Rule validation passed", actor: "SYSTEM" },
      { time: "09:46", label: "Workflow routing — auto-acknowledge", actor: "SYSTEM" },
      { time: "09:46", label: "Acknowledgement issued", actor: "SYSTEM", detail: "ACK-MR-2026-20001" },
    ],
    createdAt: "05 Jun 2026",
    submittedAt: "05 Jun 2026 09:45",
    acknowledgedAt: "05 Jun 2026 09:46",
  },
];

interface FilingState {
  dailyReturns: DailyReturn[];
  monthlyReturns: MonthlyReturn[];

  upsertDailyDraft: (r: DailyReturn) => void;
  submitDaily: (id: string) => DailyReturn | undefined;
  cancelDaily: (id: string) => void;
  getDaily: (id: string) => DailyReturn | undefined;

  upsertMonthlyDraft: (r: MonthlyReturn) => void;
  validateMonthly: (id: string) => void;
  submitMonthly: (id: string) => MonthlyReturn | undefined;
  amendMonthly: (id: string) => string | undefined;
  cancelMonthly: (id: string) => void;
  advanceMonthlyPipeline: (id: string, stage: MonthlyReturn["pipelineStage"]) => void;
  getMonthly: (id: string) => MonthlyReturn | undefined;
}

export const useFiling = create<FilingState>((set, get) => ({
  dailyReturns: seedDaily,
  monthlyReturns: seedMonthly,

  upsertDailyDraft: (r) =>
    set((s) => {
      const exists = s.dailyReturns.some((x) => x.id === r.id);
      return {
        dailyReturns: exists ? s.dailyReturns.map((x) => (x.id === r.id ? r : x)) : [r, ...s.dailyReturns],
      };
    }),
  submitDaily: (id) => {
    const reference = `ACK-${id}`;
    let updated: DailyReturn | undefined;
    set((s) => ({
      dailyReturns: s.dailyReturns.map((r) => {
        if (r.id !== id) return r;
        updated = {
          ...r,
          status: "ACKNOWLEDGED",
          reference,
          submittedAt: `${today()} ${now()}`,
          acknowledgedAt: `${today()} ${now()}`,
          events: [
            ...r.events,
            { time: now(), label: "Submitted", actor: "TAXPAYER" },
            { time: now(), label: "Validated by rule engine", actor: "SYSTEM" },
            { time: now(), label: "Acknowledgement issued", actor: "SYSTEM", detail: reference },
          ],
        };
        return updated;
      }),
    }));
    return updated;
  },
  cancelDaily: (id) =>
    set((s) => ({
      dailyReturns: s.dailyReturns.map((r) =>
        r.id === id
          ? { ...r, status: "CANCELLED", events: [...r.events, { time: now(), label: "Draft cancelled", actor: "TAXPAYER" }] }
          : r,
      ),
    })),
  getDaily: (id) => get().dailyReturns.find((r) => r.id === id),

  upsertMonthlyDraft: (r) =>
    set((s) => {
      const exists = s.monthlyReturns.some((x) => x.id === r.id);
      return {
        monthlyReturns: exists ? s.monthlyReturns.map((x) => (x.id === r.id ? r : x)) : [r, ...s.monthlyReturns],
      };
    }),
  validateMonthly: (id) =>
    set((s) => ({
      monthlyReturns: s.monthlyReturns.map((r) =>
        r.id === id
          ? { ...r, status: "VALIDATED", events: [...r.events, { time: now(), label: "Pre-submit validation passed", actor: "SYSTEM" }] }
          : r,
      ),
    })),
  submitMonthly: (id) => {
    const reference = `ACK-${id}`;
    let updated: MonthlyReturn | undefined;
    set((s) => ({
      monthlyReturns: s.monthlyReturns.map((r) => {
        if (r.id !== id) return r;
        updated = {
          ...r,
          status: "SUBMITTED",
          pipelineStage: "SUBMITTED",
          reference,
          submittedAt: `${today()} ${now()}`,
          events: [...r.events, { time: now(), label: "Submitted", actor: "TAXPAYER" }],
        };
        return updated;
      }),
    }));
    return updated;
  },
  amendMonthly: (id) => {
    const original = get().monthlyReturns.find((r) => r.id === id);
    if (!original) return undefined;
    const newId = rand("MR");
    const draft: MonthlyReturn = {
      ...original,
      id: newId,
      reference: undefined,
      status: "DRAFT",
      pipelineStage: "NONE",
      amendedFrom: original.id,
      submittedAt: undefined,
      acknowledgedAt: undefined,
      events: [{ time: now(), label: `Amendment draft from ${original.id}`, actor: "TAXPAYER" }],
      createdAt: today(),
    };
    set((s) => ({ monthlyReturns: [draft, ...s.monthlyReturns] }));
    return newId;
  },
  cancelMonthly: (id) =>
    set((s) => ({
      monthlyReturns: s.monthlyReturns.map((r) =>
        r.id === id
          ? { ...r, status: "CANCELLED", events: [...r.events, { time: now(), label: "Draft cancelled", actor: "TAXPAYER" }] }
          : r,
      ),
    })),
  advanceMonthlyPipeline: (id, stage) =>
    set((s) => ({
      monthlyReturns: s.monthlyReturns.map((r) => {
        if (r.id !== id) return r;
        const labels: Record<MonthlyReturn["pipelineStage"], string> = {
          NONE: "",
          SUBMITTED: "Submitted",
          CROSS_MATCH: "Cross-match against registries",
          RISK_SCORING: "Risk scoring complete",
          VALIDATION: "Rule validation complete",
          WORKFLOW_ROUTING: "Workflow routed",
          ACKNOWLEDGED: "Acknowledgement issued",
        };
        const isFinal = stage === "ACKNOWLEDGED";
        return {
          ...r,
          pipelineStage: stage,
          status: isFinal ? "ACKNOWLEDGED" : (stage as FilingStatus),
          acknowledgedAt: isFinal ? `${today()} ${now()}` : r.acknowledgedAt,
          events: [...r.events, { time: now(), label: labels[stage], actor: "SYSTEM", detail: isFinal ? r.reference : undefined }],
        };
      }),
    })),
  getMonthly: (id) => get().monthlyReturns.find((r) => r.id === id),
}));

// ── ID factories ──
export const newDailyId = () => `DR-${new Date().getFullYear()}-${String(Math.floor(Math.random() * 90000) + 10000)}`;
export const newMonthlyId = () => `MR-${new Date().getFullYear()}-${String(Math.floor(Math.random() * 90000) + 10000)}`;

export const filingStatusLabel: Record<FilingStatus, string> = {
  DRAFT: "DRAFT",
  SUBMITTED: "SUBMITTED",
  VALIDATED: "VALIDATED",
  ACKNOWLEDGED: "ACKNOWLEDGED",
  CROSS_MATCH: "CROSS MATCH",
  RISK_SCORING: "RISK SCORING",
  VALIDATION: "VALIDATION",
  WORKFLOW_ROUTING: "WORKFLOW ROUTING",
  AMENDED: "AMENDED",
  CANCELLED: "CANCELLED",
};