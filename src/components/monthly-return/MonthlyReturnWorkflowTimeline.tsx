import { Check, Loader2 } from "lucide-react";
import type { MonthlyReturn, TimelineEvent } from "@/lib/filing-store";

const STAGES: { key: MonthlyReturn["pipelineStage"]; label: string }[] = [
  { key: "SUBMITTED", label: "Submitted" },
  { key: "CROSS_MATCH", label: "Cross Match" },
  { key: "RISK_SCORING", label: "Risk Scoring" },
  { key: "VALIDATION", label: "Validation" },
  { key: "WORKFLOW_ROUTING", label: "Workflow Routing" },
  { key: "ACKNOWLEDGED", label: "Acknowledged" },
];

function stageIdx(s: MonthlyReturn["pipelineStage"]) {
  return Math.max(0, STAGES.findIndex((x) => x.key === s));
}

export function MonthlyReturnWorkflowTimeline({
  pipelineStage,
  events,
  active,
}: {
  pipelineStage: MonthlyReturn["pipelineStage"];
  events?: TimelineEvent[];
  active?: boolean;
}) {
  const inactive = pipelineStage === "NONE";
  const idx = inactive ? -1 : stageIdx(pipelineStage);

  return (
    <div className="bg-card border border-border rounded-lg p-5 space-y-5">
      <div className="flex items-center">
        {STAGES.map((s, i) => {
          const done = i < idx || (i === idx && pipelineStage === "ACKNOWLEDGED");
          const current = i === idx && pipelineStage !== "ACKNOWLEDGED";
          return (
            <div key={s.key} className="flex items-center flex-1 last:flex-none">
              <div
                className={`h-7 w-7 rounded-full flex items-center justify-center text-xs font-semibold shrink-0 ${
                  done
                    ? "bg-success text-success-foreground"
                    : current
                    ? "bg-accent text-accent-foreground"
                    : "bg-muted text-muted-foreground"
                }`}
              >
                {done ? <Check className="h-3.5 w-3.5" /> : current && active ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : i + 1}
              </div>
              <div className={`ml-2 text-xs font-medium hidden md:block ${done || current ? "text-foreground" : "text-muted-foreground"}`}>{s.label}</div>
              {i < STAGES.length - 1 && <div className={`flex-1 h-px mx-2 ${i < idx ? "bg-success" : "bg-border"}`} />}
            </div>
          );
        })}
      </div>
      {events && events.length > 0 && (
        <ol className="relative border-l border-border ml-3">
          {events.map((ev, i) => (
            <li key={i} className="ml-5 pb-3 last:pb-0 relative">
              <div className="absolute -left-7 top-1.5 h-2.5 w-2.5 rounded-full bg-accent" />
              <div className="flex items-baseline gap-3 flex-wrap">
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