import type { ReturnStatus, FilingPeriodStatus } from "@/lib/mock-data";
import {
  CheckCircle2,
  AlertTriangle,
  Loader2,
  FileText,
  Edit3,
  XCircle,
  ShieldAlert,
  Calendar,
} from "lucide-react";

const returnStyles: Record<
  ReturnStatus,
  { bg: string; text: string; label: string; icon?: React.ReactNode }
> = {
  DRAFT: {
    bg: "bg-muted",
    text: "text-muted-foreground",
    label: "DRAFT",
    icon: <FileText className="h-3 w-3" />,
  },
  CALCULATING: {
    bg: "bg-info/15",
    text: "text-info",
    label: "CALCULATING",
    icon: <Loader2 className="h-3 w-3 animate-spin" />,
  },
  CALCULATION_FAILED: {
    bg: "bg-destructive/15",
    text: "text-destructive",
    label: "CALC FAILED",
    icon: <XCircle className="h-3 w-3" />,
  },
  ACCEPTED: { bg: "bg-info/15", text: "text-info", label: "ACCEPTED" },
  POSTED_TO_LEDGER: { bg: "bg-accent/15", text: "text-accent", label: "POSTED" },
  UNDER_VALIDATION: {
    bg: "bg-warning/15",
    text: "text-warning",
    label: "UNDER VALIDATION",
    icon: <Loader2 className="h-3 w-3 animate-spin" />,
  },
  COMPLETED: {
    bg: "bg-success/15",
    text: "text-success",
    label: "COMPLETED",
    icon: <CheckCircle2 className="h-3 w-3" />,
  },
  MANUAL_REVIEW: {
    bg: "bg-warning/20",
    text: "text-warning",
    label: "MANUAL REVIEW",
    icon: <AlertTriangle className="h-3 w-3" />,
  },
  FRAUD_CONFIRMED: {
    bg: "bg-destructive/20",
    text: "text-destructive",
    label: "FRAUD CONFIRMED",
    icon: <ShieldAlert className="h-3 w-3" />,
  },
  AMENDMENT_DRAFT: {
    bg: "bg-purple-500/15",
    text: "text-purple-600",
    label: "AMENDMENT DRAFT",
    icon: <Edit3 className="h-3 w-3" />,
  },
  AMENDMENT_CALCULATING: {
    bg: "bg-purple-500/15",
    text: "text-purple-600",
    label: "AMEND CALCULATING",
    icon: <Loader2 className="h-3 w-3 animate-spin" />,
  },
  AMENDMENT_ACCEPTED: { bg: "bg-purple-500/15", text: "text-purple-600", label: "AMEND ACCEPTED" },
  AMENDMENT_POSTED: {
    bg: "bg-purple-500/15",
    text: "text-purple-600",
    label: "AMEND POSTED",
    icon: <CheckCircle2 className="h-3 w-3" />,
  },
};

const periodStyles: Record<FilingPeriodStatus, { bg: string; text: string; label: string }> = {
  FUTURE: { bg: "bg-muted", text: "text-muted-foreground", label: "FUTURE" },
  OPEN: { bg: "bg-info/15", text: "text-info", label: "OPEN" },
  DUE: { bg: "bg-warning/15", text: "text-warning", label: "DUE" },
  OVERDUE: { bg: "bg-destructive/15", text: "text-destructive", label: "OVERDUE" },
  FILED: { bg: "bg-success/15", text: "text-success", label: "FILED" },
};

export function StatusBadge({ status }: { status: ReturnStatus }) {
  const s = returnStyles[status] ?? {
    bg: "bg-muted",
    text: "text-muted-foreground",
    label: status,
    icon: null,
  };
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium transition-all ${s.bg} ${s.text}`}
    >
      {s.icon}
      {s.label}
    </span>
  );
}

export function PeriodStatusBadge({ status }: { status: FilingPeriodStatus }) {
  const s = periodStyles[status] ?? {
    bg: "bg-muted",
    text: "text-muted-foreground",
    label: status,
  };
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${s.bg} ${s.text}`}
    >
      {s.label}
    </span>
  );
}
