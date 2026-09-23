import { emptyPerson, normalizePerson } from "../../../incident/components/editor/editorHelpers.js";

export const SERVICES = [
  { value: "FIRE", short: "101", label: "Пожарная охрана" },
  { value: "POLICE", short: "102", label: "Полиция" },
  { value: "AMBULANCE", short: "103", label: "Скорая помощь" },
  { value: "GAS", short: "104", label: "Газовая служба" },
  { value: "ANTI_TERROR", short: "АТК", label: "Антитеррор" }
];

export const STAGE_ACTIONS = {
  ASSIGN_BRIGADE: { label: "Принята", signal: "BRIGADE_ASSIGNED" },
  WAIT_FOR_BRIGADE_STATUS_CHANGE: { label: "Начало реагирования", signal: "BRIGADE_STATUS_CHANGED" },
  CALL_BRIGADE_FOR_STATUS: { label: "Статус бригады получен", signal: "STATUS_CALL_COMPLETED" },
  REQUEST_ADDITIONAL_SERVICE: { label: "Дополнительная служба оповещена", signal: "ADDITIONAL_SERVICE_REQUESTED" },
  COMPLETE_INCIDENT: { label: "Работы завершены", signal: "INCIDENT_COMPLETED" }
};

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

export const STAGE_STATUSES = {
  ACTIVE: "В работе",
  SUCCEEDED: "Выполнено",
  FAILED: "Не выполнено",
  TIMED_OUT: "Время истекло",
  SKIPPED: "Пропущено",
  PENDING: "Ожидает"
};

export const READONLY_CALL = { phase: "idle", activeCallId: null, phone: "" };
export const serviceInfo = (value) => SERVICES.find((service) => service.value === value) || { short: "ДДС", label: value || "Служба не указана" };
export const addressText = (address = {}) => [address.city, address.street, address.house && `д. ${address.house}`, address.building && `корп. ${address.building}`, address.apartment && `кв. ${address.apartment}`].filter(Boolean).join(", ") || "Адрес не указан";
export const dateTime = (value) => value ? new Date(value).toLocaleString("ru-RU", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit", second: "2-digit" }) : "—";
export const timeOnly = (value) => value ? new Date(value).toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit", second: "2-digit" }) : "—";

export function reactionForService(progress, serviceCode) {
  return progress?.serviceReactions?.find((item) => item.serviceCode === serviceCode) || null;
}

export function serviceStatusHistory(incident, progress, serviceCode) {
  const history = reactionForService(progress, serviceCode)?.history || [];
  if (history.length) return history.map((item) => ({ label: REACTION_STATUS_LABELS[item.status] || item.status, time: timeOnly(item.changedAt), dateTime: item.changedAt, comment: item.comment }));
  const stages = (progress?.dds?.stages || []).filter((stage) => stage.status !== "PENDING").sort((left, right) => new Date(left.startedAt || 0) - new Date(right.startedAt || 0));
  return stages.map((stage) => ({ label: stage.type === "ASSIGN_BRIGADE" ? "Получена службой" : STAGE_ACTIONS[stage.type]?.label || stage.type, time: timeOnly(stage.startedAt), dateTime: stage.startedAt }));
}

export function incidentCard(incident) {
  const template = incident.preparedCardTemplate || {};
  const applicant = { ...normalizePerson(template.applicant || emptyPerson()), address: addressText(incident.address) };
  applicant.additionalInfo ||= incident.initialAssignment?.instructions || incident.title;
  return {
    cardId: String(incident.id),
    mainCardId: null,
    applicant,
    victimCount: template.victimCount ?? 0,
    incidentTypes: template.classifierCodes || [],
    additionalInfo: template.additionalInfo || {},
    services: incident.initialAssignment?.emergencyService ? [incident.initialAssignment.emergencyService] : [],
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
  const initial = progress?.dds?.stages?.find((stage) => String(stage.stageId) === String(incident?.initialStageId));
  if (initial?.type !== "ASSIGN_BRIGADE") return INCIDENT_STATUSES[progress?.status] || "Ожидает обработки";
  return { PENDING: "Ожидает направления", ACTIVE: "Ожидает подтверждения", SUCCEEDED: "Принята", FAILED: "Не принята", TIMED_OUT: "Не оповещено" }[initial.status] || INCIDENT_STATUSES[progress?.status] || "Ожидает обработки";
}
