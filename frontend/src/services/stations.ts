//Given a search string, fetch stations from the backend.

export type Station = {
  code: string;
  name: string;
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

  return (await res.json()) as Station[];
}
