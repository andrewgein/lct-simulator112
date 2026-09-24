export const prerender = false;

const REQUEST_TIMEOUT_MS = 5_000;

function addressQueries(address) {
  const withoutApartment = address.replace(/,?\s*(?:кв(?:артира)?\.?)\s*\d+.*$/iu, "").trim();
  const normalized = withoutApartment
    .replace(/(?:д|дом)\.?\s*(\d+[а-яa-z]?)/giu, "$1")
    .replace(/корп(?:ус)?\.?\s*(\d+[а-яa-z]?)/giu, "к$1")
    .replace(/\s*,\s*/g, ", ")
    .replace(/,\s*к/giu, " к")
    .replace(/\s{2,}/g, " ")
    .trim();
  const parts = normalized.split(",").map((part) => part.trim()).filter(Boolean);
  const streetKinds = /\b(улица|ул\.|проспект|пр-т|шоссе|переулок|пер\.|набережная|бульвар|проезд|площадь)\b/iu;
  const withStreetKind = parts.length >= 2 && !streetKinds.test(parts[1])
    ? [parts[0], `${parts[1]} улица`, ...parts.slice(2)].join(", ")
    : normalized;
  return [...new Set([withStreetKind, normalized, withoutApartment])];
}

async function request(url, options = {}) {
  const response = await fetch(url, { ...options, signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS) });
  if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
  return response.json();
}

async function geocodeWithOsm({ address, lat, lng }) {
  if (!address) {
    const params = new URLSearchParams({ format: "jsonv2", lat: String(lat), lon: String(lng), "accept-language": "ru" });
    const result = await request(`https://nominatim.openstreetmap.org/reverse?${params}`, { headers: { Accept: "application/json", "User-Agent": "Simulator112-training/1.0" } });
    if (!result.display_name) throw new Error("Address not found in OpenStreetMap");
    return { lat: Number(lat), lng: Number(lng), displayName: result.display_name, provider: "openstreetmap" };
  }

  for (const query of addressQueries(address)) {
    const params = new URLSearchParams({ q: query, format: "jsonv2", limit: "1", countrycodes: "ru", "accept-language": "ru" });
    const result = (await request(`https://nominatim.openstreetmap.org/search?${params}`, { headers: { Accept: "application/json", "User-Agent": "Simulator112-training/1.0" } }))[0];
    if (result) return { lat: Number(result.lat), lng: Number(result.lon), displayName: result.display_name, provider: "openstreetmap" };
  }
  throw new Error("Address not found in OpenStreetMap");
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json", "Cache-Control": "private, max-age=86400" } });
}

export async function GET({ url }) {
  const address = url.searchParams.get("address")?.trim();
  const lat = Number(url.searchParams.get("lat"));
  const lng = Number(url.searchParams.get("lng"));
  const hasCoordinates = Number.isFinite(lat) && Number.isFinite(lng) && url.searchParams.has("lat") && url.searchParams.has("lng");
  if ((!address && !hasCoordinates) || (address && address.length > 500)) return json({ error: "Некорректный запрос геокодирования" }, 400);

  try {
    return json(await geocodeWithOsm({ address, lat, lng }));
  } catch (error) {
    console.error("OpenStreetMap geocoding failed", error);
    return json({ error: "Не удалось определить адрес или координаты" }, 502);
  }
}
