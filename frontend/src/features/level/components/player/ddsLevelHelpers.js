import { emptyPerson, normalizePerson } from "../../../incident/components/editor/editorHelpers.js";

export const REACTION_STATUS_LABELS = {
  ADDED: "Добавлена",
  RECEIVED_BY_SERVICE: "Получена службой",
  ACCEPTED: "Принята",
  NOT_ACCEPTED: "Не принята",
  RESPONSE_STARTED: "Начало реагирования",
  ARRIVED: "Прибытие",
  WORK_IN_PROGRESS: "Проведение работ",
  WORK_COMPLETED: "Работы завершены",
  WORK_REFUSED: "Отказ от выполнения работ"
};

export const REACTION_STATUS_OPTIONS = {
  RECEIVED_BY_SERVICE: ["ACCEPTED", "NOT_ACCEPTED"],
  NOT_ACCEPTED: ["ACCEPTED"],
  ACCEPTED: ["RESPONSE_STARTED", "WORK_REFUSED"],
  RESPONSE_STARTED: ["ARRIVED", "WORK_REFUSED"],
  ARRIVED: ["WORK_IN_PROGRESS", "WORK_REFUSED"],
  WORK_IN_PROGRESS: ["WORK_COMPLETED", "WORK_REFUSED"]
};

export const INCIDENT_STATUSES = {
  PENDING: "Ожидает обработки",
  ACTIVE: "В работе",
  COMPLETED: "Завершено",
  FAILED: "Не выполнено"
};

export const READONLY_CALL = { phase: "idle", activeCallId: null, phone: "" };
export const addressText = (address = {}) => [address.city, address.street, address.house && `д. ${address.house}`, address.building && `корп. ${address.building}`, address.apartment && `кв. ${address.apartment}`].filter(Boolean).join(", ") || "Адрес не указан";
export const dateTime = (value) => value ? new Date(value).toLocaleString("ru-RU", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit", second: "2-digit" }) : "—";
export const timeOnly = (value) => value ? new Date(value).toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit", second: "2-digit" }) : "—";

export function reactionForService(progress, serviceCode) {
  return progress?.serviceReactions?.find((item) => item.serviceCode === serviceCode) || null;
}

export function serviceStatusHistory(incident, progress, serviceCode) {
  const history = reactionForService(progress, serviceCode)?.history || [];
  return history.map((item) => ({ label: REACTION_STATUS_LABELS[item.status] || item.status, time: timeOnly(item.changedAt), dateTime: item.changedAt, comment: item.comment }));
}

export function incidentCard(incident) {
  const template = incident.preparedCardTemplate || {};
  const applicant = { ...normalizePerson(template.applicant || emptyPerson()), address: addressText(incident.address) };
  applicant.additionalInfo ||= incident.title;
  return {
    cardId: String(incident.id),
    mainCardId: null,
    applicant,
    victimCount: template.victimCount ?? 0,
    incidentTypes: template.classifierCodes || [],
    additionalInfo: template.additionalInfo || {},
    services: template.assignedServices ?? (incident.initialAssignment?.emergencyService ? [incident.initialAssignment.emergencyService] : []),
    incident
  };
}

export function editorFor(card) {
  return {
    open: true,
    operation: "SAVE",
    editingCardId: card.cardId,
    selectedCardId: card.cardId,
    applicant: card.applicant,
    victimCount: card.victimCount,
    incidentTypes: card.incidentTypes,
    additionalInfo: card.additionalInfo,
    services: card.services,
    cardSaved: true,
    saving: false
  };
}

export function activeStageFor(incident, progress) {
  const activeId = progress?.dds?.activeStageId;
  return incident?.stages?.find((stage) => String(stage.id) === String(activeId)) || null;
}

export function notificationStatus(incident, progress, serviceCode = incident?.initialAssignment?.emergencyService) {
  const reactionStatus = reactionForService(progress, serviceCode)?.currentStatus;
  if (reactionStatus) return REACTION_STATUS_LABELS[reactionStatus] || reactionStatus;
  return INCIDENT_STATUSES[progress?.status] || "Ожидает обработки";
}
