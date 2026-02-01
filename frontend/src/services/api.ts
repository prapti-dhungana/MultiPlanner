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

export type SortBy = "FASTEST" | "FEWEST_TRANSFERS";

export type RouteMultiOptions = {
  sortBy?: SortBy;
  includeBus?: boolean;
  includeTram?: boolean;
};


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

  if (!res.ok) return throwApiError(res);
  return (await res.json()) as LegSummary;
}

export async function routeMulti(
  stops: Station[],
  options: RouteMultiOptions = {}
): Promise<MultiRouteResponse> {
  const res = await fetch("/api/route/multi", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      stops,
      preferences: {
        sortBy: options.sortBy ?? "FASTEST",
      },
      modes: {
        includeBus: options.includeBus ?? true,
        includeTram: options.includeTram ?? true,
      },
    }),
  });

  if (!res.ok) return throwApiError(res);
  return (await res.json()) as MultiRouteResponse;
}

async function throwApiError(res: Response): Promise<never> {
  let data: any = null;

  // Try JSON first (but do NOT throw inside this try, or you'll catch your own error)
  try {
    const contentType = res.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
      data = await res.json();
    }
  } catch {
    data = null;
  }

  // Prefer backend message/error fields
  const backendMsg =
    (data && (data.message || data.error)) ? String(data.message || data.error) : "there are no journeys for this route";

  // Fallback to plain text if JSON wasn't available/useful
  let textMsg = "";
  if (!backendMsg) {
    try {
      textMsg = await res.text();
    } catch {
      textMsg = "";
    }
  }

  const msg = backendMsg || textMsg || `HTTP ${res.status}`;
  throw new Error(msg);
}



