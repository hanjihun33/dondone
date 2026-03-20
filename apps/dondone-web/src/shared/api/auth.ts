import { apiRequest } from "./client";

export type AuthLoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  email: string;
  name: string;
  phoneNumber: string | null;
};

type LoginRequest = {
  email: string;
  password: string;
};

export async function loginUser(request: LoginRequest) {
  return apiRequest<AuthLoginResponse>("/api/auth/login", {
    method: "POST",
    body: request
  });
}
