import { Link, useRouterState, useNavigate } from "@tanstack/react-router";
import { useApp, type Role } from "@/lib/store";
import { useAuth } from "@/lib/auth-service";
import { ALL_SCENARIOS } from "@/lib/demo-scenarios";
import {
  Home, FileText, Bell, Settings, ClipboardList, Search, History,
  LayoutDashboard, Inbox, ShieldCheck, BarChart3, ChevronDown, User2, FilePlus2, CalendarDays, RotateCcw,
  Menu, X
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
  auditor: [
    { to: "/auditor", label: "Dashboard", icon: LayoutDashboard },
    { to: "/auditor/assigned", label: "Assigned Audits", icon: ClipboardList, badge: 2 },
    { to: "/auditor/search", label: "Taxpayer Search", icon: Search },
    { to: "/auditor/history", label: "Audit History", icon: History },
  ],
  admin: [
    { to: "/admin", label: "Admin Dashboard", icon: LayoutDashboard },
    { to: "/admin/users", label: "User Management", icon: User2 },
    { to: "/admin/logs", label: "System Logs", icon: History },
    { to: "/admin/analytics", label: "Platform Analytics", icon: BarChart3 },
    { to: "/admin/settings", label: "Settings", icon: Settings },
  ],
};

const roleLabels: Record<Role, string> = {
  taxpayer: "Taxpayer Portal",
  officer: "Officer Portal",
  auditor: "Auditor Portal",
  admin: "System Administrator",
  system: "System Dashboard",
};

export function AppShell({ children }: { children: React.ReactNode }) {
  const session = useAuth((s) => s.session);
  const role = session?.user.role?.toLowerCase() as Role || "taxpayer";
  const pathname = useRouterState({ select: (r) => r.location.pathname });
  const items = navByRole[role] || [];
  
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Close mobile menu on route change
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [pathname]);

  return (
    <div className="min-h-screen bg-background flex flex-col font-sans">
      {/* Global status bar */}
      <div className="h-1 bg-gradient-to-r from-success via-accent to-success w-full shrink-0" />

      <div className="flex flex-1 overflow-hidden relative">
        {/* Mobile menu backdrop */}
        {mobileMenuOpen && (
          <div 
            className="fixed inset-0 bg-black/40 backdrop-blur-sm z-40 lg:hidden"
            onClick={() => setMobileMenuOpen(false)}
          />
        )}

        {/* Sidebar */}
        <aside className={`
          fixed lg:static inset-y-0 left-0 z-50 w-72 lg:w-64 bg-navy text-navy-foreground flex flex-col shrink-0 
          transform transition-transform duration-300 ease-in-out shadow-2xl lg:shadow-none
          ${mobileMenuOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"}
        `}>
          <div className="flex items-center justify-between px-5 py-5 border-b border-white/10 shrink-0">
            <div>
              <div className="text-[10px] uppercase tracking-widest text-accent font-semibold mb-1">Ethiopian Revenue Authority</div>
              <div className="font-bold text-lg text-white flex items-center gap-2">
                <ShieldCheck className="h-5 w-5 text-accent" />
                E-Filing Core
              </div>
            </div>
            <button className="lg:hidden text-white/50 hover:text-white" onClick={() => setMobileMenuOpen(false)}>
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto py-4 scrollbar-thin scrollbar-thumb-white/10 scrollbar-track-transparent">
            <nav className="space-y-1 px-3">
              {items.map((it) => {
                // Determine the best active match
                let active = false;
                if (it.to === "/") {
                  active = pathname === "/";
                } else {
                  active = pathname === it.to || pathname.startsWith(it.to + "/");
                  
                  // Ensure we don't activate parent if a more specific child matches
                  if (active) {
                    const moreSpecificMatch = items.some(other => 
                      other.to !== it.to && 
                      other.to.startsWith(it.to) && 
                      (pathname === other.to || pathname.startsWith(other.to + "/"))
                    );
                    if (moreSpecificMatch) active = false;
                  }
                }

                const Icon = it.icon;
                return (
                  <Link
                    key={it.to}
                    to={it.to}
                    className={`relative flex items-center gap-3 px-3 py-3 rounded-lg text-sm font-medium transition-all duration-200 group overflow-hidden ${
                      active 
                        ? "bg-accent/20 text-white shadow-inner" 
                        : "text-white/60 hover:bg-white/5 hover:text-white"
                    }`}
                  >
                    {active && (
                      <div className="absolute left-0 top-0 bottom-0 w-1 bg-accent rounded-r-full" />
                    )}
                    <Icon className={`h-4 w-4 shrink-0 transition-all duration-200 ${
                      active 
                        ? "text-accent scale-110" 
                        : "text-white/40 group-hover:text-white/80 group-hover:scale-105"
                    }`} />
                    <span className="flex-1">{it.label}</span>
                    {it.badge ? (
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        active ? "bg-accent text-white" : "bg-white/10 text-white/80"
                      }`}>{it.badge}</span>
                    ) : null}
                  </Link>
                );
              })}
            </nav>
          </div>

          <div className="px-5 py-4 border-t border-white/10 shrink-0 bg-black/10">
            <div className="flex items-center gap-1.5 text-[10px] uppercase tracking-widest text-white/50 mb-2 font-semibold">
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
              className="w-full text-xs bg-black/20 text-white border border-white/10 rounded-md px-2.5 py-2 focus:outline-none focus:ring-1 focus:ring-accent transition-shadow cursor-pointer hover:bg-black/30"
            >
              {ALL_SCENARIOS.map((s) => (
                <option key={s.id} value={s.id} className="bg-navy text-white py-1">
                  {s.name}
                </option>
              ))}
            </select>
          </div>
        </aside>

        {/* Main Content Area */}
        <div className="flex-1 flex flex-col min-w-0 bg-background relative h-full">
          <TopBar role={role} onMenuClick={() => setMobileMenuOpen(true)} />
          
          <main className="flex-1 overflow-auto">
            <div className="max-w-[1600px] mx-auto p-4 sm:p-6 lg:p-8 fade-in">
              {children}
            </div>
          </main>
        </div>
      </div>
    </div>
  );
}

function TopBar({ role, onMenuClick }: { role: Role, onMenuClick: () => void }) {
  const [openRole, setOpenRole] = useState(false);
  const [openNotif, setOpenNotif] = useState(false);
  const notifs = useApp((s) => s.notifications);
  const markRead = useApp((s) => s.markAllRead);
  const unread = notifs.filter((n) => n.unread).length;
  const logout = useAuth((s) => s.logout);
  const navigate = useNavigate();

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

  const handleLogout = () => {
    logout(); 
    setOpenRole(false);
    navigate({ to: "/login" });
  };

  return (
    <header className="h-16 shrink-0 bg-card/80 backdrop-blur-xl border-b border-border flex items-center justify-between px-4 sm:px-6 sticky top-0 z-30 shadow-sm">
      <div className="flex items-center gap-3">
        <button 
          className="lg:hidden p-2 -ml-2 text-muted-foreground hover:text-foreground rounded-md hover:bg-muted transition-colors"
          onClick={onMenuClick}
        >
          <Menu className="h-5 w-5" />
        </button>
        <div className="hidden sm:flex items-center gap-2 text-sm text-muted-foreground font-medium">
          <span className="pulse-dot" />
          <span>Systems Operational</span>
        </div>
      </div>

      <div ref={ref} className="flex items-center gap-2 sm:gap-4">
        {/* Notifications */}
        <div className="relative">
          <button
            onClick={() => { setOpenNotif((v) => !v); setOpenRole(false); }}
            className="relative h-10 w-10 rounded-full hover:bg-muted flex items-center justify-center transition-colors"
            aria-label="Notifications"
          >
            <Bell className="h-5 w-5 text-foreground" />
            {unread > 0 && (
              <span className="absolute top-2 right-2 h-4 min-w-[1rem] px-1 rounded-full bg-destructive text-destructive-foreground text-[10px] font-bold flex items-center justify-center ring-2 ring-card shadow-sm">
                {unread}
              </span>
            )}
          </button>
          
          {openNotif && (
            <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-card border border-border rounded-xl shadow-xl shadow-black/5 z-50 slide-in overflow-hidden origin-top-right">
              <div className="flex items-center justify-between px-4 py-3 border-b border-border bg-muted/30">
                <div className="font-semibold text-sm">Notifications</div>
                <button onClick={markRead} className="text-xs font-medium text-accent hover:text-primary transition-colors">Mark all read</button>
              </div>
              <div className="max-h-[400px] overflow-auto">
                {notifs.length === 0 ? (
                  <div className="p-8 text-center text-sm text-muted-foreground">No new notifications</div>
                ) : (
                  notifs.map((n) => (
                    <div key={n.id} className={`px-4 py-3 border-b border-border last:border-0 text-sm hover:bg-muted/30 transition-colors ${n.unread ? "bg-accent/5" : ""}`}>
                      <div className="flex items-start gap-3">
                        <span className={`mt-1.5 h-2 w-2 rounded-full shrink-0 ${
                          n.kind === "warning" ? "bg-warning" : n.kind === "success" ? "bg-success" : "bg-accent"
                        }`} />
                        <div className="flex-1">
                          <div className={`text-foreground ${n.unread ? "font-medium" : ""}`}>{n.text}</div>
                          <div className="text-xs text-muted-foreground mt-1.5 flex items-center gap-1">
                            {n.time}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        <div className="h-6 w-px bg-border hidden sm:block" />

        {/* User Menu */}
        <div className="relative">
          <button
            onClick={() => { setOpenRole((v) => !v); setOpenNotif(false); }}
            className="flex items-center gap-2 pl-2 pr-3 py-1.5 rounded-full border border-border bg-card hover:bg-muted transition-colors"
          >
            <div className="h-7 w-7 rounded-full bg-accent/10 text-accent flex items-center justify-center shrink-0">
              <User2 className="h-4 w-4" />
            </div>
            <span className="font-medium text-sm hidden sm:block">{roleLabels[role]}</span>
            <ChevronDown className="h-3.5 w-3.5 text-muted-foreground hidden sm:block" />
          </button>
          
          {openRole && (
            <div className="absolute right-0 mt-2 w-48 bg-card border border-border rounded-xl shadow-xl shadow-black/5 z-50 slide-in py-2 origin-top-right">
              <div className="px-4 py-2 border-b border-border mb-2 sm:hidden">
                <div className="font-medium text-sm">{roleLabels[role]}</div>
              </div>
              <button
                onClick={handleLogout}
                className="w-full text-left px-4 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 transition-colors flex items-center gap-2"
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
