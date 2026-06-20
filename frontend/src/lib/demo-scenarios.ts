// ──────────────────────────────────────────────
// demo-scenarios.ts — Resettable demo journeys
// ──────────────────────────────────────────────

import {
  initialReturns,
  initialFilingPeriods,
  initialReviewCases,
  initialDecisions,
  initialOutbox,
  initialObligations,
  initialComplianceChecks,
  initialPenalties,
  type TaxReturn,
  type FilingPeriod,
  type ReviewCase,
  type DecisionRecord,
  type OutboxEntry,
  type ComplianceCheck,
  type PenaltyExposure,
  type TaxpayerObligation,
} from "./mock-data";

export interface DemoScenario {
  id: string;
  name: string;
  description: string;
  returns: TaxReturn[];
  filingPeriods: FilingPeriod[];
  cases: ReviewCase[];
  decisions: DecisionRecord[];
  outbox: OutboxEntry[];
  complianceChecks: ComplianceCheck[];
  penalties: PenaltyExposure[];
  obligations: TaxpayerObligation[];
}

// Full default state with all data
export const defaultScenario: DemoScenario = {
  id: "default",
  name: "Full Demo State",
  description:
    "All seed data loaded. Includes completed returns, an active manual review case, overdue obligations, and penalty exposure.",
  returns: initialReturns,
  filingPeriods: initialFilingPeriods,
  cases: initialReviewCases,
  decisions: initialDecisions,
  outbox: initialOutbox,
  complianceChecks: initialComplianceChecks,
  penalties: initialPenalties,
  obligations: initialObligations,
};

// Clean slate — only obligations and empty periods
export const cleanSlateScenario: DemoScenario = {
  id: "clean-slate",
  name: "Clean Slate",
  description:
    "Start fresh. Only obligations and unfiled periods exist. Demonstrate the entire filing lifecycle from scratch.",
  returns: [],
  filingPeriods: initialFilingPeriods.filter((p) => !p.taxReturnId),
  cases: [],
  decisions: [],
  outbox: [],
  complianceChecks: [],
  penalties: initialPenalties,
  obligations: initialObligations,
};

// Happy path ready — one OPEN period, no complications
export const happyPathScenario: DemoScenario = {
  id: "happy-path",
  name: "Happy Path Filing",
  description:
    "One open VAT period ready for filing. Walk through the entire wizard to a successful COMPLETED return.",
  returns: [],
  filingPeriods: [
    {
      id: "FP-DEMO-HP",
      tin: "TIN-2024-001842",
      taxTypeCode: "VAT",
      taxTypeLabel: "VAT",
      periodLabel: "Jun 2026",
      coversFrom: "2026-06-01",
      coversTo: "2026-06-30",
      dueDate: "2026-07-15",
      status: "OPEN",
    },
  ],
  cases: [],
  decisions: [],
  outbox: [],
  complianceChecks: [],
  penalties: [],
  obligations: initialObligations.filter((o) => o.status === "ACTIVE"),
};

// Manual review — return posted and awaiting officer review
export const manualReviewScenario: DemoScenario = {
  id: "manual-review",
  name: "Manual Review Flow",
  description:
    "A return is already under MANUAL_REVIEW. Switch to officer role to demonstrate the review and decision process.",
  returns: [initialReturns[0]], // The MANUAL_REVIEW return
  filingPeriods: [initialFilingPeriods.find((p) => p.id === "FP-2026-008")!],
  cases: [initialReviewCases[0]],
  decisions: [],
  outbox: initialOutbox.filter((o) => o.entity === "TR-2026-06-001"),
  complianceChecks: [initialComplianceChecks[0]],
  penalties: [],
  obligations: initialObligations.filter((o) => o.status === "ACTIVE"),
};

// Overdue with penalty — overdue periods and accruing penalties
export const overduePenaltyScenario: DemoScenario = {
  id: "overdue-penalty",
  name: "Overdue & Penalty Exposure",
  description:
    "Multiple overdue periods with accruing penalty exposure. Demonstrate late filing and penalty awareness.",
  returns: [],
  filingPeriods: initialFilingPeriods.filter((p) =>
    ["OVERDUE", "OPEN", "FUTURE"].includes(p.status),
  ),
  cases: [],
  decisions: [],
  outbox: [],
  complianceChecks: [],
  penalties: initialPenalties,
  obligations: initialObligations,
};

export const ALL_SCENARIOS: DemoScenario[] = [
  defaultScenario,
  cleanSlateScenario,
  happyPathScenario,
  manualReviewScenario,
  overduePenaltyScenario,
];

export function getScenarioById(id: string): DemoScenario {
  return ALL_SCENARIOS.find((s) => s.id === id) ?? defaultScenario;
}
