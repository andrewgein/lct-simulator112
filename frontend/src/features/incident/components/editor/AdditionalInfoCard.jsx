export default function AdditionalInfoCard({ incident, values, onChange }) {
  return (
    <wa-card style="flex: 1;">
      <div class="wa-stack">
        <strong class="wa-heading-l">Блок дополнительной информации</strong>
        <div class="wa-stack">
          {(incident?.fields || []).map((field) => field.type.toLowerCase() === "boolean" ? (
            <wa-checkbox key={field.id} name={field.id} required={field.required} checked={values[field.id] === true || values[field.id] === "true"} onChange={(event) => onChange({ ...values, [field.id]: String(event.currentTarget.checked) })}>{field.name}</wa-checkbox>
          ) : (
            <wa-input key={field.id} name={field.id} label={field.name} required={field.required} type={["number", "email", "tel", "url"].includes(field.type.toLowerCase()) ? field.type.toLowerCase() : "text"} value={values[field.id] ?? ""} onInput={(event) => onChange({ ...values, [field.id]: event.currentTarget.value })}>
            </wa-input>
          ))}
        </div>
      </div>
    </wa-card>
  );
}
