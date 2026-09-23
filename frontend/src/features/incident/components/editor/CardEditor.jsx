import { useEffect, useState } from "preact/hooks";
import AdditionalInfoCard from "./AdditionalInfoCard.jsx";
import ApplicantHeader from "./ApplicantHeader.jsx";
import DispatchServicesPanel, { automaticServices } from "./DispatchServicesPanel.jsx";
import IncidentTypeSearch from "./IncidentTypeSearch.jsx";
import LinkCardDialog from "./LinkCardDialog.jsx";
import PersonCard from "./PersonCard.jsx";
import PhoneField from "./PhoneField.jsx";
import VictimStatusBar from "./VictimStatusBar.jsx";
import { cardAddress, emptyPerson, findIncident, formatAdditionalInfoValue } from "./editorHelpers";
import { useClassifier } from "../../hooks/useClassifier";
import IncidentWorkspace from "../../../level/components/common/IncidentWorkspace.jsx";

const styles = `
.incident-workspace { position: fixed; z-index: 1000; inset: 0; display: grid; grid-template-rows: auto minmax(0, 1fr) auto; min-width: 48rem; background: #c8d1d5; color: var(--wa-color-text-normal); }
.workspace-callbar { display: grid; grid-template-columns: minmax(13rem, 0.8fr) repeat(3, minmax(15rem, 1fr)) auto; box-sizing: border-box; width: auto; height: auto; min-height: 6rem; padding: 0; border-block-end: 0.5rem solid #c8d1d5; background: #f4f6f6; }
.workspace-callbar > div { display: flex; box-sizing: border-box; min-width: 0; padding: var(--wa-space-m) var(--wa-space-l); border-inline-end: var(--wa-border-width-s) solid #b8c1c5; }
.workspace-applicant-summary { display: flex; box-sizing: border-box; min-height: 4.5rem; align-items: end; gap: var(--wa-space-m); padding: var(--wa-space-m); border-block-end: 0.5rem solid #c8d1d5; background: #f4f6f6; }
.workspace-applicant-name { display: flex; flex: 1; min-width: 0; gap: var(--wa-space-xs); }
.workspace-applicant-name-input, .workspace-applicant-status { min-width: 0; padding: var(--wa-space-2xs) 0; border: 0; border-block-end: 2px solid #b1bbc0; outline: 0; background: transparent; color: #35434a; font: inherit; }
.workspace-applicant-name-input { width: 50%; }
.workspace-applicant-name-input::placeholder, .workspace-applicant-status:invalid { color: #7b8b93; }
.workspace-applicant-name-input:focus, .workspace-applicant-status:focus { border-block-end-color: #008dca; }
.workspace-applicant-status { flex: 0 0 9.5rem; cursor: pointer; }
.workspace-applicant-readonly { flex-direction: column; align-items: flex-start; justify-content: center; gap: var(--wa-space-2xs); }
.workspace-applicant-readonly strong { font-size: var(--wa-font-size-l); }
.workspace-victim-status { display: flex; box-sizing: border-box; min-height: 4.5rem; align-items: center; gap: var(--wa-space-m); padding: var(--wa-space-m); border-block-end: 0.5rem solid #c8d1d5; background: #f4f6f6; }
.workspace-victim-status-label { color: #35434a; font-size: var(--wa-font-size-l); }
.workspace-victim-button { min-width: 5rem; padding: var(--wa-space-xs) var(--wa-space-m); border: var(--wa-border-width-s) solid #9ba8ae; background: transparent; color: #26343b; font: inherit; font-weight: var(--wa-font-weight-semibold); cursor: pointer; }
.workspace-victim-button--selected { border-color: #008dca; background: #008dca; color: #ffffff; }
.workspace-victim-count { width: 6rem; padding: var(--wa-space-xs); border: var(--wa-border-width-s) solid #9ba8ae; background: #ffffff; color: #26343b; font: inherit; }
.workspace-victim-readonly { font-size: var(--wa-font-size-l); font-weight: var(--wa-font-weight-semibold); }
.workspace-connection { align-items: center; gap: var(--wa-space-l); }
.workspace-connection wa-icon { color: #35434a; font-size: var(--wa-font-size-2xl); }
.workspace-connection-copy, .workspace-phone, .workspace-incident-meta { display: flex; flex-direction: column; justify-content: center; gap: var(--wa-space-2xs); }
.workspace-phone { position: relative; flex-direction: row; align-items: center; gap: var(--wa-space-s); }
.workspace-phone > wa-icon { flex: 0 0 auto; color: #687880; font-size: var(--wa-font-size-l); }
.workspace-phone-content { display: flex; flex: 1; min-width: 0; flex-direction: column; justify-content: center; gap: var(--wa-space-2xs); }
.workspace-phone strong { overflow: hidden; font-size: var(--wa-font-size-xl); font-variant-numeric: tabular-nums; text-overflow: ellipsis; white-space: nowrap; }
.workspace-phone-control { display: flex; min-width: 0; align-items: end; gap: var(--wa-space-xs); }
.workspace-phone-input { flex: 1; min-width: 0; padding: var(--wa-space-2xs) 0; border: 0; border-block-end: 2px solid #9ba8ae; outline: 0; background: transparent; color: #26343b; font: inherit; font-size: var(--wa-font-size-xl); font-variant-numeric: tabular-nums; }
.workspace-phone-input::placeholder { color: #aeb8bd; }
.workspace-phone-input:focus { border-block-end-color: #008dca; }
.workspace-aoh-button { flex: 0 0 auto; min-width: 4.25rem; padding: var(--wa-space-xs) var(--wa-space-s); border: var(--wa-border-width-s) solid #87969d; background: transparent; color: #35434a; font: inherit; font-weight: var(--wa-font-weight-semibold); cursor: pointer; }
.workspace-aoh-button:hover:not(:disabled) { border-color: #008dca; color: #007bad; }
.workspace-aoh-button:focus-visible { outline: 2px solid #008dca; outline-offset: 2px; }
.workspace-aoh-button:disabled { cursor: not-allowed; opacity: .45; }
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
.incident-details-panel { border: var(--wa-border-width-s) solid #b8c1c5; background: #f4f6f6; }
.incident-details-header { display: flex; min-height: 3.5rem; box-sizing: border-box; align-items: center; justify-content: space-between; gap: var(--wa-space-m); padding: var(--wa-space-s) var(--wa-space-m); background: #293238; color: #ffffff; font-size: var(--wa-font-size-l); }
.incident-details-header strong { text-decoration: underline; text-underline-offset: var(--wa-space-xs); }
.incident-details-header button { display: grid; width: 2.5rem; height: 2.5rem; place-items: center; border: 0; background: transparent; color: #ffffff; font-size: var(--wa-font-size-xl); cursor: pointer; }
.incident-details-header button:hover { background: rgba(255, 255, 255, .12); }
.incident-details-content { padding: var(--wa-space-m); }
.incident-details-empty { color: var(--wa-color-text-quiet); }
.incident-classifier-details { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--wa-space-s); margin: 0; }
.incident-classifier-details div { min-width: 0; }
.incident-classifier-details dt { color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-s); }
.incident-classifier-details dd { margin: var(--wa-space-3xs) 0 0; overflow-wrap: anywhere; font-weight: var(--wa-font-weight-semibold); }
.incident-details-instructions { border-block-start: var(--wa-border-width-s) solid #b8c1c5; padding-block-start: var(--wa-space-m); }
.workspace-link-dialog { --width: min(92vw, 72rem); }
.workspace-link-table-wrap { max-height: min(60vh, 36rem); overflow: auto; border: var(--wa-border-width-s) solid #b8c1c5; }
.workspace-link-table { width: 100%; border-collapse: collapse; }
.workspace-link-table th, .workspace-link-table td { padding: var(--wa-space-s) var(--wa-space-m); border-block-end: var(--wa-border-width-s) solid #d4dadd; text-align: start; vertical-align: middle; }
.workspace-link-table th { position: sticky; z-index: 1; inset-block-start: 0; background: #e8ecec; color: #45525a; }
.workspace-link-table tbody tr { cursor: pointer; }
.workspace-link-table tbody tr:hover { background: #edf7fb; }
.workspace-link-table tbody tr:has(input:checked) { background: #d9f0fa; }
.workspace-link-table td:first-child { width: 3rem; text-align: center; }
.workspace-link-empty { padding: var(--wa-space-xl); color: var(--wa-color-text-quiet); text-align: center; }
.workspace-section-title { margin: 0; padding: var(--wa-space-m); border-block-end: var(--wa-border-width-s) solid #b8c1c5; color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-xl); font-weight: var(--wa-font-weight-normal); }
.workspace-footer { min-height: 6rem; color: #ffffff; }
.workspace-footer--editable { background: #ff5b2d; }
.workspace-footer--readonly { background: #45525a; }
.workspace-actions { padding: var(--wa-space-m); }
.workspace-actions wa-button::part(button) { min-width: 8rem; border-color: #ffffff; color: #ffffff; }
.workspace-actions .workspace-save::part(button) { min-width: 13rem; }
.workspace-actions .workspace-link::part(button), .workspace-actions .workspace-close::part(button) { min-width: 4rem; }
.workspace-actions .workspace-link[appearance='filled']::part(button) { background: #ffffff; color: #ff5b2d; }
@media (max-width: 70rem) { .workspace-callbar { grid-template-columns: repeat(3, minmax(13rem, 1fr)) auto; } .workspace-connection { display: none !important; } }
@media (max-width: 48rem) { .incident-workspace { min-width: 0; } .workspace-callbar { grid-template-columns: repeat(3, minmax(12rem, 1fr)) auto; overflow-x: auto; } .workspace-body, .saved-card-body { grid-template-columns: 1fr; overflow-y: auto; } .workspace-column { overflow: visible; } }
`;

function formatTime(seconds) {
  return `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
}

function classifierDetails(incident, values) {
  const features = [incident.feature1Name, incident.feature2Name, incident.feature3Name]
    .map((value, index) => value ? { name: `Признак ${index + 1}`, value } : null)
    .filter(Boolean);
  if (incident.additionalFeatures) features.push({ name: "Дополнительные признаки", value: incident.additionalFeatures });
  return [...features, ...(incident.fields || []).map((field) => ({ name: field.name, value: formatAdditionalInfoValue(field, values[field.id]) }))];
}

export default function CardEditor({ contextId, cards, call, editor, isDev, dadataApiKey, onChange, onClose, readOnly = false, classifier, readonlyTitle = "Карточка сохранена", readonlyHint = "режим просмотра", readonlyStatus = "Карточка сохранена", readonlyTimer = "Просмотр", readonlyDetails, readonlyActions }) {
  const loadedClassifierState = useClassifier();
  const classifierState = classifier ? { classifier, loading: false, error: null } : loadedClassifierState;
  const [seconds, setSeconds] = useState(0);
  const [savedEditMode, setSavedEditMode] = useState(false);
  const [linkDialogOpen, setLinkDialogOpen] = useState(false);
  const [linkTargetId, setLinkTargetId] = useState("");
  const incidentTypes = editor.incidentTypes.filter(Boolean);
  const incidents = incidentTypes.map((code) => findIncident(classifierState.classifier, code)).filter(Boolean);
  const editingCard = cards.find((card) => card.cardId === editor.editingCardId);
  const linkCards = cards.filter((card) => card.cardId !== editor.editingCardId);
  const selectedCard = linkCards.find((card) => card.cardId === editor.selectedCardId);
  const relatedCard = !!editingCard?.mainCardId;
  const canUnlink = relatedCard && call.phase === "active" && call.activeCallId === editingCard.callId;
  const relationLocked = relatedCard || call.phase === "finished";
  const canSave = !editor.cardSaved && Number.isInteger(editor.victimCount) && editor.victimCount >= 0 && incidentTypes.length === editor.incidentTypes.length && incidentTypes.length > 0 && (editor.operation !== "LINK" || !!selectedCard);
  const aoh = call.phone || editor.applicant.phone;

  useEffect(() => setSavedEditMode(false), [editor.editingCardId, editor.open]);

  useEffect(() => {
    if (!editor.open || call.phase !== "active") {
      setSeconds(0);
      return;
    }
    const timer = window.setInterval(() => setSeconds((value) => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [editor.open, call.phase]);

  const setApplicant = (applicant) => onChange({ ...editor, applicant });
  const addIncidentType = (code) => {
    const nextIncidentTypes = [...incidentTypes, code];
    onChange({ ...editor, incidentTypes: nextIncidentTypes, services: [...new Set([...(editor.services || []), ...automaticServices(classifierState.classifier, nextIncidentTypes)])] });
  };
  const removeIncidentType = (removedIncident) => {
    const removedFields = new Set((removedIncident.fields || []).map((field) => field.id));
    onChange({ ...editor, incidentTypes: incidentTypes.filter((code) => code !== removedIncident.code), additionalInfo: Object.fromEntries(Object.entries(editor.additionalInfo).filter(([key]) => !removedFields.has(key))) });
  };
  const openLinkDialog = () => {
    if (relationLocked || !linkCards.length) return;
    setLinkTargetId(linkCards.some((card) => card.cardId === editor.selectedCardId) ? editor.selectedCardId : "");
    setLinkDialogOpen(true);
  };
  const confirmLink = () => {
    if (!linkTargetId) return;
    onChange({ ...editor, operation: "LINK", selectedCardId: linkTargetId });
    setLinkDialogOpen(false);
  };
  const autofill = () => {
    const firstIncident = classifierState.classifier.flatMap((category) => category.entries)[0];
    if (!firstIncident) return;
    const values = { boolean: "true", number: "1", email: "test@example.com", tel: "79001234567", url: "https://example.com" };
    onChange({
      ...editor,
      applicant: { ...emptyPerson(), phone: "79001234567", contactPhone: "79001234567", onScenePhone: "79001234567", lastName: "Иванов", firstName: "Иван", middleName: "Иванович", address: "г. Москва, ул. Тверская, д. 1", additionalInfo: "Тестовый заявитель" },
      victimCount: 1,
      incidentTypes: [firstIncident.code],
      additionalInfo: Object.fromEntries((firstIncident.fields || []).map((field) => [field.id, values[field.type.toLowerCase()] || "Тестовое значение"])),
      services: automaticServices(classifierState.classifier, [firstIncident.code])
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
        body: JSON.stringify({ operation, cardId: revisingCard ? editingCard.cardId : null, expectedVersion: revisingCard ? editingCard.version : null, mainCardId: operation === "LINK" ? selectedCard.cardId : null, applicant: editor.applicant, victimCount: editor.victimCount, incidentTypes, additionalInfo: editor.additionalInfo, services: editor.services })
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
  if (editingCard && (!savedEditMode || readOnly)) return (
    <IncidentWorkspace label={`Просмотр карточки ${editingCard.cardId}`}>
      <style>{styles}</style>
      <header class="workspace-callbar">
        <div class="workspace-connection">
          <wa-icon name="phone" aria-hidden="true"></wa-icon>
          <div class="workspace-connection-copy"><strong>{readonlyTitle}</strong><span class="workspace-call-label">{readonlyHint}</span></div>
        </div>
        <PhoneField label="АОН" value={editor.applicant.phone} readonly />
        <PhoneField label="Предоставленный" value={editor.applicant.contactPhone} readonly />
        <PhoneField label="Телефон на месте" value={editor.applicant.onScenePhone} readonly />
        <div class="workspace-timer saved-view-label">{readonlyTimer}</div>
      </header>
      <div class="saved-card-body">
        <section class="saved-card-column wa-stack wa-gap-m" aria-label="Сведения о заявителе">
          <ApplicantHeader person={editor.applicant} readonly />
          <div class="saved-card-panel saved-address wa-stack wa-gap-s"><span class="saved-label">Адрес</span><strong>{cardAddress(editingCard)}</strong>{editor.applicant.additionalInfo && <span>{editor.applicant.additionalInfo}</span>}</div>
          <div class="saved-card-panel saved-card-spacer"><span class="saved-label">Описание со слов заявителя</span><p>{editor.applicant.additionalInfo || "Описание не заполнено"}</p></div>
        </section>
        <section class="saved-card-column wa-stack wa-gap-m" aria-label="Сведения о происшествии">
          <VictimStatusBar victimCount={editor.victimCount} readonly />
          {incidents.map((item) => { const details = classifierDetails(item, editor.additionalInfo); return <div class="wa-stack wa-gap-0" key={item.code}>
            <div class="saved-incident-heading">
              <strong>{item.finalName}</strong>
            </div>
            <div class="saved-card-panel">
              <span class="saved-label">Признаки классификации</span>
              {!!details.length && <ul class="saved-details-list wa-stack wa-gap-xs">{details.map((detail, index) => <li class="wa-cluster wa-gap-s" key={`${index}-${detail.name}`}><span>{detail.name}:</span><strong>{detail.value}</strong></li>)}</ul>}
              {!details.length && <p>Уточняющие признаки не указаны.</p>}
            </div>
            <div class="saved-card-panel">
              <span class="saved-label">Инструкции</span>
              {item.instructions?.length ? <ul>{item.instructions.map((instruction) => <li key={instruction}>{instruction}</li>)}</ul> : <p>Инструкции не указаны.</p>}
            </div>
          </div>; })}
          {!incidents.length && <div class="saved-card-panel">Тип происшествия не выбран.</div>}
          {readonlyDetails}
          <div class="saved-card-panel saved-card-spacer"><span class="saved-label">Статус</span><p><strong>{readonlyStatus}</strong></p></div>
        </section>
      </div>
      <footer class="workspace-footer workspace-footer--readonly wa-cluster wa-gap-0 wa-align-items-stretch wa-justify-content-end wa-flex-nowrap">
        <DispatchServicesPanel classifier={classifierState.classifier} services={editor.services} readonly onChange={() => {}} />
        {readonlyActions}
        <div class="workspace-actions wa-cluster wa-gap-3xs wa-align-items-stretch wa-flex-nowrap">
          {!readOnly && <wa-button class="workspace-save" size="l" type="button" appearance="outlined" variant="neutral" onClick={() => setSavedEditMode(true)}><wa-icon slot="start" name="pencil"></wa-icon>Редактировать</wa-button>}
          {!readOnly && relatedCard && <wa-button class="workspace-link" type="button" size="l" appearance="outlined" variant="neutral" disabled={!canUnlink} loading={editor.saving} aria-label="Отвязать карточку" onClick={unlink}><wa-icon name="link-slash"></wa-icon></wa-button>}
          <wa-button class="workspace-close" type="button" size="l" appearance="outlined" variant="neutral" onClick={cancel}><wa-icon name="xmark" label="Закрыть карточку"></wa-icon></wa-button>
        </div>
      </footer>
    </IncidentWorkspace>
  );
  return (
    <IncidentWorkspace label="Карточка происшествия">
      <style>{styles}</style>
      <header class="workspace-callbar">
        <div class="workspace-connection">
          <wa-icon name={call.phase === "active" ? "phone-volume" : "phone"} aria-hidden="true"></wa-icon>
          <div class="workspace-connection-copy"><strong>{call.phase === "active" ? "На линии" : "Карточка происшествия"}</strong><span class="workspace-call-label">{call.phase === "active" ? "активное соединение" : "редактирование"}</span></div>
        </div>
        <PhoneField label="АОН" value={aoh} readonly />
        <PhoneField label="Предоставленный" value={editor.applicant.contactPhone} aoh={aoh} onChange={(contactPhone) => setApplicant({ ...editor.applicant, contactPhone })} />
        <PhoneField label="Телефон на месте" value={editor.applicant.onScenePhone} aoh={aoh} onChange={(onScenePhone) => setApplicant({ ...editor.applicant, onScenePhone })} />
        <div class="workspace-timer" aria-label={`Время звонка: ${formatTime(seconds)}`}>{formatTime(seconds)}</div>
      </header>
      <div class="workspace-body">
        <section class="workspace-column" aria-label="Заявитель и пострадавшие">
          <ApplicantHeader person={editor.applicant} onChange={setApplicant} />
          <div class="workspace-column-inner wa-stack wa-gap-m">
            <PersonCard kind="applicant" person={editor.applicant} addressRequired dadataApiKey={dadataApiKey} onChange={setApplicant} />
          </div>
        </section>
        <section class="workspace-column" aria-label="Классификация происшествия">
          <VictimStatusBar victimCount={editor.victimCount} onChange={(victimCount) => onChange({ ...editor, victimCount })} />
          <LinkCardDialog open={linkDialogOpen} cards={linkCards} selectedId={linkTargetId} onSelect={setLinkTargetId} onCancel={() => setLinkDialogOpen(false)} onConfirm={confirmLink} />
          <IncidentTypeSearch classifierState={classifierState} selectedCodes={incidentTypes} onAdd={addIncidentType} />
          <div class="workspace-column-inner wa-stack wa-gap-m">
            {incidents.map((item) => <AdditionalInfoCard key={item.code} incident={item} values={editor.additionalInfo} onChange={(additionalInfo) => onChange({ ...editor, additionalInfo })} onRemove={() => removeIncidentType(item)} />)}
          </div>
        </section>
      </div>
      <footer class={`workspace-footer ${editingCard ? "workspace-footer--readonly" : "workspace-footer--editable"} wa-cluster wa-gap-0 wa-align-items-stretch wa-justify-content-end wa-flex-nowrap`}>
        <DispatchServicesPanel classifier={classifierState.classifier} services={editor.services} readonly={!!editingCard} onChange={(services) => onChange({ ...editor, services })} />
        <div class="workspace-actions wa-cluster wa-gap-3xs wa-align-items-stretch wa-flex-nowrap">
          {isDev && <wa-button size="l" type="button" appearance="outlined" onClick={autofill}><wa-icon name="wand-magic-sparkles" label="Автозаполнение"></wa-icon></wa-button>}
          <wa-button class="workspace-save" type="button" size="l" appearance="outlined" variant="neutral" disabled={!canSave} loading={editor.saving} onClick={save}>Сохранить</wa-button>
          {relatedCard ? <wa-button class="workspace-link" type="button" size="l" appearance="outlined" variant="neutral" disabled={!canUnlink} loading={editor.saving} aria-label="Отвязать карточку" onClick={unlink}><wa-icon name="link-slash"></wa-icon></wa-button> : <wa-button class="workspace-link" type="button" size="l" appearance={editor.operation === "LINK" ? "filled" : "outlined"} variant="neutral" disabled={relationLocked || !linkCards.length} aria-label="Связать карточку" onClick={openLinkDialog}><wa-icon name="link"></wa-icon></wa-button>}
          {editingCard && <wa-button class="workspace-close" type="button" size="l" appearance="outlined" variant="neutral" aria-label="Закрыть карточку" onClick={cancel}><wa-icon name="xmark" aria-hidden="true"></wa-icon></wa-button>}
        </div>
      </footer>
    </IncidentWorkspace>
  );
}
