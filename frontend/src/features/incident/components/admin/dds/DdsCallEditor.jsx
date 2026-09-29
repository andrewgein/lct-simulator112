import { useEffect, useState } from "preact/hooks";
import EditorDialog from "../EditorDialog.jsx";
import PersonFields from "../PersonFields.jsx";
import FactsField from "../FactsField.jsx";
import { emptyPerson, personValue, splitLines } from "../editorHelpers.js";
import { syncCallAddress } from "./ddsCallAddress.js";
import { withCallContact } from "./ddsCallContacts.js";

export function normalizeDdsCall(call = {}, key = crypto.randomUUID()) {
  return {
    key,
    id: call.id || null,
    person: emptyPerson(call.person),
    direction: call.direction || "OUTBOUND",
    counterparty: call.counterparty || "BRIGADE",
    serviceCode: call.serviceCode || "",
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
    serviceCode: call.counterparty === "SERVICE" ? call.serviceCode || null : null,
    person: personValue(call.person),
    gender: call.gender || null,
    knownFacts: splitLines(call.knownFacts),
    hiddenFacts: splitLines(call.hiddenFacts),
    aiContext: call.aiContext || null,
    emotionalState: call.emotionalState || null
  };
}

export default function DdsCallEditor({ call, index, open, onClose, onSave, services, contacts = [], incidentAddress = "" }) {
  const [draft, setDraft] = useState(call);
  const [selectedContact, setSelectedContact] = useState("");
  useEffect(() => { if (open) { setDraft(structuredClone(call)); setSelectedContact(""); } }, [open]);
  useEffect(() => {
    if (open) setDraft((current) => syncCallAddress(current, incidentAddress));
  }, [incidentAddress]);
  const update = (field) => (event) => setDraft((current) => ({ ...current, [field]: event.currentTarget.value }));
  return (
    <EditorDialog className="dds-call-dialog" label={`Редактирование звонка ${index + 1}`} open={open} onCancel={onClose} onSave={() => { onSave(draft); onClose(); }}>
      <div class="wa-cluster wa-align-items-stretch wa-gap-l">
        <div class="dds-call-section wa-stack wa-gap-m">
          <div class="wa-grid">
            <wa-select value={draft.counterparty} label="Собеседник" disabled={!!selectedContact} onChange={update("counterparty")}>
              <wa-option value="BRIGADE">Бригада</wa-option>
              <wa-option value="SERVICE">Другая служба</wa-option>
            </wa-select>
            <wa-select value={draft.direction} label="Направление звонка" onChange={update("direction")}>
              <wa-option value="OUTBOUND">Исходящий из ДДС</wa-option>
              <wa-option value="INBOUND">Входящий в ДДС</wa-option>
            </wa-select>
          </div>
          {draft.counterparty === "SERVICE" && <wa-select value={draft.serviceCode} label="Другая служба" required disabled={!!selectedContact} onChange={update("serviceCode")}>
            <wa-option value="">Выберите службу</wa-option>
            {services.map((service) => <wa-option key={service.code} value={service.code}>{service.name}</wa-option>)}
          </wa-select>}
          <PersonFields title={draft.counterparty === "SERVICE" ? "Контакт другой службы" : "Контакт бригады"} person={draft.person} gender={draft.gender} showContactFields={false} showAddressFields incidentAddress={incidentAddress} disabled={!!selectedContact} afterTitle={!!contacts.length && <wa-select value={selectedContact} label="Использовать контакт из другого звонка" onChange={(event) => {
            const key = event.currentTarget.value;
            setSelectedContact(key);
            const source = contacts.find((contact) => contact.key === key)?.call;
            if (source) setDraft((current) => syncCallAddress(withCallContact(current, source), incidentAddress));
          }}>
            <wa-option value="">Заполнить вручную</wa-option>
            {contacts.map(({ key, label }) => <wa-option key={key} value={key}>{label}</wa-option>)}
          </wa-select>} onChange={(person) => setDraft((current) => ({ ...current, person }))} />
        </div>
        <wa-divider class="dds-call-divider-desktop" orientation="vertical"></wa-divider>
        <wa-divider class="dds-call-divider-mobile"></wa-divider>
        <div class="dds-call-section wa-stack wa-gap-m">
          <h3 class="wa-heading-l">Данные звонка для контекста ИИ</h3>
          <div class="wa-grid">
            <wa-select value={draft.gender} label="Пол собеседника" disabled={!!selectedContact} onChange={update("gender")}>
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
          <FactsField label="Известные факты" value={draft.knownFacts} onChange={(knownFacts) => setDraft((current) => ({ ...current, knownFacts }))} />
          <FactsField label="Скрытые факты" value={draft.hiddenFacts} onChange={(hiddenFacts) => setDraft((current) => ({ ...current, hiddenFacts }))} />
          <wa-textarea value={draft.aiContext} label="Контекст для ИИ" rows="5" onInput={update("aiContext")}></wa-textarea>
        </div>
      </div>
    </EditorDialog>
  );
}
