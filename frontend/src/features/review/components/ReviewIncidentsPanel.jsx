import { useState } from "preact/hooks";
import CardEditor from "../../incident/components/editor/CardEditor.jsx";
import { applicantName, normalizePerson } from "../../incident/components/editor/editorHelpers.js";

const styles = `
.review-card-button { display: block; box-sizing: border-box; width: 100%; padding: var(--wa-space-m); border: var(--wa-border-width-s) solid var(--wa-color-surface-border); background: var(--wa-color-surface-raised); text-align: start; font: inherit; color: inherit; cursor: pointer; }
.review-card-button:hover { border-color: var(--wa-color-brand-border-loud); background: var(--wa-color-surface-default); }
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

function CardSummary({ card }) {
  return (
    <div class="wa-stack wa-gap-2xs">
      <span>Заявитель: {applicantName(card)}{card.applicant?.phone && `, тел. ${card.applicant.phone}`}</span>
      {card.applicant?.address && <span>Адрес: {card.applicant.address}</span>}
      {card.victimCount !== null && card.victimCount !== undefined && <span>Пострадавших: {card.victimCount}</span>}
      {!!card.incidentTypes?.length && <span>Вид происшествия: {card.incidentTypes.join(", ")}</span>}
      {!!card.services?.length && <span>Привлечённые службы: {card.services.join(", ")}</span>}
      {!!Object.keys(card.additionalInfo || {}).length && (
        <span>Доп. информация: {Object.entries(card.additionalInfo).map(([key, value]) => `${key}: ${value}`).join("; ")}</span>
      )}
    </div>
  );
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

  return (
    <div class="wa-stack wa-gap-l">
      <style>{styles}</style>
      {(incidents || []).map((incident) => (
        <div class="wa-stack wa-gap-xs" key={incident.id}>
          <span>Сценарий {incident.order}: {canLinkToScenario ? <a href={`/admin/incidents/${encodeURIComponent(incident.id)}`}>{incident.title || "Без названия"}</a> : (incident.title || "Без названия")}</span>
          <span class="wa-caption-m">Пострадавших: {incident.victimCount}{!!incident.classifierCodes?.length && `, классификация: ${incident.classifierCodes.join(", ")}`}</span>
          {(cardsByIncident.get(incident.id) || []).map((card) => (
            <button type="button" class="review-card-button" onClick={() => setOpenCard(card)} key={card.cardId}>
              <CardSummary card={card} />
            </button>
          ))}
        </div>
      ))}
      {!!unassignedCards.length && (
        <div class="wa-stack wa-gap-xs">
          <span>Без привязки к сценарию:</span>
          {unassignedCards.map((card) => (
            <button type="button" class="review-card-button" onClick={() => setOpenCard(card)} key={card.cardId}>
              <CardSummary card={card} />
            </button>
          ))}
        </div>
      )}
      {openCard && (
        <CardEditor
          contextId={contextId}
          cards={[openCard]}
          call={{ phase: "finished", activeCallId: null, phone: openCard.applicant?.phone || "" }}
          editor={buildEditor(openCard)}
          onChange={() => {}}
          onClose={() => setOpenCard(null)}
          readOnly
          readonlyTitle="Карточка диспетчера"
          readonlyHint="просмотр результата"
          readonlyStatus="Карточка сохранена"
          readonlyTimer="Просмотр"
        />
      )}
    </div>
  );
}
