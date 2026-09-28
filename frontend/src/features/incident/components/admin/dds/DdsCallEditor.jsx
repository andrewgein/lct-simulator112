import { useEffect, useState } from "preact/hooks";
import EditorDialog from "../EditorDialog.jsx";
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

export default function DdsCallEditor({ call, index, open, onClose, onSave }) {
  const [draft, setDraft] = useState(call);
  useEffect(() => { if (open) setDraft(structuredClone(call)); }, [open]);
  const update = (field) => (event) => setDraft((current) => ({ ...current, [field]: event.currentTarget.value }));
  return (
    <EditorDialog className="dds-call-dialog" label={`Редактирование звонка ${index + 1}`} open={open} onCancel={onClose} onSave={() => { onSave(draft); onClose(); }}>
      <div class="wa-stack wa-gap-m">
        <div class="wa-grid">
          <wa-select value={draft.counterparty} label="Собеседник" onChange={(event) => setDraft((current) => ({ ...current, counterparty: event.currentTarget.value, direction: event.currentTarget.value === "SERVICE" ? "OUTBOUND" : current.direction }))}>
            <wa-option value="BRIGADE">Бригада</wa-option>
            <wa-option value="SERVICE">Другая служба</wa-option>
          </wa-select>
          {draft.counterparty === "BRIGADE" && <wa-select value={draft.direction} label="Направление" onChange={update("direction")}>
            <wa-option value="OUTBOUND">ДДС звонит бригаде</wa-option>
            <wa-option value="INBOUND">Бригада звонит в ДДС</wa-option>
          </wa-select>}
        </div>
        <PersonFields title={draft.counterparty === "SERVICE" ? "Контакт другой службы" : "Контакт бригады"} person={draft.person} gender={draft.gender} showContactFields={false} onChange={(person) => setDraft((current) => ({ ...current, person }))} />
        <div class="wa-grid">
          <wa-select value={draft.gender} label="Пол собеседника" onChange={update("gender")}>
            <wa-option value="">Не указан</wa-option>
            <wa-option value="MAN">Мужчина</wa-option>
            <wa-option value="WOMEN">Женщина</wa-option>
          </wa-select>
          <wa-select value={draft.emotionalState} label="Эмоциональное состояние" onChange={update("emotionalState")}>
            <wa-option value="">Не указано</wa-option>
            <wa-option value="CALM">Спокойное</wa-option>
            <wa-option value="WORRIED">Встревоженное</wa-option>
            <wa-option value="PANICKED">Паническое</wa-option>
            <wa-option value="AGGRESSIVE">Агрессивное</wa-option>
            <wa-option value="CONFUSED">Растерянное</wa-option>
          </wa-select>
        </div>
        <wa-textarea value={draft.knownFacts} label="Известные факты (один на строку)" rows="4" onInput={update("knownFacts")}></wa-textarea>
        <wa-textarea value={draft.hiddenFacts} label="Скрытые факты (один на строку)" rows="3" onInput={update("hiddenFacts")}></wa-textarea>
        <wa-textarea value={draft.aiContext} label="Контекст для ИИ" rows="4" onInput={update("aiContext")}></wa-textarea>
      </div>
    </EditorDialog>
  );
}
