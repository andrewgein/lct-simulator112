import { useEffect, useState } from "preact/hooks";
import AdditionalInfoCard from "./AdditionalInfoCard.jsx";
import InstructionsCard from "./InstructionsCard.jsx";
import PersonCard from "./PersonCard.jsx";
import ReactionPlanCard from "./ReactionPlanCard.jsx";
import { applicantName, cardAddress, emptyPerson, findIncident, normalizePerson } from "./editorHelpers";
import { useClassifier } from "../../hooks/useClassifier";

const styles = `
.incident-workspace { position: fixed; z-index: 1000; inset: 0; display: grid; grid-template-rows: auto minmax(0, 1fr) auto; min-width: 48rem; background: #c8d1d5; color: var(--wa-color-text-normal); }
.workspace-callbar { display: grid; grid-template-columns: minmax(16rem, 1fr) minmax(22rem, 1.5fr) minmax(16rem, 1fr) auto; box-sizing: border-box; width: auto; height: auto; min-height: 6rem; padding: 0; border-block-end: 0.5rem solid #c8d1d5; background: #f4f6f6; }
.workspace-callbar > div { display: flex; box-sizing: border-box; min-width: 0; padding: var(--wa-space-m) var(--wa-space-l); border-inline-end: var(--wa-border-width-s) solid #b8c1c5; }
.workspace-connection { align-items: center; gap: var(--wa-space-l); }
.workspace-connection wa-icon { color: #35434a; font-size: var(--wa-font-size-2xl); }
.workspace-connection-copy, .workspace-phone, .workspace-incident-meta { display: flex; flex-direction: column; justify-content: center; gap: var(--wa-space-2xs); }
.workspace-phone strong { font-size: var(--wa-font-size-xl); font-variant-numeric: tabular-nums; }
.workspace-call-label, .workspace-incident-meta span { color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-s); }
.workspace-incident-meta strong { overflow: hidden; font-size: var(--wa-font-size-l); text-overflow: ellipsis; white-space: nowrap; }
.workspace-timer { align-items: center; justify-content: center; min-width: 8rem; background: #293238; color: #ffffff; font-size: var(--wa-font-size-2xl); font-weight: var(--wa-font-weight-bold); font-variant-numeric: tabular-nums; }
.saved-view-label { background: #008dca; font-size: var(--wa-font-size-m); text-transform: uppercase; }
.workspace-body { display: grid; grid-template-columns: minmax(24rem, 0.9fr) minmax(30rem, 1.1fr); gap: 0.5rem; min-height: 0; padding: 0 0.5rem; }
.saved-card-body { display: grid; grid-template-columns: minmax(24rem, 0.9fr) minmax(30rem, 1.1fr); gap: 0.5rem; min-height: 0; padding: 0 0.5rem 0.5rem; background: #c8d1d5; }
.saved-card-column { min-width: 0; overflow-y: auto; }
.saved-card-panel { padding: var(--wa-space-m); background: #f4f6f6; border: var(--wa-border-width-s) solid #b8c1c5; }
.saved-person-heading strong { font-size: var(--wa-font-size-xl); }
.saved-person-heading span, .saved-label { color: var(--wa-color-text-quiet); }
.saved-address strong { font-size: var(--wa-font-size-l); }
.saved-card-spacer { flex: 1; min-height: 12rem; }
.saved-incident-heading { padding: var(--wa-space-m); background: #293238; color: #ffffff; font-size: var(--wa-font-size-l); }
.saved-incident-heading strong { text-decoration: underline dotted; text-underline-offset: var(--wa-space-xs); }
.saved-details-list { margin: 0; padding: 0; list-style: none; }
.saved-details-list span { color: var(--wa-color-text-quiet); }
.workspace-column { min-width: 0; overflow-y: auto; background: #f4f6f6; }
.workspace-column-inner { padding: var(--wa-space-m); }
.workspace-column wa-card { --spacing: var(--wa-space-m); }
.workspace-column wa-card::part(base) { border-color: #b8c1c5; border-radius: 0; box-shadow: none; }
.workspace-column .wa-stack { --wa-content-spacing: var(--wa-space-s); }
.workspace-reference { display: grid; grid-template-columns: minmax(12rem, 1fr) auto; align-items: end; gap: var(--wa-space-s); padding: var(--wa-space-m); background: #e8ecec; border-block-end: var(--wa-border-width-s) solid #b8c1c5; }
.workspace-reference:has(> wa-select[hidden]) { grid-template-columns: 1fr; }
.workspace-section-title { margin: 0; padding: var(--wa-space-m); border-block-end: var(--wa-border-width-s) solid #b8c1c5; color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-xl); font-weight: var(--wa-font-weight-normal); }
.workspace-footer { min-height: 6rem; background: #293238; color: #ffffff; }
.workspace-actions { padding: var(--wa-space-m); }
.workspace-actions wa-button::part(button) { min-width: 8rem; border-color: #ffffff; color: #ffffff; }
.workspace-actions .workspace-save::part(button) { min-width: 13rem; }
.workspace-actions .workspace-link::part(button), .workspace-actions .workspace-close::part(button) { min-width: 4rem; }
.workspace-actions .workspace-link[appearance='filled']::part(button) { background: #ffffff; color: #ff5b2d; }
@media (max-width: 70rem) { .workspace-callbar { grid-template-columns: 1fr 1.3fr auto; } .workspace-incident-meta { display: none !important; } }
@media (max-width: 48rem) { .incident-workspace { min-width: 0; } .workspace-callbar { grid-template-columns: 1fr auto; } .workspace-phone { display: none !important; } .workspace-body, .saved-card-body { grid-template-columns: 1fr; overflow-y: auto; } .workspace-column { overflow: visible; } }
`;

function formatTime(seconds) {
  return `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
}

function formatPhone(phone) {
  const digits = phone?.replace(/\D/g, "") || "";
  if (digits.length === 11 && /^[78]/.test(digits)) return `+7 (${digits.slice(1, 4)}) ${digits.slice(4, 7)}-${digits.slice(7, 9)}-${digits.slice(9)}`;
  return phone || "Номер не определён";
}

export default function CardEditor({ contextId, cards, call, editor, isDev, dadataApiKey, onChange, onClose }) {
  const classifierState = useClassifier();
  const [seconds, setSeconds] = useState(0);
  const [savedEditMode, setSavedEditMode] = useState(false);
  const incident = findIncident(classifierState.classifier, editor.incidentType);
  const editingCard = cards.find((card) => card.cardId === editor.editingCardId);
  const relationCards = cards.filter((card) => card.cardId !== editor.editingCardId && !card.mainCardId);
  const selectedCard = relationCards.find((card) => card.cardId === editor.selectedCardId);
  const referenceVisible = editor.operation === "LINK";
  const relatedCard = !!editingCard?.mainCardId;
  const canUnlink = relatedCard && call.phase === "active" && call.activeCallId === editingCard.callId;
  const relationLocked = relatedCard || call.phase === "finished";
  const canSave = !editor.cardSaved && (editor.operation !== "LINK" || !!selectedCard);

  useEffect(() => setSavedEditMode(false), [editor.editingCardId, editor.open]);

  useEffect(() => {
    if (!editor.open || call.phase !== "active") {
      setSeconds(0);
      return;
    }
    const timer = window.setInterval(() => setSeconds((value) => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [editor.open, call.phase]);

  const setPerson = (name, person) => {
    const next = { ...editor, [name]: person };
    if (name === "applicant" && editor.isApplicantVictim) next.victim = { ...next.victim, ...person };
    onChange(next);
  };
  const toggleApplicantVictim = (checked) => onChange({ ...editor, isApplicantVictim: checked, victim: checked ? { ...editor.victim, ...editor.applicant } : editor.victim });
  const selectOperation = (operation) => {
    if (relationLocked) return;
    const fallback = editor.editingCardId ? "SAVE" : "CREATE";
    const nextOperation = editor.operation === operation ? fallback : operation;
    const selectedCardId = relationCards.some((card) => card.cardId === editor.selectedCardId) ? editor.selectedCardId : relationCards[0]?.cardId || "";
    onChange({ ...editor, operation: nextOperation, selectedCardId });
  };
  const selectReference = (cardId) => {
    const card = cards.find((item) => item.cardId === cardId);
    onChange(editor.operation === "SAVE" && card ? { ...editor, selectedCardId: cardId, applicant: normalizePerson(card.applicant), victim: normalizePerson(card.victim), isApplicantVictim: false, incidentType: card.incidentType || "", additionalInfo: card.additionalInfo || {} } : { ...editor, selectedCardId: cardId });
  };
  const autofill = () => {
    const firstIncident = Object.values(classifierState.classifier).flatMap((incidents) => Object.values(incidents))[0];
    if (!firstIncident) return;
    const values = { boolean: "true", number: "1", email: "test@example.com", tel: "79001234567", url: "https://example.com" };
    onChange({
      ...editor,
      applicant: { ...emptyPerson(), phone: "79001234567", contactPhone: "79001234567", lastName: "Иванов", firstName: "Иван", middleName: "Иванович", address: "г. Москва, ул. Тверская, д. 1", additionalInfo: "Тестовый заявитель" },
      victim: { ...emptyPerson(), phone: "79007654321", contactPhone: "79007654321", lastName: "Петров", firstName: "Пётр", middleName: "Петрович", address: "г. Москва, ул. Тверская, д. 1", additionalInfo: "Сознание сохранено" },
      isApplicantVictim: false,
      incidentType: firstIncident.id,
      additionalInfo: Object.fromEntries((firstIncident.fields || []).map((field) => [field.id, values[field.type.toLowerCase()] || "Тестовое значение"]))
    });
  };
  const unlink = async () => {
    if (!canUnlink) return;
    onChange({ ...editor, saving: true });
    try {
      const response = await fetch(`/api/v1/context/${contextId}/cards/${editingCard.cardId}/revisions`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ operation: "UNLINK", cardId: editingCard.cardId, expectedVersion: editingCard.version }) });
      if (!response.ok) throw new Error(await response.text());
      onClose(false);
    } catch (error) {
      console.error("Failed to unlink card", error);
      onChange({ ...editor, saving: false });
    }
  };
  const save = async () => {
    if (!canSave) return;
    const revisingCard = !!editingCard;
    if (!revisingCard && !call.activeCallId) return console.error("Cannot create a card without callId");
    const url = revisingCard ? `/api/v1/context/${contextId}/cards/${editingCard.cardId}/revisions` : `/api/v1/context/${contextId}/calls/${call.activeCallId}/cards`;
    const operation = editor.operation === "LINK" ? "LINK" : revisingCard ? "SAVE" : "CREATE";
    onChange({ ...editor, saving: true });
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ operation, cardId: revisingCard ? editingCard.cardId : null, expectedVersion: revisingCard ? editingCard.version : null, mainCardId: operation === "LINK" ? selectedCard.cardId : null, applicant: editor.applicant, victim: editor.victim, incidentType: editor.incidentType, additionalInfo: editor.additionalInfo })
      });
      if (!response.ok) throw new Error(await response.text());
      onClose(call.phase === "finished");
    } catch (error) {
      console.error("Failed to save card", error);
      onChange({ ...editor, saving: false });
    }
  };
  const cancel = () => onChange({ ...editor, open: false, saving: false });

  if (!editor.open) return null;
  if (editingCard && !savedEditMode) return (
    <div class="incident-workspace" role="dialog" aria-modal="true" aria-label={`Просмотр карточки ${editingCard.cardId}`}>
      <style>{styles}</style>
      <header class="workspace-callbar">
        <div class="workspace-connection">
          <wa-icon name="phone" aria-hidden="true"></wa-icon>
          <div class="workspace-connection-copy"><strong>Карточка сохранена</strong><span class="workspace-call-label">режим просмотра</span></div>
        </div>
        <div class="workspace-phone"><span class="workspace-call-label">Телефон заявителя</span><strong>{formatPhone(editor.applicant.phone)}</strong></div>
        <div class="workspace-incident-meta"><strong>Происшествие {editingCard.cardId}</strong><span>{incident?.name || "Тип происшествия не выбран"}</span></div>
        <div class="workspace-timer saved-view-label">Просмотр</div>
      </header>
      <div class="saved-card-body">
        <section class="saved-card-column wa-stack wa-gap-m" aria-label="Сведения о заявителе">
          <div class="saved-card-panel saved-person-heading wa-cluster wa-gap-l wa-align-items-baseline"><strong>{applicantName(editingCard)}</strong><span>{editingCard.applicant?.isApplicantVictim ? "заявитель и пострадавший" : "заявитель"}</span></div>
          <div class="saved-card-panel saved-address wa-stack wa-gap-s"><span class="saved-label">Адрес</span><strong>{cardAddress(editingCard)}</strong>{editor.applicant.additionalInfo && <span>{editor.applicant.additionalInfo}</span>}</div>
          <div class="saved-card-panel"><span class="saved-label">Пострадавший</span><p><strong>{[editor.victim.lastName, editor.victim.firstName, editor.victim.middleName].filter(Boolean).join(" ") || "Не указан"}</strong></p>{editor.victim.phone && <p>{formatPhone(editor.victim.phone)}</p>}</div>
          <div class="saved-card-panel saved-card-spacer"><span class="saved-label">Описание со слов заявителя</span><p>{editor.applicant.additionalInfo || "Описание не заполнено"}</p></div>
        </section>
        <section class="saved-card-column wa-stack wa-gap-m" aria-label="Сведения о происшествии">
          <div class="saved-incident-heading"><strong>{incident?.name || "Тип происшествия не выбран"}</strong></div>
          <div class="saved-card-panel"><span class="saved-label">Состояние происшествия</span><ul class="saved-details-list wa-stack wa-gap-xs">{(incident?.fields || []).map((field) => <li class="wa-cluster wa-gap-s" key={field.id}><span>{field.name}:</span><strong>{String(editor.additionalInfo[field.id] ?? "—")}</strong></li>)}</ul>{!incident?.fields?.length && <p>Дополнительные сведения не предусмотрены.</p>}</div>
          <div class="saved-card-panel"><span class="saved-label">Инструкции</span>{incident?.instructions?.length ? <ul>{incident.instructions.map((instruction) => <li key={instruction}>{instruction}</li>)}</ul> : <p>Инструкции не указаны.</p>}</div>
          <div class="saved-card-panel saved-card-spacer"><span class="saved-label">Статус</span><p><strong>Карточка сохранена</strong></p></div>
        </section>
      </div>
      <footer class="workspace-footer wa-cluster wa-gap-0 wa-align-items-stretch wa-justify-content-end wa-flex-nowrap">
        <div class="workspace-actions wa-cluster wa-gap-3xs wa-align-items-stretch wa-flex-nowrap">
          <wa-button class="workspace-save" size="l" type="button" appearance="outlined" variant="neutral" onClick={() => setSavedEditMode(true)}><wa-icon slot="start" name="pencil"></wa-icon>Редактировать</wa-button>
          {relatedCard && <wa-button class="workspace-link" type="button" size="l" appearance="outlined" variant="neutral" disabled={!canUnlink} loading={editor.saving} aria-label="Отвязать карточку" onClick={unlink}><wa-icon name="link-slash"></wa-icon></wa-button>}
          <wa-button class="workspace-close" type="button" size="l" appearance="outlined" variant="neutral" onClick={cancel}><wa-icon name="xmark" label="Закрыть карточку"></wa-icon></wa-button>
        </div>
      </footer>
    </div>
  );
  return (
    <div class="incident-workspace" role="dialog" aria-modal="true" aria-label="Карточка происшествия">
      <style>{styles}</style>
      <header class="workspace-callbar">
        <div class="workspace-connection">
          <wa-icon name={call.phase === "active" ? "phone-volume" : "phone"} aria-hidden="true"></wa-icon>
          <div class="workspace-connection-copy"><strong>{call.phase === "active" ? "На линии" : "Карточка происшествия"}</strong><span class="workspace-call-label">{call.phase === "active" ? "активное соединение" : "редактирование"}</span></div>
        </div>
        <div class="workspace-phone"><span class="workspace-call-label">Телефон заявителя</span><strong>{formatPhone(call.phone || editor.applicant.phone)}</strong></div>
        <div class="workspace-incident-meta"><strong>{editingCard ? `Происшествие ${editingCard.cardId}` : "Новое происшествие"}</strong><span>{incident?.name || "Тип происшествия не выбран"}</span></div>
        <div class="workspace-timer" aria-label={`Время звонка: ${formatTime(seconds)}`}>{formatTime(seconds)}</div>
      </header>
      <div class="workspace-body">
        <section class="workspace-column" aria-label="Заявитель и пострадавший">
          <h2 class="workspace-section-title">Данные вызова</h2>
          <div class="workspace-column-inner wa-stack wa-gap-m">
            <PersonCard title="Информация о заявителе" kind="applicant" person={editor.applicant} addressRequired={!editor.victim.address} isApplicantVictim={editor.isApplicantVictim} dadataApiKey={dadataApiKey} onChange={(person) => setPerson("applicant", person)} onApplicantVictimChange={toggleApplicantVictim} />
            <PersonCard title="Информация о пострадавшем" kind="victim" person={editor.victim} addressRequired={!editor.applicant.address} isApplicantVictim={editor.isApplicantVictim} dadataApiKey={dadataApiKey} onChange={(person) => setPerson("victim", person)} />
          </div>
        </section>
        <section class="workspace-column" aria-label="Классификация происшествия">
          <div class="workspace-reference">
            <wa-select label="Связанная карточка" hidden={!referenceVisible} value={editor.selectedCardId} onChange={(event) => selectReference(event.currentTarget.value)}>
              {relationCards.map((card) => <wa-option key={card.cardId} value={card.cardId}>{applicantName(card)} / {cardAddress(card)}</wa-option>)}
            </wa-select>
            <div class="workspace-operation-buttons wa-cluster wa-gap-xs wa-justify-content-end">
              {!relatedCard && <wa-button type="button" appearance={editor.operation === "LINK" ? "filled" : "outlined"} variant={editor.operation === "LINK" ? "brand" : "neutral"} disabled={relationLocked || !relationCards.length} onClick={() => selectOperation("LINK")}><wa-icon slot="start" name="link"></wa-icon>Связать</wa-button>}
            </div>
          </div>
          <h2 class="workspace-section-title">Добавить тип происшествия</h2>
          <div class="workspace-column-inner wa-stack wa-gap-m">
            <ReactionPlanCard classifierState={classifierState} value={editor.incidentType} required onChange={(incidentType) => onChange({ ...editor, incidentType })} />
            <AdditionalInfoCard incident={incident} values={editor.additionalInfo} onChange={(additionalInfo) => onChange({ ...editor, additionalInfo })} />
            <InstructionsCard incident={incident} />
          </div>
        </section>
      </div>
      <footer class="workspace-footer wa-cluster wa-gap-0 wa-align-items-stretch wa-justify-content-end wa-flex-nowrap">
        <div class="workspace-actions wa-cluster wa-gap-3xs wa-align-items-stretch wa-flex-nowrap">
          {isDev && <wa-button size="l" type="button" appearance="outlined" onClick={autofill}><wa-icon name="wand-magic-sparkles" label="Автозаполнение"></wa-icon></wa-button>}
          <wa-button class="workspace-save" type="button" size="l" appearance="outlined" variant="neutral" disabled={!canSave} loading={editor.saving} onClick={save}>Сохранить</wa-button>
          {relatedCard ? <wa-button class="workspace-link" type="button" size="l" appearance="outlined" variant="neutral" disabled={!canUnlink} loading={editor.saving} aria-label="Отвязать карточку" onClick={unlink}><wa-icon name="link-slash"></wa-icon></wa-button> : <wa-button class="workspace-link" type="button" size="l" appearance={editor.operation === "LINK" ? "filled" : "outlined"} variant="neutral" disabled={relationLocked || !relationCards.length} aria-label="Связать карточку" onClick={() => selectOperation("LINK")}><wa-icon name="link"></wa-icon></wa-button>}
          {editingCard && <wa-button class="workspace-close" type="button" size="l" appearance="outlined" variant="neutral" aria-label="Закрыть карточку" onClick={cancel}><wa-icon name="xmark" aria-hidden="true"></wa-icon></wa-button>}
        </div>
      </footer>
    </div>
  );
}
