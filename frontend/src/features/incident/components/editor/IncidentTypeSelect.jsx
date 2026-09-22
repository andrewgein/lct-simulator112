export default function IncidentTypeSelect({ classifierState, id = "incident-type", name = "incident_type", value, excludedValues = [], required = false, onChange }) {
  return (
    <wa-select id={id} name={name} label="Тип происшествия" value={value} required={required} disabled={classifierState.loading} onChange={(event) => onChange(event.currentTarget.value)}>
      {classifierState.loading && <wa-option value="">Загрузка типов…</wa-option>}
      {classifierState.error && <wa-option value="">{classifierState.error}</wa-option>}
      {classifierState.classifier.flatMap((category) => category.entries.filter((entry) => entry.code === value || !excludedValues.includes(entry.code)).map((entry) => <wa-option key={entry.code} value={entry.code}>{category.name}: {entry.finalName}</wa-option>))}
    </wa-select>
  );
}
