import PersonFields from "../PersonFields.jsx";
import { emptyPerson, personValue, splitLines } from "../editorHelpers.js";

export function normalizeDdsCall(call = {}, key = crypto.randomUUID()) {
  return {
    key,
    id: call.id || null,
    person: emptyPerson(call.person),
    gender: call.gender || "",
    knownFacts: (call.knownFacts || []).join("\n"),
    hiddenFacts: (call.hiddenFacts || []).join("\n"),
    aiContext: call.aiContext || "",
    emotionalState: call.emotionalState || ""
  };
}

export function ddsCallValue(call, position) {
  return {
    id: call.id || null,
    position,
    direction: "OUTBOUND",
    counterparty: "BRIGADE",
    person: personValue(call.person),
    gender: call.gender || null,
    knownFacts: splitLines(call.knownFacts),
    hiddenFacts: splitLines(call.hiddenFacts),
    aiContext: call.aiContext || null,
    emotionalState: call.emotionalState || null
  };
}

export default function DdsCallEditor({ call, index, incidentAddress, onChange, onRemove }) {
  const change = (field) => (event) => onChange({ ...call, [field]: event.currentTarget.value });
  return (
    <wa-card class="dds-call" appearance="filled-outlined">
      <div class="wa-stack wa-gap-m">
        <div class="wa-split wa-align-items-center">
          <h4 class="wa-heading-m">Исходящий звонок бригаде {index + 1}</h4>
          <wa-button type="button" size="small" appearance="plain" variant="danger" aria-label={`Удалить звонок ${index + 1}`} onClick={onRemove}>
            <wa-icon name="trash" label="Удалить звонок"></wa-icon>
          </wa-button>
        </div>
        <PersonFields title="Контакт бригады" person={call.person} gender={call.gender} incidentAddress={incidentAddress} onChange={(person) => onChange({ ...call, person })} />
        <div class="wa-grid">
          <wa-select value={call.gender} label="Пол собеседника" onChange={change("gender")}>
            <wa-option value="">Не указан</wa-option>
            <wa-option value="MAN">Мужчина</wa-option>
            <wa-option value="WOMEN">Женщина</wa-option>
          </wa-select>
          <wa-select value={call.emotionalState} label="Эмоциональное состояние" onChange={change("emotionalState")}>
            <wa-option value="">Не указано</wa-option>
            <wa-option value="CALM">Спокойное</wa-option>
            <wa-option value="WORRIED">Встревоженное</wa-option>
            <wa-option value="PANICKED">Паническое</wa-option>
            <wa-option value="AGGRESSIVE">Агрессивное</wa-option>
            <wa-option value="CONFUSED">Растерянное</wa-option>
          </wa-select>
        </div>
        <wa-textarea value={call.knownFacts} label="Известные факты (один на строку)" rows="4" onInput={change("knownFacts")}></wa-textarea>
        <wa-textarea value={call.hiddenFacts} label="Скрытые факты (один на строку)" rows="3" onInput={change("hiddenFacts")}></wa-textarea>
        <wa-textarea value={call.aiContext} label="Контекст для ИИ" rows="4" onInput={change("aiContext")}></wa-textarea>
      </div>
    </wa-card>
  );
}
