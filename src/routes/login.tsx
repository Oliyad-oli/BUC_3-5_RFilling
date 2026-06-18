import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useAuth } from "@/lib/auth-service";
import { Role } from "@/mock/users";
import { ShieldCheck, User2, UserCog2, Settings2, ArrowRight } from "lucide-react";
import { toast } from "sonner";

export const Route = createFileRoute("/login")({
  component: LoginPortal,
});

const ROLE_CARDS = [
  { role: "TAXPAYER", label: "Taxpayer Portal", desc: "File returns and manage obligations", icon: User2 },
  { role: "OFFICER", label: "Tax Officer", desc: "Review returns and manage cases", icon: ShieldCheck },
  { role: "AUDITOR", label: "Auditor", desc: "Conduct tax audits and assessments", icon: UserCog2 },
  { role: "ADMIN", label: "System Administrator", desc: "Manage users and system settings", icon: Settings2 },
] as const;

function LoginPortal() {
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  
  if (!selectedRole) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-4 fade-in">
        <div className="w-full max-w-3xl space-y-8">
          <div className="text-center space-y-2">
            <h1 className="text-3xl font-semibold tracking-tight text-foreground">Ethiopian Revenue Authority</h1>
            <p className="text-muted-foreground">Select your portal to continue</p>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {ROLE_CARDS.map(({ role, label, desc, icon: Icon }) => (
              <button
                key={role}
                onClick={() => setSelectedRole(role as Role)}
                className="flex items-start gap-4 p-6 bg-card border border-border rounded-xl hover:border-accent hover:shadow-md transition-all text-left"
              >
                <div className="h-12 w-12 rounded-lg bg-accent/10 text-accent flex items-center justify-center shrink-0">
                  <Icon className="h-6 w-6" />
                </div>
                <div>
                  <h3 className="font-medium text-lg">{label}</h3>
                  <p className="text-sm text-muted-foreground mt-1">{desc}</p>
                </div>
              </button>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center p-4 slide-in">
      <div className="w-full max-w-md bg-card border border-border rounded-xl shadow-lg p-8">
        <div className="mb-8">
          <button 
            onClick={() => setSelectedRole(null)} 
            className="text-sm text-muted-foreground hover:text-foreground mb-4"
          >
            ← Back to portals
          </button>
          <h2 className="text-2xl font-semibold">
            {ROLE_CARDS.find(r => r.role === selectedRole)?.label} Login
          </h2>
        </div>
        
        {selectedRole === "TAXPAYER" ? <TaxpayerForm /> : <InternalForm role={selectedRole} />}
      </div>
    </div>
  );
}

function TaxpayerForm() {
  const [tin, setTin] = useState("1000123456");
  const [name, setName] = useState("Abebe Kebede");
  const login = useAuth(s => s.loginTaxpayer);
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (login(tin, name)) {
      toast.success("Login successful");
      navigate({ to: "/" });
    } else {
      toast.error("Invalid TIN or Name");
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <label className="text-sm font-medium">TIN Number</label>
        <input 
          type="text" 
          value={tin}
          onChange={e => setTin(e.target.value)}
          className="w-full px-3 py-2 border border-border rounded-md bg-background focus:ring-2 focus:ring-accent outline-none mono" 
          required
        />
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium">Taxpayer Name</label>
        <input 
          type="text" 
          value={name}
          onChange={e => setName(e.target.value)}
          className="w-full px-3 py-2 border border-border rounded-md bg-background focus:ring-2 focus:ring-accent outline-none" 
          required
        />
      </div>
      <button type="submit" className="w-full bg-accent text-accent-foreground py-2.5 rounded-md font-medium hover:opacity-90 flex items-center justify-center gap-2 mt-6">
        Sign In <ArrowRight className="h-4 w-4" />
      </button>
    </form>
  );
}

function InternalForm({ role }: { role: "OFFICER" | "AUDITOR" | "ADMIN" }) {
  const [username, setUsername] = useState(role.toLowerCase());
  const [password, setPassword] = useState(`${role.charAt(0) + role.slice(1).toLowerCase()}@123`);
  const loginOfficer = useAuth(s => s.loginOfficer);
  const loginAuditor = useAuth(s => s.loginAuditor);
  const loginAdmin = useAuth(s => s.loginAdmin);
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    let success = false;
    if (role === "OFFICER") success = loginOfficer(username, password);
    else if (role === "AUDITOR") success = loginAuditor(username, password);
    else if (role === "ADMIN") success = loginAdmin(username, password);

    if (success) {
      toast.success("Login successful");
      if (role === "OFFICER") navigate({ to: "/officer" });
      else if (role === "AUDITOR") navigate({ to: "/auditor" }); // Note: route might not exist yet
      else if (role === "ADMIN") navigate({ to: "/admin" }); // Note: route might not exist yet
    } else {
      toast.error("Invalid Username or Password");
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <label className="text-sm font-medium">Username</label>
        <input 
          type="text" 
          value={username}
          onChange={e => setUsername(e.target.value)}
          className="w-full px-3 py-2 border border-border rounded-md bg-background focus:ring-2 focus:ring-accent outline-none" 
          required
        />
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium">Password</label>
        <input 
          type="password" 
          value={password}
          onChange={e => setPassword(e.target.value)}
          className="w-full px-3 py-2 border border-border rounded-md bg-background focus:ring-2 focus:ring-accent outline-none" 
          required
        />
      </div>
      <button type="submit" className="w-full bg-accent text-accent-foreground py-2.5 rounded-md font-medium hover:opacity-90 flex items-center justify-center gap-2 mt-6">
        Sign In <ArrowRight className="h-4 w-4" />
      </button>
    </form>
  );
}
