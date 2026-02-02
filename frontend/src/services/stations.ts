//Given a search string, fetch stations from the backend.

export type Station = {
  code: string;
  name: string;
};

export async function searchStations(query: string): Promise<Station[]> {
  const trimmed = query.trim();
  if (!trimmed) return [];

  // Call via Nginx proxy so we stay same-origin and avoid CORS issues.
  const res = await fetch(
    `/api/stations?query=${encodeURIComponent(trimmed)}`
  );

  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }

  const data = (await res.json()) as Station[];
  return data.slice(0, 5);
}
