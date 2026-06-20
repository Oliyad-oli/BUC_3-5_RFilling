import { AlertTriangle, RotateCcw, ArrowLeft, RefreshCw } from "lucide-react";
import { Link, useRouter } from "@tanstack/react-router";

export function RouteError({ error, reset }: { error: Error; reset: () => void }) {
  const router = useRouter();

  return (
    <div className="min-h-[60vh] flex items-center justify-center p-6">
      <div className="max-w-xl w-full bg-card border border-destructive/20 rounded-2xl shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-300">
        <div className="bg-destructive/10 border-b border-destructive/20 px-6 py-8 text-center flex flex-col items-center">
          <div className="h-16 w-16 bg-destructive/20 rounded-full flex items-center justify-center mb-4 ring-8 ring-destructive/5">
            <AlertTriangle className="h-8 w-8 text-destructive" />
          </div>
          <h2 className="text-2xl font-bold tracking-tight text-foreground">Something went wrong</h2>
          <p className="text-muted-foreground mt-2 text-sm max-w-sm">
            We encountered an unexpected error while trying to load this page. Our team has been notified.
          </p>
        </div>
        
        <div className="p-6 bg-muted/10">
          <div className="bg-background border border-border rounded-lg p-4 font-mono text-xs overflow-auto max-h-48">
            <div className="font-semibold text-destructive mb-2">{error.message}</div>
            <div className="text-muted-foreground break-all whitespace-pre-wrap">{error.stack}</div>
          </div>
        </div>

        <div className="px-6 py-4 border-t border-border bg-card flex flex-col sm:flex-row gap-3">
          <button 
            onClick={reset}
            className="flex-1 btn-primary bg-accent hover:bg-accent/90 flex justify-center items-center gap-2 h-11"
          >
            <RotateCcw className="h-4 w-4" /> Try Again
          </button>
          <button 
            onClick={() => window.location.reload()}
            className="flex-1 bg-muted hover:bg-muted/80 text-foreground font-medium rounded-lg flex justify-center items-center gap-2 h-11 transition-colors"
          >
            <RefreshCw className="h-4 w-4 text-muted-foreground" /> Reload Page
          </button>
          <button
            onClick={() => router.history.back()}
            className="flex-1 bg-transparent border border-border hover:bg-muted text-foreground font-medium rounded-lg flex justify-center items-center gap-2 h-11 transition-colors"
          >
            <ArrowLeft className="h-4 w-4 text-muted-foreground" /> Go Back
          </button>
        </div>
      </div>
    </div>
  );
}
