export type HealthResponse = {
  status: string;
  service: string;
  timestamp: string;
};

export async function fetchHealth(): Promise<HealthResponse> {
  const res = await fetch("http://localhost:8081/health");
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return (await res.json()) as HealthResponse;
}
