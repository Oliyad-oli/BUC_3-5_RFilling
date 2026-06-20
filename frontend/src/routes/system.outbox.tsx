import { createFileRoute } from "@tanstack/react-router";
import { useApp } from "@/lib/store";
import { useState, useEffect } from "react";
import type { OutboxEntry } from "@/lib/mock-data";

export const Route = createFileRoute("/system/outbox")({ component: Outbox });

const styles: Record<OutboxEntry["status"], string> = {
  PENDING: "bg-muted text-muted-foreground",
  SENT: "bg-success/15 text-success",
  FAILED: "bg-destructive/15 text-destructive",
};

function Outbox() {
  const outbox = useApp((s) => s.outbox);
  const retryOutboxEntry = useApp((s) => s.retryOutboxEntry);
  const [selected, setSelected] = useState<OutboxEntry | null>(null);
  const [tick, setTick] = useState(0);
  useEffect(() => { const i = setInterval(() => setTick((t) => t + 1), 5000); return () => clearInterval(i); }, []);
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Outbox Monitor</h1>
          <p className="text-sm text-muted-foreground mt-1">Live event publishing queue</p>
        </div>
        <div className="text-sm text-muted-foreground inline-flex items-center gap-2">
          <span className="pulse-dot" /> Live · refresh #{tick}
        </div>
      </div>
      <div className="bg-card border border-border rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs text-muted-foreground uppercase">
            <tr>
              <th className="px-5 py-3 text-left font-medium">ID</th>
              <th className="px-5 py-3 text-left font-medium">Topic</th>
              <th className="px-5 py-3 text-left font-medium">Entity</th>
              <th className="px-5 py-3 text-left font-medium">Status</th>
              <th className="px-5 py-3 text-right font-medium">Tries</th>
            </tr>
          </thead>
          <tbody>
            {outbox.map((o) => (
              <tr key={o.id} onClick={() => setSelected(o)} className={`border-t border-border cursor-pointer hover:bg-muted/30 ${selected?.id === o.id ? "bg-accent/5" : ""}`}>
                <td className="px-5 py-3 mono text-xs">{o.id}</td>
                <td className="px-5 py-3 mono text-xs">{o.topic}</td>
                <td className="px-5 py-3 mono text-xs">{o.entity}</td>
                <td className="px-5 py-3"><span className={`text-[10px] uppercase font-semibold px-2 py-0.5 rounded ${styles[o.status]}`}>{o.status}</span></td>
                <td className="px-5 py-3 text-right mono">{o.tries}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {selected && (
        <div className="bg-card border border-border rounded-lg p-5 fade-in">
          <h2 className="font-semibold mono">{selected.id} — {selected.status}</h2>
          <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
            <div><div className="text-xs text-muted-foreground">Topic</div><div className="mono">{selected.topic}</div></div>
            <div><div className="text-xs text-muted-foreground">Entity</div><div className="mono">{selected.entity}</div></div>
            <div><div className="text-xs text-muted-foreground">Attempts</div><div className="mono">{selected.tries} / 5{selected.tries >= 5 ? " (MAX)" : ""}</div></div>
          </div>
          {selected.lastError && (
            <div className="mt-3 bg-destructive/10 border border-destructive/30 rounded p-3 text-xs text-destructive mono">{selected.lastError}</div>
          )}
          {selected.status === "FAILED" && (
            <div className="mt-4 flex gap-2">
              <button className="btn-primary" onClick={() => { retryOutboxEntry(selected.id); setSelected({ ...selected, status: "SENT", tries: selected.tries + 1, lastError: undefined }); }}>Retry Manually</button>
              <button className="btn-ghost">Mark Void</button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
