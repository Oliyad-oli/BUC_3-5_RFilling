import { create } from "zustand";
import {
  initialReturns,
  initialReviewCases,
  initialDecisions,
  initialOutbox,
  initialFilingPeriods,
  initialObligations,
  initialComplianceChecks,
  initialPenalties,
  type TaxReturn,
  type ReviewCase,
  type DecisionRecord,
  type OutboxEntry,
  type ReturnStatus,
  type FilingPeriod,
  type FilingPeriodStatus,
  type ComplianceCheck,
  type PenaltyExposure,
  type TaxpayerObligation,
  type OfficerDecision,
  type AmendmentRecord,
} from "./mock-data";
import {
  makeAuditEvent,
  makeOutboxEntry,
  makeComplianceCheck,
  makeReviewCase,
  makeAmendmentRecord,
  timeNow,
  dateNow,
  dateNowFull,
  nextId,
} from "./demo-engine";
import { type DemoScenario, defaultScenario } from "./demo-scenarios";

export type Role = "taxpayer" | "officer" | "auditor" | "admin" | "system";

export interface Notification {
  id: string;
  kind: "info" | "success" | "warning";
  text: string;
  time: string;
  unread: boolean;
}

interface State {
  // ── Role ──
  role: Role;
  setRole: (r: Role) => void;

  // ── Core Data ──
  returns: TaxReturn[];
  cases: ReviewCase[];
  decisions: DecisionRecord[];
  outbox: OutboxEntry[];
  notifications: Notification[];
  filingPeriods: FilingPeriod[];
  obligations: TaxpayerObligation[];
  complianceChecks: ComplianceCheck[];
  penalties: PenaltyExposure[];

  // ── Basic Mutations ──
  addReturn: (r: TaxReturn) => void;
  updateReturn: (id: string, patch: Partial<TaxReturn>) => void;
  appendEvent: (id: string, ev: TaxReturn["events"][number]) => void;
  setStatus: (id: string, status: ReturnStatus) => void;

  addCase: (c: ReviewCase) => void;
  removeCase: (id: string) => void;
  resolveCase: (id: string) => void;

  addDecision: (d: DecisionRecord) => void;
  pushNotification: (n: Omit<Notification, "id">) => void;
  markAllRead: () => void;

  // ── Filing Period Actions ──
  linkReturnToPeriod: (periodId: string, taxReturnId: string) => void;
  markPeriodFiled: (taxReturnId: string) => void;
  setPeriodStatus: (periodId: string, status: FilingPeriodStatus) => void;

  // ── Compliance ──
  addComplianceCheck: (c: ComplianceCheck) => void;

  // ── Outbox ──
  addOutboxEntry: (entry: OutboxEntry) => void;
  retryOutboxEntry: (id: string) => void;

  // ── Penalties ──
  addPenalty: (p: PenaltyExposure) => void;

  // ── Lifecycle Actions ──
  /** Post-ledger validation: run risk + rule checks, transition return accordingly */
  runValidation: (returnId: string, scenario: "pass" | "high_risk" | "rule_fail") => void;

  /** Officer submits a decision on a review case */
  submitOfficerDecision: (
    caseId: string,
    decision: OfficerDecision,
    narrative: string,
    externalCaseId?: string,
  ) => void;

  /** Start amendment flow on a return */
  startAmendment: (returnId: string, reason: string) => void;

  /** Complete amendment: mark return COMPLETED again */
  completeAmendment: (returnId: string, newNetTax: number) => void;

  // ── Scenario Reset ──
  activeScenarioId: string;
  loadScenario: (scenario: DemoScenario) => void;
}

export const useApp = create<State>((set, get) => ({
  role: "taxpayer",
  setRole: (r) => set({ role: r }),

  returns: initialReturns,
  cases: initialReviewCases,
  decisions: initialDecisions,
  outbox: initialOutbox,
  filingPeriods: initialFilingPeriods,
  obligations: initialObligations,
  complianceChecks: initialComplianceChecks,
  penalties: initialPenalties,
  notifications: [
    {
      id: "N1",
      kind: "warning",
      text: "Your Jun 2026 VAT return is under officer review.",
      time: "08 May 2026, 14:39",
      unread: true,
    },
    {
      id: "N2",
      kind: "success",
      text: "Your May 2026 VAT return was cleared. Status: COMPLETED.",
      time: "07 May 2026, 11:22",
      unread: true,
    },
    {
      id: "N3",
      kind: "info",
      text: "Amendment requested for Q1 2026 Income Tax.",
      time: "05 May 2026, 09:15",
      unread: false,
    },
    {
      id: "N4",
      kind: "warning",
      text: "Income Tax Q1 2026 is OVERDUE. File immediately to avoid penalties.",
      time: "01 May 2026, 09:00",
      unread: true,
    },
  ],

  // ── Basic Mutations ──
  addReturn: (r) => set((s) => ({ returns: [r, ...s.returns] })),
  updateReturn: (id, patch) =>
    set((s) => ({ returns: s.returns.map((r) => (r.id === id ? { ...r, ...patch } : r)) })),
  appendEvent: (id, ev) =>
    set((s) => ({
      returns: s.returns.map((r) => (r.id === id ? { ...r, events: [...r.events, ev] } : r)),
    })),
  setStatus: (id, status) =>
    set((s) => ({ returns: s.returns.map((r) => (r.id === id ? { ...r, status } : r)) })),

  addCase: (c) => set((s) => ({ cases: [c, ...s.cases] })),
  removeCase: (id) => set((s) => ({ cases: s.cases.filter((c) => c.id !== id) })),
  resolveCase: (id) =>
    set((s) => ({
      cases: s.cases.map((c) => (c.id === id ? { ...c, status: "RESOLVED" as const } : c)),
    })),

  addDecision: (d) => set((s) => ({ decisions: [d, ...s.decisions] })),
  pushNotification: (n) =>
    set((s) => ({
      notifications: [
        { ...n, id: "N" + Math.random().toString(36).slice(2, 8) },
        ...s.notifications,
      ],
    })),
  markAllRead: () =>
    set((s) => ({ notifications: s.notifications.map((n) => ({ ...n, unread: false })) })),

  // ── Filing Period ──
  linkReturnToPeriod: (periodId, taxReturnId) =>
    set((s) => ({
      filingPeriods: s.filingPeriods.map((p) => (p.id === periodId ? { ...p, taxReturnId } : p)),
    })),
  markPeriodFiled: (taxReturnId) =>
    set((s) => ({
      filingPeriods: s.filingPeriods.map((p) =>
        p.taxReturnId === taxReturnId ? { ...p, status: "FILED" as const, filedAt: dateNow() } : p,
      ),
    })),
  setPeriodStatus: (periodId, status) =>
    set((s) => ({
      filingPeriods: s.filingPeriods.map((p) => (p.id === periodId ? { ...p, status } : p)),
    })),

  // ── Compliance ──
  addComplianceCheck: (c) => set((s) => ({ complianceChecks: [c, ...s.complianceChecks] })),

  // ── Outbox ──
  addOutboxEntry: (entry) => set((s) => ({ outbox: [entry, ...s.outbox] })),
  retryOutboxEntry: (id) =>
    set((s) => ({
      outbox: s.outbox.map((o) =>
        o.id === id
          ? { ...o, status: "SENT" as const, tries: o.tries + 1, lastError: undefined }
          : o,
      ),
    })),

  // ── Penalties ──
  addPenalty: (p) => set((s) => ({ penalties: [p, ...s.penalties] })),

  // ── Lifecycle: Validation ──
  runValidation: (returnId, scenario) => {
    const state = get();
    const ret = state.returns.find((r) => r.id === returnId);
    if (!ret) return;

    const check = makeComplianceCheck(returnId, scenario);

    set((s) => ({
      complianceChecks: [check, ...s.complianceChecks],
      returns: s.returns.map((r) => {
        if (r.id !== returnId) return r;
        const newStatus: ReturnStatus =
          check.outcome === "COMPLETED" ? "COMPLETED" : "MANUAL_REVIEW";
        return {
          ...r,
          status: newStatus,
          riskScore: check.riskScore,
          riskLevel: check.riskLevel,
          rulePassed: check.rulePassed,
          events: [
            ...r.events,
            makeAuditEvent("Validation started", "SYSTEM"),
            makeAuditEvent(
              `Risk engine: ${check.riskLevel} (${check.riskScore.toFixed(2)})`,
              "SYSTEM",
              `${check.riskIndicators.length} indicators`,
            ),
            makeAuditEvent(`Rule engine: ${check.rulePassed ? "PASSED" : "FAILED"}`, "SYSTEM"),
            makeAuditEvent(`→ ${newStatus}`, "SYSTEM"),
          ],
          reviewId: newStatus === "MANUAL_REVIEW" ? nextId("REV") : r.reviewId,
        };
      }),
      outbox: [makeOutboxEntry("PostLedgerValidationCompletedEvent", returnId), ...s.outbox],
    }));

    // If manual review, create review case
    if (check.outcome === "MANUAL_REVIEW") {
      const updatedRet = get().returns.find((r) => r.id === returnId)!;
      const reviewCase = makeReviewCase(updatedRet, check);
      set((s) => ({
        cases: [reviewCase, ...s.cases],
        returns: s.returns.map((r) => (r.id === returnId ? { ...r, reviewId: reviewCase.id } : r)),
        notifications: [
          {
            id: "N" + Math.random().toString(36).slice(2, 8),
            kind: "warning" as const,
            text: `Your ${ret.period} ${ret.taxType} return requires officer review.`,
            time: dateNowFull(),
            unread: true,
          },
          ...s.notifications,
        ],
      }));
    } else {
      // Completed — mark period filed
      get().markPeriodFiled(returnId);
      set((s) => ({
        notifications: [
          {
            id: "N" + Math.random().toString(36).slice(2, 8),
            kind: "success" as const,
            text: `Your ${ret.period} ${ret.taxType} return is COMPLETED.`,
            time: dateNowFull(),
            unread: true,
          },
          ...s.notifications,
        ],
      }));
    }
  },

  // ── Lifecycle: Officer Decision ──
  submitOfficerDecision: (caseId, decision, narrative, externalCaseId) => {
    const state = get();
    const reviewCase = state.cases.find((c) => c.id === caseId);
    if (!reviewCase) return;

    // Record decision
    const decisionRecord: DecisionRecord = {
      id: nextId("D"),
      caseId,
      tin: reviewCase.tin,
      party: reviewCase.party,
      period: reviewCase.period,
      decision,
      officer: reviewCase.assignedOfficer ?? "Abebe Girma",
      decidedAt: dateNowFull(),
      narrative,
      externalCaseId: decision === "CONFIRM_FRAUD" ? externalCaseId || nextId("CASE") : undefined,
    };

    // Determine new return status
    let newReturnStatus: ReturnStatus;
    let notificationText: string;
    let notificationKind: "success" | "warning";

    switch (decision) {
      case "CLEAR":
        newReturnStatus = "COMPLETED";
        notificationText = `Your ${reviewCase.period} ${reviewCase.taxType} return was cleared by officer review.`;
        notificationKind = "success";
        break;
      case "REQUEST_AMENDMENT":
        newReturnStatus = "AMENDMENT_DRAFT";
        notificationText = `Amendment requested for your ${reviewCase.period} ${reviewCase.taxType} return.`;
        notificationKind = "warning";
        break;
      case "CONFIRM_FRAUD":
        newReturnStatus = "FRAUD_CONFIRMED";
        notificationText = `Your ${reviewCase.period} ${reviewCase.taxType} return has been flagged. Contact the revenue authority.`;
        notificationKind = "warning";
        break;
    }

    set((s) => ({
      decisions: [decisionRecord, ...s.decisions],
      cases: s.cases.map((c) => (c.id === caseId ? { ...c, status: "RESOLVED" as const } : c)),
      returns: s.returns.map((r) => {
        if (r.id !== reviewCase.returnId) return r;
        return {
          ...r,
          status: newReturnStatus,
          externalCaseId:
            decision === "CONFIRM_FRAUD" ? decisionRecord.externalCaseId : r.externalCaseId,
          events: [
            ...r.events,
            makeAuditEvent(`Officer decision: ${decision}`, "OFFICER", narrative),
            makeAuditEvent(`→ ${newReturnStatus}`, "SYSTEM"),
          ],
        };
      }),
      outbox: [
        makeOutboxEntry(`OfficerDecision${decision}Event`, reviewCase.returnId),
        ...s.outbox,
      ],
      notifications: [
        {
          id: "N" + Math.random().toString(36).slice(2, 8),
          kind: notificationKind,
          text: notificationText,
          time: dateNowFull(),
          unread: true,
        },
        ...s.notifications,
      ],
    }));

    // If CLEAR, mark period filed
    if (decision === "CLEAR") {
      get().markPeriodFiled(reviewCase.returnId);
    }
  },

  // ── Lifecycle: Amendment ──
  startAmendment: (returnId, reason) => {
    const ret = get().returns.find((r) => r.id === returnId);
    if (!ret) return;

    const amendment = makeAmendmentRecord(returnId, reason, ret.netTax ?? 0);
    set((s) => ({
      returns: s.returns.map((r) => {
        if (r.id !== returnId) return r;
        return {
          ...r,
          status: "AMENDMENT_DRAFT" as const,
          amendmentReason: reason,
          amendmentHistory: [...(r.amendmentHistory ?? []), amendment],
          events: [...r.events, makeAuditEvent(`Amendment started: ${reason}`, "PORTAL")],
        };
      }),
    }));
  },

  completeAmendment: (returnId, newNetTax) => {
    set((s) => ({
      returns: s.returns.map((r) => {
        if (r.id !== returnId) return r;
        const delta = newNetTax - (r.netTax ?? 0);
        return {
          ...r,
          status: "COMPLETED" as const,
          netTax: newNetTax,
          amendmentHistory: (r.amendmentHistory ?? []).map((a) =>
            a.status === "OPEN"
              ? {
                  ...a,
                  status: "COMPLETED" as const,
                  amendedNetTax: newNetTax,
                  delta,
                  completedAt: dateNowFull(),
                }
              : a,
          ),
          events: [
            ...r.events,
            makeAuditEvent(
              `Amendment completed — delta: ${delta >= 0 ? "+" : ""}${delta}`,
              "SYSTEM",
            ),
          ],
        };
      }),
    }));
    get().markPeriodFiled(returnId);
  },

  // ── Scenario Reset ──
  activeScenarioId: "default",
  loadScenario: (scenario) =>
    set({
      activeScenarioId: scenario.id,
      returns: scenario.returns,
      filingPeriods: scenario.filingPeriods,
      cases: scenario.cases,
      decisions: scenario.decisions,
      outbox: scenario.outbox,
      complianceChecks: scenario.complianceChecks,
      penalties: scenario.penalties,
      obligations: scenario.obligations,
      notifications: [],
    }),
}));
