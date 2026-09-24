export default function AdditionalInfoCard({ incident, routingFacts, values, routingError, routingPending, onChange, onRemove }) {
  if (!incident) return null;
  const factsByCode = new Map(routingFacts.map((fact) => [fact.code, fact]));
  const facts = incident.routingFactCodes.map((code) => factsByCode.get(code)).filter(Boolean);
  return (
    <section class="incident-details-panel" id={`incident-panel-${incident.code}`}>
      <header class="incident-details-header">
        <strong>{incident.finalName}</strong>
        <button type="button" aria-label={`Удалить тип происшествия ${incident.finalName}`} onClick={onRemove}><wa-icon name="xmark" aria-hidden="true"></wa-icon></button>
      </header>
      <div class="incident-details-content wa-stack wa-gap-m">
        {!!facts.length && <div class="incident-routing-facts">{facts.map((fact) => <div class="incident-routing-row" key={fact.code}><span>{fact.label}</span><div class="incident-routing-options">{fact.options.map((option) => <button class={`incident-routing-option ${values[fact.code] === option.value ? "incident-routing-option--selected" : ""}`} type="button" key={option.value} aria-pressed={values[fact.code] === option.value} onClick={() => onChange(fact.code, option.value)}>{option.label}</button>)}</div></div>)}</div>}
        {!facts.length && <p class="incident-details-empty">Для этого типа уточняющие вопросы классификатором не предусмотрены.</p>}
        {!!routingPending && <p class="incident-routing-status">Рассчитываются подключаемые службы…</p>}
        {!!routingError && <p class="incident-routing-error">{routingError}</p>}
        {!!incident.additionalFeatures && <div class="incident-details-instructions"><strong>Дополнительные признаки</strong><p>{incident.additionalFeatures}</p></div>}
        {!!incident.instructions?.length && <div class="incident-details-instructions">
          <strong>Инструкции</strong>
          <ul>
            {incident.instructions.map((instruction) => <li key={instruction}>{instruction}</li>)}
          </ul>
        </div>}
      </div>
    </section>
  );
}
