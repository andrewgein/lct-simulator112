const text = (value) => String(value ?? "").trim();
const uniqueParts = (parts) => [...new Map(parts.map(text).filter(Boolean).map((part) => [part.toLowerCase(), part])).values()];

export const emptyAddressDetails = () => ({ country: "", region: "", city: "", district: "", area: "", street: "", house: "", block: "", apartment: "", entrance: "", floor: "", postalCode: "" });

export function addressFromSuggestion(suggestion) {
  const data = suggestion.data || {};
  return {
    details: {
      country: data.country || "",
      region: data.region_with_type || data.region || "",
      city: uniqueParts([data.city_with_type || data.city, data.settlement_with_type || data.settlement]).join(", "),
      district: data.city_district_with_type || data.city_district || "",
      area: data.area_with_type || data.area || "",
      street: data.street_with_type || data.street || "",
      house: data.house || "",
      block: data.block || "",
      apartment: data.flat || "",
      entrance: data.entrance || "",
      floor: data.floor || "",
      postalCode: data.postal_code || ""
    },
    types: { house: data.house_type || "д", block: data.block_type || "корп", apartment: data.flat_type || "кв" },
    incident: {
      city: data.city || data.settlement || "", street: data.street || "", house: data.house || "",
      building: data.block || "", apartment: data.flat || "", floor: data.floor || ""
    }
  };
}

const numberedPart = (value, type, fallback) => text(value) ? `${text(type) || fallback} ${text(value)}` : "";

export function formatAddressDetails(details, types = {}) {
  return uniqueParts([
    details.postalCode, details.country, details.region, details.area, details.city, details.district, details.street,
    numberedPart(details.house, types.house, "д"), numberedPart(details.block, types.block, "корп"),
    numberedPart(details.apartment, types.apartment, "кв"),
    numberedPart(details.entrance, "", "подъезд"), numberedPart(details.floor, "", "этаж")
  ]).join(", ");
}

export function formatIncidentAddress(address, selected = null) {
  return formatAddressDetails({
    city: selected && selected.incident.city === address.city ? selected.details.city : address.city,
    street: selected && selected.incident.street === address.street ? selected.details.street : address.street,
    house: address.house,
    block: address.building, apartment: address.apartment, floor: Number(address.floor) > 0 ? address.floor : "",
    country: "", region: "", district: "", area: "", entrance: "", postalCode: ""
  }, selected?.types);
}
