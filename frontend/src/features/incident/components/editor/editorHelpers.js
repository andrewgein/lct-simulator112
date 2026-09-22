export const emptyPerson = () => ({ phone: "", contactPhone: "", onScenePhone: "", lastName: "", firstName: "", middleName: "", address: "", additionalInfo: "" });
export const normalizePerson = (person = {}) => ({ ...emptyPerson(), ...person });
export const applicantName = (card) => [card.applicant?.lastName, card.applicant?.firstName, card.applicant?.middleName].filter(Boolean).join(" ") || "Заявитель не указан";
export const cardAddress = (card) => card.applicant?.address || "Адрес не указан";

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
