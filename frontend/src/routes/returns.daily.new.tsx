import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { ArrowLeft, Eye, Save, UploadCloud, XCircle } from "lucide-react";
import { Card, CardHeader, Footer } from "@/components/form-primitives";
import { DailyReturnForm, type DailyReturnFormState } from "@/components/daily-return/DailyReturnForm";
import { DailyReturnItemsTable } from "@/components/daily-return/DailyReturnItemsTable";
import { DailyReturnSummaryCard } from "@/components/daily-return/DailyReturnSummaryCard";
import { DailyReturnAttachments } from "@/components/daily-return/DailyReturnAttachments";
import { DailyReturnTimeline } from "@/components/daily-return/DailyReturnTimeline";
import { DailyReturnAcknowledgement } from "@/components/daily-return/DailyReturnAcknowledgement";
import { newDailyId, useFiling, type Attachment, type DailyReturn, type DailyReturnItem } from "@/lib/filing-store";
import { TAXPAYER } from "@/lib/mock-data";

export const Route = createFileRoute("/returns/daily/new")({
  component: NewDailyReturn,
});

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

function NewDailyReturn() {
  const navigate = useNavigate();
  const upsert = useFiling((s) => s.upsertDailyDraft);
  const submit = useFiling((s) => s.submitDaily);
  const cancel = useFiling((s) => s.cancelDaily);

  const [id] = useState(newDailyId);
  const [form, setForm] = useState<DailyReturnFormState>({
    tin: TAXPAYER.tin,
    taxpayerName: TAXPAYER.party,
    taxType: "VAT",
    filingPeriod: "",
    businessSector: "Wholesale & Retail Trade",
    submissionDate: todayISO(),
  });
  const [items, setItems] = useState<DailyReturnItem[]>([]);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [drafted, setDrafted] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [preview, setPreview] = useState(false);
  const [ackOpen, setAckOpen] = useState(false);
  const [ackReturn, setAckReturn] = useState<DailyReturn | null>(null);

  const summary = useMemo(() => {
    const grossSales = items.reduce((s, i) => s + i.amount, 0);
    const taxableSales = items.filter((i) => i.taxable).reduce((s, i) => s + i.amount, 0);
    const exemptSales = items.filter((i) => !i.taxable).reduce((s, i) => s + i.amount, 0);
    const taxAmount = items.reduce((s, i) => s + i.taxAmount, 0);
    const penalty = 0;
    const netPayable = +(taxAmount + penalty).toFixed(2);
    return { grossSales, taxableSales, exemptSales, taxAmount, penalty, netPayable };
  }, [items]);

  const buildReturn = (status: DailyReturn["status"]): DailyReturn => ({
    id,
    tin: form.tin,
    taxpayerName: form.taxpayerName,
    taxType: form.taxType,
    filingPeriod: form.filingPeriod || form.submissionDate,
    businessSector: form.businessSector,
    submissionDate: form.submissionDate,
    status,
    items,
    summary,
    attachments,
    events: [{ time: timeNow(), label: status === "DRAFT" ? "Draft saved" : "Draft created", actor: "PORTAL" }],
    createdAt: new Date().toISOString().slice(0, 10),
  });

  const validate = (): boolean => {
    if (!form.tin.trim()) return toastErr("TIN is required");
    if (!form.taxpayerName.trim()) return toastErr("Taxpayer name is required");
    if (!form.submissionDate) return toastErr("Submission date is required");
    if (items.length === 0) return toastErr("Add at least one return item");
    if (items.some((i) => !i.description.trim())) return toastErr("All items must have a description");
    return true;
  };

  const onSaveDraft = () => {
    if (!form.tin.trim() || !form.taxpayerName.trim()) {
      toast.error("Taxpayer details are required to save a draft");
      return;
    }
    upsert(buildReturn("DRAFT"));
    setDrafted(true);
    toast.success(`Draft saved as ${id}`);
  };

  const onSubmit = () => {
    if (submitted) return;
    if (!validate()) return;
    upsert(buildReturn("DRAFT"));
    const updated = submit(id);
    setSubmitted(true);
    if (updated) {
      setAckReturn(updated);
      setAckOpen(true);
    }
  };

  const onCancel = () => {
    if (drafted) cancel(id);
    navigate({ to: "/returns/daily" });
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-semibold">Register Daily Return</h1>
          <p className="text-sm text-muted-foreground mt-1">Reference: <span className="mono">{id}</span></p>
        </div>
      </div>

      <Card>
        <CardHeader title="Taxpayer Information" subtitle="Daily Return — BUC-EFR-003" />
        <DailyReturnForm value={form} onChange={setForm} readOnly={submitted} />
      </Card>

      <Card>
        <CardHeader title="Return Items" subtitle="Auto-calculated totals" />
        <DailyReturnItemsTable items={items} onChange={setItems} readOnly={submitted} />
        <div className="mt-5">
          <DailyReturnSummaryCard summary={summary} />
        </div>
      </Card>

      <Card>
        <CardHeader title="Attachments" subtitle="Supporting documents" />
        <DailyReturnAttachments attachments={attachments} onChange={setAttachments} readOnly={submitted} />
      </Card>

      <DailyReturnTimeline status={submitted ? "ACKNOWLEDGED" : drafted ? "DRAFT" : "DRAFT"} />

      <Card>
        <CardHeader title="Actions" subtitle="Submit or save your draft" />
        <Footer>
          <button className="btn-ghost" onClick={onCancel}>
            <XCircle className="h-4 w-4" /> Cancel
          </button>
          <div className="flex flex-wrap gap-2">
            <button className="btn-ghost" onClick={() => setPreview(true)} disabled={items.length === 0}>
              <Eye className="h-4 w-4" /> Preview
            </button>
            <button className="btn-ghost" onClick={onSaveDraft} disabled={submitted}>
              <Save className="h-4 w-4" /> Save Draft
            </button>
            <button className="btn-primary" onClick={onSubmit} disabled={submitted}>
              <UploadCloud className="h-4 w-4" /> {submitted ? "Submitted" : "Submit"}
            </button>
          </div>
        </Footer>
      </Card>

      <button className="text-sm text-muted-foreground hover:text-foreground inline-flex items-center gap-1" onClick={() => navigate({ to: "/returns/daily" })}>
        <ArrowLeft className="h-4 w-4" /> Back to daily returns
      </button>

      {preview && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4 fade-in" onClick={() => setPreview(false)}>
          <div className="bg-card border border-border rounded-lg max-w-3xl w-full max-h-[85vh] overflow-auto shadow-lg" onClick={(e) => e.stopPropagation()}>
            <div className="px-5 py-4 border-b border-border flex items-center justify-between">
              <div>
                <div className="text-xs uppercase tracking-wide text-muted-foreground">Preview</div>
                <h2 className="text-lg font-semibold">{id}</h2>
              </div>
              <button className="btn-ghost" onClick={() => setPreview(false)}>Close</button>
            </div>
            <div className="p-5 space-y-4">
              <DailyReturnForm value={form} onChange={() => undefined} readOnly />
              <DailyReturnItemsTable items={items} onChange={() => undefined} readOnly />
              <DailyReturnSummaryCard summary={summary} />
              <DailyReturnAttachments attachments={attachments} onChange={() => undefined} readOnly />
            </div>
          </div>
        </div>
      )}

      <DailyReturnAcknowledgement open={ackOpen} onClose={() => setAckOpen(false)} ret={ackReturn} />
    </div>
  );
}

function toastErr(msg: string) {
  toast.error(msg);
  return false;
}

function timeNow() {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}