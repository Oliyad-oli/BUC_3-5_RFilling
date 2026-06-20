import { CheckCircle2, Loader2, Circle, Clock, Check, AlertCircle } from "lucide-react";
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
    <div className="glass-card border border-border rounded-2xl p-5 sm:p-8 space-y-8">
      {/* 
        Pipeline Cards Container 
        Uses Grid to ensure equal width and graceful wrapping on all screen sizes.
      */}
      <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-6 gap-3 sm:gap-4 relative">
        {STAGES.map((s, i) => {
          const done = i < idx || (i === idx && pipelineStage === "ACKNOWLEDGED");
          const current = i === idx && pipelineStage !== "ACKNOWLEDGED";
          
          let cardStyle = "bg-muted/10 border-border/50";
          let iconStyle = "text-muted-foreground";
          let badgeText = "Pending";
          let badgeStyle = "bg-muted text-muted-foreground";
          let Icon = Circle;

          if (done) {
            cardStyle = "bg-success/5 border-success/30 shadow-sm shadow-success/10";
            iconStyle = "text-success";
            badgeText = "Completed";
            badgeStyle = "bg-success/15 text-success";
            Icon = CheckCircle2;
          } else if (current) {
            cardStyle = "bg-accent/5 border-accent shadow-md shadow-accent/20 ring-1 ring-accent";
            iconStyle = "text-accent";
            badgeText = active ? "Processing" : "In Progress";
            badgeStyle = "bg-accent text-accent-foreground";
            Icon = active ? Loader2 : Clock;
          }

          return (
            <div 
              key={s.key} 
              className={`relative flex flex-col items-center justify-between p-4 rounded-xl border transition-all duration-300 hover:scale-[1.02] ${cardStyle}`}
            >
              {/* Connector line for desktop - absolute positioned behind cards (optional enhancement later) */}
              
              <div className="flex flex-col items-center flex-1 w-full gap-3">
                <div className={`h-8 w-8 rounded-full flex items-center justify-center bg-background border ${done ? 'border-success' : current ? 'border-accent' : 'border-border'}`}>
                  <Icon className={`h-4 w-4 ${iconStyle} ${current && active ? 'animate-spin' : ''}`} />
                </div>
                
                <div className={`flex-1 flex items-center justify-center w-full text-center`}>
                  <h3 className={`font-semibold leading-tight break-words text-[clamp(0.75rem,2vw,0.875rem)] ${done || current ? "text-foreground" : "text-muted-foreground"}`}>
                    {s.label.split(' ').map((word, i) => (
                      <span key={i} className="block">{word}</span>
                    ))}
                  </h3>
                </div>
              </div>

              <div className="mt-4 w-full flex justify-center">
                <span className={`text-[10px] uppercase tracking-widest font-bold px-2 py-1 rounded-full whitespace-nowrap ${badgeStyle}`}>
                  {badgeText}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {events && events.length > 0 && (
        <ol className="relative border-l border-border ml-4 mt-8 pt-4">
          {events.map((ev, i) => (
            <li key={i} className="ml-6 pb-5 last:pb-0 relative group">
              <div className="absolute -left-[1.8rem] top-1.5 h-3 w-3 rounded-full bg-background border-2 border-accent transition-transform group-hover:scale-125" />
              <div className="flex items-baseline gap-3 flex-wrap bg-card/40 p-3 rounded-lg border border-border hover:bg-card/80 transition-colors">
                <span className="mono text-xs text-muted-foreground min-w-[3rem] font-semibold">{ev.time}</span>
                <span className="text-sm font-semibold flex-1 min-w-[150px] text-foreground">{ev.label}</span>
                <span
                  className={`text-[10px] uppercase tracking-widest font-bold px-2 py-1 rounded-md ${
                    ev.actor === "SYSTEM"
                      ? "bg-accent/10 text-accent ring-1 ring-accent/20"
                      : ev.actor === "TAXPAYER"
                      ? "bg-success/10 text-success ring-1 ring-success/20"
                      : "bg-muted text-muted-foreground ring-1 ring-border"
                  }`}
                >
                  {ev.actor}
                </span>
                {ev.detail && <span className="text-xs text-muted-foreground w-full mono bg-muted/20 p-2 rounded mt-1 break-all">{ev.detail}</span>}
              </div>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}