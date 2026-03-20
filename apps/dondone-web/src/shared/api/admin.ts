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
  hasJoinedEmployer: boolean;
  employerCount: number;
  latestEmployerJoinedAt: string | null;
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
  hasJoinedEmployer: boolean;
  employerCount: number;
  latestEmployerJoinedAt: string | null;
  employerSignupCode: string;
  employerSignupCodeIssuedAt: string;
  createdAt: string;
};

export type AdminEmployerSignupCodeResponse = {
  companyId: number;
  companyName: string;
  defaultWorkplaceId: number;
  defaultWorkplaceName: string;
  employerSignupCode: string;
  issuedAt: string;
};

export type AdminEmployerCompanyEmployerSummaryResponse = {
  employerProfileId: number;
  accountId: number;
  displayName: string;
  email: string | null;
  profileStatus: string;
  defaultWorkplaceId: number;
  defaultWorkplaceName: string | null;
  workplaceSettingsConfigured: boolean;
  joinedAt: string;
};

export type AdminEmployerCompanyEmployersResponse = {
  companyId: number;
  companyName: string;
  employers: AdminEmployerCompanyEmployerSummaryResponse[];
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

export async function getAdminEmployerSignupCode(token: string, companyId: number) {
  return apiRequest<AdminEmployerSignupCodeResponse>(
    `/api/admin/employers/companies/${companyId}/signup-code`,
    {
      token
    }
  );
}

export async function getAdminEmployerCompanyEmployers(token: string, companyId: number) {
  return apiRequest<AdminEmployerCompanyEmployersResponse>(
    `/api/admin/employers/companies/${companyId}/employers`,
    {
      token
    }
  );
}
