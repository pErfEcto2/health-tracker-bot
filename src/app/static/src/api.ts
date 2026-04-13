// HTTP client. Uses session cookie (set by server) and echoes CSRF cookie back as header.

const BASE = "/api/v1";

function getCsrf(): string {
  const m = document.cookie.match(/(?:^|;\s*)csrf_token=([^;]+)/);
  return m ? decodeURIComponent(m[1]) : "";
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T | null> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (method !== "GET" && method !== "HEAD") headers["X-CSRF-Token"] = getCsrf();

  const res = await fetch(BASE + path, {
    method,
    credentials: "same-origin",
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204) return null;

  let data: unknown = null;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); } catch { data = { detail: text }; }
  }

  if (!res.ok) {
    const msg = (data as { detail?: string })?.detail ?? `HTTP ${res.status}`;
    throw new ApiError(res.status, msg);
  }
  return data as T;
}

export const api = {
  get: <T = unknown>(p: string) => request<T>("GET", p) as Promise<T>,
  post: <T = unknown>(p: string, b?: unknown) => request<T>("POST", p, b) as Promise<T>,
  put: <T = unknown>(p: string, b?: unknown) => request<T>("PUT", p, b) as Promise<T>,
  del: <T = unknown>(p: string) => request<T>("DELETE", p) as Promise<T>,
};
