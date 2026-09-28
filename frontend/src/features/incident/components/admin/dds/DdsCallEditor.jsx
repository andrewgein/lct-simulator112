import PersonFields from "../PersonFields.jsx";
import { emptyPerson, personValue, splitLines } from "../editorHelpers.js";

export function normalizeDdsCall(call = {}, key = crypto.randomUUID()) {
  return {
    key,
    id: call.id || null,
    person: emptyPerson(call.person),
    direction: call.direction || "OUTBOUND",
    counterparty: call.counterparty || "BRIGADE",
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
    direction: call.direction,
    counterparty: call.counterparty,
    person: personValue(call.person),
    gender: call.gender || null,
    knownFacts: splitLines(call.knownFacts),
    hiddenFacts: splitLines(call.hiddenFacts),
    aiContext: call.aiContext || null,
    emotionalState: call.emotionalState || null
  };
}

export default function DdsCallEditor({ call, index, onChange, onRemove }) {
  const change = (field) => (event) => onChange({ ...call, [field]: event.currentTarget.value });
  return (
    <wa-card class="dds-call" appearance="filled-outlined">
      <div class="wa-stack wa-gap-m">
        <div class="wa-split wa-align-items-center">
          <h4 class="wa-heading-m">Звонок {index + 1}</h4>
          <wa-button type="button" size="small" appearance="plain" variant="danger" aria-label={`Удалить звонок ${index + 1}`} onClick={onRemove}>
            <wa-icon name="trash" label="Удалить звонок"></wa-icon>
          </wa-button>
        </div>
        <div class="wa-grid">
          <wa-select value={call.counterparty} label="Собеседник" onChange={(event) => onChange({ ...call, counterparty: event.currentTarget.value, direction: event.currentTarget.value === "SERVICE" ? "OUTBOUND" : call.direction })}>
            <wa-option value="BRIGADE">Бригада</wa-option>
            <wa-option value="SERVICE">Другая служба</wa-option>
          </wa-select>
          {call.counterparty === "BRIGADE" && <wa-select value={call.direction} label="Направление" onChange={change("direction")}>
            <wa-option value="OUTBOUND">ДДС звонит бригаде</wa-option>
            <wa-option value="INBOUND">Бригада звонит в ДДС</wa-option>
          </wa-select>}
        </div>
        <PersonFields title={call.counterparty === "SERVICE" ? "Контакт другой службы" : "Контакт бригады"} person={call.person} gender={call.gender} showContactFields={false} onChange={(person) => onChange({ ...call, person })} />
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
