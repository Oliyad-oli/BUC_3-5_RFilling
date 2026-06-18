import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/$")({
  component: () => (
    <div className="bg-card border border-dashed border-border rounded-lg p-12 text-center text-muted-foreground">
      <h2 className="text-lg font-semibold text-foreground">Coming soon</h2>
      <p className="mt-2 text-sm">This area of the demo is not implemented yet.</p>
    </div>
  ),
});
