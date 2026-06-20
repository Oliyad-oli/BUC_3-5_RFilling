import { FileText, Paperclip, Trash2, UploadCloud } from "lucide-react";
import { useRef } from "react";
import type { Attachment } from "@/lib/filing-store";
import { toast } from "sonner";

const ACCEPT = ".pdf,.jpg,.jpeg,.png";
const ACCEPTED_MIME = ["application/pdf", "image/jpeg", "image/png"];

export function DailyReturnAttachments({
  attachments,
  onChange,
  accept = ACCEPT,
  acceptedMime = ACCEPTED_MIME,
  acceptLabel = "PDF, JPG, PNG",
  readOnly,
}: {
  attachments: Attachment[];
  onChange: (a: Attachment[]) => void;
  accept?: string;
  acceptedMime?: string[];
  acceptLabel?: string;
  readOnly?: boolean;
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  const onFiles = (files: FileList | null) => {
    if (!files) return;
    const next: Attachment[] = [];
    for (const file of Array.from(files)) {
      if (acceptedMime.length > 0 && !acceptedMime.includes(file.type)) {
        toast.error(`Rejected ${file.name}: unsupported format`);
        continue;
      }
      next.push({ id: "AT" + Math.random().toString(36).slice(2, 7), name: file.name, size: file.size, mime: file.type });
    }
    if (next.length > 0) {
      onChange([...attachments, ...next]);
      toast.success(`${next.length} file${next.length > 1 ? "s" : ""} attached`);
    }
  };

  const remove = (id: string) => onChange(attachments.filter((a) => a.id !== id));

  return (
    <div className="space-y-3">
      {!readOnly && (
        <div
          onClick={() => inputRef.current?.click()}
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault();
            onFiles(e.dataTransfer.files);
          }}
          className="border-2 border-dashed border-border rounded-md p-6 text-center cursor-pointer hover:border-accent hover:bg-accent/5 transition-colors"
        >
          <UploadCloud className="h-6 w-6 mx-auto text-muted-foreground" />
          <div className="text-sm mt-2">
            <span className="font-medium text-accent">Click to upload</span>
            <span className="text-muted-foreground"> or drag and drop</span>
          </div>
          <div className="text-xs text-muted-foreground mt-1">{acceptLabel}</div>
          <input ref={inputRef} type="file" multiple accept={accept} className="hidden" onChange={(e) => onFiles(e.target.files)} />
        </div>
      )}
      {attachments.length === 0
        ? readOnly && (
            <div className="text-sm text-muted-foreground border border-dashed border-border rounded-md p-4 text-center">
              No attachments
            </div>
          )
        : (
            <ul className="border border-border rounded-md divide-y divide-border">
              {attachments.map((a) => (
                <li key={a.id} className="flex items-center gap-3 px-4 py-2.5 text-sm">
                  {a.mime === "application/pdf" ? <FileText className="h-4 w-4 text-destructive" /> : <Paperclip className="h-4 w-4 text-muted-foreground" />}
                  <span className="flex-1 truncate">{a.name}</span>
                  <span className="text-xs text-muted-foreground mono">{(a.size / 1024).toFixed(1)} KB</span>
                  {!readOnly && (
                    <button onClick={() => remove(a.id)} className="text-muted-foreground hover:text-destructive" aria-label="Remove file">
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
    </div>
  );
}