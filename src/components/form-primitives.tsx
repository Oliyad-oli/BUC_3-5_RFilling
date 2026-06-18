import type { ReactNode } from "react";

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={`bg-card border border-border rounded-lg p-6 fade-in ${className}`}>{children}</div>;
}

export function CardHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="mb-5 pb-4 border-b border-border">
      {subtitle && <div className="text-xs uppercase tracking-wide text-muted-foreground">{subtitle}</div>}
      <h2 className="text-lg font-semibold mt-1">{title}</h2>
    </div>
  );
}

export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="block">
      <div className="text-xs font-medium text-muted-foreground mb-1.5">{label}</div>
      {children}
      {hint && <div className="text-[11px] text-muted-foreground mt-1">{hint}</div>}
    </label>
  );
}

export function Footer({ children }: { children: ReactNode }) {
  return <div className="mt-6 pt-4 border-t border-border flex justify-between gap-3 flex-wrap">{children}</div>;
}

export function Row({ label, value, bold, negative }: { label: string; value: string; bold?: boolean; negative?: boolean }) {
  return (
    <div className={`flex justify-between px-4 py-3 ${bold ? "bg-muted/40 font-semibold" : ""}`}>
      <div className="text-sm">{label}</div>
      <div className={`mono text-sm ${negative ? "text-destructive" : ""}`}>{value}</div>
    </div>
  );
}

export function Info2({ label, value, tone }: { label: string; value: string; tone?: "success" | "warning" | "danger" }) {
  const t =
    tone === "success" ? "text-success" : tone === "warning" ? "text-warning" : tone === "danger" ? "text-destructive" : "";
  return (
    <div>
      <div className="text-[11px] uppercase text-muted-foreground tracking-wide">{label}</div>
      <div className={`mt-0.5 mono text-sm ${t}`}>{value}</div>
    </div>
  );
}

export function Empty({ text }: { text: string }) {
  return (
    <div className="text-sm text-muted-foreground bg-muted/30 border border-dashed border-border rounded-md p-8 text-center">
      {text}
    </div>
  );
}