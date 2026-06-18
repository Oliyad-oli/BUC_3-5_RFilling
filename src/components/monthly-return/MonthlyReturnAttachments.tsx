import { DailyReturnAttachments } from "@/components/daily-return/DailyReturnAttachments";
import type { Attachment } from "@/lib/filing-store";

const ACCEPT = ".pdf,.jpg,.jpeg,.png,.xls,.xlsx";
const ACCEPTED_MIME = [
  "application/pdf",
  "image/jpeg",
  "image/png",
  "application/vnd.ms-excel",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
];

export function MonthlyReturnAttachments({
  attachments,
  onChange,
  readOnly,
}: {
  attachments: Attachment[];
  onChange: (a: Attachment[]) => void;
  readOnly?: boolean;
}) {
  return (
    <DailyReturnAttachments
      attachments={attachments}
      onChange={onChange}
      readOnly={readOnly}
      accept={ACCEPT}
      acceptedMime={ACCEPTED_MIME}
      acceptLabel="Invoices — PDF, JPG, PNG, Excel"
    />
  );
}