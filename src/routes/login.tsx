import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useAuth } from "@/lib/auth-service";
import { Role } from "@/mock/users";
import { 
  ShieldCheck, User2, UserCog2, Settings2, ArrowRight, 
  Eye, EyeOff, ArrowLeft, KeyRound, Mail, CheckCircle2 
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { InputOTP, InputOTPGroup, InputOTPSlot } from "@/components/ui/input-otp";

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
      <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4 sm:p-8 md:p-12 fade-in">
        <div className="w-full max-w-4xl space-y-10">
          <div className="text-center space-y-3">
            <h1 className="text-4xl sm:text-5xl font-bold tracking-tight text-slate-900">Ethiopian Revenue Authority</h1>
            <p className="text-lg text-slate-500">Select your portal to continue</p>
          </div>
          
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            {ROLE_CARDS.map(({ role, label, desc, icon: Icon }) => (
              <button
                key={role}
                onClick={() => setSelectedRole(role as Role)}
                className="group flex flex-col items-start gap-4 p-8 bg-white border border-slate-200 rounded-2xl hover:border-blue-500 hover:shadow-xl hover:shadow-blue-500/10 transition-all duration-300 text-left w-full relative overflow-hidden"
              >
                <div className="absolute top-0 right-0 p-8 opacity-0 group-hover:opacity-[0.03] transition-opacity duration-500">
                  <Icon className="h-40 w-40 text-blue-600 -mt-10 -mr-10" />
                </div>
                <div className="h-14 w-14 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform duration-300 shadow-sm">
                  <Icon className="h-7 w-7" />
                </div>
                <div className="space-y-2 relative z-10">
                  <h3 className="font-semibold text-xl text-slate-900 group-hover:text-blue-600 transition-colors">{label}</h3>
                  <p className="text-sm text-slate-500 leading-relaxed">{desc}</p>
                </div>
              </button>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4 sm:p-8 slide-in">
      <div className="w-full max-w-md bg-white border border-slate-200 rounded-2xl shadow-xl shadow-slate-200/50 p-8 sm:p-10 relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-1.5 bg-gradient-to-r from-blue-600 to-cyan-400"></div>
        <div className="mb-8">
          <button 
            onClick={() => setSelectedRole(null)} 
            className="flex items-center gap-2 text-sm text-slate-500 hover:text-slate-900 font-medium mb-6 transition-colors"
          >
            <ArrowLeft className="h-4 w-4" /> Back to portals
          </button>
          <div className="flex items-center gap-3">
            <div className="h-12 w-12 rounded-xl bg-blue-50 flex items-center justify-center text-blue-600 shadow-sm">
              {(() => {
                const Icon = ROLE_CARDS.find(r => r.role === selectedRole)?.icon || User2;
                return <Icon className="h-6 w-6" />;
              })()}
            </div>
            <div>
              <h2 className="text-2xl font-bold text-slate-900">
                {ROLE_CARDS.find(r => r.role === selectedRole)?.label}
              </h2>
              <p className="text-sm text-slate-500 mt-1">Sign in to your account to continue</p>
            </div>
          </div>
        </div>
        
        {selectedRole === "TAXPAYER" ? <TaxpayerForm /> : <InternalForm role={selectedRole} />}
      </div>
    </div>
  );
}

type AuthFormStep = "login" | "forgot-email" | "forgot-otp" | "forgot-reset";

function TaxpayerForm() {
  const [step, setStep] = useState<AuthFormStep>("login");
  
  // Login State
  const [tin, setTin] = useState("1000123456");
  const [password, setPassword] = useState("Taxpayer@123");
  const [showPassword, setShowPassword] = useState(false);

  // Forgot Password State
  const [resetEmail, setResetEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);

  const login = useAuth(s => s.loginTaxpayer);
  const navigate = useNavigate();

  const handleLoginSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (login(tin, password)) {
      toast.success("Login successful");
      navigate({ to: "/" });
    } else {
      toast.error("Invalid TIN or Password");
    }
  };

  const handleSendResetEmail = (e: React.FormEvent) => {
    e.preventDefault();
    if (!resetEmail) return toast.error("Please enter your email");
    toast.success("Recovery code sent to your email!");
    setStep("forgot-otp");
  };

  const handleVerifyOtp = (e: React.FormEvent) => {
    e.preventDefault();
    if (otp.length !== 6) return toast.error("Please enter the complete 6-digit code");
    toast.success("Code verified successfully");
    setStep("forgot-reset");
  };

  const handleResetPassword = (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) return toast.error("Passwords do not match");
    if (newPassword.length < 8) return toast.error("Password must be at least 8 characters");
    toast.success("Password reset successfully! Please login with your new password.");
    setPassword(newPassword);
    setStep("login");
  };

  if (step === "forgot-email") {
    return (
      <form onSubmit={handleSendResetEmail} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
        <div className="space-y-2 text-center mb-6">
          <div className="mx-auto w-12 h-12 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center mb-4">
            <Mail className="h-6 w-6" />
          </div>
          <h3 className="text-xl font-semibold text-slate-900">Forgot Password</h3>
          <p className="text-sm text-slate-500">Enter your email address to receive a verification code.</p>
        </div>
        <div className="space-y-2">
          <Label htmlFor="email">Email Address</Label>
          <Input 
            id="email"
            type="email" 
            placeholder="name@example.com"
            value={resetEmail}
            onChange={e => setResetEmail(e.target.value)}
            required
            className="h-11"
          />
        </div>
        <div className="pt-4 space-y-3">
          <Button type="submit" className="w-full h-11 text-base font-medium">Send Verification Code</Button>
          <Button type="button" variant="ghost" className="w-full h-11" onClick={() => setStep("login")}>
            Return to Login
          </Button>
        </div>
      </form>
    );
  }

  if (step === "forgot-otp") {
    return (
      <form onSubmit={handleVerifyOtp} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
        <div className="space-y-2 text-center mb-6">
          <div className="mx-auto w-12 h-12 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center mb-4">
            <KeyRound className="h-6 w-6" />
          </div>
          <h3 className="text-xl font-semibold text-slate-900">Enter Verification Code</h3>
          <p className="text-sm text-slate-500">We sent a 6-digit code to <span className="font-medium text-slate-900">{resetEmail}</span></p>
        </div>
        <div className="flex justify-center py-4">
          <InputOTP maxLength={6} value={otp} onChange={setOtp}>
            <InputOTPGroup>
              <InputOTPSlot index={0} className="h-12 w-12 text-lg" />
              <InputOTPSlot index={1} className="h-12 w-12 text-lg" />
              <InputOTPSlot index={2} className="h-12 w-12 text-lg" />
              <InputOTPSlot index={3} className="h-12 w-12 text-lg" />
              <InputOTPSlot index={4} className="h-12 w-12 text-lg" />
              <InputOTPSlot index={5} className="h-12 w-12 text-lg" />
            </InputOTPGroup>
          </InputOTP>
        </div>
        <div className="pt-4 space-y-3">
          <Button type="submit" className="w-full h-11 text-base font-medium">Verify Code</Button>
          <Button type="button" variant="ghost" className="w-full h-11" onClick={() => setStep("forgot-email")}>
            Back
          </Button>
        </div>
      </form>
    );
  }

  if (step === "forgot-reset") {
    return (
      <form onSubmit={handleResetPassword} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
        <div className="space-y-2 text-center mb-6">
          <div className="mx-auto w-12 h-12 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center mb-4">
            <CheckCircle2 className="h-6 w-6" />
          </div>
          <h3 className="text-xl font-semibold text-slate-900">Set New Password</h3>
          <p className="text-sm text-slate-500">Your new password must be different from previously used passwords.</p>
        </div>
        
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="newPassword">New Password</Label>
            <div className="relative">
              <Input 
                id="newPassword"
                type={showNewPassword ? "text" : "password"} 
                value={newPassword}
                onChange={e => setNewPassword(e.target.value)}
                className="pr-10 h-11"
                placeholder="••••••••"
                required
              />
              <button
                type="button"
                onClick={() => setShowNewPassword(!showNewPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none transition-colors"
                aria-label={showNewPassword ? "Hide password" : "Show password"}
              >
                {showNewPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
              </button>
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="confirmPassword">Confirm Password</Label>
            <Input 
              id="confirmPassword"
              type={showNewPassword ? "text" : "password"} 
              value={confirmPassword}
              onChange={e => setConfirmPassword(e.target.value)}
              className="pr-10 h-11"
              placeholder="••••••••"
              required
            />
          </div>
        </div>

        <div className="pt-6 space-y-3">
          <Button type="submit" className="w-full h-11 text-base font-medium">Reset Password</Button>
        </div>
      </form>
    );
  }

  return (
    <form onSubmit={handleLoginSubmit} className="space-y-5 animate-in fade-in slide-in-from-left-4 duration-300">
      <div className="space-y-2">
        <Label htmlFor="tin">TIN Number</Label>
        <Input 
          id="tin"
          type="text" 
          value={tin}
          onChange={e => setTin(e.target.value)}
          className="font-mono text-lg tracking-wider h-11"
          placeholder="Enter your TIN"
          required
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="password">Password</Label>
        <div className="relative">
          <Input 
            id="password"
            type={showPassword ? "text" : "password"} 
            value={password}
            onChange={e => setPassword(e.target.value)}
            className="pr-10 h-11"
            placeholder="••••••••"
            required
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none transition-colors"
            aria-label={showPassword ? "Hide password" : "Show password"}
          >
            {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
          </button>
        </div>
        <div className="flex justify-end pt-1">
          <button 
            type="button" 
            onClick={() => setStep("forgot-email")}
            className="text-sm text-blue-600 hover:text-blue-700 font-medium hover:underline focus:outline-none transition-colors"
          >
            Forgot Password?
          </button>
        </div>
      </div>
      <Button type="submit" className="w-full mt-4 h-12 text-base font-medium group">
        Sign In 
        <ArrowRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform" />
      </Button>
    </form>
  );
}

function InternalForm({ role }: { role: "OFFICER" | "AUDITOR" | "ADMIN" }) {
  const [step, setStep] = useState<AuthFormStep>("login");
  
  // Login State
  const [username, setUsername] = useState(role.toLowerCase());
  const [password, setPassword] = useState(`${role.charAt(0) + role.slice(1).toLowerCase()}@123`);
  const [showPassword, setShowPassword] = useState(false);

  // Forgot Password State
  const [resetEmail, setResetEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);

  const loginOfficer = useAuth(s => s.loginOfficer);
  const loginAuditor = useAuth(s => s.loginAuditor);
  const loginAdmin = useAuth(s => s.loginAdmin);
  const navigate = useNavigate();

  const handleLoginSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    let success = false;
    if (role === "OFFICER") success = loginOfficer(username, password);
    else if (role === "AUDITOR") success = loginAuditor(username, password);
    else if (role === "ADMIN") success = loginAdmin(username, password);

    if (success) {
      toast.success("Login successful");
      if (role === "OFFICER") navigate({ to: "/officer" });
      else if (role === "AUDITOR") navigate({ to: "/auditor" }); 
      else if (role === "ADMIN") navigate({ to: "/admin" }); 
    } else {
      toast.error("Invalid Username or Password");
    }
  };

  const handleSendResetEmail = (e: React.FormEvent) => {
    e.preventDefault();
    if (!resetEmail) return toast.error("Please enter your email");
    toast.success("Recovery code sent to your email!");
    setStep("forgot-otp");
  };

  const handleVerifyOtp = (e: React.FormEvent) => {
    e.preventDefault();
    if (otp.length !== 6) return toast.error("Please enter the complete 6-digit code");
    toast.success("Code verified successfully");
    setStep("forgot-reset");
  };

  const handleResetPassword = (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) return toast.error("Passwords do not match");
    if (newPassword.length < 8) return toast.error("Password must be at least 8 characters");
    toast.success("Password reset successfully! Please login with your new password.");
    setPassword(newPassword); // Mock setting the newly created password so the user can just click Sign In
    setStep("login");
  };

  if (step === "forgot-email") {
    return (
      <form onSubmit={handleSendResetEmail} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
        <div className="space-y-2 text-center mb-6">
          <div className="mx-auto w-12 h-12 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center mb-4">
            <Mail className="h-6 w-6" />
          </div>
          <h3 className="text-xl font-semibold text-slate-900">Forgot Password</h3>
          <p className="text-sm text-slate-500">Enter your email address to receive a verification code.</p>
        </div>
        <div className="space-y-2">
          <Label htmlFor="email">Email Address</Label>
          <Input 
            id="email"
            type="email" 
            placeholder="name@example.com"
            value={resetEmail}
            onChange={e => setResetEmail(e.target.value)}
            required
            className="h-11"
          />
        </div>
        <div className="pt-4 space-y-3">
          <Button type="submit" className="w-full h-11 text-base font-medium">Send Verification Code</Button>
          <Button type="button" variant="ghost" className="w-full h-11" onClick={() => setStep("login")}>
            Return to Login
          </Button>
        </div>
      </form>
    );
  }

  if (step === "forgot-otp") {
    return (
      <form onSubmit={handleVerifyOtp} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
        <div className="space-y-2 text-center mb-6">
          <div className="mx-auto w-12 h-12 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center mb-4">
            <KeyRound className="h-6 w-6" />
          </div>
          <h3 className="text-xl font-semibold text-slate-900">Enter Verification Code</h3>
          <p className="text-sm text-slate-500">We sent a 6-digit code to <span className="font-medium text-slate-900">{resetEmail}</span></p>
        </div>
        <div className="flex justify-center py-4">
          <InputOTP maxLength={6} value={otp} onChange={setOtp}>
            <InputOTPGroup>
              <InputOTPSlot index={0} className="h-12 w-12 text-lg" />
              <InputOTPSlot index={1} className="h-12 w-12 text-lg" />
              <InputOTPSlot index={2} className="h-12 w-12 text-lg" />
              <InputOTPSlot index={3} className="h-12 w-12 text-lg" />
              <InputOTPSlot index={4} className="h-12 w-12 text-lg" />
              <InputOTPSlot index={5} className="h-12 w-12 text-lg" />
            </InputOTPGroup>
          </InputOTP>
        </div>
        <div className="pt-4 space-y-3">
          <Button type="submit" className="w-full h-11 text-base font-medium">Verify Code</Button>
          <Button type="button" variant="ghost" className="w-full h-11" onClick={() => setStep("forgot-email")}>
            Back
          </Button>
        </div>
      </form>
    );
  }

  if (step === "forgot-reset") {
    return (
      <form onSubmit={handleResetPassword} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
        <div className="space-y-2 text-center mb-6">
          <div className="mx-auto w-12 h-12 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center mb-4">
            <CheckCircle2 className="h-6 w-6" />
          </div>
          <h3 className="text-xl font-semibold text-slate-900">Set New Password</h3>
          <p className="text-sm text-slate-500">Your new password must be different from previously used passwords.</p>
        </div>
        
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="newPassword">New Password</Label>
            <div className="relative">
              <Input 
                id="newPassword"
                type={showNewPassword ? "text" : "password"} 
                value={newPassword}
                onChange={e => setNewPassword(e.target.value)}
                className="pr-10 h-11"
                placeholder="••••••••"
                required
              />
              <button
                type="button"
                onClick={() => setShowNewPassword(!showNewPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none transition-colors"
                aria-label={showNewPassword ? "Hide password" : "Show password"}
              >
                {showNewPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
              </button>
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="confirmPassword">Confirm Password</Label>
            <Input 
              id="confirmPassword"
              type={showNewPassword ? "text" : "password"} 
              value={confirmPassword}
              onChange={e => setConfirmPassword(e.target.value)}
              className="pr-10 h-11"
              placeholder="••••••••"
              required
            />
          </div>
        </div>

        <div className="pt-6 space-y-3">
          <Button type="submit" className="w-full h-11 text-base font-medium">Reset Password</Button>
        </div>
      </form>
    );
  }

  return (
    <form onSubmit={handleLoginSubmit} className="space-y-5 animate-in fade-in slide-in-from-left-4 duration-300">
      <div className="space-y-2">
        <Label htmlFor="username">Username</Label>
        <Input 
          id="username"
          type="text" 
          value={username}
          onChange={e => setUsername(e.target.value)}
          className="h-11"
          placeholder="Enter your username"
          required
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="password">Password</Label>
        <div className="relative">
          <Input 
            id="password"
            type={showPassword ? "text" : "password"} 
            value={password}
            onChange={e => setPassword(e.target.value)}
            className="pr-10 h-11"
            placeholder="••••••••"
            required
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none transition-colors"
            aria-label={showPassword ? "Hide password" : "Show password"}
          >
            {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
          </button>
        </div>
        <div className="flex justify-end pt-1">
          <button 
            type="button" 
            onClick={() => setStep("forgot-email")}
            className="text-sm text-blue-600 hover:text-blue-700 font-medium hover:underline focus:outline-none transition-colors"
          >
            Forgot Password?
          </button>
        </div>
      </div>
      <Button type="submit" className="w-full mt-4 h-12 text-base font-medium group">
        Sign In 
        <ArrowRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform" />
      </Button>
    </form>
  );
}
