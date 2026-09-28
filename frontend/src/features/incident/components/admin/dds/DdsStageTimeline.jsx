import { useEffect, useState } from "preact/hooks";
import EditorDialog from "../EditorDialog.jsx";
import DdsCallEditor, { ddsCallValue, normalizeDdsCall } from "./DdsCallEditor.jsx";

const STAGE_TYPES = [
  { value: "ASSIGN_BRIGADE", label: "Получение карточки" },
  { value: "WAIT_FOR_BRIGADE_STATUS_CHANGE", label: "Реагирование бригады" },
  { value: "CALL_BRIGADE_FOR_STATUS", label: "Контроль статуса" },
  { value: "REQUEST_ADDITIONAL_SERVICE", label: "Связь с другой службой" },
  { value: "COMPLETE_INCIDENT", label: "Завершение реагирования" }
];
const REACTION_STATUSES = [
  ["", "Без изменения"], ["ACCEPTED", "Принята"], ["NOT_ACCEPTED", "Не принята"],
  ["RESPONSE_STARTED", "Начало реагирования"], ["ARRIVED", "Прибытие"],
  ["WORK_IN_PROGRESS", "Проведение работ"], ["WORK_COMPLETED", "Работы завершены"],
  ["WORK_REFUSED", "Отказ от выполнения работ"]
];
const newStage = () => ({ id: crypto.randomUUID(), title: "", description: "", type: "WAIT_FOR_BRIGADE_STATUS_CHANGE", timeLimitSeconds: 60, actualStatus: "", calls: [], expectedComment: "" });
const initialStage = () => ({ ...newStage(), title: "Получение карточки", type: "ASSIGN_BRIGADE", timeLimitSeconds: 30 });

export function timelineValue(stages) {
  return {
    stages: stages.map((stage, position) => ({
      id: stage.id, position, title: stage.title.trim(), description: stage.description.trim() || null,
      type: stage.type, timeLimitSeconds: Number(stage.timeLimitSeconds),
      calls: stage.calls.map(ddsCallValue), expectedComment: stage.expectedComment?.trim() || null,
      actualStatus: stage.actualStatus || null
    }))
  };
}

export function validateTimeline(stages) {
  stages.forEach((stage, index) => {
    if (!stage.title.trim()) throw new Error(`Укажите название этапа ${index + 1}`);
    if (!Number.isInteger(Number(stage.timeLimitSeconds)) || Number(stage.timeLimitSeconds) <= 0) throw new Error(`Укажите положительную длительность этапа «${stage.title}»`);
    if (index && stage.type === "ASSIGN_BRIGADE") throw new Error("Получение карточки может быть только первым этапом");
    if (stage.expectedComment?.trim() && !stage.calls.length) throw new Error(`Для комментария на этапе «${stage.title}» добавьте звонок`);
  });
}

function StageEditor({ stage, index, onSave }) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(stage);
  const edit = () => { setDraft(structuredClone(stage)); setOpen(true); };
  const update = (field, value) => setDraft((current) => ({ ...current, [field]: value }));
  return <>
    <wa-button type="button" size="small" appearance="outlined" onClick={edit}>
      Редактировать
    </wa-button>
    <EditorDialog className="dds-stage-dialog" label={`Этап ${index + 1}`} open={open} onCancel={() => setOpen(false)} onSave={() => { onSave(draft); setOpen(false); }}>
      <div class="wa-stack wa-gap-l">
        <div class="wa-grid">
          <wa-input value={draft.title} label="Название этапа" required onInput={(event) => update("title", event.currentTarget.value)}>
          </wa-input>
          <wa-select value={draft.type} label="Событие сценария" disabled={!index} onChange={(event) => update("type", event.currentTarget.value)}>
            {STAGE_TYPES.filter((item) => !index || item.value !== "ASSIGN_BRIGADE").map((item) =>
              <wa-option key={item.value} value={item.value}>
                {item.label}
              </wa-option>)}
          </wa-select>
          <wa-number-input value={draft.timeLimitSeconds} label="Длительность, секунд" min="1" step="1" required disabled={!index} onInput={(event) => update("timeLimitSeconds", event.currentTarget.value)}>
          </wa-number-input>
          <wa-select value={draft.actualStatus || ""} label="Фактический статус после этапа" onChange={(event) => update("actualStatus", event.currentTarget.value)}>
            {REACTION_STATUSES.map(([value, label]) =>
              <wa-option key={value} value={value}>
                {label}
              </wa-option>)}
          </wa-select>
        </div>
        <wa-textarea value={draft.description} label="Описание события" rows="3" onInput={(event) => update("description", event.currentTarget.value)}>
        </wa-textarea>
        <section class="wa-stack wa-gap-m">
          <div class="wa-split wa-align-items-center">
            <h3 class="wa-heading-l">
              Звонки этапа
            </h3>
            <wa-button type="button" appearance="outlined" variant="brand" onClick={() => update("calls", [...draft.calls, normalizeDdsCall()])}>
              <wa-icon name="plus" slot="start">
              </wa-icon>
              Добавить звонок
            </wa-button>
          </div>
          {draft.calls.map((call, callIndex) =>
            <DdsCallEditor key={call.key} call={call} index={callIndex} onChange={(value) => update("calls", draft.calls.map((item) => item.key === call.key ? value : item))} onRemove={() => update("calls", draft.calls.filter((item) => item.key !== call.key))} />)}
          <wa-textarea value={draft.expectedComment || ""} label="Ожидаемый смысл комментария (для ревью)" rows="2" onInput={(event) => update("expectedComment", event.currentTarget.value)}>
          </wa-textarea>
        </section>
      </div>
    </EditorDialog>
  </>;
}

export default function DdsStageTimeline({ initialIncident, onChange }) {
  const [stages, setStages] = useState(() => initialIncident.stages?.length ? initialIncident.stages.map((stage) => ({ ...stage, description: stage.description || "", expectedComment: stage.expectedComment || "", calls: (stage.calls || []).map((call) => normalizeDdsCall(call)) })) : [initialStage()]);
  useEffect(() => onChange(stages), [stages]);
  const change = (next) => setStages(next);
  return <section class="wa-stack wa-gap-m">
    <div>
      <h2 class="wa-heading-xl">
        Ход реагирования
      </h2>
      <p class="dds-section-hint">
        Этапы идут друг за другом по времени, независимо от действий диспетчера.
      </p>
    </div>
    <ol class="dds-timeline wa-stack wa-gap-s">
      {stages.map((stage, index) =>
        <li key={stage.id}>
          <wa-card appearance="filled-outlined">
            <div class="wa-split wa-align-items-center">
              <div class="wa-stack wa-gap-2xs">
                <strong>
                  {index + 1}. {stage.title || "Новый этап"}
                </strong>
                <span class="dds-stage-meta">
                  {STAGE_TYPES.find((item) => item.value === stage.type)?.label} · {stage.timeLimitSeconds} сек. · {REACTION_STATUSES.find(([value]) => value === (stage.actualStatus || ""))?.[1]} · звонков: {stage.calls.length}
                </span>
              </div>
              <div class="wa-cluster wa-gap-2xs">
                <StageEditor stage={stage} index={index} onSave={(value) => change(stages.map((item) => item.id === stage.id ? value : item))} />
                {index > 1 && <wa-button type="button" size="small" appearance="plain" aria-label="Вверх" disabled={index === 1} onClick={() => { const next = [...stages]; [next[index - 1], next[index]] = [next[index], next[index - 1]]; change(next); }}>
                  <wa-icon name="arrow-up">
                  </wa-icon>
                </wa-button>}
                {index > 0 && index < stages.length - 1 && <wa-button type="button" size="small" appearance="plain" aria-label="Вниз" onClick={() => { const next = [...stages]; [next[index], next[index + 1]] = [next[index + 1], next[index]]; change(next); }}>
                  <wa-icon name="arrow-down">
                  </wa-icon>
                </wa-button>}
                {index > 0 && <wa-button type="button" size="small" appearance="plain" variant="danger" aria-label="Удалить этап" onClick={() => change(stages.filter((item) => item.id !== stage.id))}>
                  <wa-icon name="trash">
                  </wa-icon>
                </wa-button>}
              </div>
            </div>
          </wa-card>
        </li>)}
    </ol>
    <wa-button type="button" appearance="outlined" variant="brand" onClick={() => change([...stages, newStage()])}>
      <wa-icon name="plus" slot="start">
      </wa-icon>
      Добавить этап
    </wa-button>
  </section>;
}
