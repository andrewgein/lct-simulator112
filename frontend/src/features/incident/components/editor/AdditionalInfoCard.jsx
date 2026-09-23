export default function AdditionalInfoCard({ incident, values, onChange, onRemove }) {
  if (!incident) return null;
  return (
    <section class="incident-details-panel" id={`incident-panel-${incident.code}`}>
      <header class="incident-details-header">
        <strong>{incident.finalName}</strong>
        <button type="button" aria-label={`Удалить тип происшествия ${incident.finalName}`} onClick={onRemove}><wa-icon name="xmark" aria-hidden="true"></wa-icon></button>
      </header>
      <div class="incident-details-content wa-stack wa-gap-m">
        {!!incident.fields?.length && <div class="wa-stack wa-gap-s">
          {incident.fields.map((field) => field.type.toLowerCase() === "boolean" ? (
            <wa-checkbox key={field.id} name={field.id} required={field.required} checked={values[field.id] === true || values[field.id] === "true"} onChange={(event) => onChange({ ...values, [field.id]: String(event.currentTarget.checked) })}>{field.name}</wa-checkbox>
          ) : (
            <wa-input key={field.id} name={field.id} label={field.name} required={field.required} type={["number", "email", "tel", "url"].includes(field.type.toLowerCase()) ? field.type.toLowerCase() : "text"} value={values[field.id] ?? ""} onInput={(event) => onChange({ ...values, [field.id]: event.currentTarget.value })}></wa-input>
          ))}
        </div>}
        {!incident.fields?.length && <span class="incident-details-empty">Для этого типа дополнительные поля не предусмотрены.</span>}
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
