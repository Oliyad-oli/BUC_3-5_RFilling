// ──────────────────────────────────────────────
// demo-engine.ts — Lifecycle transition rules
// ──────────────────────────────────────────────

import type {
  ReturnStatus,
  FilingPeriodStatus,
  TaxReturn,
  FilingPeriod,
  ComplianceCheck,
  ReviewCase,
  OutboxEntry,
  AuditEvent,
  PenaltyExposure,
  AmendmentRecord,
  RiskLevel,
} from "./mock-data";

// ── ID Generators ──

let _seq = 1000;
export function nextId(prefix: string) {
  _seq++;
  return `${prefix}-${_seq}`;
}

export function nextReturnId() {
  return "TR-2026-" + String(Math.floor(Math.random() * 90000) + 10000);
}

// ── Time Helpers ──

export function timeNow() {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

export function dateNow() {
  const months = [
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec",
  ];
  const d = new Date();
  return `${String(d.getDate()).padStart(2, "0")} ${months[d.getMonth()]} ${d.getFullYear()}`;
}

export function dateNowFull() {
  return new Date().toLocaleString("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

// ── Period Status Derivation ──

export function derivePeriodStatus(period: FilingPeriod, today = new Date()): FilingPeriodStatus {
  if (period.status === "FILED") return "FILED";
  const coversFrom = new Date(`${period.coversFrom}T00:00:00`);
  const dueDate = new Date(`${period.dueDate}T00:00:00`);
  const coversTo = new Date(`${period.coversTo}T00:00:00`);

  if (today < coversFrom) return "FUTURE";
  if (today > dueDate) return "OVERDUE";
  if (today > coversTo) return "DUE";
  return "OPEN";
}

// ── Transition Validation ──

const ALLOWED_TRANSITIONS: Record<ReturnStatus, ReturnStatus[]> = {
  DRAFT: ["CALCULATING"],
  CALCULATING: ["DRAFT", "CALCULATION_FAILED"],
  CALCULATION_FAILED: ["CALCULATING", "DRAFT"],
  ACCEPTED: ["POSTED_TO_LEDGER", "COMPLETED"], // COMPLETED for zero-net
  POSTED_TO_LEDGER: ["UNDER_VALIDATION"],
  UNDER_VALIDATION: ["COMPLETED", "MANUAL_REVIEW"],
  COMPLETED: ["AMENDMENT_DRAFT"],
  MANUAL_REVIEW: ["COMPLETED", "AMENDMENT_DRAFT", "FRAUD_CONFIRMED"],
  FRAUD_CONFIRMED: [], // terminal
  AMENDMENT_DRAFT: ["AMENDMENT_CALCULATING"],
  AMENDMENT_CALCULATING: ["AMENDMENT_DRAFT"],
  AMENDMENT_ACCEPTED: ["AMENDMENT_POSTED"],
  AMENDMENT_POSTED: ["UNDER_VALIDATION"],
};

export function canTransition(from: ReturnStatus, to: ReturnStatus): boolean {
  return ALLOWED_TRANSITIONS[from]?.includes(to) ?? false;
}

// ── Factory: Audit Event ──

export function makeAuditEvent(
  label: string,
  actor: AuditEvent["actor"],
  detail?: string,
): AuditEvent {
  return { time: timeNow(), label, actor, detail };
}

// ── Factory: Outbox Entry ──

export function makeOutboxEntry(topic: string, entity: string): OutboxEntry {
  return {
    id: nextId("OUT"),
    topic,
    entity,
    status: "PENDING",
    tries: 0,
    createdAt: dateNowFull(),
  };
}

// ── Factory: Compliance Check ──

export function makeComplianceCheck(
  returnId: string,
  scenario: "pass" | "high_risk" | "rule_fail",
): ComplianceCheck {
  const base = { id: nextId("CC"), returnId, checkedAt: dateNowFull() };

  switch (scenario) {
    case "pass":
      return {
        ...base,
        riskLevel: "LOW",
        riskScore: 0.15 + Math.random() * 0.25,
        riskIndicators: [],
        rulePassed: true,
        outcome: "COMPLETED",
      };
    case "high_risk":
      return {
        ...base,
        riskLevel: "HIGH",
        riskScore: 0.7 + Math.random() * 0.25,
        riskIndicators: [
          "VAT ratio anomaly detected",
          "Input credit exceeds 40% threshold",
          "Rapid growth vs prior periods",
        ],
        rulePassed: true,
        outcome: "MANUAL_REVIEW",
      };
    case "rule_fail":
      return {
        ...base,
        riskLevel: "MEDIUM",
        riskScore: 0.5 + Math.random() * 0.15,
        riskIndicators: ["Expense ratio exceeds threshold"],
        rulePassed: false,
        ruleFailures: ["INCOME_EXPENSE_RATIO_VIOLATION"],
        outcome: "MANUAL_REVIEW",
      };
  }
}

// ── Factory: Review Case ──

export function makeReviewCase(ret: TaxReturn, check: ComplianceCheck): ReviewCase {
  return {
    id: nextId("REV"),
    returnId: ret.id,
    tin: ret.tin,
    party: ret.party,
    taxType: ret.taxType,
    period: ret.period,
    netTax: ret.netTax ?? 0,
    reviewType: check.rulePassed ? "RISK_FRAUD" : "POST_LEDGER_RULE_FAIL",
    severity:
      check.riskLevel === "HIGH" ? "CRITICAL" : check.riskLevel === "MEDIUM" ? "HIGH" : "MEDIUM",
    status: "OPEN",
    workflowInstanceId: nextId("WF-POSTLEDGER"),
    evidencePayload: {
      riskScore: check.riskScore,
      indicators: check.riskIndicators,
      ruleResults: check.rulePassed ? ["POST_LEDGER_RULES_PASSED"] : check.ruleFailures,
    },
    createdAt: dateNowFull(),
  };
}

// ── Factory: Penalty Exposure ──

export function makePenaltyExposure(period: FilingPeriod): PenaltyExposure | null {
  if (period.status !== "OVERDUE") return null;
  const today = new Date();
  const dueDate = new Date(`${period.dueDate}T00:00:00`);
  const daysLate = Math.max(1, Math.ceil((today.getTime() - dueDate.getTime()) / 86_400_000));
  // Simplified penalty: 2% of estimated average tax per day late, capped at 25%
  const estimatedPenalty = Math.round(daysLate * 250);
  return {
    id: nextId("PEN"),
    filingPeriodId: period.id,
    tin: period.tin,
    taxType: period.taxTypeLabel,
    periodLabel: period.periodLabel,
    daysLate,
    estimatedPenalty,
    status: "ACCRUING",
  };
}

// ── Factory: Amendment Record ──

export function makeAmendmentRecord(
  returnId: string,
  reason: string,
  previousNetTax: number,
): AmendmentRecord {
  return {
    id: nextId("AMD"),
    returnId,
    reason,
    previousNetTax,
    amendedNetTax: 0,
    delta: 0,
    status: "OPEN",
    startedAt: dateNowFull(),
  };
}

// ── Hash Generator (same as wizard) ──

export function makeHash(input: string): string {
  let hash = 0;
  for (let i = 0; i < input.length; i++) {
    hash = (Math.imul(31, hash) + input.charCodeAt(i)) | 0;
  }
  return `sha256:${Math.abs(hash).toString(16).padStart(64, "0")}`;
}

// ── Days Until Due ──

export function getDaysUntilDue(dueDate: string): number {
  const today = new Date();
  const due = new Date(`${dueDate}T00:00:00`);
  return Math.ceil((due.getTime() - today.getTime()) / 86_400_000);
}
