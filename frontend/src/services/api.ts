import { Station } from "./stations";

export type HealthResponse = {
  status: string;
  service: string;
  timestamp: string;
};

export async function fetchHealth(): Promise<HealthResponse> {
  // backend health endpoint 
  const res = await fetch("/health");
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return (await res.json()) as HealthResponse;
}

export type Segment = {
  mode: string | null;
  line?: string;
  direction?: string;
  from?: string;
  to?: string;
  durationMinutes: number;
};

export type LegSummary = {
  fromName: string;
  toName: string;
  fromStopPointId: string;
  toStopPointId: string;
  durationMinutes: number;
  startDateTime?: string;
  arrivalDateTime?: string;
  interchanges: number;
  summary: string;
  segments: Segment[];
};

export type MultiRouteResponse = {
  mode: "multi";
  legs: number; // count
  totalDurationMinutes: number;
  totalInterchanges: number;
  results: LegSummary[];
};

export async function routeLeg(from: Station, to: Station): Promise<LegSummary> {
  const res = await fetch("/api/route", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ from, to }),
  });

  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return (await res.json()) as LegSummary;
}

export async function routeMulti(stops: Station[]): Promise<MultiRouteResponse> {
  const res = await fetch("/api/route/multi", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ stops }),
  });

  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return (await res.json()) as MultiRouteResponse;
}
