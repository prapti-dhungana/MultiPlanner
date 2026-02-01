//Given a search string, fetch stations from the backend.

export type Station = {
  code: string;
  name: string;
  town?: string;
};

export async function searchStations(query: string): Promise<Station[]> {
  const trimmed = query.trim();
  if (!trimmed) return [];

  const res = await fetch(
    `http://localhost:8081/api/stations?query=${encodeURIComponent(trimmed)}`
  );

  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }

  const data = (await res.json()) as Station[];
  return data.slice(0, 5);
}
