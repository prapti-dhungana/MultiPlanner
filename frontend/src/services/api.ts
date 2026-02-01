import { Station } from "./stations";

export type HealthResponse = {
  status: string;
  service: string;
  timestamp: string;
};

export async function fetchHealth(): Promise<HealthResponse> {
  // use proxy (works in dev + deployment)
  const res = await fetch("/health");
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return (await res.json()) as HealthResponse;
}

export async function routeLeg(from: Station, to: Station) {
  const res = await fetch("/api/route", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ from, to }),
  });

  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

export async function routeMulti(stops: Station[]) {
  const res = await fetch("/api/route/multi", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ stops }),
  });

  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}
