import { create } from "zustand";
import { persist } from "zustand/middleware";
import { mockAuthData, Session } from "../mock/auth";

interface AuthState {
  session: Session | null;
  loginTaxpayer: (tin: string, password: string) => boolean;
  loginOfficer: (username: string, password: string) => boolean;
  loginAuditor: (username: string, password: string) => boolean;
  loginAdmin: (username: string, password: string) => boolean;
  logout: () => void;
  validateSession: () => boolean;
  refreshSession: () => void;
  signupTaxpayer: (userData: any) => boolean;
}

export const useAuth = create<AuthState>()(
  persist(
    (set, get) => ({
      session: null,

      loginTaxpayer: (tin, password) => {
        const user = mockAuthData.taxpayers.find(
          (u) => u.tin === tin && u.password === password
        );
        if (user) {
          const now = new Date().toISOString();
          set({ session: { user, sessionStart: now, lastActivity: now } });
          return true;
        }
        return false;
      },

      signupTaxpayer: (userData) => {
        // Mock a successful signup by returning true
        // For a real app, this would make an API call to create the user
        // and perhaps automatically log them in or redirect them to login.
        return true;
      },

      loginOfficer: (username, password) => {
        const user = mockAuthData.internal.find(
          (u) => u.username === username && u.password === password && u.role === "OFFICER"
        );
        if (user) {
          const now = new Date().toISOString();
          set({ session: { user, sessionStart: now, lastActivity: now } });
          return true;
        }
        return false;
      },

      loginAuditor: (username, password) => {
        const user = mockAuthData.internal.find(
          (u) => u.username === username && u.password === password && u.role === "AUDITOR"
        );
        if (user) {
          const now = new Date().toISOString();
          set({ session: { user, sessionStart: now, lastActivity: now } });
          return true;
        }
        return false;
      },

      loginAdmin: (username, password) => {
        const user = mockAuthData.internal.find(
          (u) => u.username === username && u.password === password && u.role === "ADMIN"
        );
        if (user) {
          const now = new Date().toISOString();
          set({ session: { user, sessionStart: now, lastActivity: now } });
          return true;
        }
        return false;
      },

      logout: () => {
        set({ session: null });
      },

      validateSession: () => {
        const { session } = get();
        if (!session) return false;
        
        // Example logic: Session expires after 2 hours of inactivity
        const lastActivityDate = new Date(session.lastActivity);
        const now = new Date();
        const diffMs = now.getTime() - lastActivityDate.getTime();
        const hours = diffMs / (1000 * 60 * 60);

        if (hours > 2) {
          set({ session: null });
          return false;
        }

        return true;
      },

      refreshSession: () => {
        const { session } = get();
        if (session) {
          set({ session: { ...session, lastActivity: new Date().toISOString() } });
        }
      },
    }),
    {
      name: "auth-storage",
    }
  )
);
