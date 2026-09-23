export const prerender = false;

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

export async function GET({ url }) {
  const address = url.searchParams.get("address")?.trim();
  if (!address || address.length > 500) return new Response(JSON.stringify({ error: "Некорректный адрес" }), { status: 400, headers: { "Content-Type": "application/json" } });

  try {
    for (const query of addressQueries(address)) {
      const params = new URLSearchParams({ q: query, format: "jsonv2", limit: "1", countrycodes: "ru", "accept-language": "ru" });
      const response = await fetch(`https://nominatim.openstreetmap.org/search?${params}`, { headers: { Accept: "application/json", "User-Agent": "Simulator112-training/1.0" } });
      if (!response.ok) continue;
      const result = (await response.json())[0];
      if (result) return new Response(JSON.stringify({ lat: Number(result.lat), lng: Number(result.lon), displayName: result.display_name }), { headers: { "Content-Type": "application/json", "Cache-Control": "private, max-age=86400" } });
    }
    return new Response(JSON.stringify({ error: "Адрес не найден" }), { status: 404, headers: { "Content-Type": "application/json" } });
  } catch (error) {
    console.error("OpenStreetMap geocoding failed", error);
    return new Response(JSON.stringify({ error: "Не удалось определить координаты" }), { status: 502, headers: { "Content-Type": "application/json" } });
  }
}
