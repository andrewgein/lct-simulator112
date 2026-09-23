import { activeStageFor, dateTime, INCIDENT_STATUSES, STAGE_ACTIONS, STAGE_STATUSES } from "./ddsLevelHelpers.js";

export default function DdsProgressDetails({ incident, progress, comments }) {
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
