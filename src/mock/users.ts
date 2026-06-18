export type Role = "TAXPAYER" | "OFFICER" | "AUDITOR" | "ADMIN";

export interface BaseUser {
  id: string;
  role: Role;
}

export interface TaxpayerUser extends BaseUser {
  role: "TAXPAYER";
  tin: string;
  name: string;
  password?: string;
}

export interface InternalUser extends BaseUser {
  role: "OFFICER" | "AUDITOR" | "ADMIN";
  username: string;
}

export type User = TaxpayerUser | InternalUser;

export const MOCK_TAXPAYERS = [
  { tin: "1000123456", name: "Abebe Kebede", password: "Taxpayer@123", id: "t1", role: "TAXPAYER" as const },
  { tin: "1000456789", name: "Hana Bekele", password: "Taxpayer@123", id: "t2", role: "TAXPAYER" as const },
  { tin: "1000789123", name: "Samuel Tadesse", password: "Taxpayer@123", id: "t3", role: "TAXPAYER" as const },
];

export const MOCK_INTERNAL_USERS = [
  { username: "officer", password: "Officer@123", id: "o1", role: "OFFICER" as const },
  { username: "auditor", password: "Auditor@123", id: "a1", role: "AUDITOR" as const },
  { username: "admin", password: "Admin@123", id: "ad1", role: "ADMIN" as const },
];
