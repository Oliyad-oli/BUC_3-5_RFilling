import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { useApp } from "@/lib/store";
import { TAXPAYER, formatETB, type FilingPeriod, type LineItem, type Schedule, type TaxReturn } from "@/lib/mock-data";
import { toast } from "sonner";
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Check,
  FileCheck2,
  Info,
  Landmark,
  Loader2,
  Plus,
  RefreshCcw,
  Trash2,
  UploadCloud,
} from "lucide-react";

export const Route = createFileRoute("/returns/new")({
  validateSearch: (search: Record<string, unknown>): { filingPeriodId?: string } => ({
    filingPeriodId: typeof search.filingPeriodId === "string" ? search.filingPeriodId : undefined,
  }),
  component: NewReturnWizard,
});

const STEPS = ["Draft", "Schedules", "Line Items", "Calculate", "Submit", "Ledger Post", "Validation"];

type TaxRole = "OUTPUT" | "INPUT" | "NONE" | "COMPUTED";

interface LineTypeDefinition {
  lineCode: string;
  label: string;
  ratePercent: number;
  taxRole: Exclude<TaxRole, "COMPUTED">;
}

interface ScheduleDefinition {
  scheduleCode: string;
  label: string;
  required: boolean;
  lineTypes: LineTypeDefinition[];
}

interface SubmissionFormDefinition {
  formCode: string;
  rulePackage: string;
  schedules: ScheduleDefinition[];
}

const SUBMISSION_FORMS: Record<TaxReturn["taxType"], SubmissionFormDefinition> = {
  VAT: {
    formCode: "VAT-FORM-2026-MONTHLY",
    rulePackage: "VAT-v2.1.4",
    schedules: [
      {
        scheduleCode: "VAT_OUTPUT",
        label: "Sales & Output VAT",
        required: true,
        lineTypes: [
          { lineCode: "VAT-OUT-001", label: "Standard-rated domestic sales", ratePercent: 15, taxRole: "OUTPUT" },
          { lineCode: "VAT-OUT-002", label: "Standard-rated services", ratePercent: 15, taxRole: "OUTPUT" },
          { lineCode: "VAT-OUT-003", label: "Zero-rated exports", ratePercent: 0, taxRole: "OUTPUT" },
        ],
      },
      {
        scheduleCode: "VAT_INPUT",
        label: "Purchases & Input VAT",
        required: true,
        lineTypes: [
          { lineCode: "VAT-IN-001", label: "Local taxable purchases", ratePercent: 15, taxRole: "INPUT" },
          { lineCode: "VAT-IN-002", label: "Import VAT credit", ratePercent: 15, taxRole: "INPUT" },
        ],
      },
    ],
  },
  "Income Tax": {
    formCode: "IT-FORM-2026-Q",
    rulePackage: "INCOMETAX-v3.0.1",
    schedules: [
      {
        scheduleCode: "IT_INCOME",
        label: "Taxable Business Income",
        required: true,
        lineTypes: [{ lineCode: "IT-GROSS-001", label: "Gross business income", ratePercent: 30, taxRole: "OUTPUT" }],
      },
      {
        scheduleCode: "IT_DEDUCTIONS",
        label: "Allowable Deductions",
        required: true,
        lineTypes: [{ lineCode: "IT-DED-001", label: "Deductible operating expenses", ratePercent: 30, taxRole: "INPUT" }],
      },
    ],
  },
  "Withholding Tax": {
    formCode: "WHT-FORM-2026-MONTHLY",
    rulePackage: "WHT-v1.1.0",
    schedules: [
      {
        scheduleCode: "WHT_PAYMENTS",
        label: "Withholding Transactions",
        required: true,
        lineTypes: [{ lineCode: "WHT-PAY-002", label: "Supplier payments withheld at 2%", ratePercent: 2, taxRole: "OUTPUT" }],
      },
    ],
  },
  PAYE: {
    formCode: "PAYE-FORM-2026-MONTHLY",
    rulePackage: "PAYE-v1.3.0",
    schedules: [
      {
        scheduleCode: "PAYE_PAYROLL",
        label: "Payroll Summary",
        required: true,
        lineTypes: [
          { lineCode: "PAYE-BAND-A", label: "Gross salaries, band A", ratePercent: 10, taxRole: "OUTPUT" },
          { lineCode: "PAYE-BAND-B", label: "Gross salaries, band B", ratePercent: 15, taxRole: "OUTPUT" },
        ],
      },
    ],
  },
  "Excise Tax": {
    formCode: "EXCISE-FORM-2026-MONTHLY",
    rulePackage: "EXCISE-v1.0.0",
    schedules: [
      {
        scheduleCode: "EXCISE_GOODS",
        label: "Excisable Goods",
        required: true,
        lineTypes: [{ lineCode: "EXC-GOODS-001", label: "Alcoholic beverages", ratePercent: 50, taxRole: "OUTPUT" }],
      },
    ],
  },
};

function NewReturnWizard() {
  const search = Route.useSearch();
  const navigate = useNavigate();
  const filingPeriods = useApp((s) => s.filingPeriods);
  const addReturn = useApp((s) => s.addReturn);
  const updateReturn = useApp((s) => s.updateReturn);
  const appendEvent = useApp((s) => s.appendEvent);
  const setStatus = useApp((s) => s.setStatus);
  const linkReturnToPeriod = useApp((s) => s.linkReturnToPeriod);
  const runValidation = useApp((s) => s.runValidation);

  const availablePeriods = useMemo(
    () => filingPeriods.filter((p) => p.tin === TAXPAYER.tin && ["OPEN", "OVERDUE"].includes(p.status) && !p.taxReturnId),
    [filingPeriods],
  );
  const allMyPeriods = filingPeriods.filter((p) => p.tin === TAXPAYER.tin);
  const initialPeriodId =
    search.filingPeriodId && allMyPeriods.some((p) => p.id === search.filingPeriodId)
      ? search.filingPeriodId
      : availablePeriods[0]?.id ?? "";

  const [step, setStep] = useState(0);
  const [returnId] = useState(() => "TR-2026-07-" + String(Math.floor(Math.random() * 900) + 100));
  const [selectedPeriodId, setSelectedPeriodId] = useState(initialPeriodId);
  const [channel, setChannel] = useState<TaxReturn["channel"]>("PORTAL");
  const [draftCreated, setDraftCreated] = useState(false);
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [calculating, setCalculating] = useState(false);
  const [calcState, setCalcState] = useState<"idle" | "accepted" | "failed">("idle");
  const [calcError, setCalcError] = useState<string | null>(null);
  const [iterationNo, setIterationNo] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const [submitResponse, setSubmitResponse] = useState<{ filingDeadline: string; paymentDueDate: string } | null>(null);
  const [ledgerPosting, setLedgerPosting] = useState(false);
  const [ledgerPosted, setLedgerPosted] = useState(false);
  const [ledgerError, setLedgerError] = useState<string | null>(null);
  const [validating, setValidating] = useState(false);
  const [validationDone, setValidationDone] = useState(false);
  const [validationOutcome, setValidationOutcome] = useState<string | null>(null);

  const selectedPeriod = allMyPeriods.find((p) => p.id === selectedPeriodId);
  const taxType = taxTypeFromPeriod(selectedPeriod);
  const form = SUBMISSION_FORMS[taxType];
  const calc = computeTax(schedules);
  const hasLineItems = schedules.some((s) => s.items.length > 0);

  const goStep1Next = () => {
    if (!selectedPeriod) {
      toast.error("Select an open filing period first.");
      return;
    }

    const draft: TaxReturn = {
      id: returnId,
      tin: TAXPAYER.tin,
      party: TAXPAYER.party,
      taxType,
      filingPeriodId: selectedPeriod.id,
      period: selectedPeriod.periodLabel,
      channel,
      status: "DRAFT",
      netTax: null,
      schedules: [],
      calculationIterations: [],
      events: [
        { time: timeNow(), label: "Draft created from CreateDraftTaxReturnRequest", actor: "PORTAL" },
      ],
      createdAt: dateNow(),
    };

    if (draftCreated) {
      updateReturn(returnId, draft);
    } else {
      addReturn(draft);
      linkReturnToPeriod(selectedPeriod.id, returnId);
      setDraftCreated(true);
    }

    toast.success(`Draft ${returnId} created with filingPeriodId ${selectedPeriod.id}`);
    setStep(1);
  };

  const loadSchedulesFromCatalog = () => {
    const loaded = form.schedules.map((schedule) => ({
      scheduleCode: schedule.scheduleCode,
      label: schedule.label,
      items: [],
    }));
    setSchedules(loaded);
    updateReturn(returnId, { schedules: loaded });
    appendEvent(returnId, { time: timeNow(), label: `Submission form loaded: ${form.formCode}`, actor: "SYSTEM" });
    toast.success(`${loaded.length} schedule definitions loaded from config catalog`);
    setStep(2);
  };

  const runCalculation = (fail: boolean) => {
    if (iterationNo > 10) {
      toast.error("Calculation retry limit reached.");
      return;
    }

    setCalculating(true);
    setCalcState("idle");
    setCalcError(null);
    setStatus(returnId, "CALCULATING");
    appendEvent(returnId, { time: timeNow(), label: `Calculation iteration ${iterationNo} requested`, actor: "TAXPAYER" });

    window.setTimeout(() => {
      const iteration = {
        iterationNo,
        status: fail ? "FAILED" as const : "COMPLETED" as const,
        inputHash: makeHash(`${returnId}:input:${iterationNo}:${calc.gross}:${calc.credit}`),
        outputHash: fail ? undefined : makeHash(`${returnId}:output:${iterationNo}:${calc.net}`),
        errorPayload: fail ? "Rule package cache hash validation failed before iteration output was accepted." : undefined,
        durationMs: fail ? 1260 : 418,
      };
      const existing = useApp.getState().returns.find((r) => r.id === returnId)?.calculationIterations ?? [];

      if (fail) {
        setStatus(returnId, "CALCULATION_FAILED");
        setCalcError(iteration.errorPayload ?? "Calculation failed.");
        setCalcState("failed");
        updateReturn(returnId, { schedules, calculationIterations: [...existing, iteration] });
        appendEvent(returnId, { time: timeNow(), label: `Calculation iteration ${iterationNo} failed`, actor: "SYSTEM" });
        toast.error("Calculation failed. You can correct lines and retry.");
      } else {
        setStatus(returnId, "ACCEPTED");
        setCalcState("accepted");
        updateReturn(returnId, {
          schedules,
          grossTax: calc.gross,
          inputCredit: calc.credit,
          netTax: calc.net,
          calculationIterations: [...existing, iteration],
        });
        appendEvent(returnId, { time: timeNow(), label: `Calculation iteration ${iterationNo} accepted`, actor: "SYSTEM" });
        toast.success("Calculation accepted. Status: ACCEPTED");
      }

      setIterationNo((n) => n + 1);
      setCalculating(false);
    }, 1200);
  };

  const submitReturn = () => {
    if (!selectedPeriod) return;
    setSubmitting(true);
    window.setTimeout(() => {
      const response = {
        filingDeadline: selectedPeriod.dueDate,
        paymentDueDate: addDays(selectedPeriod.dueDate, 5),
      };
      setSubmitResponse(response);
      updateReturn(returnId, response);
      appendEvent(returnId, { time: timeNow(), label: "Submit accepted and calendar dates resolved", actor: "SYSTEM" });
      setSubmitting(false);
      toast.success("Submit accepted. Status remains ACCEPTED until ledger post.");
    }, 700);
  };

  const postToLedger = (fail = false) => {
    setLedgerPosting(true);
    setLedgerError(null);
    window.setTimeout(() => {
      if (fail) {
        setLedgerError("Ledger service returned 502. The return remains ACCEPTED and can be retried with the same Idempotency-Key.");
        setLedgerPosting(false);
        toast.error("Ledger unavailable. Retry is safe.");
        return;
      }

      const ledgerEntry = "LED-2026-" + String(Math.floor(Math.random() * 90000) + 10000);
      setStatus(returnId, "POSTED_TO_LEDGER");
      updateReturn(returnId, { ledgerEntry });
      appendEvent(returnId, { time: timeNow(), label: "Posted to Ledger", actor: "SYSTEM", detail: ledgerEntry });
      setLedgerPosted(true);
      setLedgerPosting(false);
      toast.success("Posted to ledger. Status: POSTED_TO_LEDGER");
    }, 900);
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      <div>
        <h1 className="text-2xl font-semibold">File New Tax Return</h1>
        <p className="text-sm text-muted-foreground mt-1">Reference: <span className="mono">{returnId}</span></p>
      </div>

      <Stepper step={step} />

      {step === 0 && (
        <Card>
          <CardHeader title="Draft Request" subtitle="Step 1 of 7" />
          {availablePeriods.length === 0 && !selectedPeriod && (
            <div className="mb-5 bg-warning/10 border border-warning/40 rounded-md p-4 text-sm text-warning">
              No open filing periods are available for this taxpayer.
            </div>
          )}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <Field label="TIN Number"><input value={TAXPAYER.tin} disabled className="input mono" /></Field>
            <Field label="Actor ID"><input value="user-001" disabled className="input mono" /></Field>
            <Field label="Filing Period ID">
              <select value={selectedPeriodId} onChange={(e) => setSelectedPeriodId(e.target.value)} className="input">
                {availablePeriods.map((period) => (
                  <option key={period.id} value={period.id}>
                    {period.id} — {period.taxTypeLabel} {period.periodLabel} ({period.status})
                  </option>
                ))}
                {selectedPeriod && !availablePeriods.some((p) => p.id === selectedPeriod.id) && (
                  <option value={selectedPeriod.id}>
                    {selectedPeriod.id} — {selectedPeriod.taxTypeLabel} {selectedPeriod.periodLabel} ({selectedPeriod.status})
                  </option>
                )}
              </select>
            </Field>
            <Field label="Tax Type"><input value={taxType} disabled className="input" /></Field>
            <Field label="Period"><input value={selectedPeriod?.periodLabel ?? "Select a filing period"} disabled className="input" /></Field>
            <Field label="Channel">
              <div className="flex gap-4 mt-1">
                {(["PORTAL", "API", "BATCH"] as const).map((m) => (
                  <label key={m} className="flex items-center gap-2 text-sm cursor-pointer">
                    <input type="radio" checked={channel === m} onChange={() => setChannel(m)} className="accent-accent" />
                    {m}
                  </label>
                ))}
              </div>
            </Field>
          </div>
          <div className="mt-5 bg-muted/40 border border-border rounded-md p-4 text-sm">
            <div className="text-xs uppercase tracking-wide text-muted-foreground mb-2">CreateDraftTaxReturnRequest</div>
            <pre className="mono text-xs overflow-auto">{JSON.stringify({
              tin: TAXPAYER.tin,
              taxTypeCode: selectedPeriod?.taxTypeCode ?? "VAT",
              filingPeriodId: selectedPeriod?.id ?? null,
              channel,
              actorId: "user-001",
            }, null, 2)}</pre>
          </div>
          <Footer>
            <button className="btn-ghost" onClick={() => navigate({ to: "/" })}>Cancel</button>
            <button className="btn-primary" onClick={goStep1Next} disabled={!selectedPeriod}>Create Draft <ArrowRight className="h-4 w-4" /></button>
          </Footer>
        </Card>
      )}

      {step === 1 && (
        <Card>
          <CardHeader title="Schedules From Config Catalog" subtitle="Step 2 of 7" />
          <div className="mb-4 flex items-start gap-3 bg-accent/5 border border-accent/30 rounded-md p-3">
            <Info className="h-4 w-4 text-accent shrink-0 mt-0.5" />
            <div className="text-sm">
              <span className="font-medium text-accent">Active SubmissionFormDefinition</span>
              <span className="text-muted-foreground ml-1">
                loads schedule sections from the config catalog. The backend does not auto-create line items for the return.
              </span>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm mb-5">
            <Info2 label="Form Code" value={form.formCode} />
            <Info2 label="Rule Package" value={form.rulePackage} />
            <Info2 label="Tax Type" value={taxType} />
          </div>
          <div className="space-y-2">
            {form.schedules.map((schedule) => (
              <div key={schedule.scheduleCode} className="flex items-center gap-3 px-4 py-3 border border-border rounded-md bg-muted/20">
                <Check className="h-4 w-4 text-success shrink-0" />
                <span className="mono text-xs text-muted-foreground w-36">{schedule.scheduleCode}</span>
                <span className="text-sm">{schedule.label}</span>
                <span className="ml-auto text-xs text-muted-foreground">{schedule.lineTypes.length} line type{schedule.lineTypes.length > 1 ? "s" : ""}</span>
              </div>
            ))}
          </div>
          <Footer>
            <button className="btn-ghost" onClick={() => setStep(0)}><ArrowLeft className="h-4 w-4" /> Back</button>
            <button className="btn-primary" onClick={loadSchedulesFromCatalog}>Load Schedule Definitions <ArrowRight className="h-4 w-4" /></button>
          </Footer>
        </Card>
      )}

      {step === 2 && (
        <Card>
          <CardHeader title="Line Items" subtitle="Step 3 of 7 — AddLineItemRequest fields" />
          <div className="space-y-4">
            {schedules.map((schedule, idx) => (
              <SchedulePanel
                key={schedule.scheduleCode}
                schedule={schedule}
                definition={form.schedules.find((s) => s.scheduleCode === schedule.scheduleCode) ?? form.schedules[0]}
                onChange={(items) => {
                  const next = [...schedules];
                  next[idx] = { ...schedule, items: normalizeDisplayOrder(items) };
                  setSchedules(next);
                  updateReturn(returnId, { schedules: next });
                }}
              />
            ))}
          </div>
          <Footer>
            <button className="btn-ghost" onClick={() => setStep(1)}><ArrowLeft className="h-4 w-4" /> Back</button>
            <button className="btn-primary" onClick={() => setStep(3)} disabled={!hasLineItems}>Review Calculation <ArrowRight className="h-4 w-4" /></button>
          </Footer>
        </Card>
      )}

      {step === 3 && (
        <Card>
          <CardHeader title="Calculation" subtitle="Step 4 of 7 — RequestCalculationRequest" />
          {calcState === "idle" && !calculating && (
            <div className="space-y-5">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                <Info2 label="Next Iteration" value={`ITER-${String(iterationNo).padStart(3, "0")}`} />
                <Info2 label="Max Iterations" value="10" />
                <Info2 label="Rule Package" value={form.rulePackage} />
                <Info2 label="Current Status" value="DRAFT" />
              </div>
              <div className="border border-border rounded-md divide-y divide-border">
                <Row label="Estimated output tax" value={formatETB(calc.gross)} />
                <Row label="Estimated input credit" value={`(${formatETB(calc.credit)})`} negative />
                <Row label="Estimated net tax" value={formatETB(calc.net)} bold />
              </div>
              <div className="flex flex-wrap gap-3">
                <button className="btn-primary" onClick={() => runCalculation(false)}>
                  <RefreshCcw className="h-4 w-4" /> Run Calculation
                </button>
                <button className="btn-ghost" onClick={() => runCalculation(true)}>
                  <AlertTriangle className="h-4 w-4 text-destructive" /> Simulate Failure
                </button>
              </div>
            </div>
          )}
          {calculating && (
            <div className="space-y-3 py-6">
              <CalcStep text="Building input hash from line items..." active />
              <CalcStep text="Applying active rule package..." />
              <CalcStep text="Validating output hash..." />
            </div>
          )}
          {calcState === "failed" && !calculating && (
            <div className="fade-in">
              <div className="bg-destructive/10 border border-destructive/40 rounded-md p-4 text-sm">
                <div className="font-semibold text-destructive flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4" /> CALCULATION_FAILED
                </div>
                <div className="text-muted-foreground mt-1">{calcError}</div>
              </div>
              <Footer>
                <button className="btn-ghost" onClick={() => setStep(2)}><ArrowLeft className="h-4 w-4" /> Edit Line Items</button>
                <button className="btn-primary" onClick={() => runCalculation(false)}>Retry Calculation <RefreshCcw className="h-4 w-4" /></button>
              </Footer>
            </div>
          )}
          {calcState === "accepted" && !calculating && (
            <div className="fade-in">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm mb-5">
                <Info2 label="Iteration" value={`ITER-${String(iterationNo - 1).padStart(3, "0")}`} />
                <Info2 label="Computed On" value={`${dateNow()}, ${timeNow()}`} />
                <Info2 label="Rule Package" value={form.rulePackage} />
                <Info2 label="Status" value="ACCEPTED" tone="success" />
              </div>
              <div className="border border-border rounded-md divide-y divide-border">
                <Row label="Output tax" value={formatETB(calc.gross)} />
                <Row label="Input credit" value={`(${formatETB(calc.credit)})`} negative />
                <Row label="NET TAX PAYABLE" value={formatETB(calc.net)} bold />
              </div>
              <Footer>
                <button className="btn-ghost" onClick={() => setStep(2)}><ArrowLeft className="h-4 w-4" /> Edit Lines</button>
                <button className="btn-primary" onClick={() => setStep(4)}>Continue to Submit <ArrowRight className="h-4 w-4" /></button>
              </Footer>
            </div>
          )}
        </Card>
      )}

      {step === 4 && (
        <Card>
          <CardHeader title="Submit Return" subtitle="Step 5 of 7 — POST /tax-returns/{id}/submit" />
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
            <Info2 label="Return ID" value={returnId} />
            <Info2 label="Status Before Submit" value="ACCEPTED" />
            <Info2 label="Idempotency Key" value={`${returnId}-submit`} />
          </div>
          {submitResponse ? (
            <div className="mt-5 bg-success/10 border border-success/40 rounded-md p-4 text-sm">
              <div className="font-semibold text-success flex items-center gap-2"><FileCheck2 className="h-4 w-4" /> Submit accepted</div>
              <div className="mt-3 grid grid-cols-1 md:grid-cols-2 gap-3">
                <Info2 label="Filing Deadline" value={submitResponse.filingDeadline} />
                <Info2 label="Payment Due Date" value={submitResponse.paymentDueDate} />
              </div>
            </div>
          ) : (
            <div className="mt-5 bg-muted/40 border border-border rounded-md p-4 text-sm text-muted-foreground">
              Submit resolves calendar dates. It does not post to ledger by itself in this frontend flow.
            </div>
          )}
          <Footer>
            <button className="btn-ghost" onClick={() => setStep(3)}><ArrowLeft className="h-4 w-4" /> Back</button>
            {submitResponse ? (
              <button className="btn-primary" onClick={() => setStep(5)}>Continue to Ledger Post <ArrowRight className="h-4 w-4" /></button>
            ) : (
              <button className="btn-primary" onClick={submitReturn} disabled={submitting}>
                {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <UploadCloud className="h-4 w-4" />} Submit Return
              </button>
            )}
          </Footer>
        </Card>
      )}

      {step === 5 && (
        <Card>
          <CardHeader title="Post to Ledger" subtitle="Step 6 of 7 — POST /tax-returns/{id}/post-to-ledger" />
          {!ledgerPosted && (
            <div className="space-y-4">
              <div className="bg-info/5 border border-info/30 rounded-md p-4 text-sm">
                <div className="font-semibold flex items-center gap-2"><Landmark className="h-4 w-4 text-info" /> Ready to post assessment</div>
                <div className="text-muted-foreground mt-1">A ledger failure keeps the return ACCEPTED so the post can be retried safely.</div>
              </div>
              {ledgerError && (
                <div className="bg-warning/10 border border-warning/40 rounded-md p-4 text-sm text-warning">
                  {ledgerError}
                </div>
              )}
              <div className="flex flex-wrap gap-3">
                <button className="btn-primary" onClick={() => postToLedger(false)} disabled={ledgerPosting}>
                  {ledgerPosting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Landmark className="h-4 w-4" />} Post to Ledger
                </button>
                <button className="btn-ghost" onClick={() => postToLedger(true)} disabled={ledgerPosting}>
                  <AlertTriangle className="h-4 w-4 text-warning" /> Simulate 502
                </button>
              </div>
            </div>
          )}
          {ledgerPosted && (
            <div className="text-center py-8 fade-in">
              <div className="mx-auto h-16 w-16 rounded-full bg-success/15 text-success flex items-center justify-center mb-4">
                <FileCheck2 className="h-8 w-8" />
              </div>
              <h2 className="text-xl font-semibold">Return Posted to Ledger</h2>
              <div className="mt-5 inline-block text-left bg-muted/40 border border-border rounded-md p-5 text-sm space-y-2">
                <div><span className="text-muted-foreground w-36 inline-block">Reference:</span><span className="mono">{returnId}</span></div>
                <div><span className="text-muted-foreground w-36 inline-block">Period:</span>{selectedPeriod?.periodLabel} — {taxType}</div>
                <div><span className="text-muted-foreground w-36 inline-block">Net Tax:</span><span className="mono">{formatETB(calc.net)}</span></div>
                <div><span className="text-muted-foreground w-36 inline-block">Status:</span><span className="text-accent font-medium">POSTED_TO_LEDGER</span></div>
              </div>
              <Footer>
                <button className="btn-ghost" onClick={() => navigate({ to: "/returns" })}>View My Returns</button>
                <button className="btn-primary" onClick={() => setStep(6)}>Continue to Validation <ArrowRight className="h-4 w-4" /></button>
              </Footer>
            </div>
          )}
        </Card>
      )}
      {step === 6 && (
        <Card>
          <CardHeader title="Post-Ledger Validation" subtitle="Step 7 of 7 — PostLedgerValidation (Risk + Rule Engine)" />
          {!validationDone && !validating && (
            <div className="space-y-5">
              <div className="bg-info/5 border border-info/30 rounded-md p-4 text-sm">
                <div className="font-semibold flex items-center gap-2"><Info className="h-4 w-4 text-info" /> Validation runs automatically after ledger post</div>
                <div className="text-muted-foreground mt-1">
                  The backend runs a risk engine (fraud indicators, anomaly detection) and a rule engine (post-ledger business rules).
                  Choose a scenario to simulate the outcome.
                </div>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <button
                  className="border border-success/40 bg-success/5 rounded-lg p-4 text-left hover:bg-success/10 transition-colors"
                  onClick={() => {
                    setValidating(true);
                    setStatus(returnId, "UNDER_VALIDATION");
                    window.setTimeout(() => {
                      runValidation(returnId, "pass");
                      setValidating(false);
                      setValidationDone(true);
                      setValidationOutcome("COMPLETED");
                      toast.success("Validation passed. Return → COMPLETED");
                    }, 1500);
                  }}
                >
                  <div className="text-sm font-semibold text-success">Happy Path</div>
                  <div className="text-xs text-muted-foreground mt-1">LOW risk, all rules pass → COMPLETED</div>
                </button>
                <button
                  className="border border-warning/40 bg-warning/5 rounded-lg p-4 text-left hover:bg-warning/10 transition-colors"
                  onClick={() => {
                    setValidating(true);
                    setStatus(returnId, "UNDER_VALIDATION");
                    window.setTimeout(() => {
                      runValidation(returnId, "high_risk");
                      setValidating(false);
                      setValidationDone(true);
                      setValidationOutcome("MANUAL_REVIEW");
                      toast.warning("HIGH risk detected. Return → MANUAL_REVIEW");
                    }, 1500);
                  }}
                >
                  <div className="text-sm font-semibold text-warning">High Risk</div>
                  <div className="text-xs text-muted-foreground mt-1">HIGH risk score → MANUAL_REVIEW (officer queue)</div>
                </button>
                <button
                  className="border border-destructive/40 bg-destructive/5 rounded-lg p-4 text-left hover:bg-destructive/10 transition-colors"
                  onClick={() => {
                    setValidating(true);
                    setStatus(returnId, "UNDER_VALIDATION");
                    window.setTimeout(() => {
                      runValidation(returnId, "rule_fail");
                      setValidating(false);
                      setValidationDone(true);
                      setValidationOutcome("MANUAL_REVIEW");
                      toast.warning("Rule failed. Return → MANUAL_REVIEW");
                    }, 1500);
                  }}
                >
                  <div className="text-sm font-semibold text-destructive">Rule Failure</div>
                  <div className="text-xs text-muted-foreground mt-1">Rule engine fails → MANUAL_REVIEW (officer queue)</div>
                </button>
              </div>
            </div>
          )}
          {validating && (
            <div className="space-y-3 py-6">
              <CalcStep text="Running risk engine analysis..." active />
              <CalcStep text="Checking post-ledger business rules..." />
              <CalcStep text="Publishing validation event..." />
            </div>
          )}
          {validationDone && (
            <div className="text-center py-8 fade-in">
              <div className={`mx-auto h-16 w-16 rounded-full flex items-center justify-center mb-4 ${
                validationOutcome === "COMPLETED" ? "bg-success/15 text-success" : "bg-warning/15 text-warning"
              }`}>
                {validationOutcome === "COMPLETED" ? <FileCheck2 className="h-8 w-8" /> : <AlertTriangle className="h-8 w-8" />}
              </div>
              <h2 className="text-xl font-semibold">
                {validationOutcome === "COMPLETED" ? "Return Completed" : "Sent to Manual Review"}
              </h2>
              <p className="mt-2 text-sm text-muted-foreground">
                {validationOutcome === "COMPLETED"
                  ? "All checks passed. The return is now COMPLETED and the filing period is marked FILED."
                  : "The return has been flagged for officer review. Switch to the Officer Portal to review the case."}
              </p>
              <div className="mt-6 flex justify-center gap-3">
                <button className="btn-ghost" onClick={() => navigate({ to: "/returns" })}>View My Returns</button>
                <button className="btn-primary" onClick={() => navigate({ to: "/returns/$id", params: { id: returnId } })}>Open Return</button>
              </div>
            </div>
          )}
        </Card>
      )}
    </div>
  );
}

function taxTypeFromPeriod(period?: FilingPeriod): TaxReturn["taxType"] {
  if (!period) return "VAT";
  if (period.taxTypeCode === "INCOME_TAX") return "Income Tax";
  if (period.taxTypeCode === "WHT") return "Withholding Tax";
  return period.taxTypeCode;
}

function computeTax(schedules: Schedule[]) {
  const userItems = schedules.flatMap((s) => s.items).filter((item) => item.origin === "USER_ENTERED");
  const gross = userItems
    .filter((item) => getTaxRole(item) === "OUTPUT")
    .reduce((sum, item) => sum + item.amount * (getRatePercent(item) / 100), 0);
  const credit = userItems
    .filter((item) => getTaxRole(item) === "INPUT")
    .reduce((sum, item) => sum + item.amount * (getRatePercent(item) / 100), 0);
  return { gross, credit, net: gross - credit };
}

function parseEntryData(value?: string): { ratePercent?: number; taxRole?: TaxRole } {
  if (!value) return {};
  try {
    return JSON.parse(value) as { ratePercent?: number; taxRole?: TaxRole };
  } catch {
    return {};
  }
}

function stringifyEntryData(data: { ratePercent?: number; taxRole?: TaxRole }) {
  return JSON.stringify(data);
}

function getRatePercent(item: LineItem) {
  return parseEntryData(item.entrySpecificData).ratePercent ?? 0;
}

function getTaxRole(item: LineItem): TaxRole {
  return parseEntryData(item.entrySpecificData).taxRole ?? "NONE";
}

function normalizeDisplayOrder(items: LineItem[]) {
  return items.map((item, index) => ({ ...item, displayOrder: index + 1 }));
}

function makeHash(input: string) {
  let hash = 0;
  for (let i = 0; i < input.length; i += 1) {
    hash = Math.imul(31, hash) + input.charCodeAt(i) | 0;
  }
  return `sha256:${Math.abs(hash).toString(16).padStart(64, "0")}`;
}

function addDays(date: string, days: number) {
  const d = new Date(`${date}T00:00:00`);
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
}

function timeNow() {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function dateNow() {
  const d = new Date();
  const months = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];
  return `${String(d.getDate()).padStart(2, "0")} ${months[d.getMonth()]} ${d.getFullYear()}`;
}

function Stepper({ step }: { step: number }) {
  return (
    <div className="flex items-center gap-2">
      {STEPS.map((s, i) => (
        <div key={s} className="flex items-center gap-2 flex-1">
          <div className={`h-7 w-7 rounded-full flex items-center justify-center text-xs font-semibold shrink-0 ${
            i < step ? "bg-success text-success-foreground" : i === step ? "bg-accent text-accent-foreground" : "bg-muted text-muted-foreground"
          }`}>
            {i < step ? <Check className="h-3.5 w-3.5" /> : i + 1}
          </div>
          <div className={`text-xs font-medium hidden md:block ${i === step ? "text-foreground" : "text-muted-foreground"}`}>{s}</div>
          {i < STEPS.length - 1 && <div className={`flex-1 h-px ${i < step ? "bg-success" : "bg-border"}`} />}
        </div>
      ))}
    </div>
  );
}

function Card({ children }: { children: React.ReactNode }) {
  return <div className="bg-card border border-border rounded-lg p-6 fade-in">{children}</div>;
}

function CardHeader({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="mb-5 pb-4 border-b border-border">
      <div className="text-xs uppercase tracking-wide text-muted-foreground">{subtitle}</div>
      <h2 className="text-lg font-semibold mt-1">{title}</h2>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <div className="text-xs font-medium text-muted-foreground mb-1.5">{label}</div>
      {children}
    </label>
  );
}

function Footer({ children }: { children: React.ReactNode }) {
  return <div className="mt-6 pt-4 border-t border-border flex justify-between gap-3 flex-wrap">{children}</div>;
}

function CalcStep({ text, active }: { text: string; active?: boolean }) {
  return (
    <div className={`flex items-center gap-2 text-sm ${active ? "text-foreground" : "text-muted-foreground"}`}>
      <Loader2 className="h-4 w-4 animate-spin" />
      {text}
    </div>
  );
}

function Row({ label, value, bold, negative }: { label: string; value: string; bold?: boolean; negative?: boolean }) {
  return (
    <div className={`flex justify-between px-4 py-3 ${bold ? "bg-muted/40 font-semibold" : ""}`}>
      <div className="text-sm">{label}</div>
      <div className={`mono text-sm ${negative ? "text-destructive" : ""}`}>{value}</div>
    </div>
  );
}

function Info2({ label, value, tone }: { label: string; value: string; tone?: "success" }) {
  return (
    <div>
      <div className="text-[11px] uppercase text-muted-foreground tracking-wide">{label}</div>
      <div className={`mt-0.5 mono text-sm ${tone === "success" ? "text-success" : ""}`}>{value}</div>
    </div>
  );
}

function SchedulePanel({ schedule, definition, onChange }: { schedule: Schedule; definition: ScheduleDefinition; onChange: (items: LineItem[]) => void }) {
  const [open, setOpen] = useState(true);

  const addItem = () => {
    const line = definition.lineTypes[0];
    const next: LineItem = {
      id: "L" + Math.random().toString(36).slice(2, 6),
      scheduleCode: schedule.scheduleCode,
      lineCode: line.lineCode,
      displayOrder: schedule.items.length + 1,
      amount: 0,
      currency: "ETB",
      lineTextValue: line.label,
      entrySpecificData: stringifyEntryData({ ratePercent: line.ratePercent, taxRole: line.taxRole }),
      origin: "USER_ENTERED",
    };
    onChange([...schedule.items, next]);
  };

  const update = (i: number, patch: Partial<LineItem>) => onChange(schedule.items.map((item, idx) => idx === i ? { ...item, ...patch } : item));
  const remove = (i: number) => onChange(schedule.items.filter((_, idx) => idx !== i));

  const changeLineType = (i: number, lineCode: string) => {
    const line = definition.lineTypes.find((entry) => entry.lineCode === lineCode) ?? definition.lineTypes[0];
    update(i, {
      lineCode: line.lineCode,
      lineTextValue: line.label,
      entrySpecificData: stringifyEntryData({ ratePercent: line.ratePercent, taxRole: line.taxRole }),
    });
  };

  const changeRate = (i: number, ratePercent: number) => {
    const item = schedule.items[i];
    update(i, {
      entrySpecificData: stringifyEntryData({ ...parseEntryData(item.entrySpecificData), ratePercent }),
    });
  };

  return (
    <div className="border border-border rounded-md overflow-hidden">
      <button onClick={() => setOpen((v) => !v)} className="w-full flex items-center justify-between px-4 py-3 bg-muted/40 hover:bg-muted/60 text-sm">
        <div className="flex items-center gap-2 min-w-0">
          <span className="mono text-xs text-muted-foreground">{schedule.scheduleCode}</span>
          <span className="font-medium truncate">{schedule.label}</span>
        </div>
        <span className="text-xs text-muted-foreground">{schedule.items.length} items {open ? "up" : "down"}</span>
      </button>
      {open && (
        <div className="p-4 space-y-3">
          {schedule.items.length === 0 ? (
            <div className="text-sm text-muted-foreground border border-dashed border-border rounded-md p-4 text-center">
              No line items yet. Add one to create an AddLineItemRequest payload.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[880px] text-sm">
                <thead className="text-xs text-muted-foreground">
                  <tr>
                    <th className="text-left font-normal pb-2 w-12">Order</th>
                    <th className="text-left font-normal pb-2 w-48">Line Code</th>
                    <th className="text-left font-normal pb-2">Text Value</th>
                    <th className="text-right font-normal pb-2 w-36">Amount</th>
                    <th className="text-right font-normal pb-2 w-24">Rate %</th>
                    <th className="text-left font-normal pb-2 w-28">Origin</th>
                    <th className="w-8"></th>
                  </tr>
                </thead>
                <tbody>
                  {schedule.items.map((item, i) => (
                    <tr key={item.id} className="border-t border-border">
                      <td className="py-2 mono text-xs text-muted-foreground">{item.displayOrder}</td>
                      <td className="py-2 pr-2">
                        <select value={item.lineCode} onChange={(e) => changeLineType(i, e.target.value)} className="input-ghost w-full mono">
                          {definition.lineTypes.map((line) => (
                            <option key={line.lineCode} value={line.lineCode}>{line.lineCode}</option>
                          ))}
                        </select>
                      </td>
                      <td className="py-2 pr-2">
                        <input value={item.lineTextValue ?? ""} onChange={(e) => update(i, { lineTextValue: e.target.value })} className="input-ghost w-full" />
                      </td>
                      <td className="py-2 pr-2">
                        <input type="number" value={item.amount} onChange={(e) => update(i, { amount: +e.target.value })} className="input-ghost w-full text-right mono" />
                      </td>
                      <td className="py-2 pr-2">
                        <input type="number" value={getRatePercent(item)} onChange={(e) => changeRate(i, +e.target.value)} className="input-ghost w-full text-right mono" />
                      </td>
                      <td className="py-2">
                        <span className="text-[10px] uppercase font-semibold px-2 py-0.5 rounded bg-success/15 text-success">{item.origin.replace(/_/g, " ")}</span>
                      </td>
                      <td className="py-2 text-right">
                        <button onClick={() => remove(i)} className="text-muted-foreground hover:text-destructive" aria-label="Remove line item">
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <button onClick={addItem} className="inline-flex items-center gap-1 text-sm text-accent hover:underline">
            <Plus className="h-3.5 w-3.5" /> Add line item
          </button>
        </div>
      )}
    </div>
  );
}
