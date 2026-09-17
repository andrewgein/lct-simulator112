export const serviceLabels = {
  "fire-service": "Пожарно-спасательная служба",
  "police-service": "Полиция",
  "ambulance-service": "Скорая медицинская помощь",
  "gas-service": "Аварийная газовая служба",
  "anti-terror-service": "Антитеррористическая служба"
};

export const serviceOrder = ["fire-service", "police-service", "ambulance-service", "gas-service", "anti-terror-service"];
export const emptyPerson = () => ({ phone: "", contactPhone: "", lastName: "", firstName: "", middleName: "", address: "", additionalInfo: "", incidentType: "" });
export const normalizePerson = (person = {}) => ({ ...emptyPerson(), ...person });
export const applicantName = (card) => [card.applicant?.lastName, card.applicant?.firstName, card.applicant?.middleName].filter(Boolean).join(" ") || "Заявитель не указан";
export const cardAddress = (card) => card.applicant?.address || card.victim?.address || "Адрес не указан";

export function findIncident(classifier, id) {
  for (const incidents of Object.values(classifier)) {
    for (const incident of Object.values(incidents)) {
      if (incident.id === id) return incident;
    }
  }
}

export function cardIsComplete(card, classifier) {
  const incident = findIncident(classifier, card.incidentType);
  const applicant = card.applicant || {};
  const victim = card.victim || {};
  const hasValue = (value) => value !== null && value !== undefined && String(value).trim() !== "";
  return !!incident
    && [applicant.firstName, applicant.lastName, applicant.phone].every(hasValue)
    && [victim.firstName, victim.lastName, victim.phone].every(hasValue)
    && hasValue(applicant.address || victim.address)
    && (incident.fields || []).every((field) => !field.required || hasValue(card.additionalInfo?.[field.id]));
}
