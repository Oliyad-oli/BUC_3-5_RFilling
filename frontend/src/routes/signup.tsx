import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { ShieldCheck, Mail, KeyRound, CheckCircle2, ArrowRight, ArrowLeft, Clock, Zap, FileText, CheckCircle } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { InputOTP, InputOTPGroup, InputOTPSlot } from "@/components/ui/input-otp";
import { useAuth } from "@/lib/auth-service";

export const Route = createFileRoute("/signup")({
  component: TaxpayerSignup,
});

type Step = "registration" | "otp" | "password";

const TRUST_MESSAGES = [
  "Start filing your taxes completely online today.",
  "Join over 1.2 million active digital taxpayers.",
  "Your financial data is protected by enterprise encryption.",
  "Track, manage, and pay obligations in real-time.",
  "Eliminate paperwork and manual filing queues."
];

function TaxpayerSignup() {
  const [step, setStep] = useState<Step>("registration");
  const [formData, setFormData] = useState({
    tin: "",
    name: "",
    email: "",
    password: "",
    confirmPassword: ""
  });
  const [otp, setOtp] = useState("");
  const navigate = useNavigate();
  const signup = useAuth(s => s.signupTaxpayer);

  // Rotating Messages
  const [msgIdx, setMsgIdx] = useState(0);
  useEffect(() => {
    const int = setInterval(() => setMsgIdx(i => (i + 1) % TRUST_MESSAGES.length), 4000);
    return () => clearInterval(int);
  }, []);

  const handleNext = (e: React.FormEvent) => {
    e.preventDefault();
    if (step === "registration") {
      if (!formData.tin || !formData.email || !formData.name) {
        return toast.error("Please fill in all details");
      }
      toast.success("OTP sent to your email");
      setStep("otp");
    } else if (step === "otp") {
      if (otp.length !== 6) {
        return toast.error("Please enter the 6-digit OTP");
      }
      toast.success("Email verified");
      setStep("password");
    } else if (step === "password") {
      if (formData.password !== formData.confirmPassword) {
        return toast.error("Passwords do not match");
      }
      if (formData.password.length < 8) {
        return toast.error("Password must be at least 8 characters");
      }
      signup(formData);
      toast.success("Account created successfully!");
      navigate({ to: "/login" });
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col lg:flex-row overflow-hidden">
      
      {/* Left Column: Trust & Hero Section */}
      <div className="hidden lg:flex lg:w-[45%] xl:w-[50%] bg-navy text-white flex-col justify-between p-12 xl:p-20 relative overflow-hidden shrink-0 shadow-2xl z-10">
        {/* Background embellishments */}
        <div className="absolute top-0 right-0 w-full h-[40vh] bg-gradient-to-bl from-accent/20 to-transparent pointer-events-none" />
        <div className="absolute -top-[20vw] -right-[10vw] w-[50vw] h-[50vw] rounded-full bg-accent/10 blur-[120px] pointer-events-none" />

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
              <Zap className="h-5 w-5 text-success" />
              <span className="text-sm font-medium">Faster Processing Times</span>
            </div>
            <div className="flex items-center gap-3 text-white/80">
              <FileText className="h-5 w-5 text-success" />
              <span className="text-sm font-medium">Reduced Paperwork & Manual Errors</span>
            </div>
            <div className="flex items-center gap-3 text-white/80">
              <CheckCircle className="h-5 w-5 text-success" />
              <span className="text-sm font-medium">Transparent Status Tracking</span>
            </div>
          </div>
        </div>

        {/* Info Card */}
        <div className="relative z-10 bg-white/5 border border-white/10 backdrop-blur-xl p-6 rounded-2xl shadow-2xl animate-in slide-in-from-bottom-8 duration-1000 delay-500">
          <div className="flex items-center gap-4">
            <div className="h-12 w-12 rounded-full bg-accent/20 flex items-center justify-center shrink-0">
              <Clock className="h-6 w-6 text-accent" />
            </div>
            <div>
              <div className="font-semibold text-white">Takes less than 2 minutes</div>
              <div className="text-xs text-white/60 mt-1">Have your TIN number ready. We will verify your identity via a secure email OTP.</div>
            </div>
          </div>
        </div>
      </div>

      {/* Right Column: Registration Form */}
      <div className="flex-1 flex flex-col relative overflow-y-auto">
        {/* Mobile Header */}
        <div className="lg:hidden bg-navy text-white p-6 flex flex-col gap-4 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-full bg-gradient-to-bl from-accent/20 to-transparent pointer-events-none" />
          <div className="flex items-center gap-3 relative z-10">
            <div className="h-10 w-10 rounded-xl bg-white/10 border border-white/20 flex items-center justify-center backdrop-blur-md">
              <ShieldCheck className="h-5 w-5 text-accent" />
            </div>
            <div>
              <div className="font-bold text-lg tracking-tight">E-Filing Portal</div>
              <div className="text-xs text-accent">Join 1.2M+ Digital Taxpayers</div>
            </div>
          </div>
        </div>

        <div className="flex-1 flex flex-col justify-center items-center p-6 sm:p-12 xl:p-20 relative">
          <div className="absolute bottom-[20vh] -left-[15vw] w-[30vw] h-[30vw] rounded-full bg-primary/5 blur-[120px] pointer-events-none hidden lg:block" />

          <div className="w-full max-w-md z-10">
            <div className="mb-10 text-center lg:text-left">
              <h2 className="text-3xl font-bold tracking-tight text-foreground">Create Account</h2>
              <p className="text-muted-foreground mt-2 text-sm">Register your TIN for secure digital tax filing.</p>
            </div>

            <div className="glass-card rounded-3xl p-6 sm:p-10 shadow-xl border-border/50 relative overflow-hidden">
              
              {/* Progress Indicator */}
              <div className="flex items-center justify-center gap-4 mb-10">
                <StepIcon active={true} completed={step === "otp" || step === "password"} icon={<ShieldCheck className="h-4 w-4" />} />
                <div className={`h-[2px] w-12 transition-colors duration-500 ${step === "otp" || step === "password" ? "bg-accent" : "bg-border"}`} />
                <StepIcon active={step === "otp" || step === "password"} completed={step === "password"} icon={<Mail className="h-4 w-4" />} />
                <div className={`h-[2px] w-12 transition-colors duration-500 ${step === "password" ? "bg-accent" : "bg-border"}`} />
                <StepIcon active={step === "password"} completed={false} icon={<KeyRound className="h-4 w-4" />} />
              </div>

              <form onSubmit={handleNext} className="space-y-6">
                {step === "registration" && (
                  <div className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
                    <div className="space-y-2">
                      <Label htmlFor="tin" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">TIN Number</Label>
                      <Input id="tin" required value={formData.tin} onChange={e => setFormData({...formData, tin: e.target.value})} className="h-12 font-mono tracking-wider focus:ring-accent/50 focus:border-accent" placeholder="1000123456" />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="name" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Full Name / Entity Name</Label>
                      <Input id="name" required value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} className="h-12 focus:ring-accent/50 focus:border-accent" placeholder="ACME Trading PLC" />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="email" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Email Address</Label>
                      <Input id="email" type="email" required value={formData.email} onChange={e => setFormData({...formData, email: e.target.value})} className="h-12 focus:ring-accent/50 focus:border-accent" placeholder="contact@acme.com" />
                    </div>
                  </div>
                )}

                {step === "otp" && (
                  <div className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300 text-center">
                    <p className="text-sm text-muted-foreground mb-8">Enter the secure 6-digit code sent to <span className="font-medium text-foreground">{formData.email}</span></p>
                    <div className="flex justify-center">
                      <InputOTP maxLength={6} value={otp} onChange={setOtp}>
                        <InputOTPGroup className="gap-2">
                          <InputOTPSlot index={0} className="h-12 w-10 sm:w-12 text-lg rounded-md border" />
                          <InputOTPSlot index={1} className="h-12 w-10 sm:w-12 text-lg rounded-md border" />
                          <InputOTPSlot index={2} className="h-12 w-10 sm:w-12 text-lg rounded-md border" />
                          <InputOTPSlot index={3} className="h-12 w-10 sm:w-12 text-lg rounded-md border" />
                          <InputOTPSlot index={4} className="h-12 w-10 sm:w-12 text-lg rounded-md border" />
                          <InputOTPSlot index={5} className="h-12 w-10 sm:w-12 text-lg rounded-md border" />
                        </InputOTPGroup>
                      </InputOTP>
                    </div>
                    <div className="pt-4">
                      <button type="button" onClick={() => setStep("registration")} className="text-sm font-medium text-accent hover:text-primary transition-colors">Change email address</button>
                    </div>
                  </div>
                )}

                {step === "password" && (
                  <div className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
                    <div className="space-y-2">
                      <Label htmlFor="pass" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Create Secure Password</Label>
                      <Input id="pass" type="password" required value={formData.password} onChange={e => setFormData({...formData, password: e.target.value})} className="h-12 focus:ring-accent/50 focus:border-accent" placeholder="••••••••" />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="cpass" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Confirm Password</Label>
                      <Input id="cpass" type="password" required value={formData.confirmPassword} onChange={e => setFormData({...formData, confirmPassword: e.target.value})} className="h-12 focus:ring-accent/50 focus:border-accent" placeholder="••••••••" />
                    </div>
                  </div>
                )}

                <div className="pt-4 flex gap-3">
                  {step !== "registration" && (
                    <Button type="button" variant="outline" onClick={() => setStep(step === "password" ? "otp" : "registration")} className="h-12 w-12 shrink-0 p-0 rounded-xl">
                      <ArrowLeft className="h-5 w-5" />
                    </Button>
                  )}
                  <Button type="submit" className="flex-1 h-12 text-base font-medium group rounded-xl shadow-lg shadow-primary/20">
                    {step === "password" ? "Complete Setup" : "Secure Continue"}
                    {step !== "password" && <ArrowRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform" />}
                    {step === "password" && <CheckCircle2 className="ml-2 h-4 w-4" />}
                  </Button>
                </div>
              </form>
            </div>

            <div className="mt-8 text-center">
              <p className="text-sm text-muted-foreground">
                Already have an account?{" "}
                <Link to="/login" className="font-semibold text-accent hover:text-primary transition-colors hover:underline">
                  Sign In
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

function StepIcon({ active, completed, icon }: { active: boolean, completed: boolean, icon: React.ReactNode }) {
  if (completed) {
    return <div className="h-10 w-10 rounded-full bg-accent text-accent-foreground flex items-center justify-center shrink-0 shadow-lg shadow-accent/20 transition-all duration-300"><CheckCircle2 className="h-5 w-5" /></div>;
  }
  if (active) {
    return <div className="h-10 w-10 rounded-full bg-primary text-primary-foreground flex items-center justify-center shrink-0 ring-4 ring-primary/20 transition-all duration-300">{icon}</div>;
  }
  return <div className="h-10 w-10 rounded-full bg-muted text-muted-foreground flex items-center justify-center shrink-0 transition-all duration-300">{icon}</div>;
}
