import { apiRequest } from "./client";

export type AdminEmployerCompanySummaryResponse = {
  companyId: number;
  companyName: string;
  companyCode: string;
  defaultWorkplaceId: number;
  defaultWorkplaceName: string;
  address: string;
  detailAddress: string | null;
  latitude: number;
  longitude: number;
  allowedRadiusMeters: number;
  workplaceSettingsConfigured: boolean;
  hasActiveEmployerSignupCode: boolean;
  latestEmployerSignupCodeIssuedAt: string | null;
  createdAt: string;
};

export type AdminEmployerCompaniesResponse = {
  companies: AdminEmployerCompanySummaryResponse[];
};

export type AdminEmployerCompanyCreatedResponse = {
  companyId: number;
  companyName: string;
  companyCode: string;
  defaultWorkplaceId: number;
  defaultWorkplaceName: string;
  address: string;
  detailAddress: string | null;
  latitude: number;
  longitude: number;
  allowedRadiusMeters: number;
  workplaceSettingsConfigured: boolean;
  employerSignupCode: string;
  employerSignupCodeIssuedAt: string;
  createdAt: string;
};

type CreateAdminEmployerCompanyRequest = {
  companyName: string;
  companyCode: string;
};

export async function getAdminEmployerCompanies(token: string) {
  return apiRequest<AdminEmployerCompaniesResponse>("/api/admin/employers/companies", {
    token
  });
}

export async function createAdminEmployerCompany(
  token: string,
  request: CreateAdminEmployerCompanyRequest
) {
  return apiRequest<AdminEmployerCompanyCreatedResponse>("/api/admin/employers/companies", {
    method: "POST",
    token,
    body: request
  });
}
