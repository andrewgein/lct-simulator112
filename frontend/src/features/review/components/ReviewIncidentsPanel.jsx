import { useState } from "preact/hooks";
import CardEditor from "../../incident/components/editor/CardEditor.jsx";
import { normalizePerson } from "../../incident/components/editor/editorHelpers.js";

const styles = `
.review-card-button { display: inline-block; box-sizing: border-box; padding: var(--wa-space-s) var(--wa-space-m); border: var(--wa-border-width-s) solid var(--wa-color-surface-border); background: var(--wa-color-surface-raised); font: inherit; color: inherit; cursor: pointer; }
.review-card-button:hover { border-color: var(--wa-color-brand-border-loud); background: var(--wa-color-surface-default); }
.review-card-list { display: flex; flex-wrap: wrap; gap: var(--wa-space-s); }
.review-card-item { display: flex; flex-direction: column; gap: var(--wa-space-3xs); }
`;

function buildEditor(card) {
  return {
    open: true, operation: "SAVE", editingCardId: card.cardId, selectedCardId: card.cardId,
    applicant: normalizePerson(card.applicant), victimCount: card.victimCount ?? 0,
    incidentTypes: card.incidentTypes?.length ? card.incidentTypes : [""],
    additionalInfo: card.additionalInfo || {}, services: card.services || [],
    cardSaved: true, saving: false
  };
}

export default function ReviewIncidentsPanel({ contextId, incidents, cards, canLinkToScenario }) {
  const [openCard, setOpenCard] = useState(null);
  const cardsByIncident = new Map();
  const unassignedCards = [];
  (cards || []).forEach((card) => {
    if (!card.incidentId) { unassignedCards.push(card); return; }
    if (!cardsByIncident.has(card.incidentId)) cardsByIncident.set(card.incidentId, []);
    cardsByIncident.get(card.incidentId).push(card);
  });

  const cardLabels = new Map();
  const labelGroup = (group, scenario) => group.forEach((card, index) => cardLabels.set(card.cardId, { label: `Карточка ${index + 1}`, scenario, incidentId: card.incidentId }));
  (incidents || []).forEach((incident) => labelGroup(cardsByIncident.get(incident.id) || [], `сценарий ${incident.order}`));
  labelGroup(unassignedCards, "без сценария");
  const linkText = (card) => {
    if (!card.mainCardId) return null;
    const target = cardLabels.get(card.mainCardId);
    if (!target) return "Связана с другой карточкой";
    return `Связана с: ${target.label}${target.incidentId === card.incidentId ? "" : ` (${target.scenario})`}`;
  };
  const renderCard = (card, index) => (
    <div class="review-card-item" key={card.cardId}>
      <button type="button" class="review-card-button" onClick={() => setOpenCard(card)}>Карточка {index + 1}</button>
      {!!card.mainCardId && <span class="wa-caption-m">{linkText(card)}</span>}
    </div>
  );

  return (
    <div class="wa-stack wa-gap-l">
      <style>{styles}</style>
      {(incidents || []).map((incident) => (
        <div class="wa-stack wa-gap-xs" key={incident.id}>
          <span>Сценарий {incident.order}: {canLinkToScenario ? <a href={`/admin/incidents/${encodeURIComponent(incident.id)}`}>{incident.title || "Без названия"}</a> : (incident.title || "Без названия")}</span>
          <span class="wa-caption-m">Пострадавших: {incident.victimCount}{!!incident.classifierCodes?.length && `, классификация: ${incident.classifierCodes.join(", ")}`}</span>
          <div class="review-card-list">
            {(cardsByIncident.get(incident.id) || []).map(renderCard)}
          </div>
        </div>
      ))}
      {!!unassignedCards.length && (
        <div class="wa-stack wa-gap-xs">
          <span>Без привязки к сценарию:</span>
          <div class="review-card-list">
            {unassignedCards.map(renderCard)}
          </div>
        </div>
      )}
      {openCard && (
        <CardEditor
          contextId={contextId}
          cards={[openCard]}
          call={{ phase: "finished", activeCallId: null, phone: openCard.applicant?.phone || "" }}
          editor={buildEditor(openCard)}
          onChange={(nextEditor) => { if (!nextEditor.open) setOpenCard(null); }}
          onClose={() => setOpenCard(null)}
          readOnly
          readonlyTitle="Карточка диспетчера"
          readonlyHint="просмотр результата"
          readonlyStatus="Карточка сохранена"
          readonlyTimer="Просмотр"
          readonlyDetails={openCard.mainCardId ? <div class="saved-card-panel"><span class="saved-label">Связь</span><p>{linkText(openCard)}</p></div> : null}
        />
      )}
    </div>
  );
}
