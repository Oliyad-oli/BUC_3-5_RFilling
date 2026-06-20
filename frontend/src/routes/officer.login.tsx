import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useAuth } from "@/lib/auth-service";
import { Eye, EyeOff, ArrowRight, Building2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export const Route = createFileRoute("/officer/login")({
  component: OfficerLogin,
});

type AuthStep = "login" | "forgot-email" | "forgot-otp" | "forgot-reset";

function OfficerLogin() {
  const [step, setStep] = useState<AuthStep>("login");
  const [isLoading, setIsLoading] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [roleType, setRoleType] = useState<"OFFICER" | "AUDITOR" | "ADMIN">("OFFICER");
  
  const loginOfficer = useAuth(s => s.loginOfficer);
  const loginAuditor = useAuth(s => s.loginAuditor);
  const loginAdmin = useAuth(s => s.loginAdmin);
  const navigate = useNavigate();

  // Forgot Password State
  const [resetEmail, setResetEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const handleLoginSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      const finalUser = username || roleType.toLowerCase();
      const finalPass = password || `${roleType.charAt(0) + roleType.slice(1).toLowerCase()}@123`;

    let success = false;
    if (roleType === "OFFICER") success = loginOfficer(finalUser, finalPass);
    else if (roleType === "AUDITOR") success = loginAuditor(finalUser, finalPass);
    else if (roleType === "ADMIN") success = loginAdmin(finalUser, finalPass);

      if (success) {
        toast.success("Login successful");
        if (roleType === "OFFICER") navigate({ to: "/officer" });
        else if (roleType === "AUDITOR") navigate({ to: "/auditor" }); 
        else if (roleType === "ADMIN") navigate({ to: "/admin" }); 
      } else {
        toast.error("Invalid Username or Password");
      }
    }, 800);
  };

  const handleSendResetEmail = (e: React.FormEvent) => {
    e.preventDefault();
    if (!resetEmail) return toast.error("Please enter your email");
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      toast.success("Recovery code sent to your internal email!");
      setStep("forgot-otp");
    }, 1200);
  };

  const handleVerifyOtp = (e: React.FormEvent) => {
    e.preventDefault();
    if (otp.length !== 6) return toast.error("Please enter the complete 6-digit code");
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      toast.success("Code verified successfully");
      setStep("forgot-reset");
    }, 1000);
  };

  const handleResetPassword = (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) return toast.error("Passwords do not match");
    if (newPassword.length < 8) return toast.error("Password must be at least 8 characters");
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      toast.success("Password reset successfully! Please login with your new password.");
      setPassword(newPassword);
      setStep("login");
    }, 1500);
  };

  return (
    <div className="min-h-screen bg-navy flex flex-col items-center justify-center p-4 sm:p-8 slide-in relative overflow-hidden">
      {/* Background embellishments for dark theme */}
      <div className="absolute top-0 w-full h-[35vh] bg-gradient-to-b from-accent/20 to-transparent pointer-events-none" />
      <div className="absolute -top-[20vw] -right-[10vw] w-[40vw] h-[40vw] rounded-full bg-accent/10 blur-[100px] pointer-events-none" />

      <div className="w-full max-w-md z-10">
        <div className="mb-8 text-center text-white">
          <div className="h-16 w-16 mx-auto bg-white/10 text-white rounded-2xl flex items-center justify-center border border-white/20 shadow-lg shadow-black/50 mb-6">
            <Building2 className="h-8 w-8" />
          </div>
          <h1 className="text-3xl font-bold tracking-tight">Internal Portal</h1>
          <p className="text-white/60 mt-2 text-sm">Authorized personnel only</p>
        </div>

        <div className="bg-card/10 backdrop-blur-xl border border-white/10 rounded-2xl p-8 sm:p-10 relative overflow-hidden shadow-2xl">
          <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-accent to-blue-400" />
          
          <div className="flex gap-2 mb-6 bg-black/20 p-1 rounded-lg">
            {(["OFFICER", "AUDITOR", "ADMIN"] as const).map((r) => (
              <button
                key={r}
                type="button"
                onClick={() => setRoleType(r)}
                className={`flex-1 py-2 px-3 text-xs font-medium rounded-md transition-all ${
                  roleType === r 
                    ? "bg-accent text-white shadow-md" 
                    : "text-white/60 hover:text-white hover:bg-white/5"
                }`}
              >
                {r.charAt(0) + r.slice(1).toLowerCase()}
              </button>
            ))}
          </div>

          {step === "login" && (
            <form onSubmit={handleLoginSubmit} className="space-y-6 animate-in fade-in slide-in-from-left-4 duration-300">
              <div className="space-y-2">
                <Label htmlFor="username" className="text-xs font-semibold uppercase tracking-wider text-white/70">Username</Label>
                <Input 
                  id="username"
                  type="text" 
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  className="bg-black/20 border-white/10 text-white placeholder:text-white/30 h-12 focus:border-accent focus:ring-accent/50"
                  placeholder={`${roleType.toLowerCase()}123`}
                  disabled={isLoading}
                />
              </div>
              
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="password" className="text-xs font-semibold uppercase tracking-wider text-white/70">Password</Label>
                  <button 
                    type="button" 
                    onClick={() => setStep("forgot-email")}
                    className="text-xs font-medium text-white/60 hover:text-white transition-colors focus:outline-none hover:underline"
                    disabled={isLoading}
                  >
                    Forgot Password?
                  </button>
                </div>
                <div className="relative">
                  <Input 
                    id="password"
                    type={showPassword ? "text" : "password"} 
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    className="bg-black/20 border-white/10 text-white placeholder:text-white/30 pr-12 h-12 focus:border-accent focus:ring-accent/50"
                    placeholder="••••••••"
                    disabled={isLoading}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-white/50 hover:text-white focus:outline-none transition-colors"
                  >
                    {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                  </button>
                </div>
              </div>

              <Button type="submit" disabled={isLoading} className="w-full mt-4 h-12 text-base font-medium group bg-accent hover:bg-accent/90 text-white border-0">
                {isLoading ? "Authenticating..." : "Secure Sign In"} 
                {!isLoading && <ArrowRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform" />}
              </Button>
            </form>
          )}

          {step === "forgot-email" && (
            <form onSubmit={handleSendResetEmail} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
              <div className="space-y-2 text-center mb-6">
                <h3 className="text-xl font-bold text-white">Forgot Password</h3>
                <p className="text-sm text-white/60">Enter your internal email address to receive a secure recovery code.</p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="email" className="text-xs font-semibold uppercase tracking-wider text-white/70">Internal Email Address</Label>
                <Input 
                  id="email"
                  type="email" 
                  placeholder="officer@era.gov.et"
                  value={resetEmail}
                  onChange={e => setResetEmail(e.target.value)}
                  required
                  className="bg-black/20 border-white/10 text-white placeholder:text-white/30 h-12 focus:border-accent focus:ring-accent/50"
                  disabled={isLoading}
                />
              </div>
              <div className="pt-4 space-y-3">
                <Button type="submit" disabled={isLoading} className="w-full h-12 text-base font-medium bg-accent hover:bg-accent/90 text-white border-0">
                  {isLoading ? "Sending..." : "Send Recovery Code"}
                </Button>
                <Button type="button" disabled={isLoading} variant="ghost" className="w-full h-12 text-white/70 hover:text-white hover:bg-white/10" onClick={() => setStep("login")}>
                  Return to Login
                </Button>
              </div>
            </form>
          )}

          {step === "forgot-otp" && (
            <form onSubmit={handleVerifyOtp} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
              <div className="space-y-2 text-center mb-6">
                <h3 className="text-xl font-bold text-white">Verify OTP</h3>
                <p className="text-sm text-white/60">We sent a 6-digit code to <span className="font-medium text-white">{resetEmail}</span></p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="otp" className="text-xs font-semibold uppercase tracking-wider text-white/70">Security Code</Label>
                <Input 
                  id="otp"
                  type="text" 
                  placeholder="123456"
                  maxLength={6}
                  value={otp}
                  onChange={e => setOtp(e.target.value)}
                  required
                  className="bg-black/20 border-white/10 text-white placeholder:text-white/30 h-12 text-center text-xl tracking-[0.5em] font-mono focus:border-accent focus:ring-accent/50"
                  disabled={isLoading}
                />
              </div>
              <div className="pt-4 space-y-3">
                <Button type="submit" disabled={isLoading} className="w-full h-12 text-base font-medium bg-accent hover:bg-accent/90 text-white border-0">
                  {isLoading ? "Verifying..." : "Verify Code"}
                </Button>
                <Button type="button" disabled={isLoading} variant="ghost" className="w-full h-12 text-white/70 hover:text-white hover:bg-white/10" onClick={() => setStep("forgot-email")}>
                  Back
                </Button>
              </div>
            </form>
          )}

          {step === "forgot-reset" && (
            <form onSubmit={handleResetPassword} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
              <div className="space-y-2 text-center mb-6">
                <h3 className="text-xl font-bold text-white">Set New Password</h3>
                <p className="text-sm text-white/60">Create a new strong password for your account.</p>
              </div>
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="newPassword" className="text-xs font-semibold uppercase tracking-wider text-white/70">New Password</Label>
                  <div className="relative">
                    <Input 
                      id="newPassword"
                      type={showPassword ? "text" : "password"} 
                      value={newPassword}
                      onChange={e => setNewPassword(e.target.value)}
                      className="bg-black/20 border-white/10 text-white placeholder:text-white/30 pr-12 h-12 focus:border-accent focus:ring-accent/50"
                      placeholder="••••••••"
                      required
                      disabled={isLoading}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-white/50 hover:text-white focus:outline-none transition-colors"
                    >
                      {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                    </button>
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="confirmPassword" className="text-xs font-semibold uppercase tracking-wider text-white/70">Confirm Password</Label>
                  <Input 
                    id="confirmPassword"
                    type={showPassword ? "text" : "password"} 
                    value={confirmPassword}
                    onChange={e => setConfirmPassword(e.target.value)}
                    className="bg-black/20 border-white/10 text-white placeholder:text-white/30 pr-12 h-12 focus:border-accent focus:ring-accent/50"
                    placeholder="••••••••"
                    required
                    disabled={isLoading}
                  />
                </div>
              </div>
              <div className="pt-6 space-y-3">
                <Button type="submit" disabled={isLoading} className="w-full h-12 text-base font-medium bg-accent hover:bg-accent/90 text-white border-0">
                  {isLoading ? "Saving..." : "Update Password"}
                </Button>
              </div>
            </form>
          )}
        </div>

        <div className="mt-8 text-center text-xs text-white/50">
          <Link to="/login" className="hover:text-white transition-colors hover:underline">
            Go to Taxpayer Portal
          </Link>
        </div>
      </div>
    </div>
  );
}
