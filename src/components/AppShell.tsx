import { Link, useRouterState, useNavigate } from "@tanstack/react-router";
import { useApp, type Role } from "@/lib/store";
import { useAuth } from "@/lib/auth-service";
import { ALL_SCENARIOS } from "@/lib/demo-scenarios";
import {
  Home, FileText, Bell, Settings, ClipboardList, Search, History,
  LayoutDashboard, Inbox, ShieldCheck, BarChart3, ChevronDown, User2, FilePlus2, CalendarDays, RotateCcw,
} from "lucide-react";
import { useState, useRef, useEffect } from "react";
import { toast } from "sonner";

interface NavItem { to: string; label: string; icon: React.ComponentType<{ className?: string }>; badge?: number }

const navByRole: Record<Role, NavItem[]> = {
  taxpayer: [
    { to: "/", label: "Dashboard", icon: Home },
    { to: "/returns/new", label: "File New Return", icon: FilePlus2 },
    { to: "/returns/daily/new", label: "Register Daily Return", icon: FilePlus2 },
    { to: "/returns/monthly/new", label: "Submit Monthly Return", icon: FilePlus2 },
    { to: "/obligations", label: "Filing Calendar", icon: CalendarDays, badge: 3 },
    { to: "/returns", label: "My Filing History", icon: FileText },
    { to: "/returns/daily", label: "Daily Returns", icon: FileText },
    { to: "/returns/monthly", label: "Monthly Returns", icon: FileText },
    { to: "/notifications", label: "Notifications", icon: Bell, badge: 2 },
    { to: "/settings", label: "Settings", icon: Settings },
  ],
  officer: [
    { to: "/officer", label: "Dashboard", icon: LayoutDashboard },
    { to: "/officer/queue", label: "Review Queue", icon: ClipboardList, badge: 3 },
    { to: "/officer/search", label: "Search Returns", icon: Search },
    { to: "/officer/history", label: "Decision History", icon: History },
    { to: "/officer/settings", label: "Settings", icon: Settings },
  ],
  system: [
    { to: "/system", label: "System Overview", icon: LayoutDashboard },
    { to: "/system/outbox", label: "Outbox Monitor", icon: Inbox },
    { to: "/system/validation", label: "Validation Engine", icon: ShieldCheck },
    { to: "/system/analytics", label: "Analytics", icon: BarChart3 },
  ],
};

const roleLabels: Record<Role, string> = {
  taxpayer: "Taxpayer Portal",
  officer: "Officer Portal",
  system: "System Dashboard",
};

const roleHomes: Record<Role, string> = {
  taxpayer: "/",
  officer: "/officer",
  system: "/system",
};

export function AppShell({ children }: { children: React.ReactNode }) {
  const session = useAuth((s) => s.session);
  const role = session?.user.role?.toLowerCase() as Role || "taxpayer";
  const navigate = useNavigate();
  const pathname = useRouterState({ select: (r) => r.location.pathname });
  const items = navByRole[role];

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Global status bar */}
      <div className="h-1 bg-success w-full" />

      <div className="flex flex-1">
        {/* Sidebar */}
        <aside className="w-60 bg-navy text-navy-foreground flex flex-col shrink-0">
          <div className="px-5 py-5 border-b border-white/10">
            <div className="text-[11px] uppercase tracking-wider text-white/60">Ethiopian Revenue Authority</div>
            <div className="font-semibold text-base mt-0.5">E-Filing Core</div>
          </div>

          <nav className="flex-1 py-3">
            {items.map((it) => {
              const active = pathname === it.to || (it.to !== "/" && pathname.startsWith(it.to));
              const Icon = it.icon;
              return (
                <Link
                  key={it.to}
                  to={it.to}
                  className={`flex items-center gap-3 px-5 py-2.5 text-sm transition-colors ${
                    active ? "bg-white/10 text-white border-l-2 border-accent" : "text-white/75 hover:bg-white/5 hover:text-white border-l-2 border-transparent"
                  }`}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  <span className="flex-1">{it.label}</span>
                  {it.badge ? (
                    <span className="bg-accent text-accent-foreground text-[10px] font-semibold px-1.5 py-0.5 rounded-full">{it.badge}</span>
                  ) : null}
                </Link>
              );
            })}
          </nav>

          <div className="px-4 py-3 border-t border-white/10">
            <div className="flex items-center gap-1.5 text-[10px] uppercase tracking-wide text-white/50 mb-2">
              <RotateCcw className="h-3 w-3" /> Demo Scenario
            </div>
            <select
              value={useApp.getState().activeScenarioId}
              onChange={(e) => {
                const scenario = ALL_SCENARIOS.find((s) => s.id === e.target.value);
                if (scenario) {
                  useApp.getState().loadScenario(scenario);
                  toast.success(`Loaded scenario: ${scenario.name}`);
                }
              }}
              className="w-full text-xs bg-white/10 text-white border border-white/20 rounded px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-accent"
            >
              {ALL_SCENARIOS.map((s) => (
                <option key={s.id} value={s.id} className="bg-navy text-white">
                  {s.name}
                </option>
              ))}
            </select>
          </div>

          <div className="px-5 py-4 border-t border-white/10 text-[11px] text-white/40">
            v1.0.0 · Demo Build
          </div>
        </aside>

        {/* Main */}
        <div className="flex-1 flex flex-col min-w-0">
          <TopBar role={role} />
          <main className="flex-1 overflow-auto">
            <div className="max-w-7xl mx-auto p-6 lg:p-8 fade-in">{children}</div>
          </main>
        </div>
      </div>
    </div>
  );
}

function TopBar({ role }: { role: Role }) {
  const [openRole, setOpenRole] = useState(false);
  const [openNotif, setOpenNotif] = useState(false);
  const notifs = useApp((s) => s.notifications);
  const markRead = useApp((s) => s.markAllRead);
  const unread = notifs.filter((n) => n.unread).length;
  const logout = useAuth((s) => s.logout);

  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpenRole(false); setOpenNotif(false);
      }
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  return (
    <header className="h-14 border-b border-border bg-card flex items-center justify-between px-6 shrink-0">
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <span className="pulse-dot" />
        <span>All systems operational</span>
      </div>

      <div ref={ref} className="flex items-center gap-3">
        {/* Notifications */}
        <div className="relative">
          <button
            onClick={() => { setOpenNotif((v) => !v); setOpenRole(false); }}
            className="relative h-9 w-9 rounded-md hover:bg-muted flex items-center justify-center"
            aria-label="Notifications"
          >
            <Bell className="h-4 w-4 text-foreground" />
            {unread > 0 && (
              <span className="absolute top-1 right-1 h-4 min-w-4 px-1 rounded-full bg-destructive text-destructive-foreground text-[10px] font-semibold flex items-center justify-center">
                {unread}
              </span>
            )}
          </button>
          {openNotif && (
            <div className="absolute right-0 mt-2 w-96 bg-card border border-border rounded-lg shadow-lg z-50 slide-in overflow-hidden">
              <div className="flex items-center justify-between px-4 py-3 border-b border-border">
                <div className="font-semibold text-sm">Notifications</div>
                <button onClick={markRead} className="text-xs text-accent hover:underline">Mark all read</button>
              </div>
              <div className="max-h-96 overflow-auto">
                {notifs.map((n) => (
                  <div key={n.id} className={`px-4 py-3 border-b border-border last:border-0 text-sm ${n.unread ? "bg-accent/5" : ""}`}>
                    <div className="flex items-start gap-2">
                      <span className={`mt-1 h-2 w-2 rounded-full shrink-0 ${
                        n.kind === "warning" ? "bg-warning" : n.kind === "success" ? "bg-success" : "bg-accent"
                      }`} />
                      <div className="flex-1">
                        <div className="text-foreground">{n.text}</div>
                        <div className="text-xs text-muted-foreground mt-1">{n.time}</div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Role switcher / User Menu */}
        <div className="relative">
          <button
            onClick={() => { setOpenRole((v) => !v); setOpenNotif(false); }}
            className="flex items-center gap-2 px-3 h-9 rounded-md border border-border bg-card hover:bg-muted text-sm"
          >
            <User2 className="h-4 w-4" />
            <span className="font-medium">{roleLabels[role]}</span>
            <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
          </button>
          {openRole && (
            <div className="absolute right-0 mt-2 w-48 bg-card border border-border rounded-lg shadow-lg z-50 slide-in py-1">
              <button
                onClick={() => { logout(); setOpenRole(false); }}
                className="w-full text-left px-3 py-2 text-sm hover:bg-muted text-destructive"
              >
                Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
