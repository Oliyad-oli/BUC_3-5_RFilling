import { User, MOCK_TAXPAYERS, MOCK_INTERNAL_USERS } from "./users";

export interface Session {
  user: User;
  sessionStart: string;
  lastActivity: string;
}

export const mockAuthData = {
  taxpayers: MOCK_TAXPAYERS,
  internal: MOCK_INTERNAL_USERS,
};
