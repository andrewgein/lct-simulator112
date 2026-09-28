import { useCallback, useEffect, useMemo, useRef, useState } from "preact/hooks";
import { dismissCall, initializeDialogSession, requestCall, startDialog, stopDialog } from "../../../dialog/api/DialogApi.js";
import CardEditor from "../../../incident/components/editor/CardEditor.jsx";
import LevelCompletionNotice from "../common/LevelCompletionNotice.jsx";
import LevelSearchInput from "../common/LevelSearchInput.jsx";
import { useLevelClock } from "../../hooks/useLevelClock.js";
import ActiveCards from "./ActiveCards.jsx";
import DdsProgressDetails from "./DdsProgressDetails.jsx";
import DdsStageActions from "./DdsStageActions.jsx";
import LevelCommandBar from "./LevelCommandBar.jsx";
import RemainingTime from "./RemainingTime.jsx";
import { activeStageFor, editorFor, incidentCard, notificationStatus, reactionForService, READONLY_CALL, REACTION_STATUS_OPTIONS, serviceStatusHistory } from "./ddsLevelHelpers.js";

const styles = `
.dds-level-message { margin: var(--wa-space-l) var(--wa-space-l) 0; }
.dds-time--urgent { color: var(--wa-color-danger-on-quiet); }
.dds-progress { flex: 1; }
.dds-progress > p { margin: 0; }
.dds-progress ol { margin: var(--wa-space-xs) 0 0; padding: 0; list-style: none; }
.dds-progress li { display: grid; grid-template-columns: auto 1fr; gap: var(--wa-space-s); padding: var(--wa-space-xs) 0; }
.dds-progress li + li { border-block-start: var(--wa-border-width-s) solid var(--wa-color-surface-border); }
.dds-progress li > div { display: flex; flex-direction: column; }
.dds-progress li span { color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-xs); }
.dds-progress li p { margin: var(--wa-space-2xs) 0 0; }
.dds-progress-mark { width: var(--wa-space-s); height: var(--wa-space-s); margin-block-start: var(--wa-space-2xs); border-radius: var(--wa-border-radius-circle); background: var(--wa-color-neutral-fill-loud); }
.dds-progress-mark--active { background: var(--wa-color-brand-fill-loud); }
.dds-progress-mark--succeeded { background: var(--wa-color-success-fill-loud); }
.dds-progress-mark--failed, .dds-progress-mark--timed_out { background: var(--wa-color-danger-fill-loud); }
.dds-stage-actions { display: grid; grid-template-columns: minmax(11rem, .8fr) minmax(16rem, 1.2fr); gap: var(--wa-space-m); width: min(42rem, calc(100vw - 5rem)); align-items: end; }
.dds-stage-buttons, .dds-stage-error { grid-column: 1 / -1; }
.dds-stage-error { color: var(--wa-color-danger-on-quiet); }
.dds-stage-buttons wa-button::part(button) { min-width: 8rem; }
.dds-stage-cancel::part(button) { min-width: 3.5rem; }
@media (max-width: 40rem) { .dds-stage-actions { grid-template-columns: 1fr; width: min(24rem, calc(100vw - 5rem)); } }
@media (max-width: 48rem) { .dds-level-message { margin: var(--wa-space-s) var(--wa-space-s) 0; } }
`;

export default function DdsLevelApp({ contextId, courseId, incidents, classifier, routingFacts, dialogEndpoint, userService }) {
  const [progress, setProgress] = useState(null);
  const [services, setServices] = useState([]);
  const [incidentDefinitions, setIncidentDefinitions] = useState(incidents);
  const [editor, setEditor] = useState({ open: false });
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [finishing, setFinishing] = useState(false);
  const [call, setCall] = useState(READONLY_CALL);
  const requestedCallId = useRef(null);
  const incomingCallId = useRef(null);
  const offeredCalls = useRef(new Set());
  const progressVersion = useRef(0);
  const now = useLevelClock();

  const loadProgress = useCallback(async () => {
    const version = progressVersion.current;
    try {
      const response = await fetch(`/api/v1/context/${encodeURIComponent(contextId)}/progress`, { cache: "no-store" });
      if (!response.ok) throw new Error(await response.text());
      const updated = await response.json();
      if (version !== progressVersion.current) return;
      setProgress(updated);
      setError("");
    } catch (requestError) {
      if (version !== progressVersion.current) return;
      console.error("Failed to load DDS progress", requestError);
      setError("Не удалось обновить состояние происшествий");
    }
  }, [contextId]);

  useEffect(() => {
    fetch("/api/v1/classifier/services").then((response) => response.ok ? response.json() : []).then(setServices).catch((requestError) => console.error("Failed to load dispatch services", requestError));
  }, []);

  useEffect(() => {
    loadProgress();
    const timer = window.setInterval(loadProgress, 3000);
    return () => window.clearInterval(timer);
  }, [loadProgress]);

  useEffect(() => {
    const ready = ({ detail }) => {
      if (detail?.callId !== requestedCallId.current) return;
      requestedCallId.current = null;
      if (detail.callId === incomingCallId.current) {
        setCall({ phase: "incoming", activeCallId: detail.callId, phone: detail.phoneNumber || "" });
      } else {
        setCall({ phase: "active", activeCallId: detail.callId, phone: detail.phoneNumber || "" });
        startDialog(dialogEndpoint, contextId);
      }
    };
    const restored = ({ detail }) => {
      if (!detail?.callId) return;
      setCall({ phase: "incoming", activeCallId: detail.callId, phone: detail.phoneNumber || "" });
    };
    const finished = () => {
      requestedCallId.current = null;
      incomingCallId.current = null;
      setCall(READONLY_CALL);
      loadProgress();
    };
    const failed = ({ detail }) => {
      requestedCallId.current = null;
      setCall(READONLY_CALL);
      setError(detail?.message || "Не удалось начать звонок");
    };
    const listeners = { "dialog:call_ready": ready, "dialog:session_restored": restored, "dialog:call_finished": finished, "dialog:error": failed };
    Object.entries(listeners).forEach(([name, listener]) => window.addEventListener(name, listener));
    initializeDialogSession(dialogEndpoint, contextId);
    return () => Object.entries(listeners).forEach(([name, listener]) => window.removeEventListener(name, listener));
  }, [contextId, dialogEndpoint, loadProgress]);

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
  useEffect(() => {
    if (call.phase !== "idle" || requestedCallId.current) return;
    const target = progress?.incidents?.filter((item) => item.status === "ACTIVE").map((item) => {
      const card = cards.find((entry) => entry.cardId === String(item.incidentId));
      const stage = activeStageFor(card?.incident, item);
      const incoming = stage?.calls?.find((entry) => entry.direction === "INBOUND" && !offeredCalls.current.has(entry.id));
      return incoming ? { card, stage, incoming } : null;
    }).find(Boolean);
    if (!target || !requestCall(target.incoming.id)) return;
    offeredCalls.current.add(target.incoming.id);
    requestedCallId.current = target.incoming.id;
    incomingCallId.current = target.incoming.id;
    setEditor(editorFor(target.card));
    setCall({ phase: "dialing", activeCallId: target.incoming.id, phone: "" });
  }, [progress, cards, call.phase]);
  useEffect(() => {
    if (call.phase !== "incoming" || !progress || !cards.length) return;
    const active = progress.incidents.some((item) => {
      if (item.status !== "ACTIVE") return false;
      const card = cards.find((entry) => entry.cardId === String(item.incidentId));
      const stage = activeStageFor(card?.incident, item);
      return stage?.calls?.some((entry) => entry.id === call.activeCallId);
    });
    if (!active) dismissCall();
  }, [progress, cards, call.phase, call.activeCallId]);
  const selectedCard = cards.find((card) => card.cardId === editor.editingCardId);
  const selectedProgress = progress?.incidents?.find((item) => String(item.incidentId) === selectedCard?.cardId);
  const classifierState = { classifier, routingFacts, loading: false, error: null };
  const finished = call.phase === "idle" && !!progress?.incidents?.length && progress.incidents.every((item) => ["COMPLETED", "FAILED"].includes(item.status));
  const getCardMeta = useCallback((card) => {
    const item = progress?.incidents?.find((entry) => String(entry.incidentId) === card.cardId);
    const receivedAt = item?.serviceReactions?.flatMap((reaction) => reaction.history || []).find((event) => event.status === "RECEIVED_BY_SERVICE")?.changedAt;
    return { complete: item?.status === "COMPLETED", status: notificationStatus(card.incident, item), receivedAt, kindLabel: "ДДС", className: item?.status === "COMPLETED" ? "is-complete" : "is-incomplete" };
  }, [progress]);

  const applyReactionStatus = async (incidentId, serviceCode, status, comment) => {
    try {
      const response = await fetch(`/api/v1/context/${encodeURIComponent(contextId)}/dds/incidents/${encodeURIComponent(incidentId)}/reaction-status`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ serviceCode, status, comment: comment.trim() || null }) });
      if (!response.ok) throw new Error(await response.text());
      const updated = await response.json();
      progressVersion.current++;
      setProgress(updated);
      setError("");
      return true;
    } catch (requestError) {
      console.error("Failed to apply DDS reaction status", requestError);
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

  const selectedServiceCode = selectedCard?.incident?.initialAssignment?.emergencyService || selectedCard?.services?.[0];
  const reactionStatus = reactionForService(selectedProgress, selectedServiceCode)?.currentStatus;
  const canEditStatus = ["ACTIVE", "COMPLETED"].includes(selectedProgress?.status) && !["IN_REVIEW", "DONE"].includes(progress?.status) && !!REACTION_STATUS_OPTIONS[reactionStatus]?.length;
  const activeStage = activeStageFor(selectedCard?.incident, selectedProgress);
  const serviceCalls = selectedProgress?.status === "ACTIVE" ? (activeStage?.calls || []).filter((item) => item.direction === "OUTBOUND") : [];
  const callService = (callId) => {
    if (requestedCallId.current || call.phase !== "idle") return;
    requestedCallId.current = callId;
    if (!requestCall(callId)) {
      requestedCallId.current = null;
      setError("Нет соединения с сервисом диалога. Обновите страницу и попробуйте снова.");
    } else {
      setCall({ phase: "dialing", activeCallId: callId, phone: "" });
      setError("");
    }
  };
  return (
    <div class="level-app wa-stack wa-gap-0">
      <style>{styles}</style>
      <LevelCommandBar call={call} now={now} onAccept={() => { setCall((current) => ({ ...current, phase: "active" })); startDialog(dialogEndpoint, contextId); }} onDrop={() => call.phase === "incoming" ? dismissCall() : stopDialog()} exitHref={`/courses/${courseId}`} modeLabel={`Учебный режим · АРМ ДДС · ${services.find((service) => service.code === userService)?.name || userService || "Служба не указана"}`} showIdleStatus={false}>
        <LevelSearchInput value={query} hint="Поиск по номеру, типу, заявителю и адресу" iconSlot="end" onInput={(event) => setQuery(event.currentTarget.value)} />
      </LevelCommandBar>
      {error && <wa-callout class="dds-level-message" variant="danger"><wa-icon slot="icon" name="triangle-exclamation"></wa-icon>{error}</wa-callout>}
      {finished && <LevelCompletionNotice className="dds-level-message" complete ready finishing={finishing} completeMessage="Все происшествия обработаны. Завершите уровень, чтобы перейти к разбору." onFinish={finishLevel} />}
      {selectedCard && <CardEditor contextId={contextId} cards={cards} call={call} editor={editor} classifier={classifier} routingFacts={routingFacts} dispatchServices={services} readOnly readonlyTitle="Карточка ДДС" readonlyHint="режим просмотра" readonlyStatus={notificationStatus(selectedCard.incident, selectedProgress)} readonlyTimer={<RemainingTime deadline={selectedProgress?.dds?.deadline} now={now} />} readonlyDetails={<DdsProgressDetails incident={selectedCard.incident} progress={selectedProgress} />} readonlyServiceStatus={notificationStatus(selectedCard.incident, selectedProgress, selectedServiceCode)} readonlyServiceHistory={serviceStatusHistory(selectedCard.incident, selectedProgress, selectedServiceCode)} readonlyServiceEditor={canEditStatus ? <DdsStageActions incidentId={selectedCard.cardId} serviceCode={selectedServiceCode} currentStatus={reactionStatus} onApply={applyReactionStatus} /> : null} readonlyServiceCalls={serviceCalls} readonlyCallEnabled={call.phase === "idle"} onServiceCall={callService} onAcceptCall={() => { setCall((current) => ({ ...current, phase: "active" })); startDialog(dialogEndpoint, contextId); }} onDropCall={() => call.phase === "incoming" ? dismissCall() : stopDialog()} onChange={setEditor} onClose={() => {}} />}
      <ActiveCards cards={cards} loading={!progress} error={false} classifierState={classifierState} searchQuery={query} onOpen={(card) => setEditor(editorFor(card))} getCardMeta={getCardMeta} heading="Список происшествий" emptyMessage="Карточки ДДС пока не поступили" statusLabel="Статус" />
    </div>
  );
}
