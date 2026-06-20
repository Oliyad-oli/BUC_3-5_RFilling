import type { ReactNode } from "react";

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={`glass-card border border-border rounded-2xl p-6 sm:p-8 fade-in ${className}`}>{children}</div>;
}

export function CardHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="mb-6 pb-5 border-b border-border bg-muted/5 -mx-6 -mt-6 px-6 pt-6 sm:-mx-8 sm:-mt-8 sm:px-8 sm:pt-8 rounded-t-2xl">
      {subtitle && <div className="text-[11px] font-bold uppercase tracking-widest text-muted-foreground mb-1">{subtitle}</div>}
      <h2 className="text-xl font-bold text-foreground">{title}</h2>
    </div>
  );
}

export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="block">
      <div className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">{label}</div>
      {children}
      {hint && <div className="text-xs text-muted-foreground mt-2 font-medium">{hint}</div>}
    </label>
  );
}

export function Footer({ children }: { children: ReactNode }) {
  return <div className="mt-8 pt-6 border-t border-border flex justify-between items-center gap-4 flex-wrap bg-muted/5 -mx-6 -mb-6 px-6 pb-6 sm:-mx-8 sm:-mb-8 sm:px-8 sm:pb-8 rounded-b-2xl">{children}</div>;
}

export function Row({ label, value, bold, negative }: { label: string; value: string; bold?: boolean; negative?: boolean }) {
  return (
    <div className={`flex justify-between px-5 py-3.5 transition-colors hover:bg-muted/10 ${bold ? "bg-muted/20 font-bold text-foreground" : "text-muted-foreground"}`}>
      <div className="text-sm">{label}</div>
      <div className={`mono text-sm font-medium ${negative ? "text-destructive" : ""}`}>{value}</div>
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
    <div className="text-sm text-muted-foreground bg-muted/10 border border-dashed border-border rounded-xl p-10 flex items-center justify-center text-center font-medium">
      {text}
    </div>
  );
}