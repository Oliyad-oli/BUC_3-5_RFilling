// ──────────────────────────────────────────────
// mock-data.ts — ERA E-Filing demo types & seed
// ──────────────────────────────────────────────

// ── Return Status ──
export type ReturnStatus =
  | "DRAFT"
  | "CALCULATING"
  | "CALCULATION_FAILED" // frontend-only transient state for demo
  | "ACCEPTED"
  | "POSTED_TO_LEDGER"
  | "UNDER_VALIDATION"
  | "COMPLETED"
  | "MANUAL_REVIEW"
  | "FRAUD_CONFIRMED"
  | "AMENDMENT_DRAFT"
  | "AMENDMENT_CALCULATING"
  | "AMENDMENT_ACCEPTED"
  | "AMENDMENT_POSTED";

// ── Filing Period Status (backend-aligned) ──
export type FilingPeriodStatus = "FUTURE" | "OPEN" | "DUE" | "OVERDUE" | "FILED";

// ── Severity & Review ──
export type Severity = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
export type ReviewType =
  | "RISK_FRAUD"
  | "POST_LEDGER_RULE_FAIL"
  | "POST_LEDGER_CLAIM_MISMATCH"
  | "ITERATION_CAP"
  | "CALC_FAILURE"
  | "AMENDMENT_ANOMALY";
export type ReviewItemStatus = "OPEN" | "RESOLVED";

// ── Officer Decisions (backend-aligned) ──
export type OfficerDecision = "CLEAR" | "REQUEST_AMENDMENT" | "CONFIRM_FRAUD";

// ── Legacy alias kept for backward compat during migration ──
export type ResolutionAction = OfficerDecision;

// ── Risk Level ──
export type RiskLevel = "HIGH" | "MEDIUM" | "LOW";

// ── Line Item ──
export interface LineItem {
  id: string;
  scheduleCode: string;
  lineCode: string;
  displayOrder: number;
  amount: number;
  currency: string;
  lineTextValue?: string;
  entrySpecificData?: string;
  origin: "USER_ENTERED" | "ENGINE_COMPUTED" | "OFFICER_OVERRIDE";
  calculatedInIterationNo?: number;
}

// ── Schedule ──
export interface Schedule {
  scheduleCode: string;
  label: string;
  items: LineItem[];
}

// ── Calculation Iteration ──
export interface CalculationIteration {
  iterationNo: number;
  status: "COMPLETED" | "FAILED";
  inputHash: string;
  outputHash?: string;
  errorPayload?: string;
  durationMs: number;
  accepted?: boolean; // true when taxpayer explicitly accepts this iteration
}

// ── Audit Event ──
export interface AuditEvent {
  time: string;
  label: string;
  actor: "TAXPAYER" | "SYSTEM" | "OFFICER" | "PORTAL";
  detail?: string;
}

// ── Tax Return (main aggregate) ──
export interface TaxReturn {
  id: string;
  tin: string;
  party: string;
  taxType: "VAT" | "Income Tax" | "Withholding Tax" | "PAYE" | "Excise Tax";
  filingPeriodId?: string;
  period: string;
  channel: "PORTAL" | "API" | "BATCH";
  status: ReturnStatus;
  netTax: number | null;
  grossTax?: number;
  inputCredit?: number;
  schedules: Schedule[];
  calculationIterations?: CalculationIteration[];
  filingDeadline?: string;
  paymentDueDate?: string;
  ledgerEntry?: string;
  riskScore?: number;
  riskLevel?: RiskLevel;
  rulePassed?: boolean;
  reviewId?: string;
  externalCaseId?: string; // set on FRAUD_CONFIRMED
  events: AuditEvent[];
  createdAt: string;
  // Amendment tracking
  amendmentReason?: string;
  amendmentHistory?: AmendmentRecord[];
}

// ── Taxpayer ──
export interface Taxpayer {
  tin: string;
  party: string;
  status: "ACTIVE" | "SUSPENDED" | "DEREGISTERED";
  registeredAt: string;
  taxTypes: string[];
}

// ── Taxpayer Obligation ──
export interface TaxpayerObligation {
  id: string;
  tin: string;
  taxType: string;
  frequency: "MONTHLY" | "QUARTERLY" | "ANNUAL";
  effectiveFrom: string;
  effectiveTo?: string; // set on deregistration/closure
  status: "ACTIVE" | "CLOSED";
}

// ── Filing Period ──
export interface FilingPeriod {
  id: string;
  tin: string;
  taxTypeCode: "VAT" | "INCOME_TAX" | "WHT" | "PAYE";
  taxTypeLabel: string;
  periodLabel: string;
  coversFrom: string;
  coversTo: string;
  dueDate: string;
  status: FilingPeriodStatus;
  taxReturnId?: string;
  filedAt?: string;
  isPartial?: boolean;
}

// ── Compliance Check ──
export interface ComplianceCheck {
  id: string;
  returnId: string;
  riskLevel: RiskLevel;
  riskScore: number;
  riskIndicators: string[];
  rulePassed: boolean;
  ruleFailures?: string[];
  outcome: "COMPLETED" | "MANUAL_REVIEW";
  checkedAt: string;
}

// ── Review Case (officer review item) ──
export interface ReviewCase {
  id: string;
  returnId: string;
  tin: string;
  party: string;
  taxType: string;
  period: string;
  netTax: number;
  reviewType: ReviewType;
  severity: Severity;
  status: ReviewItemStatus;
  evidencePayload: {
    riskScore?: number;
    indicators: string[];
    ruleResults?: string[];
  };
  assignedOfficer?: string;
  workflowInstanceId: string;
  createdAt: string;
}

// ── Decision Record ──
export interface DecisionRecord {
  id: string;
  caseId: string;
  tin: string;
  party: string;
  period: string;
  decision: OfficerDecision;
  officer: string;
  decidedAt: string;
  externalCaseId?: string; // set on CONFIRM_FRAUD
  narrative?: string;
}

// ── Penalty Exposure ──
export interface PenaltyExposure {
  id: string;
  filingPeriodId: string;
  tin: string;
  taxType: string;
  periodLabel: string;
  daysLate: number;
  estimatedPenalty: number;
  status: "ACCRUING" | "ASSESSED" | "PAID";
}

// ── Amendment Record ──
export interface AmendmentRecord {
  id: string;
  returnId: string;
  reason: string;
  previousNetTax: number;
  amendedNetTax: number;
  delta: number;
  status: "OPEN" | "COMPLETED";
  startedAt: string;
  completedAt?: string;
}

// ── Outbox Entry ──
export interface OutboxEntry {
  id: string;
  topic: string;
  entity: string;
  status: "PENDING" | "SENT" | "FAILED";
  tries: number;
  lastError?: string;
  createdAt?: string;
}

// ── Notification ──
export interface DemoNotification {
  id: string;
  kind: "info" | "success" | "warning";
  text: string;
  time: string;
  unread: boolean;
}

// ══════════════════════════════════════════════
// SEED DATA
// ══════════════════════════════════════════════

export const TAXPAYER: Taxpayer = {
  tin: "TIN-2024-001842",
  party: "Addis Trading PLC",
  status: "ACTIVE",
  registeredAt: "2024-09-15",
  taxTypes: ["VAT", "INCOME_TAX", "PAYE", "WHT"],
};

// ── Obligations ──
export const initialObligations: TaxpayerObligation[] = [
  {
    id: "OBL-001",
    tin: "TIN-2024-001842",
    taxType: "VAT",
    frequency: "MONTHLY",
    effectiveFrom: "2024-10-01",
    status: "ACTIVE",
  },
  {
    id: "OBL-002",
    tin: "TIN-2024-001842",
    taxType: "INCOME_TAX",
    frequency: "QUARTERLY",
    effectiveFrom: "2024-10-01",
    status: "ACTIVE",
  },
  {
    id: "OBL-003",
    tin: "TIN-2024-001842",
    taxType: "PAYE",
    frequency: "MONTHLY",
    effectiveFrom: "2024-10-01",
    status: "ACTIVE",
  },
  {
    id: "OBL-004",
    tin: "TIN-2024-001842",
    taxType: "WHT",
    frequency: "MONTHLY",
    effectiveFrom: "2024-10-01",
    effectiveTo: "2026-03-31",
    status: "CLOSED",
  },
];

// ── Returns ──
export const initialReturns: TaxReturn[] = [
  {
    id: "TR-2026-06-001",
    tin: "TIN-2024-001842",
    party: "Addis Trading PLC",
    taxType: "VAT",
    filingPeriodId: "FP-2026-008",
    period: "Jun 2026",
    channel: "PORTAL",
    status: "MANUAL_REVIEW",
    netTax: 61425,
    grossTax: 94500,
    inputCredit: 33075,
    schedules: [
      {
        scheduleCode: "VAT_OUTPUT",
        label: "Sales & Output VAT",
        items: [
          {
            id: "L1",
            scheduleCode: "VAT_OUTPUT",
            lineCode: "VAT-OUT-001",
            displayOrder: 1,
            amount: 450000,
            currency: "ETB",
            lineTextValue: "Product Sales - Goods",
            entrySpecificData: '{"ratePercent":15,"taxRole":"OUTPUT"}',
            origin: "USER_ENTERED",
          },
          {
            id: "L2",
            scheduleCode: "VAT_OUTPUT",
            lineCode: "VAT-OUT-002",
            displayOrder: 2,
            amount: 180000,
            currency: "ETB",
            lineTextValue: "Service Revenue",
            entrySpecificData: '{"ratePercent":15,"taxRole":"OUTPUT"}',
            origin: "USER_ENTERED",
          },
          {
            id: "L3",
            scheduleCode: "VAT_OUTPUT",
            lineCode: "VAT-OUT-003",
            displayOrder: 3,
            amount: 95000,
            currency: "ETB",
            lineTextValue: "Export Sales",
            entrySpecificData: '{"ratePercent":0,"taxRole":"OUTPUT"}',
            origin: "USER_ENTERED",
          },
          {
            id: "L6",
            scheduleCode: "VAT_OUTPUT",
            lineCode: "VAT-OUT-TAX",
            displayOrder: 4,
            amount: 94500,
            currency: "ETB",
            lineTextValue: "Output VAT computed by rule package",
            entrySpecificData: '{"taxRole":"COMPUTED"}',
            origin: "ENGINE_COMPUTED",
            calculatedInIterationNo: 1,
          },
        ],
      },
      {
        scheduleCode: "VAT_INPUT",
        label: "Purchases & Input VAT",
        items: [
          {
            id: "L4",
            scheduleCode: "VAT_INPUT",
            lineCode: "VAT-IN-001",
            displayOrder: 1,
            amount: 220000,
            currency: "ETB",
            lineTextValue: "Raw Materials",
            entrySpecificData: '{"ratePercent":15,"taxRole":"INPUT"}',
            origin: "USER_ENTERED",
          },
          {
            id: "L5",
            scheduleCode: "VAT_INPUT",
            lineCode: "VAT-IN-002",
            displayOrder: 2,
            amount: 15500,
            currency: "ETB",
            lineTextValue: "Office Supplies",
            entrySpecificData: '{"ratePercent":15,"taxRole":"INPUT"}',
            origin: "USER_ENTERED",
          },
          {
            id: "L7",
            scheduleCode: "VAT_INPUT",
            lineCode: "VAT-IN-CREDIT",
            displayOrder: 3,
            amount: 33075,
            currency: "ETB",
            lineTextValue: "Input credit computed by rule package",
            entrySpecificData: '{"taxRole":"COMPUTED"}',
            origin: "ENGINE_COMPUTED",
            calculatedInIterationNo: 1,
          },
        ],
      },
    ],
    calculationIterations: [
      {
        iterationNo: 1,
        status: "COMPLETED",
        accepted: true,
        inputHash: "sha256:6fd6c0b5e9a7c1f09ec90f03e6a6b98f3b90e7f8e0a8d2c9a637b5d8a6d4b221",
        outputHash: "sha256:9870cdb43681fb9162c78284f2e89d403bf7c111233ad2a6095a9aa8c13d6e08",
        durationMs: 418,
      },
    ],
    ledgerEntry: "LED-2026-00483",
    riskScore: 0.82,
    riskLevel: "HIGH",
    rulePassed: true,
    reviewId: "REV-2026-001842-07",
    createdAt: "07 May 2026",
    events: [
      { time: "14:30", label: "Draft created", actor: "PORTAL" },
      { time: "14:35", label: "Line items added", actor: "TAXPAYER" },
      { time: "14:37", label: "Calculation requested — iteration 1", actor: "TAXPAYER" },
      { time: "14:37", label: "Calculation iteration 1 completed", actor: "SYSTEM" },
      { time: "14:38", label: "Calculation accepted by taxpayer", actor: "TAXPAYER" },
      { time: "14:38", label: "Return submitted — calendar dates resolved", actor: "SYSTEM" },
      { time: "14:38", label: "Posted to ledger", actor: "SYSTEM", detail: "LED-2026-00483" },
      { time: "14:39", label: "Validation started", actor: "SYSTEM" },
      { time: "14:39", label: "Risk engine: HIGH (0.82)", actor: "SYSTEM", detail: "5 indicators" },
      { time: "14:39", label: "Rule engine: PASSED", actor: "SYSTEM" },
      { time: "14:39", label: "→ MANUAL_REVIEW — officer review item created", actor: "SYSTEM" },
    ],
  },
  {
    id: "TR-2026-05-001",
    tin: "TIN-2024-001842",
    party: "Addis Trading PLC",
    taxType: "VAT",
    filingPeriodId: "FP-2026-009",
    period: "May 2026",
    channel: "PORTAL",
    status: "COMPLETED",
    netTax: 62100,
    grossTax: 89000,
    inputCredit: 26900,
    riskScore: 0.31,
    riskLevel: "LOW",
    rulePassed: true,
    schedules: [],
    calculationIterations: [
      {
        iterationNo: 1,
        status: "COMPLETED",
        accepted: true,
        inputHash: "sha256:aabb01",
        outputHash: "sha256:aabb02",
        durationMs: 312,
      },
    ],
    ledgerEntry: "LED-2026-00401",
    createdAt: "05 May 2026",
    events: [
      { time: "09:00", label: "Draft created", actor: "PORTAL" },
      { time: "09:15", label: "Calculation accepted", actor: "TAXPAYER" },
      { time: "09:16", label: "Posted to ledger", actor: "SYSTEM", detail: "LED-2026-00401" },
      { time: "09:16", label: "Validation passed — COMPLETED", actor: "SYSTEM" },
    ],
  },
  {
    id: "TR-2026-04-001",
    tin: "TIN-2024-001842",
    party: "Addis Trading PLC",
    taxType: "Income Tax",
    filingPeriodId: "FP-2026-010",
    period: "Q1 2026",
    channel: "PORTAL",
    status: "COMPLETED",
    netTax: 198750,
    grossTax: 265000,
    inputCredit: 66250,
    riskScore: 0.22,
    riskLevel: "LOW",
    rulePassed: true,
    schedules: [],
    calculationIterations: [
      {
        iterationNo: 1,
        status: "COMPLETED",
        accepted: true,
        inputHash: "sha256:ccdd01",
        outputHash: "sha256:ccdd02",
        durationMs: 520,
      },
    ],
    ledgerEntry: "LED-2026-00320",
    createdAt: "12 Apr 2026",
    events: [
      { time: "10:00", label: "Draft created", actor: "PORTAL" },
      { time: "10:30", label: "Calculation accepted", actor: "TAXPAYER" },
      { time: "10:31", label: "Posted to ledger", actor: "SYSTEM", detail: "LED-2026-00320" },
      { time: "10:31", label: "Validation passed — COMPLETED", actor: "SYSTEM" },
    ],
  },
  {
    id: "TR-2026-02-001",
    tin: "TIN-2024-001842",
    party: "Addis Trading PLC",
    taxType: "VAT",
    filingPeriodId: "FP-2026-012",
    period: "Feb 2026",
    channel: "PORTAL",
    status: "COMPLETED",
    netTax: 55200,
    grossTax: 78000,
    inputCredit: 22800,
    riskScore: 0.18,
    riskLevel: "LOW",
    rulePassed: true,
    schedules: [],
    calculationIterations: [
      {
        iterationNo: 1,
        status: "COMPLETED",
        accepted: true,
        inputHash: "sha256:eeff01",
        outputHash: "sha256:eeff02",
        durationMs: 290,
      },
    ],
    ledgerEntry: "LED-2026-00218",
    createdAt: "10 Feb 2026",
    events: [],
  },
];

// ── Filing Periods (backend-aligned statuses) ──
export const initialFilingPeriods: FilingPeriod[] = [
  // FUTURE — not yet open for filing
  {
    id: "FP-2026-001",
    tin: "TIN-2024-001842",
    taxTypeCode: "VAT",
    taxTypeLabel: "VAT",
    periodLabel: "Jul 2026",
    coversFrom: "2026-07-01",
    coversTo: "2026-07-31",
    dueDate: "2026-08-15",
    status: "FUTURE",
  },
  {
    id: "FP-2026-002",
    tin: "TIN-2024-001842",
    taxTypeCode: "PAYE",
    taxTypeLabel: "PAYE",
    periodLabel: "Jul 2026",
    coversFrom: "2026-07-01",
    coversTo: "2026-07-31",
    dueDate: "2026-08-05",
    status: "FUTURE",
  },
  // OPEN — coverage started, deadline not passed
  {
    id: "FP-2026-003",
    tin: "TIN-2024-001842",
    taxTypeCode: "VAT",
    taxTypeLabel: "VAT",
    periodLabel: "May 2026",
    coversFrom: "2026-05-01",
    coversTo: "2026-05-31",
    dueDate: "2026-06-15",
    status: "OPEN",
  },
  {
    id: "FP-2026-004",
    tin: "TIN-2024-001842",
    taxTypeCode: "PAYE",
    taxTypeLabel: "PAYE",
    periodLabel: "May 2026",
    coversFrom: "2026-05-01",
    coversTo: "2026-05-31",
    dueDate: "2026-06-30",
    status: "OPEN",
  },
  // DUE — coverage ended, deadline approaching
  {
    id: "FP-2026-006",
    tin: "TIN-2024-001842",
    taxTypeCode: "PAYE",
    taxTypeLabel: "PAYE",
    periodLabel: "Apr 2026",
    coversFrom: "2026-04-01",
    coversTo: "2026-04-30",
    dueDate: "2026-06-15",
    status: "DUE",
  },
  // OVERDUE — deadline passed, not filed
  {
    id: "FP-2026-005",
    tin: "TIN-2024-001842",
    taxTypeCode: "WHT",
    taxTypeLabel: "Withholding Tax",
    periodLabel: "Apr 2026",
    coversFrom: "2026-04-01",
    coversTo: "2026-04-30",
    dueDate: "2026-05-20",
    status: "OVERDUE",
  },
  {
    id: "FP-2026-007",
    tin: "TIN-2024-001842",
    taxTypeCode: "INCOME_TAX",
    taxTypeLabel: "Income Tax",
    periodLabel: "Q1 2026 (Jan–Mar)",
    coversFrom: "2026-01-01",
    coversTo: "2026-03-31",
    dueDate: "2026-04-30",
    status: "OVERDUE",
  },
  // In-progress (OPEN with linked return — NOT using period status DRAFT)
  {
    id: "FP-2026-008",
    tin: "TIN-2024-001842",
    taxTypeCode: "VAT",
    taxTypeLabel: "VAT",
    periodLabel: "Jun 2026",
    coversFrom: "2026-06-01",
    coversTo: "2026-06-30",
    dueDate: "2026-07-15",
    status: "OPEN",
    taxReturnId: "TR-2026-06-001",
  },
  // FILED — return completed
  {
    id: "FP-2026-009",
    tin: "TIN-2024-001842",
    taxTypeCode: "VAT",
    taxTypeLabel: "VAT",
    periodLabel: "May 2026",
    coversFrom: "2026-05-01",
    coversTo: "2026-05-31",
    dueDate: "2026-06-15",
    status: "FILED",
    taxReturnId: "TR-2026-05-001",
    filedAt: "2026-05-07",
  },
  {
    id: "FP-2026-010",
    tin: "TIN-2024-001842",
    taxTypeCode: "INCOME_TAX",
    taxTypeLabel: "Income Tax",
    periodLabel: "Q1 2026",
    coversFrom: "2026-01-01",
    coversTo: "2026-03-31",
    dueDate: "2026-04-30",
    status: "FILED",
    taxReturnId: "TR-2026-04-001",
    filedAt: "2026-04-12",
  },
  {
    id: "FP-2026-012",
    tin: "TIN-2024-001842",
    taxTypeCode: "VAT",
    taxTypeLabel: "VAT",
    periodLabel: "Feb 2026",
    coversFrom: "2026-02-01",
    coversTo: "2026-02-28",
    dueDate: "2026-03-15",
    status: "FILED",
    taxReturnId: "TR-2026-02-001",
    filedAt: "2026-02-10",
  },
];

// ── Compliance Checks ──
export const initialComplianceChecks: ComplianceCheck[] = [
  {
    id: "CC-001",
    returnId: "TR-2026-06-001",
    riskLevel: "HIGH",
    riskScore: 0.82,
    riskIndicators: [
      "VAT ratio anomaly",
      "Input credit > 40%",
      "Rapid growth vs prior 3 periods",
      "Export zero-rate without certificate",
      "Entity age < 18 months",
    ],
    rulePassed: true,
    outcome: "MANUAL_REVIEW",
    checkedAt: "2026-05-08 14:39",
  },
  {
    id: "CC-002",
    returnId: "TR-2026-05-001",
    riskLevel: "LOW",
    riskScore: 0.31,
    riskIndicators: [],
    rulePassed: true,
    outcome: "COMPLETED",
    checkedAt: "2026-05-07 09:16",
  },
  {
    id: "CC-003",
    returnId: "TR-2026-04-001",
    riskLevel: "LOW",
    riskScore: 0.22,
    riskIndicators: [],
    rulePassed: true,
    outcome: "COMPLETED",
    checkedAt: "2026-04-12 10:31",
  },
  {
    id: "CC-004",
    returnId: "TR-2026-02-001",
    riskLevel: "LOW",
    riskScore: 0.18,
    riskIndicators: [],
    rulePassed: true,
    outcome: "COMPLETED",
    checkedAt: "2026-02-10 11:05",
  },
];

// ── Review Cases ──
export const initialReviewCases: ReviewCase[] = [
  {
    id: "REV-2026-001842-07",
    returnId: "TR-2026-06-001",
    tin: "TIN-2024-001842",
    party: "Addis Trading PLC",
    taxType: "VAT",
    period: "Jun 2026",
    netTax: 61425,
    reviewType: "RISK_FRAUD",
    severity: "CRITICAL",
    status: "OPEN",
    assignedOfficer: "Abebe Girma",
    workflowInstanceId: "WF-POSTLEDGER-2026-001842-07",
    evidencePayload: {
      riskScore: 0.82,
      indicators: [
        "Output VAT / turnover ratio anomaly",
        "Input credit exceeds 40% of output",
        "Rapid increase vs prior 3 periods",
        "Export zero-rate without certificate ref",
        "Entity age < 18 months — elevated risk",
      ],
      ruleResults: ["POST_LEDGER_RULES_PASSED"],
    },
    createdAt: "08 May 2026 14:39",
  },
  {
    id: "REV-2026-001790-06",
    returnId: "TR-2026-05-088",
    tin: "TIN-2024-001790",
    party: "Bole Importers Ltd",
    taxType: "VAT",
    period: "May 2026",
    netTax: 198300,
    reviewType: "POST_LEDGER_CLAIM_MISMATCH",
    severity: "HIGH",
    status: "OPEN",
    assignedOfficer: "Sara Tadesse",
    workflowInstanceId: "WF-POSTLEDGER-2026-001790-06",
    evidencePayload: {
      riskScore: 0.74,
      indicators: [
        "Large input credit refund claim",
        "Multiple high-value import entries",
        "Mismatch with customs records",
      ],
      ruleResults: ["CUSTOMS_CLAIM_MISMATCH"],
    },
    createdAt: "07 May 2026 09:15",
  },
  {
    id: "REV-2026-001655-05",
    returnId: "TR-2026-04-055",
    tin: "TIN-2024-001655",
    party: "Merkato General Trading",
    taxType: "Income Tax",
    period: "Apr 2026",
    netTax: 45000,
    reviewType: "POST_LEDGER_RULE_FAIL",
    severity: "MEDIUM",
    status: "OPEN",
    workflowInstanceId: "WF-POSTLEDGER-2026-001655-05",
    evidencePayload: {
      riskScore: 0.61,
      indicators: ["Declared income below industry median", "Expense ratio exceeds threshold"],
      ruleResults: ["INCOME_EXPENSE_RATIO_WARNING"],
    },
    createdAt: "06 May 2026 11:30",
  },
];

// ── Decisions (backend-aligned) ──
export const initialDecisions: DecisionRecord[] = [
  {
    id: "D1",
    caseId: "REV-001840",
    tin: "TIN-001840",
    party: "Yeka Traders",
    period: "May 2026",
    decision: "CLEAR",
    officer: "Abebe Girma",
    decidedAt: "07 May 2026",
    narrative: "All indicators within acceptable range after manual review.",
  },
  {
    id: "D2",
    caseId: "REV-001812",
    tin: "TIN-001812",
    party: "Piassa Import",
    period: "Apr 2026",
    decision: "REQUEST_AMENDMENT",
    officer: "Sara Tadesse",
    decidedAt: "05 May 2026",
    narrative: "Input credit claims require supporting documentation.",
  },
  {
    id: "D3",
    caseId: "REV-001789",
    tin: "TIN-001789",
    party: "Bole Logistics",
    period: "Apr 2026",
    decision: "CONFIRM_FRAUD",
    officer: "Dawit Mengistu",
    decidedAt: "04 May 2026",
    externalCaseId: "CASE-2026-118",
    narrative: "Confirmed fraudulent invoices from phantom supplier.",
  },
];

// ── Penalties ──
export const initialPenalties: PenaltyExposure[] = [
  {
    id: "PEN-001",
    filingPeriodId: "FP-2026-005",
    tin: "TIN-2024-001842",
    taxType: "WHT",
    periodLabel: "Apr 2026",
    daysLate: 22,
    estimatedPenalty: 5500,
    status: "ACCRUING",
  },
  {
    id: "PEN-002",
    filingPeriodId: "FP-2026-007",
    tin: "TIN-2024-001842",
    taxType: "Income Tax",
    periodLabel: "Q1 2026",
    daysLate: 42,
    estimatedPenalty: 19875,
    status: "ACCRUING",
  },
];

// ── Outbox (backend-aligned: SENT not DISPATCHED) ──
export const initialOutbox: OutboxEntry[] = [
  {
    id: "OUT-00483",
    topic: "TaxReturnSubmittedEvent",
    entity: "TR-2026-06-001",
    status: "SENT",
    tries: 1,
    createdAt: "2026-05-08 14:38",
  },
  {
    id: "OUT-00484",
    topic: "TaxReturnPostedToLedgerEvent",
    entity: "TR-2026-06-001",
    status: "SENT",
    tries: 1,
    createdAt: "2026-05-08 14:38",
  },
  {
    id: "OUT-00485",
    topic: "PostLedgerValidationCompletedEvent",
    entity: "TR-2026-06-001",
    status: "PENDING",
    tries: 0,
    createdAt: "2026-05-08 14:39",
  },
  {
    id: "OUT-00479",
    topic: "TaxReturnDraftedEvent",
    entity: "TR-2026-06-001",
    status: "SENT",
    tries: 1,
    createdAt: "2026-05-08 14:30",
  },
  {
    id: "OUT-00471",
    topic: "PostLedgerValidationFailedEvent",
    entity: "TR-2026-04-055",
    status: "FAILED",
    tries: 5,
    lastError: "Connection timeout to event broker after 30000ms",
    createdAt: "2026-05-06 11:35",
  },
];

// ── Helpers ──
export const formatETB = (n: number | null | undefined) => {
  if (n === null || n === undefined) return "—";
  return "ETB " + n.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

export const formatDate = (d: string) => {
  if (!d) return "—";
  return d;
};
