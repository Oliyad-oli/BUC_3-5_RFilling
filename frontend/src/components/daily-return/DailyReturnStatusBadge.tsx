import type { FilingStatus } from "@/lib/filing-store";
import { CheckCircle2, Clock, FileText, Loader2, ShieldCheck, XCircle } from "lucide-react";

const styles: Record<FilingStatus, { bg: string; text: string; label: string; icon?: React.ReactNode }> = {
  DRAFT: { bg: "bg-muted", text: "text-muted-foreground", label: "DRAFT", icon: <FileText className="h-3 w-3" /> },
  SUBMITTED: { bg: "bg-info/15", text: "text-info", label: "SUBMITTED", icon: <Loader2 className="h-3 w-3 animate-spin" /> },
  VALIDATED: { bg: "bg-accent/15", text: "text-accent", label: "VALIDATED", icon: <ShieldCheck className="h-3 w-3" /> },
  ACKNOWLEDGED: { bg: "bg-success/15", text: "text-success", label: "ACKNOWLEDGED", icon: <CheckCircle2 className="h-3 w-3" /> },
  CROSS_MATCH: { bg: "bg-info/15", text: "text-info", label: "CROSS MATCH", icon: <Loader2 className="h-3 w-3 animate-spin" /> },
  RISK_SCORING: { bg: "bg-warning/15", text: "text-warning", label: "RISK SCORING", icon: <Loader2 className="h-3 w-3 animate-spin" /> },
  VALIDATION: { bg: "bg-warning/15", text: "text-warning", label: "VALIDATION", icon: <Loader2 className="h-3 w-3 animate-spin" /> },
  WORKFLOW_ROUTING: { bg: "bg-accent/15", text: "text-accent", label: "WORKFLOW ROUTING", icon: <Clock className="h-3 w-3" /> },
  AMENDED: { bg: "bg-purple-500/15", text: "text-purple-600", label: "AMENDED" },
  CANCELLED: { bg: "bg-destructive/15", text: "text-destructive", label: "CANCELLED", icon: <XCircle className="h-3 w-3" /> },
};

export function DailyReturnStatusBadge({ status }: { status: FilingStatus }) {
  const s = styles[status];
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${s.bg} ${s.text}`}>
      {s.icon}
      {s.label}
    </span>
  );
}