import { Check } from "lucide-react";
import type { DailyReturn, FilingStatus } from "@/lib/filing-store";

const STAGES: { key: FilingStatus; label: string }[] = [
  { key: "DRAFT", label: "Draft" },
  { key: "SUBMITTED", label: "Submitted" },
  { key: "VALIDATED", label: "Validated" },
  { key: "ACKNOWLEDGED", label: "Acknowledged" },
];

function stageIdx(status: FilingStatus) {
  if (status === "ACKNOWLEDGED") return 3;
  if (status === "VALIDATED") return 2;
  if (status === "SUBMITTED") return 1;
  return 0;
}

export function DailyReturnTimeline({ status, events }: { status: FilingStatus; events?: DailyReturn["events"] }) {
  const idx = stageIdx(status);
  return (
    <div className="bg-card border border-border rounded-lg p-5 space-y-5">
      <div className="flex items-center">
        {STAGES.map((s, i) => (
          <div key={s.key} className="flex items-center flex-1 last:flex-none">
            <div
              className={`h-7 w-7 rounded-full flex items-center justify-center text-xs font-semibold shrink-0 ${
                i < idx
                  ? "bg-success text-success-foreground"
                  : i === idx
                  ? "bg-accent text-accent-foreground"
                  : "bg-muted text-muted-foreground"
              }`}
            >
              {i < idx ? <Check className="h-3.5 w-3.5" /> : i + 1}
            </div>
            <div className={`ml-2 text-xs font-medium hidden md:block ${i <= idx ? "text-foreground" : "text-muted-foreground"}`}>{s.label}</div>
            {i < STAGES.length - 1 && <div className={`flex-1 h-px mx-2 ${i < idx ? "bg-success" : "bg-border"}`} />}
          </div>
        ))}
      </div>
      {events && events.length > 0 && (
        <ol className="relative border-l border-border ml-3">
          {events.map((ev, i) => (
            <li key={i} className="ml-5 pb-3 last:pb-0 relative">
              <div className="absolute -left-7 top-1.5 h-2.5 w-2.5 rounded-full bg-accent" />
              <div className="flex items-baseline gap-3">
                <span className="mono text-xs text-muted-foreground w-12">{ev.time}</span>
                <span className="text-sm font-medium">{ev.label}</span>
                <span
                  className={`text-[10px] uppercase tracking-wide font-semibold px-1.5 py-0.5 rounded ${
                    ev.actor === "SYSTEM"
                      ? "bg-accent/15 text-accent"
                      : ev.actor === "TAXPAYER"
                      ? "bg-success/15 text-success"
                      : "bg-muted text-muted-foreground"
                  }`}
                >
                  {ev.actor}
                </span>
                {ev.detail && <span className="text-xs text-muted-foreground mono">{ev.detail}</span>}
              </div>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}