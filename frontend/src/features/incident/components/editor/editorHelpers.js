export const emptyPerson = () => ({ phone: "", contactPhone: "", onScenePhone: "", lastName: "", firstName: "", middleName: "", status: "", address: "", additionalInfo: "" });
export const normalizePerson = (person = {}) => ({ ...emptyPerson(), ...person });
const fullName = (person) => [person?.lastName, person?.firstName, person?.middleName].filter(Boolean).join(" ");
export const applicantName = (card) => fullName(card.applicant) || "Заявитель не указан";
export const cardAddress = (card) => card.applicant?.address || "Адрес не указан";
const normalizeText = (value) => (value || "").trim().toLowerCase();
const normalizePhone = (value) => (value || "").replace(/\D/g, "");

/** Finds already saved cards that look like the same case: same address, same applicant's phone or same applicant name. */
export function findLinkSuggestions(applicant, candidateCards) {
  const address = normalizeText(applicant?.address);
  const name = normalizeText(fullName(applicant));
  const phones = new Set([applicant?.phone, applicant?.contactPhone, applicant?.onScenePhone].map(normalizePhone).filter(Boolean));
  if (!address && !name && !phones.size) return [];
  return candidateCards.filter((card) => {
    const cardPhones = [card.applicant?.phone, card.applicant?.contactPhone, card.applicant?.onScenePhone].map(normalizePhone).filter(Boolean);
    return (!!address && normalizeText(card.applicant?.address) === address)
      || (!!name && normalizeText(fullName(card.applicant)) === name)
      || cardPhones.some((phone) => phones.has(phone));
  });
}
export const formatPhone = (phone) => {
  const digits = phone?.replace(/\D/g, "") || "";
  if (digits.length === 11 && /^[78]/.test(digits)) return `+7 (${digits.slice(1, 4)}) ${digits.slice(4, 7)}-${digits.slice(7, 9)}-${digits.slice(9)}`;
  return phone || "Номер не определён";
};

export function formatAdditionalInfoValue(field, value) {
  if (value === null || value === undefined || value === "") return "—";
  if (field.type.toLowerCase() !== "boolean") return String(value);
  return value === true || value === "true" ? "Да" : "Нет";
}

export function findIncident(classifier, code) {
  for (const category of classifier) {
    const incident = category.entries.find((entry) => entry.code === code);
    if (incident) return incident;
  }
}

export function cardIsComplete(card, classifier) {
  const incidentTypes = card.incidentTypes || [];
  const incidents = incidentTypes.map((code) => findIncident(classifier, code));
  const applicant = card.applicant || {};
  const hasValue = (value) => value !== null && value !== undefined && String(value).trim() !== "";
  return incidentTypes.length > 0
    && incidents.every(Boolean)
    && [applicant.firstName, applicant.lastName, applicant.phone].every(hasValue)
    && Number.isInteger(card.victimCount) && card.victimCount >= 0
    && hasValue(applicant.address)
    && incidents.every((incident) => (incident.fields || []).every((field) => !field.required || hasValue(card.additionalInfo?.[field.id])));
}
