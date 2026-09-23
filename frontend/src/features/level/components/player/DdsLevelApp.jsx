import { useCallback, useEffect, useMemo, useState } from "preact/hooks";
import CardEditor from "../../../incident/components/editor/CardEditor.jsx";
import { emptyPerson, normalizePerson } from "../../../incident/components/editor/editorHelpers.js";
import LevelCompletionNotice from "../common/LevelCompletionNotice.jsx";
import LevelSearchInput from "../common/LevelSearchInput.jsx";
import { useLevelClock } from "../../hooks/useLevelClock.js";
import ActiveCards from "./ActiveCards.jsx";
import LevelCommandBar from "./LevelCommandBar.jsx";

const styles = `
.dds-level-message { margin: var(--wa-space-l) var(--wa-space-l) 0; }
.dds-time--urgent { color: #ffb49f; }
.dds-progress { flex: 1; }
.dds-progress > p { margin: 0; }
.dds-progress ol { margin: var(--wa-space-xs) 0 0; padding: 0; list-style: none; }
.dds-progress li { display: grid; grid-template-columns: auto 1fr; gap: var(--wa-space-s); padding: var(--wa-space-xs) 0; }
.dds-progress li + li { border-block-start: var(--wa-border-width-s) solid #d4dadd; }
.dds-progress li > div { display: flex; flex-direction: column; }
.dds-progress li span { color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-xs); }
.dds-progress li p { margin: var(--wa-space-2xs) 0 0; }
.dds-progress-mark { width: .7rem; height: .7rem; margin-block-start: .35rem; border-radius: 50%; background: #89999f; }
.dds-progress-mark--active { background: #008dca; }
.dds-progress-mark--succeeded { background: #168448; }
.dds-progress-mark--failed, .dds-progress-mark--timed_out { background: #c94225; }
.dds-stage-actions { display: grid; grid-template-columns: minmax(11rem, .8fr) minmax(16rem, 1.2fr); gap: var(--wa-space-m); width: min(42rem, calc(100vw - 5rem)); align-items: end; }
.dds-stage-buttons { display: flex; grid-column: 1 / -1; justify-content: flex-end; gap: var(--wa-space-xs); }
.dds-stage-buttons wa-button::part(button) { min-width: 8rem; }
.dds-stage-cancel::part(button) { min-width: 3.5rem; }
@media (max-width: 40rem) { .dds-stage-actions { grid-template-columns: 1fr; width: min(24rem, calc(100vw - 5rem)); } .dds-stage-buttons { grid-column: 1; } }
@media (max-width: 48rem) { .dds-level-message { margin: var(--wa-space-s) var(--wa-space-s) 0; } }
`;

const SERVICES = [
  { value: "FIRE", short: "101", label: "Пожарная охрана" },
  { value: "POLICE", short: "102", label: "Полиция" },
  { value: "AMBULANCE", short: "103", label: "Скорая помощь" },
  { value: "GAS", short: "104", label: "Газовая служба" },
  { value: "ANTI_TERROR", short: "АТК", label: "Антитеррор" }
];

const STAGE_ACTIONS = {
  ASSIGN_BRIGADE: { label: "Принята", signal: "BRIGADE_ASSIGNED" },
  WAIT_FOR_BRIGADE_STATUS_CHANGE: { label: "Начало реагирования", signal: "BRIGADE_STATUS_CHANGED" },
  CALL_BRIGADE_FOR_STATUS: { label: "Статус бригады получен", signal: "STATUS_CALL_COMPLETED" },
  REQUEST_ADDITIONAL_SERVICE: { label: "Дополнительная служба оповещена", signal: "ADDITIONAL_SERVICE_REQUESTED" },
  COMPLETE_INCIDENT: { label: "Работы завершены", signal: "INCIDENT_COMPLETED" }
};

const INCIDENT_STATUSES = {
  PENDING: "Ожидает обработки",
  ACTIVE: "В работе",
  COMPLETED: "Завершено",
  FAILED: "Не выполнено"
};

const STAGE_STATUSES = {
  ACTIVE: "В работе",
  SUCCEEDED: "Выполнено",
  FAILED: "Не выполнено",
  TIMED_OUT: "Время истекло",
  SKIPPED: "Пропущено",
  PENDING: "Ожидает"
};

const READONLY_CALL = { phase: "idle", activeCallId: null, phone: "" };
const serviceInfo = (value) => SERVICES.find((service) => service.value === value) || { short: "ДДС", label: value || "Служба не указана" };
const addressText = (address = {}) => [address.city, address.street, address.house && `д. ${address.house}`, address.building && `корп. ${address.building}`, address.apartment && `кв. ${address.apartment}`].filter(Boolean).join(", ") || "Адрес не указан";
const dateTime = (value) => value ? new Date(value).toLocaleString("ru-RU", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit", second: "2-digit" }) : "—";

function incidentCard(incident) {
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

function editorFor(card) {
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

function activeStageFor(incident, progress) {
  const activeId = progress?.dds?.activeStageId;
  return incident?.stages?.find((stage) => String(stage.id) === String(activeId)) || null;
}

function notificationStatus(incident, progress) {
  const initial = progress?.dds?.stages?.find((stage) => String(stage.stageId) === String(incident?.initialStageId));
  if (initial?.type !== "ASSIGN_BRIGADE") return INCIDENT_STATUSES[progress?.status] || "Ожидает обработки";
  return { PENDING: "Ожидает направления", ACTIVE: "Ожидает подтверждения", SUCCEEDED: "Принята", FAILED: "Не принята", TIMED_OUT: "Не оповещено" }[initial.status] || INCIDENT_STATUSES[progress?.status] || "Ожидает обработки";
}

function RemainingTime({ deadline, now }) {
  if (!deadline) return <span>Без таймера</span>;
  const seconds = Math.max(0, Math.ceil((new Date(deadline).getTime() - now.getTime()) / 1000));
  return <span class={seconds < 30 ? "dds-time--urgent" : ""}>{String(Math.floor(seconds / 60)).padStart(2, "0")}:{String(seconds % 60).padStart(2, "0")}</span>;
}

function DdsProgressDetails({ incident, progress, comments }) {
  const activeStage = activeStageFor(incident, progress);
  const history = (progress?.dds?.stages || []).filter((stage) => stage.status !== "PENDING");
  return (
    <div class="saved-card-panel dds-progress wa-stack wa-gap-s">
      <span class="saved-label">Ход реагирования</span>
      <strong>{activeStage?.title || INCIDENT_STATUSES[progress?.status] || "Ожидает обработки"}</strong>
      {activeStage?.description && <p>{activeStage.description}</p>}
      {!!history.length && <ol>{history.map((item) => { const definition = incident.stages?.find((stage) => String(stage.id) === String(item.stageId)); return <li key={item.stageId}><span class={`dds-progress-mark dds-progress-mark--${item.status.toLowerCase()}`}></span><div><strong>{definition?.title || STAGE_ACTIONS[item.type]?.label || item.type}</strong><span>{STAGE_STATUSES[item.status] || item.status} · {dateTime(item.startedAt)}</span>{comments[item.stageId] && <p>{comments[item.stageId]}</p>}</div></li>; })}</ol>}
    </div>
  );
}

function DdsStageActions({ incident, progress, onApply }) {
  const activeStage = activeStageFor(incident, progress);
  const action = STAGE_ACTIONS[activeStage?.type];
  const [decision, setDecision] = useState("success");
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const active = progress?.status === "ACTIVE" && action;

  useEffect(() => {
    setDecision("success");
    setComment("");
  }, [activeStage?.id]);

  if (!active) return null;
  const submit = async (event) => {
    const popover = event.currentTarget.closest("wa-popover");
    setSubmitting(true);
    const saved = await onApply(incident.id, action.signal, decision === "success", comment);
    if (saved) {
      setComment("");
      popover?.hide();
    }
    setSubmitting(false);
  };
  return (
    <div class="dds-stage-actions">
      <wa-select label="Результат" size="s" value={decision} onChange={(event) => setDecision(event.currentTarget.value)}>
        <wa-option value="success">{activeStage.type === "ASSIGN_BRIGADE" ? "Принята" : action.label}</wa-option>
        <wa-option value="failure">{activeStage.type === "ASSIGN_BRIGADE" ? "Не принята" : "Отказ от выполнения"}</wa-option>
      </wa-select>
      <wa-input label="Комментарий" size="s" placeholder="Комментарий" value={comment} onInput={(event) => setComment(event.currentTarget.value)}></wa-input>
      <div class="dds-stage-buttons">
        <wa-button type="button" size="m" appearance="filled" variant="brand" loading={submitting} onClick={submit}><wa-icon slot="start" name="check"></wa-icon>Подтвердить</wa-button>
        <wa-button class="dds-stage-cancel" type="button" size="m" appearance="outlined" variant="neutral" disabled={submitting} data-popover="close" aria-label="Отменить изменение статуса"><wa-icon name="xmark" aria-hidden="true"></wa-icon></wa-button>
      </div>
    </div>
  );
}

export default function DdsLevelApp({ contextId, level, incidents, classifier, userService }) {
  const [progress, setProgress] = useState(null);
  const [incidentDefinitions, setIncidentDefinitions] = useState(incidents);
  const [editor, setEditor] = useState({ open: false });
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [finishing, setFinishing] = useState(false);
  const [comments, setComments] = useState({});
  const now = useLevelClock();

  const loadProgress = useCallback(async () => {
    try {
      const response = await fetch(`/api/v1/context/${encodeURIComponent(contextId)}/progress`);
      if (!response.ok) throw new Error(await response.text());
      setProgress(await response.json());
      setError("");
    } catch (requestError) {
      console.error("Failed to load DDS progress", requestError);
      setError("Не удалось обновить состояние происшествий");
    }
  }, [contextId]);

  useEffect(() => {
    loadProgress();
    const timer = window.setInterval(loadProgress, 3000);
    return () => window.clearInterval(timer);
  }, [loadProgress]);

  const progressIncidentIds = progress?.incidents?.map((item) => item.incidentId).join(",") || "";
  useEffect(() => {
    if (!progressIncidentIds) return;
    let active = true;
    Promise.all(progressIncidentIds.split(",").map(async (incidentId) => {
      const response = await fetch(`/api/v1/incidents/${encodeURIComponent(incidentId)}`);
      if (!response.ok) throw new Error(await response.text());
      return response.json();
    })).then((loaded) => {
      if (active) setIncidentDefinitions(loaded);
    }).catch((requestError) => {
      console.error("Failed to load DDS incidents", requestError);
      if (active) setError("Не удалось загрузить карточки происшествий");
    });
    return () => { active = false; };
  }, [progressIncidentIds]);

  const cards = useMemo(() => incidentDefinitions.map(incidentCard), [incidentDefinitions]);
  const selectedCard = cards.find((card) => card.cardId === editor.editingCardId);
  const selectedProgress = progress?.incidents?.find((item) => String(item.incidentId) === selectedCard?.cardId);
  const classifierState = { classifier, loading: false, error: null };
  const finished = !!progress?.incidents?.length && progress.incidents.every((item) => ["COMPLETED", "FAILED"].includes(item.status));
  const getCardMeta = useCallback((card) => {
    const item = progress?.incidents?.find((entry) => String(entry.incidentId) === card.cardId);
    return { complete: item?.status === "COMPLETED", status: notificationStatus(card.incident, item), kindLabel: "ДДС", className: item?.status === "COMPLETED" ? "is-complete" : "is-incomplete" };
  }, [progress]);

  const applyStage = async (incidentId, expectedSignal, success, comment) => {
    const signals = Object.values(STAGE_ACTIONS).map((item) => item.signal);
    const signal = success ? expectedSignal : signals.find((item) => item !== expectedSignal);
    try {
      const response = await fetch(`/api/v1/context/${encodeURIComponent(contextId)}/dds/incidents/${encodeURIComponent(incidentId)}/signals`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ signal }) });
      if (!response.ok) throw new Error(await response.text());
      const nextProgress = await response.json();
      const previous = progress?.incidents?.find((item) => String(item.incidentId) === String(incidentId));
      const completedStageId = previous?.dds?.activeStageId;
      if (comment.trim() && completedStageId) setComments((current) => ({ ...current, [completedStageId]: comment.trim() }));
      setProgress(nextProgress);
      setError("");
      return true;
    } catch (requestError) {
      console.error("Failed to apply DDS stage", requestError);
      setError("Не удалось сохранить статус реагирования");
      return false;
    }
  };

  const finishLevel = async () => {
    setFinishing(true);
    try {
      const response = await fetch(`/api/v1/context/${encodeURIComponent(contextId)}/close`, { method: "POST" });
      if (!response.ok) throw new Error(await response.text());
      window.location.href = `/review/${encodeURIComponent(contextId)}`;
    } catch (requestError) {
      console.error("Failed to finish DDS level", requestError);
      setError("Не удалось завершить уровень");
      setFinishing(false);
    }
  };

  const activeStage = activeStageFor(selectedCard?.incident, selectedProgress);
  const canEditStatus = selectedProgress?.status === "ACTIVE" && !!STAGE_ACTIONS[activeStage?.type];
  return (
    <div class="level-app wa-stack wa-gap-0">
      <style>{styles}</style>
      <LevelCommandBar call={READONLY_CALL} now={now} modeLabel={`Учебный режим · АРМ ДДС · ${serviceInfo(userService).label}`} idleLabel="Обработка карточек" idleIcon="tower-broadcast">
        <LevelSearchInput value={query} hint="Поиск по номеру, типу, заявителю и адресу" iconSlot="end" onInput={(event) => setQuery(event.currentTarget.value)} />
      </LevelCommandBar>
      {error && <wa-callout class="dds-level-message" variant="danger"><wa-icon slot="icon" name="triangle-exclamation"></wa-icon>{error}</wa-callout>}
      {finished && <LevelCompletionNotice className="dds-level-message" complete ready finishing={finishing} completeMessage="Все происшествия обработаны. Завершите уровень, чтобы перейти к разбору." onFinish={finishLevel} />}
      {selectedCard && <CardEditor contextId={contextId} cards={cards} call={READONLY_CALL} editor={editor} classifier={classifier} readOnly readonlyTitle="Карточка ДДС" readonlyHint="режим просмотра" readonlyStatus={notificationStatus(selectedCard.incident, selectedProgress)} readonlyTimer={<RemainingTime deadline={selectedProgress?.dds?.deadline} now={now} />} readonlyDetails={<DdsProgressDetails incident={selectedCard.incident} progress={selectedProgress} comments={comments} />} readonlyServiceStatus={notificationStatus(selectedCard.incident, selectedProgress)} readonlyServiceEditor={canEditStatus ? <DdsStageActions incident={selectedCard.incident} progress={selectedProgress} onApply={applyStage} /> : null} onChange={setEditor} onClose={() => {}} />}
      <ActiveCards cards={cards} loading={!progress} error={false} classifierState={classifierState} searchQuery={query} onOpen={(card) => setEditor(editorFor(card))} getCardMeta={getCardMeta} heading="Список происшествий" emptyMessage="Карточки ДДС пока не поступили" statusLabel="Статус" />
    </div>
  );
}
