import { useEffect, useState } from "preact/hooks";
import EditorDialog from "../EditorDialog.jsx";
import { EditorAddCard, EditorCallCard, EditorCallRow, EditorStageContainer } from "../EditorContainers.jsx";
import DdsCallEditor, { ddsCallValue, normalizeDdsCall } from "./DdsCallEditor.jsx";
import { syncCallAddress } from "./ddsCallAddress.js";
import { availableCallContacts } from "./ddsCallContacts.js";

const STAGE_TYPES = [
  { value: "ASSIGN_BRIGADE", label: "Получение карточки" },
  { value: "WAIT_FOR_BRIGADE_STATUS_CHANGE", label: "Реагирование бригады" },
  { value: "CALL_BRIGADE_FOR_STATUS", label: "Контроль статуса" },
  { value: "REQUEST_ADDITIONAL_SERVICE", label: "Связь с другой службой" },
  { value: "COMPLETE_INCIDENT", label: "Завершение реагирования" }
];
const REACTION_STATUSES = [
  ["", "Без изменения"],
  ["ADDED", "Добавлена"], ["RECEIVED_BY_SERVICE", "Получена службой"],
  ["ACCEPTED", "Принята"], ["NOT_ACCEPTED", "Не принята"],
  ["RESPONSE_STARTED", "Начало реагирования"], ["ARRIVED", "Прибытие"],
  ["WORK_IN_PROGRESS", "Проведение работ"], ["WORK_COMPLETED", "Работы завершены"],
  ["WORK_REFUSED", "Отказ от выполнения работ"]
];
const COMPLETION_TRIGGERS = [["TIME", "Время"], ["STATUS", "Статус"], ["CALLS", "Конец всех звонков"]];
const REACTION_STATUS_VALUES = new Set(REACTION_STATUSES.map(([value]) => value).filter(Boolean));
const newStage = () => ({ id: crypto.randomUUID(), title: "", description: "", type: "WAIT_FOR_BRIGADE_STATUS_CHANGE", timeLimitSeconds: 60, completionTriggers: ["TIME"], failOnTimeout: false, actualStatus: "", calls: [], expectedComment: "" });
const initialStage = () => ({ ...newStage(), title: "Получение карточки", type: "ASSIGN_BRIGADE", timeLimitSeconds: 30, completionTriggers: ["STATUS"], actualStatus: "ACCEPTED" });

export function timelineValue(stages) {
  return {
    stages: stages.map((stage, position) => ({
      id: stage.id, position, title: stage.title.trim(), description: stage.description.trim() || null,
      type: stage.type, timeLimitSeconds: Number(stage.timeLimitSeconds),
      completionTriggers: stage.completionTriggers ?? ["TIME"],
      failOnTimeout: Boolean(stage.failOnTimeout) && (stage.completionTriggers ?? ["TIME"]).includes("TIME"),
      calls: stage.calls.map(ddsCallValue), expectedComment: stage.actualStatus ? stage.expectedComment?.trim() || null : null,
      actualStatus: stage.actualStatus || null
    }))
  };
}

export function timelineBoundaryErrors(stages) {
  if (!stages.length) return ["Добавьте этапы реагирования"];
  const errors = [];
  if (stages[0].type !== "ASSIGN_BRIGADE") errors.push("Первый этап должен быть «Получение карточки»");
  if (stages.at(-1).type !== "COMPLETE_INCIDENT") errors.push("Последний этап должен быть «Завершение реагирования»");
  return errors;
}

export function validateTimeline(stages, assignedService) {
  const [boundaryError] = timelineBoundaryErrors(stages);
  if (boundaryError) throw new Error(boundaryError);
  stages.forEach((stage, index) => {
    if (!stage.title.trim()) throw new Error(`Укажите название этапа ${index + 1}`);
    if (!Number.isInteger(Number(stage.timeLimitSeconds)) || Number(stage.timeLimitSeconds) <= 0) throw new Error(`Укажите положительную длительность этапа «${stage.title}»`);
    if (index && stage.type === "ASSIGN_BRIGADE") throw new Error("Получение карточки может быть только первым этапом");
    const triggers = stage.completionTriggers ?? ["TIME"];
    if (!triggers.length) throw new Error(`Выберите условие завершения этапа «${stage.title}»`);
    if (stage.failOnTimeout && !triggers.includes("TIME")) throw new Error(`Для завершения происшествия по таймауту включите «Время» на этапе «${stage.title}»`);
    if (stage.actualStatus && !REACTION_STATUS_VALUES.has(stage.actualStatus)) throw new Error(`Выберите статус реагирования на этапе «${stage.title}»`);
    if (triggers.includes("STATUS") && (index === 0 ? stage.actualStatus !== "ACCEPTED" : !REACTION_STATUS_VALUES.has(stage.actualStatus))) throw new Error(`Для завершения этапа «${stage.title}» по статусу укажите следующий статус реагирования`);
    if (triggers.includes("CALLS") && !stage.calls.length) throw new Error(`Для завершения этапа «${stage.title}» по звонкам добавьте звонок`);
    if (stage.expectedComment?.trim() && !stage.calls.length) throw new Error(`Для комментария на этапе «${stage.title}» добавьте звонок`);
    if (stage.calls.some((call) => call.counterparty === "SERVICE" && !call.serviceCode)) throw new Error(`Выберите службу для звонка на этапе «${stage.title}»`);
    if (stage.calls.some((call) => call.counterparty === "SERVICE" && call.serviceCode === assignedService)) throw new Error(`Для звонка на этапе «${stage.title}» выберите службу, отличную от службы ДДС`);
  });
}

function StageEditor({ stage, index, open, onOpen, onClose, onSave }) {
  const [draft, setDraft] = useState(stage);
  const [error, setError] = useState("");
  useEffect(() => { if (open) { setDraft(structuredClone(stage)); setError(""); } }, [open]);
  const update = (field, value) => { setError(""); setDraft((current) => ({ ...current, [field]: value })); };
  const save = () => {
    const triggers = draft.completionTriggers ?? ["TIME"];
    if (!triggers.length) {
      setError("Выберите хотя бы одно действие, завершающее этап");
      return;
    }
    if (draft.actualStatus && !REACTION_STATUS_VALUES.has(draft.actualStatus)) {
      setError("Выберите статус реагирования из списка");
      return;
    }
    if (triggers.includes("STATUS") && (index === 0 ? draft.actualStatus !== "ACCEPTED" : !REACTION_STATUS_VALUES.has(draft.actualStatus))) {
      setError("Для завершения этапа по статусу выберите следующий статус реагирования");
      return;
    }
    onSave({ ...draft, failOnTimeout: triggers.includes("TIME") && !!draft.failOnTimeout, expectedComment: draft.actualStatus ? draft.expectedComment : "" });
    onClose();
  };
  return <>
    <wa-button type="button" size="small" appearance="outlined" aria-label={`Изменить этап ${index + 1}`} onClick={onOpen}>
      <wa-icon name="pencil" label="Изменить"></wa-icon>
    </wa-button>
    <EditorDialog className="dds-stage-dialog" label={`Этап ${index + 1}`} open={open} onCancel={onClose} onSave={save}>
      <div class="wa-stack wa-gap-l">
        <div class="wa-grid">
          <wa-input value={draft.title} label="Название этапа" required onInput={(event) => update("title", event.currentTarget.value)}>
          </wa-input>
          <wa-select value={draft.type} label="Событие сценария" onChange={(event) => update("type", event.currentTarget.value)}>
            {STAGE_TYPES.filter((item) => !index || item.value !== "ASSIGN_BRIGADE").map((item) =>
              <wa-option key={item.value} value={item.value}>
                {item.label}
              </wa-option>)}
          </wa-select>
          <wa-select value={draft.actualStatus || ""} label="Фактический статус после этапа" onChange={(event) => { const actualStatus = event.currentTarget.value; setError(""); setDraft((current) => ({ ...current, actualStatus, completionTriggers: actualStatus ? current.completionTriggers : (current.completionTriggers ?? ["TIME"]).filter((item) => item !== "STATUS"), expectedComment: actualStatus ? current.expectedComment : "" })); }}>
            {REACTION_STATUSES.map(([value, label]) =>
              <wa-option key={value} value={value}>
                {label}
              </wa-option>)}
          </wa-select>
        </div>
        <fieldset class="dds-trigger-options wa-stack wa-gap-xs">
          <legend>Какое действие является завершением этапа (любое выбранное)</legend>
          {COMPLETION_TRIGGERS.map(([value, label]) => <wa-card key={value} class="dds-trigger-card" appearance="filled-outlined">
            <div class="wa-cluster wa-align-items-center wa-gap-m">
              <wa-checkbox checked={(draft.completionTriggers ?? ["TIME"]).includes(value) && (value !== "STATUS" || !!draft.actualStatus)} disabled={value === "STATUS" && !draft.actualStatus} onChange={(event) => { setError(""); setDraft((current) => ({ ...current, completionTriggers: event.currentTarget.checked ? [...(current.completionTriggers ?? ["TIME"]), value] : (current.completionTriggers ?? ["TIME"]).filter((item) => item !== value), failOnTimeout: value === "TIME" && !event.currentTarget.checked ? false : current.failOnTimeout })); }}>{label}</wa-checkbox>
              {value === "TIME" && (draft.completionTriggers ?? ["TIME"]).includes("TIME") && <><wa-input class="dds-trigger-duration" type="number" value={String(index === 0 ? 30 : draft.timeLimitSeconds ?? 60)} aria-label="Длительность этапа в секундах" min="1" step="1" required disabled={!index} onInput={(event) => update("timeLimitSeconds", event.currentTarget.value)}></wa-input><span>сек.</span></>}
            </div>
            {value === "TIME" && (draft.completionTriggers ?? ["TIME"]).includes("TIME") && <wa-checkbox class="dds-timeout-failure" checked={!!draft.failOnTimeout} onChange={(event) => update("failOnTimeout", event.currentTarget.checked)}>Если время вышло, то завершить происшествие с ошибкой</wa-checkbox>}
          </wa-card>)}
        </fieldset>
        {error && <wa-callout variant="danger" role="alert">{error}</wa-callout>}
        <wa-textarea value={draft.description} label="Описание события" rows="3" onInput={(event) => update("description", event.currentTarget.value)}>
        </wa-textarea>
        {!!draft.actualStatus && <wa-textarea value={draft.expectedComment || ""} label="Ожидаемый смысл комментария (для ревью)" rows="2" onInput={(event) => update("expectedComment", event.currentTarget.value)}></wa-textarea>}
      </div>
    </EditorDialog>
  </>;
}

export default function DdsStageTimeline({ initialIncident, onChange, services = [], incidentAddress = "" }) {
  const [stages, setStages] = useState(() => (initialIncident.stages || []).map((stage) => ({ ...stage, completionTriggers: stage.actualStatus ? stage.completionTriggers ?? ["TIME"] : (stage.completionTriggers ?? ["TIME"]).filter((item) => item !== "STATUS"), failOnTimeout: Boolean(stage.failOnTimeout) && (stage.completionTriggers ?? ["TIME"]).includes("TIME"), description: stage.description || "", expectedComment: stage.actualStatus ? stage.expectedComment || "" : "", calls: (stage.calls || []).map((call) => normalizeDdsCall(call)) })));
  const [editingStageId, setEditingStageId] = useState(null);
  const [editingCallKey, setEditingCallKey] = useState(null);
  useEffect(() => onChange(stages), [stages]);
  useEffect(() => {
    setStages((items) => items.map((stage) => ({
      ...stage,
      calls: stage.calls.map((call) => syncCallAddress(call, incidentAddress))
    })));
  }, [incidentAddress]);
  useEffect(() => {
    const form = document.querySelector("#incident-form");
    const applyGenerated = (event) => {
      if (!Array.isArray(event.detail)) return;
      setStages((previous) => event.detail.map((stage) => {
        const existing = previous.find((item) => item.id === stage.id);
        return {
          ...stage,
          id: stage.id || crypto.randomUUID(),
          description: stage.description || "",
          expectedComment: stage.actualStatus ? stage.expectedComment || "" : "",
          actualStatus: stage.actualStatus || "",
          completionTriggers: stage.actualStatus ? stage.completionTriggers ?? ["TIME"] : (stage.completionTriggers ?? ["TIME"]).filter((item) => item !== "STATUS"),
          failOnTimeout: Boolean(stage.failOnTimeout) && (stage.completionTriggers ?? ["TIME"]).includes("TIME"),
          calls: (stage.calls || []).map((call) => normalizeDdsCall(call,
            existing?.calls.find((item) => item.id && item.id === call.id)?.key || crypto.randomUUID()))
        };
      }));
      setEditingStageId(null);
      setEditingCallKey(null);
    };
    form?.addEventListener("apply-generated-dds-stages", applyGenerated);
    return () => form?.removeEventListener("apply-generated-dds-stages", applyGenerated);
  }, []);
  const change = (next) => setStages(next);
  const boundaryErrors = timelineBoundaryErrors(stages);
  return <section class="wa-stack wa-gap-m">
    <div>
      <h2 class="wa-heading-xl">
        Ход реагирования
      </h2>
      <p class="dds-section-hint">
Этап завершается по первому из выбранных условий: время, статус диспетчера или конец всех звонков. Отказ завершает текущее происшествие.
      </p>
    </div>
    {!!stages.length && !!boundaryErrors.length && <wa-callout variant="danger" role="alert">
      {boundaryErrors.map((message) => <div key={message}>{message}</div>)}
    </wa-callout>}
    <ol class="dds-timeline wa-stack wa-gap-l">
      {stages.map((stage, index) =>
        <li key={stage.id}>
          <EditorStageContainer className="dds-timeline-stage" topControl={<wa-button type="button" size="small" appearance="plain" aria-label="Переместить этап выше" disabled={index <= 1} onClick={() => { const next = [...stages]; [next[index - 1], next[index]] = [next[index], next[index - 1]]; change(next); }}><wa-icon name="chevron-up" label="Переместить выше"></wa-icon></wa-button>} bottomControl={<wa-button type="button" size="small" appearance="plain" aria-label="Переместить этап ниже" disabled={index === 0 || index === stages.length - 1} onClick={() => { const next = [...stages]; [next[index], next[index + 1]] = [next[index + 1], next[index]]; change(next); }}><wa-icon name="chevron-down" label="Переместить ниже"></wa-icon></wa-button>}>
          <div class="stage-top dds-stage-heading wa-cluster wa-justify-content-space-between wa-align-items-center wa-gap-s">
            <strong class="stage-number wa-heading-l">Этап {index + 1}</strong>
            <wa-input class="stage-title" value={stage.title} aria-label={`Название этапа ${index + 1}`} placeholder="Название этапа" onInput={(event) => change(stages.map((item) => item.id === stage.id ? { ...item, title: event.currentTarget.value } : item))}></wa-input>
            <div class="wa-cluster wa-gap-xs">
              <StageEditor stage={stage} index={index} open={editingStageId === stage.id} onOpen={() => setEditingStageId(stage.id)} onClose={() => setEditingStageId(null)} onSave={(value) => change(stages.map((item) => item.id === stage.id ? value : item))} />
              {index > 0 && <wa-button type="button" size="small" appearance="outlined" variant="danger" aria-label="Удалить этап" onClick={() => change(stages.filter((item) => item.id !== stage.id))}>
                <wa-icon name="trash" label="Удалить"></wa-icon>
              </wa-button>}
            </div>
          </div>
          <div class="dds-stage-meta wa-cluster wa-gap-s">
            <span>{STAGE_TYPES.find((item) => item.value === stage.type)?.label}</span>
            <span>{(stage.completionTriggers ?? ["TIME"]).map((value) => COMPLETION_TRIGGERS.find(([code]) => code === value)?.[1] || value).join(" / ")}{(stage.completionTriggers ?? ["TIME"]).includes("TIME") ? ` · ${stage.timeLimitSeconds} сек.` : ""}{stage.failOnTimeout && (stage.completionTriggers ?? ["TIME"]).includes("TIME") ? " · таймаут — ошибка" : ""}</span>
            <span>{REACTION_STATUSES.find(([value]) => value === (stage.actualStatus || ""))?.[1]}</span>
          </div>
          <EditorCallRow>
            {stage.calls.map((call, callIndex) => <EditorCallCard key={call.key} leftControl={<wa-button type="button" size="small" appearance="plain" aria-label={`Переместить звонок ${callIndex + 1} влево`} disabled={callIndex === 0} onClick={() => { const calls = [...stage.calls]; [calls[callIndex - 1], calls[callIndex]] = [calls[callIndex], calls[callIndex - 1]]; change(stages.map((item) => item.id === stage.id ? { ...item, calls } : item)); }}><wa-icon name="chevron-left" label="Переместить влево"></wa-icon></wa-button>} rightControl={<wa-button type="button" size="small" appearance="plain" aria-label={`Переместить звонок ${callIndex + 1} вправо`} disabled={callIndex === stage.calls.length - 1} onClick={() => { const calls = [...stage.calls]; [calls[callIndex], calls[callIndex + 1]] = [calls[callIndex + 1], calls[callIndex]]; change(stages.map((item) => item.id === stage.id ? { ...item, calls } : item)); }}><wa-icon name="chevron-right" label="Переместить вправо"></wa-icon></wa-button>}>
              <div class="wa-cluster wa-justify-content-space-between wa-align-items-center">
                <strong>Звонок {callIndex + 1}</strong>
                <div class="wa-cluster wa-gap-xs">
                  <wa-button type="button" size="small" appearance="outlined" aria-label={`Изменить звонок ${callIndex + 1}`} onClick={() => setEditingCallKey(call.key)}><wa-icon name="pencil" label="Изменить"></wa-icon></wa-button>
                  <wa-button type="button" size="small" appearance="outlined" variant="danger" aria-label={`Удалить звонок ${callIndex + 1}`} onClick={() => change(stages.map((item) => item.id === stage.id ? { ...item, calls: item.calls.filter((value) => value.key !== call.key) } : item))}><wa-icon name="trash" label="Удалить"></wa-icon></wa-button>
                </div>
              </div>
              <span class="dds-stage-meta">{call.direction === "INBOUND" ? "Входящий" : "Исходящий"} · {call.counterparty === "SERVICE" ? services.find((service) => service.code === call.serviceCode)?.name || "Другая служба" : "Бригада"}</span>
              {(call.person?.lastName || call.person?.firstName) && <span class="dds-stage-meta">{[call.person.lastName, call.person.firstName].filter(Boolean).join(" ")}</span>}
            </EditorCallCard>)}
            <EditorAddCard className="editor-add-call-card">
              <wa-button type="button" appearance="plain" variant="brand" onClick={() => { const call = normalizeDdsCall(); change(stages.map((item) => item.id === stage.id ? { ...item, calls: [...item.calls, call] } : item)); setEditingCallKey(call.key); }}>+ Добавить звонок</wa-button>
            </EditorAddCard>
          </EditorCallRow>
          </EditorStageContainer>
          {stage.calls.map((call, callIndex) => <DdsCallEditor key={call.key} call={call} index={callIndex} services={services} incidentAddress={incidentAddress} contacts={availableCallContacts(stages, call.key).map((contact) => ({ ...contact, label: `${contact.call.counterparty === "SERVICE" ? services.find((service) => service.code === contact.call.serviceCode)?.name || contact.call.serviceCode || "Другая служба" : "Бригада"} · ${[contact.call.person?.lastName, contact.call.person?.firstName, contact.call.person?.middleName].filter(Boolean).join(" ") || "без имени"} (этап ${contact.stageIndex + 1}, звонок ${contact.callIndex + 1})` }))} open={editingCallKey === call.key} onClose={() => setEditingCallKey(null)} onSave={(value) => change(stages.map((item) => item.id === stage.id ? { ...item, calls: item.calls.map((current) => current.key === call.key ? value : current) } : item))} />)}
        </li>)}
    </ol>
    <EditorAddCard className="editor-add-stage-card">
      <wa-button type="button" appearance="plain" variant="brand" onClick={() => { const stage = stages.length ? newStage() : initialStage(); change([...stages, stage]); setEditingStageId(stage.id); }}>
        <wa-icon name="plus" slot="start"></wa-icon>
        Добавить этап
      </wa-button>
    </EditorAddCard>
  </section>;
}
