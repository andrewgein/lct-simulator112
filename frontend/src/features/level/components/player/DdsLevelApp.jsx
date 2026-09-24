import { useCallback, useEffect, useMemo, useState } from "preact/hooks";
import CardEditor from "../../../incident/components/editor/CardEditor.jsx";
import LevelCompletionNotice from "../common/LevelCompletionNotice.jsx";
import LevelSearchInput from "../common/LevelSearchInput.jsx";
import { useLevelClock } from "../../hooks/useLevelClock.js";
import ActiveCards from "./ActiveCards.jsx";
import DdsProgressDetails from "./DdsProgressDetails.jsx";
import DdsStageActions from "./DdsStageActions.jsx";
import LevelCommandBar from "./LevelCommandBar.jsx";
import RemainingTime from "./RemainingTime.jsx";
import { editorFor, incidentCard, notificationStatus, reactionForService, READONLY_CALL, REACTION_STATUS_OPTIONS, serviceInfo, serviceStatusHistory } from "./ddsLevelHelpers.js";

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
.dds-stage-buttons { grid-column: 1 / -1; }
.dds-stage-buttons wa-button::part(button) { min-width: 8rem; }
.dds-stage-cancel::part(button) { min-width: 3.5rem; }
@media (max-width: 40rem) { .dds-stage-actions { grid-template-columns: 1fr; width: min(24rem, calc(100vw - 5rem)); } .dds-stage-buttons { grid-column: 1; } }
@media (max-width: 48rem) { .dds-level-message { margin: var(--wa-space-s) var(--wa-space-s) 0; } }
`;

export default function DdsLevelApp({ contextId, incidents, classifier, routingFacts, userService }) {
  const [progress, setProgress] = useState(null);
  const [incidentDefinitions, setIncidentDefinitions] = useState(incidents);
  const [editor, setEditor] = useState({ open: false });
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [finishing, setFinishing] = useState(false);
  const comments = {};
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
  const classifierState = { classifier, routingFacts, loading: false, error: null };
  const finished = !!progress?.incidents?.length && progress.incidents.every((item) => ["COMPLETED", "FAILED"].includes(item.status));
  const getCardMeta = useCallback((card) => {
    const item = progress?.incidents?.find((entry) => String(entry.incidentId) === card.cardId);
    return { complete: item?.status === "COMPLETED", status: notificationStatus(card.incident, item), kindLabel: "ДДС", className: item?.status === "COMPLETED" ? "is-complete" : "is-incomplete" };
  }, [progress]);

  const applyReactionStatus = async (incidentId, serviceCode, status, comment) => {
    try {
      const response = await fetch(`/api/v1/context/${encodeURIComponent(contextId)}/dds/incidents/${encodeURIComponent(incidentId)}/reaction-status`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ serviceCode, status, comment: comment.trim() || null }) });
      if (!response.ok) throw new Error(await response.text());
      setProgress(await response.json());
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

  const selectedServiceCode = selectedCard?.services?.[0];
  const reactionStatus = reactionForService(selectedProgress, selectedServiceCode)?.currentStatus;
  const canEditStatus = selectedProgress?.status === "ACTIVE" && !!REACTION_STATUS_OPTIONS[reactionStatus]?.length;
  return (
    <div class="level-app wa-stack wa-gap-0">
      <style>{styles}</style>
      <LevelCommandBar call={READONLY_CALL} now={now} modeLabel={`Учебный режим · АРМ ДДС · ${serviceInfo(userService).label}`} idleLabel="Обработка карточек" idleIcon="tower-broadcast">
        <LevelSearchInput value={query} hint="Поиск по номеру, типу, заявителю и адресу" iconSlot="end" onInput={(event) => setQuery(event.currentTarget.value)} />
      </LevelCommandBar>
      {error && <wa-callout class="dds-level-message" variant="danger"><wa-icon slot="icon" name="triangle-exclamation"></wa-icon>{error}</wa-callout>}
      {finished && <LevelCompletionNotice className="dds-level-message" complete ready finishing={finishing} completeMessage="Все происшествия обработаны. Завершите уровень, чтобы перейти к разбору." onFinish={finishLevel} />}
      {selectedCard && <CardEditor contextId={contextId} cards={cards} call={READONLY_CALL} editor={editor} classifier={classifier} routingFacts={routingFacts} readOnly readonlyTitle="Карточка ДДС" readonlyHint="режим просмотра" readonlyStatus={notificationStatus(selectedCard.incident, selectedProgress)} readonlyTimer={<RemainingTime deadline={selectedProgress?.dds?.deadline} now={now} />} readonlyDetails={<DdsProgressDetails incident={selectedCard.incident} progress={selectedProgress} comments={comments} />} readonlyServiceStatus={notificationStatus(selectedCard.incident, selectedProgress, selectedServiceCode)} readonlyServiceHistory={serviceStatusHistory(selectedCard.incident, selectedProgress, selectedServiceCode)} readonlyServiceEditor={canEditStatus ? <DdsStageActions incidentId={selectedCard.cardId} serviceCode={selectedServiceCode} currentStatus={reactionStatus} onApply={applyReactionStatus} /> : null} onChange={setEditor} onClose={() => {}} />}
      <ActiveCards cards={cards} loading={!progress} error={false} classifierState={classifierState} searchQuery={query} onOpen={(card) => setEditor(editorFor(card))} getCardMeta={getCardMeta} heading="Список происшествий" emptyMessage="Карточки ДДС пока не поступили" statusLabel="Статус" />
    </div>
  );
}
