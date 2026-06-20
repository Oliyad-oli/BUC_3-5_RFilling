import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { useAuth } from "@/lib/auth-service";
import { Eye, EyeOff, ArrowRight, ShieldCheck, Lock, Activity, BarChart3, Fingerprint, RefreshCw } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export const Route = createFileRoute("/login")({
  component: TaxpayerLogin,
});

type AuthStep = "login" | "forgot-email" | "forgot-otp" | "forgot-reset";

const TRUST_MESSAGES = [
  "Protect your tax records with secure digital filing.",
  "Track every filing with complete transparency.",
  "Your tax information is protected with enterprise-grade security.",
  "Submit returns faster and safer than manual filing.",
  "Real-time status tracking for every submission."
];

function TaxpayerLogin() {
  const [step, setStep] = useState<AuthStep>("login");
  const [isLoading, setIsLoading] = useState(false);
  
  // Login State
  const [tin, setTin] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  
  // Forgot Password State
  const [resetEmail, setResetEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  
  const login = useAuth(s => s.loginTaxpayer);
  const navigate = useNavigate();

  // Rotating Messages
  const [msgIdx, setMsgIdx] = useState(0);
  useEffect(() => {
    const int = setInterval(() => setMsgIdx(i => (i + 1) % TRUST_MESSAGES.length), 4000);
    return () => clearInterval(int);
  }, []);

  const handleLoginSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      const finalTin = tin || "1000123456";
      const finalPass = password || "Taxpayer@123";

      if (login(finalTin, finalPass)) {
        toast.success("Welcome back to the portal");
        navigate({ to: "/" });
      } else {
        toast.error("Invalid TIN or Password");
      }
    }, 800);
  };

  const handleSendResetEmail = (e: React.FormEvent) => {
    e.preventDefault();
    if (!resetEmail) return toast.error("Please enter your email");
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      toast.success("Recovery code sent to your email!");
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
    <div className="min-h-screen bg-background flex flex-col lg:flex-row overflow-hidden">
      
      {/* Left Column: Trust & Hero Section */}
      <div className="hidden lg:flex lg:w-[45%] xl:w-[50%] bg-navy text-white flex-col justify-between p-12 xl:p-20 relative overflow-hidden shrink-0 shadow-2xl z-10">
        {/* Background embellishments */}
        <div className="absolute top-0 left-0 w-full h-[40vh] bg-gradient-to-br from-accent/20 to-transparent pointer-events-none" />
        <div className="absolute -bottom-[20vw] -left-[10vw] w-[50vw] h-[50vw] rounded-full bg-accent/10 blur-[120px] pointer-events-none" />

        <div className="relative z-10">
          <div className="flex items-center gap-3 mb-16">
            <div className="h-12 w-12 rounded-xl bg-white/10 border border-white/20 flex items-center justify-center backdrop-blur-md shadow-lg">
              <ShieldCheck className="h-6 w-6 text-accent" />
            </div>
            <div>
              <div className="text-[10px] uppercase tracking-widest text-accent font-bold">Ethiopian Revenue Authority</div>
              <div className="font-bold text-xl tracking-tight">E-Filing Portal</div>
            </div>
          </div>

          <div className="min-h-[160px] xl:min-h-[180px] mb-8 flex items-center">
            <h1 className="text-3xl xl:text-4xl font-bold leading-tight mb-4 animate-in fade-in slide-in-from-bottom-4 duration-700" key={msgIdx}>
              {TRUST_MESSAGES[msgIdx]}
            </h1>
          </div>
          
          <div className="space-y-4 mb-16 animate-in fade-in slide-in-from-bottom-6 duration-1000 delay-200">
            <div className="flex items-center gap-3 text-white/80">
              <Lock className="h-5 w-5 text-success" />
              <span className="text-sm font-medium">Enterprise-Grade Encryption</span>
            </div>
            <div className="flex items-center gap-3 text-white/80">
              <Fingerprint className="h-5 w-5 text-success" />
              <span className="text-sm font-medium">Secure Multi-Factor Authentication</span>
            </div>
            <div className="flex items-center gap-3 text-white/80">
              <RefreshCw className="h-5 w-5 text-success" />
              <span className="text-sm font-medium">Real-Time Processing & Transparency</span>
            </div>
          </div>
        </div>

        {/* Dashboard Preview Card */}
        <div className="relative z-10 bg-white/5 border border-white/10 backdrop-blur-xl p-6 rounded-2xl shadow-2xl animate-in slide-in-from-bottom-8 duration-1000 delay-500">
          <div className="flex items-center justify-between mb-4 pb-4 border-b border-white/10">
            <div className="font-semibold flex items-center gap-2"><BarChart3 className="h-4 w-4 text-accent" /> Network Overview</div>
            <div className="flex items-center gap-2 text-xs font-medium text-success bg-success/20 px-2 py-1 rounded-full"><Activity className="h-3 w-3" /> 99.9% Uptime</div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <div className="text-xs text-white/50 uppercase tracking-wider font-semibold">Active Taxpayers</div>
              <div className="text-2xl font-bold mt-1 text-white">1.2M+</div>
            </div>
            <div>
              <div className="text-xs text-white/50 uppercase tracking-wider font-semibold">Returns Processed</div>
              <div className="text-2xl font-bold mt-1 text-white">8.4M+</div>
            </div>
          </div>
        </div>
      </div>

      {/* Right Column: Authentication Form */}
      <div className="flex-1 flex flex-col relative overflow-y-auto">
        {/* Mobile Header */}
        <div className="lg:hidden bg-navy text-white p-6 flex flex-col gap-4 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-full bg-gradient-to-br from-accent/20 to-transparent pointer-events-none" />
          <div className="flex items-center gap-3 relative z-10">
            <div className="h-10 w-10 rounded-xl bg-white/10 border border-white/20 flex items-center justify-center backdrop-blur-md">
              <ShieldCheck className="h-5 w-5 text-accent" />
            </div>
            <div>
              <div className="font-bold text-lg tracking-tight">E-Filing Portal</div>
              <div className="text-xs text-accent">Secure Digital Tax Administration</div>
            </div>
          </div>
        </div>

        {/* Form Container */}
        <div className="flex-1 flex flex-col justify-center items-center p-6 sm:p-12 xl:p-20 relative">
          <div className="absolute top-[20vh] -right-[15vw] w-[30vw] h-[30vw] rounded-full bg-primary/5 blur-[120px] pointer-events-none hidden lg:block" />

          <div className="w-full max-w-md z-10">
            <div className="mb-10 text-center lg:text-left">
              <h2 className="text-3xl font-bold tracking-tight text-foreground">Welcome Back</h2>
              <p className="text-muted-foreground mt-2 text-sm">Sign in to manage your tax obligations securely.</p>
            </div>

            <div className="glass-card rounded-3xl p-6 sm:p-10 shadow-xl border-border/50">
              {step === "login" && (
                <form onSubmit={handleLoginSubmit} className="space-y-6 animate-in fade-in slide-in-from-left-4 duration-300">
                  <div className="space-y-2">
                    <Label htmlFor="tin" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">TIN Number</Label>
                    <Input 
                      id="tin"
                      type="text" 
                      value={tin}
                      onChange={e => setTin(e.target.value)}
                      className="font-mono text-lg tracking-wider h-12 focus:ring-accent/50 focus:border-accent"
                      placeholder="1000123456"
                      disabled={isLoading}
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <Label htmlFor="password" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Password</Label>
                      <button 
                        type="button" 
                        onClick={() => setStep("forgot-email")}
                        className="text-xs font-medium text-accent hover:text-primary transition-colors focus:outline-none hover:underline"
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
                        className="pr-12 h-12 focus:ring-accent/50 focus:border-accent"
                        placeholder="••••••••"
                        disabled={isLoading}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground focus:outline-none transition-colors"
                        aria-label={showPassword ? "Hide password" : "Show password"}
                      >
                        {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                      </button>
                    </div>
                  </div>

                  <Button type="submit" disabled={isLoading} className="w-full mt-4 h-12 text-base font-medium group rounded-xl shadow-lg shadow-primary/20">
                    {isLoading ? "Authenticating securely..." : "Sign In"}
                    {!isLoading && <ArrowRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform" />}
                  </Button>
                </form>
              )}

              {step === "forgot-email" && (
                <form onSubmit={handleSendResetEmail} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
                  <div className="space-y-2 text-center mb-6">
                    <h3 className="text-xl font-bold text-foreground">Forgot Password</h3>
                    <p className="text-sm text-muted-foreground">Enter your email address to receive a secure recovery code.</p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="email" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Email Address</Label>
                    <Input 
                      id="email"
                      type="email" 
                      placeholder="contact@example.com"
                      value={resetEmail}
                      onChange={e => setResetEmail(e.target.value)}
                      required
                      className="h-12 focus:ring-accent/50 focus:border-accent"
                      disabled={isLoading}
                    />
                  </div>
                  <div className="pt-4 space-y-3">
                    <Button type="submit" disabled={isLoading} className="w-full h-12 text-base font-medium rounded-xl shadow-lg shadow-primary/20">
                      {isLoading ? "Transmitting..." : "Send Secure Recovery Code"}
                    </Button>
                    <Button type="button" disabled={isLoading} variant="ghost" className="w-full h-12 rounded-xl" onClick={() => setStep("login")}>
                      Return to Login
                    </Button>
                  </div>
                </form>
              )}

              {step === "forgot-otp" && (
                <form onSubmit={handleVerifyOtp} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
                  <div className="space-y-2 text-center mb-6">
                    <h3 className="text-xl font-bold text-foreground">Verify Identity</h3>
                    <p className="text-sm text-muted-foreground">We sent a 6-digit verification code to <span className="font-medium text-foreground">{resetEmail}</span></p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="otp" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Security Code</Label>
                    <Input 
                      id="otp"
                      type="text" 
                      placeholder="123456"
                      maxLength={6}
                      value={otp}
                      onChange={e => setOtp(e.target.value)}
                      required
                      className="h-12 text-center text-xl tracking-[0.5em] font-mono focus:ring-accent/50 focus:border-accent"
                      disabled={isLoading}
                    />
                  </div>
                  <div className="pt-4 space-y-3">
                    <Button type="submit" disabled={isLoading} className="w-full h-12 text-base font-medium rounded-xl shadow-lg shadow-primary/20">
                      {isLoading ? "Verifying..." : "Verify Identity"}
                    </Button>
                    <Button type="button" disabled={isLoading} variant="ghost" className="w-full h-12 rounded-xl" onClick={() => setStep("forgot-email")}>
                      Back
                    </Button>
                  </div>
                </form>
              )}

              {step === "forgot-reset" && (
                <form onSubmit={handleResetPassword} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
                  <div className="space-y-2 text-center mb-6">
                    <h3 className="text-xl font-bold text-foreground">Set New Password</h3>
                    <p className="text-sm text-muted-foreground">Create a new strong password to secure your account.</p>
                  </div>
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="newPassword" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">New Password</Label>
                      <div className="relative">
                        <Input 
                          id="newPassword"
                          type={showPassword ? "text" : "password"} 
                          value={newPassword}
                          onChange={e => setNewPassword(e.target.value)}
                          className="pr-12 h-12 focus:ring-accent/50 focus:border-accent"
                          placeholder="••••••••"
                          required
                          disabled={isLoading}
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground focus:outline-none transition-colors"
                        >
                          {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                        </button>
                      </div>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="confirmPassword" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Confirm Password</Label>
                      <Input 
                        id="confirmPassword"
                        type={showPassword ? "text" : "password"} 
                        value={confirmPassword}
                        onChange={e => setConfirmPassword(e.target.value)}
                        className="pr-12 h-12 focus:ring-accent/50 focus:border-accent"
                        placeholder="••••••••"
                        required
                        disabled={isLoading}
                      />
                    </div>
                  </div>
                  <div className="pt-6 space-y-3">
                    <Button type="submit" disabled={isLoading} className="w-full h-12 text-base font-medium rounded-xl shadow-lg shadow-primary/20">
                      {isLoading ? "Securing account..." : "Update Password"}
                    </Button>
                  </div>
                </form>
              )}
            </div>

            <div className="mt-8 text-center">
              <p className="text-sm text-muted-foreground">
                Don't have an account?{" "}
                <Link to="/signup" className="font-semibold text-accent hover:text-primary transition-colors hover:underline">
                  Create Account
                </Link>
              </p>
              <div className="mt-8 pt-6 border-t border-border flex flex-wrap justify-center gap-6 text-xs text-muted-foreground font-medium">
                <button className="hover:text-foreground transition-colors font-medium">Support & Help Desk</button>
                <button className="hover:text-foreground transition-colors font-medium">Privacy Policy</button>
                <button className="hover:text-foreground transition-colors font-medium">Amharic / English</button>
                <Link to="/officer/login" className="hover:text-foreground transition-colors font-semibold">Staff & Officer Portal</Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
