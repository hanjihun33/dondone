type PrimitiveQueryValue = string | number | boolean;

type RequestQuery = Record<
  string,
  PrimitiveQueryValue | PrimitiveQueryValue[] | null | undefined
>;

type ApiEnvelope<T> = {
  code: string;
  message: string | null;
  data: T;
  details: unknown;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: unknown;

  constructor(status: number, code: string, message: string, details: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

type RequestOptions = {
  method?: "GET" | "POST" | "PUT";
  body?: unknown;
  query?: RequestQuery;
  token?: string | null;
  signal?: AbortSignal;
};

const DEFAULT_API_BASE_URL = "http://localhost:8080";

function getApiBaseUrl() {
  const configured = import.meta.env.VITE_API_BASE_URL;
  return (configured || DEFAULT_API_BASE_URL).replace(/\/$/, "");
}

function buildUrl(path: string, query?: RequestQuery) {
  const url = new URL(`${getApiBaseUrl()}${path}`);

  if (!query) {
    return url.toString();
  }

  Object.entries(query).forEach(([key, value]) => {
    if (value == null) {
      return;
    }

    if (Array.isArray(value)) {
      value.forEach((item) => {
        url.searchParams.append(key, String(item));
      });
      return;
    }

    url.searchParams.set(key, String(value));
  });

  return url.toString();
}

async function readJsonSafely<T>(response: Response): Promise<ApiEnvelope<T> | null> {
  const text = await response.text();
  if (!text) {
    return null;
  }

  return JSON.parse(text) as ApiEnvelope<T>;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}) {
  const headers = new Headers();

  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  if (options.token) {
    headers.set("Authorization", `Bearer ${options.token}`);
  }

  const response = await fetch(buildUrl(path, options.query), {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: options.signal
  });

  const payload = await readJsonSafely<T>(response);

  if (!response.ok) {
    throw new ApiError(
      response.status,
      payload?.code ?? "HTTP_ERROR",
      payload?.message ?? `Request failed with status ${response.status}.`,
      payload?.details ?? null
    );
  }

  return payload?.data as T;
}
