import { faker } from "@faker-js/faker/locale/ru";

const personFields = ["lastName", "firstName", "middleName", "age", "phone", "contactPhone", "address", "additionalInfo"];
export const splitLines = (value) => value.split("\n").map((item) => item.trim()).filter(Boolean);
const clientId = () => crypto.randomUUID();
const randomPhone = () => `+79${String(Math.floor(Math.random() * 1_000_000_000)).padStart(9, "0")}`;

export function generatePerson(gender) {
  const resolvedGender = ["MAN", "WOMEN"].includes(gender) ? gender : faker.helpers.arrayElement(["MAN", "WOMEN"]);
  const sex = resolvedGender === "WOMEN" ? "female" : "male";
  return {
    lastName: faker.person.lastName(sex),
    firstName: faker.person.firstName(sex),
    middleName: faker.person.middleName(sex),
    age: faker.number.int({ min: 18, max: 85 }),
    phone: randomPhone(),
    contactPhone: randomPhone()
  };
}

export function emptyPerson(person = {}) {
  return Object.fromEntries(personFields.map((field) => [field, person?.[field] ?? ""]));
}

export function personValue(person) {
  const values = Object.fromEntries(personFields.map((field) => [field, person[field] === "" ? null : field === "age" ? Number(person[field]) : person[field]]));
  return Object.values(values).some((value) => value !== null) ? values : null;
}

export function personIsIncomplete(person) {
  const value = personValue(person);
  return value && (!value.firstName || !value.lastName || !value.phone);
}

export function normalizeDialup(dialup = {}, key = clientId()) {
  const details = dialup.dialupDetails || {};
  return {
    key,
    id: dialup.id || "",
    applicant: emptyPerson(dialup.applicant),
    gender: details.gender || "",
    emotionalState: details.emotionalState || "",
    knownFacts: (details.knownFacts || []).join("\n"),
    hiddenFacts: (details.hiddenFacts || []).join("\n"),
    aiContext: details.aiContext || ""
  };
}

export function normalizeStage(stage = {}, key = clientId()) {
  const additionalInfo = Array.isArray(stage.additionalInfo)
    ? Object.fromEntries(stage.additionalInfo.map((item) => [item.additionalInfoId, item.fieldValue]))
    : stage.additionalInfo || {};
  return {
    key,
    id: stage.id || "",
    title: stage.title || "",
    typeId: stage.type?.id || stage.typeId || "",
    description: stage.description || "",
    victim: emptyPerson(stage.victim),
    additionalInfo,
    dialups: (stage.dialups || []).map((dialup, index) => normalizeDialup(dialup, dialup.id || `${key}-dialup-${index}`))
  };
}

export function moveItem(items, index, direction) {
  const nextIndex = index + direction;
  if (nextIndex < 0 || nextIndex >= items.length) return items;
  const result = [...items];
  [result[index], result[nextIndex]] = [result[nextIndex], result[index]];
  return result;
}

export function findIncident(classifier, id) {
  for (const incidents of Object.values(classifier)) {
    for (const incident of Object.values(incidents)) {
      if (incident.id === id) return incident;
    }
  }
}

export async function request(path, method, body) {
  const response = await fetch(path, { method, headers: body ? { "Content-Type": "application/json" } : undefined, body: body ? JSON.stringify(body) : undefined });
  if (response.ok) return response.status === 204 ? null : response.json();
  const error = await response.json().catch(() => ({}));
  throw new Error(error.message || error.detail || "Не удалось сохранить этапы и звонки");
}
