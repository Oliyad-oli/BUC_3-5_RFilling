import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { ArrowLeft, Eye, Save, ShieldCheck, UploadCloud, XCircle } from "lucide-react";
import { Card, CardHeader, Footer } from "@/components/form-primitives";
import { MonthlyReturnForm, type MonthlyReturnFormState } from "@/components/monthly-return/MonthlyReturnForm";
import { MonthlyReturnLinesTable } from "@/components/monthly-return/MonthlyReturnLinesTable";
import { MonthlyReturnSchedules } from "@/components/monthly-return/MonthlyReturnSchedules";
import { MonthlyReturnSummary } from "@/components/monthly-return/MonthlyReturnSummary";
import { MonthlyReturnAttachments } from "@/components/monthly-return/MonthlyReturnAttachments";
import { MonthlyReturnWorkflowTimeline } from "@/components/monthly-return/MonthlyReturnWorkflowTimeline";
import { MonthlyReturnAcknowledgement } from "@/components/monthly-return/MonthlyReturnAcknowledgement";
import {
  newMonthlyId,
  useFiling,
  type Attachment,
  type MonthlyReturn,
  type MonthlyReturnLine,
  type MonthlySchedule,
} from "@/lib/filing-store";
import { TAXPAYER } from "@/lib/mock-data";

export const Route = createFileRoute("/returns/monthly/new")({
  component: NewMonthlyReturn,
});

const PIPELINE_STAGES: MonthlyReturn["pipelineStage"][] = [
  "SUBMITTED",
  "CROSS_MATCH",
  "RISK_SCORING",
  "VALIDATION",
  "WORKFLOW_ROUTING",
  "ACKNOWLEDGED",
];

function NewMonthlyReturn() {
  const navigate = useNavigate();
  const upsert = useFiling((s) => s.upsertMonthlyDraft);
  const validateApi = useFiling((s) => s.validateMonthly);
  const submitApi = useFiling((s) => s.submitMonthly);
  const advance = useFiling((s) => s.advanceMonthlyPipeline);
  const cancelApi = useFiling((s) => s.cancelMonthly);

  const [id] = useState(newMonthlyId);
  const [form, setForm] = useState<MonthlyReturnFormState>({
    tin: TAXPAYER.tin,
    taxpayerName: TAXPAYER.party,
    taxType: "VAT",
    filingPeriod: "Jun 2026",
    businessCategory: "Large Taxpayer",
    taxOffice: "Addis Ababa LTO",
  });
  const [lines, setLines] = useState<MonthlyReturnLine[]>([]);
  const [schedules, setSchedules] = useState<MonthlySchedule[]>([]);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [validated, setValidated] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [pipelineStage, setPipelineStage] = useState<MonthlyReturn["pipelineStage"]>("NONE");
  const [pipelineActive, setPipelineActive] = useState(false);
  const [drafted, setDrafted] = useState(false);
  const [preview, setPreview] = useState(false);
  const [ackOpen, setAckOpen] = useState(false);
  const [ackReturn, setAckReturn] = useState<MonthlyReturn | null>(null);

  const summary = useMemo(() => {
    const grossAmount = lines.reduce((s, l) => s + l.grossAmount, 0);
    const taxableAmount = lines.reduce((s, l) => s + l.taxableAmount, 0);
    const totalTax = lines.reduce((s, l) => s + l.vatAmount, 0);
    const credits = lines.reduce((s, l) => s + l.creditAmount, 0);
    const penalty = 0;
    const interest = 0;
    const netPayable = +(totalTax - credits + penalty + interest).toFixed(2);
    return { grossAmount, taxableAmount, totalTax, credits, penalty, interest, netPayable };
  }, [lines]);

  const build = (status: MonthlyReturn["status"]): MonthlyReturn => ({
    id,
    tin: form.tin,
    taxpayerName: form.taxpayerName,
    taxType: form.taxType,
    filingPeriod: form.filingPeriod,
    businessCategory: form.businessCategory,
    taxOffice: form.taxOffice,
    status,
    pipelineStage: "NONE",
    lines,
    schedules,
    attachments,
    summary,
    events: [{ time: timeNow(), label: status === "DRAFT" ? "Draft saved" : "Draft created", actor: "PORTAL" }],
    createdAt: new Date().toISOString().slice(0, 10),
  });

  const requireBasics = () => {
    if (!form.tin.trim() || !form.taxpayerName.trim()) {
      toast.error("TIN and taxpayer name are required");
      return false;
    }
    if (lines.length === 0) {
      toast.error("Add at least one return line");
      return false;
    }
    return true;
  };

  const onSaveDraft = () => {
    if (!form.tin.trim() || !form.taxpayerName.trim()) {
      toast.error("Taxpayer details are required");
      return;
    }
    upsert(build("DRAFT"));
    setDrafted(true);
    toast.success(`Draft ${id} saved`);
  };

  const onValidate = () => {
    if (!requireBasics()) return;
    upsert(build("DRAFT"));
    setDrafted(true);
    validateApi(id);
    setValidated(true);
    toast.success("Pre-submit validation passed");
  };

  const onSubmit = () => {
    if (submitted) return;
    if (!requireBasics()) return;
    upsert(build("DRAFT"));
    const result = submitApi(id);
    if (!result) return;
    setSubmitted(true);
    setPipelineActive(true);
    setPipelineStage("SUBMITTED");
    // Animate the pipeline stages
    PIPELINE_STAGES.forEach((stage, i) => {
      window.setTimeout(() => {
        setPipelineStage(stage);
        advance(id, stage);
        if (stage === "ACKNOWLEDGED") {
          setPipelineActive(false);
          const ret = useFiling.getState().getMonthly(id) ?? null;
          setAckReturn(ret);
          setAckOpen(true);
        }
      }, (i + 1) * 700);
    });
  };

  const onAmend = () => {
    const newId = useFiling.getState().amendMonthly(id);
    if (newId) {
      toast.success(`Amendment draft ${newId} created`);
      navigate({ to: "/returns/monthly/$id", params: { id: newId } });
    }
  };

  const onCancel = () => {
    if (drafted) cancelApi(id);
    navigate({ to: "/returns/monthly" });
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-semibold">Submit Monthly Return</h1>
          <p className="text-sm text-muted-foreground mt-1">Reference: <span className="mono">{id}</span></p>
        </div>
      </div>

      <Card>
        <CardHeader title="Taxpayer Information" subtitle="Section 1 — BUC-EFR-005" />
        <MonthlyReturnForm value={form} onChange={setForm} readOnly={submitted} />
      </Card>

      <Card>
        <CardHeader title="Monthly Return Lines" subtitle="Section 2" />
        <MonthlyReturnLinesTable lines={lines} onChange={setLines} readOnly={submitted} />
      </Card>

      <Card>
        <CardHeader title="Schedules" subtitle="Section 3 — Schedule A, B, C" />
        <MonthlyReturnSchedules schedules={schedules} onChange={setSchedules} readOnly={submitted} />
      </Card>

      <Card>
        <CardHeader title="Attachments" subtitle="Section 4 — Invoices" />
        <MonthlyReturnAttachments attachments={attachments} onChange={setAttachments} readOnly={submitted} />
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader title="Summary" subtitle="Section 5" />
          <MonthlyReturnSummary summary={summary} />
        </Card>
        <Card>
          <CardHeader title="Processing Pipeline" subtitle="Submission workflow" />
          <MonthlyReturnWorkflowTimeline pipelineStage={pipelineStage} active={pipelineActive} />
        </Card>
      </div>

      <Card>
        <CardHeader title="Submission Workflow" subtitle="Section 6" />
        <Footer>
          <button className="btn-ghost" onClick={onCancel}>
            <XCircle className="h-4 w-4" /> Cancel
          </button>
          <div className="flex flex-wrap gap-2">
            <button className="btn-ghost" onClick={() => setPreview(true)} disabled={lines.length === 0}>
              <Eye className="h-4 w-4" /> Preview
            </button>
            <button className="btn-ghost" onClick={onSaveDraft} disabled={submitted}>
              <Save className="h-4 w-4" /> Save Draft
            </button>
            <button className="btn-ghost" onClick={onValidate} disabled={submitted}>
              <ShieldCheck className="h-4 w-4" /> Validate
            </button>
            <button className="btn-primary" onClick={onSubmit} disabled={submitted}>
              <UploadCloud className="h-4 w-4" /> {submitted ? "Submitted" : "Submit Return"}
            </button>
            {submitted && (
              <button className="btn-ghost" onClick={onAmend}>
                Amend Return
              </button>
            )}
          </div>
        </Footer>
        {validated && !submitted && (
          <div className="mt-4 bg-success/10 border border-success/40 rounded-md p-3 text-sm text-success">
            Pre-submit validation passed. You can now submit the return.
          </div>
        )}
      </Card>

      <button className="text-sm text-muted-foreground hover:text-foreground inline-flex items-center gap-1" onClick={() => navigate({ to: "/returns/monthly" })}>
        <ArrowLeft className="h-4 w-4" /> Back to monthly returns
      </button>

      {preview && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4 fade-in" onClick={() => setPreview(false)}>
          <div className="bg-card border border-border rounded-lg max-w-4xl w-full max-h-[85vh] overflow-auto shadow-lg" onClick={(e) => e.stopPropagation()}>
            <div className="px-5 py-4 border-b border-border flex items-center justify-between">
              <div>
                <div className="text-xs uppercase tracking-wide text-muted-foreground">Preview</div>
                <h2 className="text-lg font-semibold">{id}</h2>
              </div>
              <button className="btn-ghost" onClick={() => setPreview(false)}>Close</button>
            </div>
            <div className="p-5 space-y-4">
              <MonthlyReturnForm value={form} onChange={() => undefined} readOnly />
              <MonthlyReturnLinesTable lines={lines} onChange={() => undefined} readOnly />
              <MonthlyReturnSchedules schedules={schedules} onChange={() => undefined} readOnly />
              <MonthlyReturnSummary summary={summary} />
              <MonthlyReturnAttachments attachments={attachments} onChange={() => undefined} readOnly />
            </div>
          </div>
        </div>
      )}

      <MonthlyReturnAcknowledgement open={ackOpen} onClose={() => setAckOpen(false)} ret={ackReturn} />
    </div>
  );
}

function timeNow() {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}