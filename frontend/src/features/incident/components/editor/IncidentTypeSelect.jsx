import { serviceLabels, serviceOrder } from "./editorHelpers";

export default function IncidentTypeSelect({ classifierState, id = "incident-type", name = "incident_type", value, required = false, onChange }) {
  const services = Object.entries(classifierState.classifier).sort(([first], [second]) => serviceOrder.indexOf(first) - serviceOrder.indexOf(second));
  return (
    <wa-select id={id} name={name} label="Тип происшествия" value={value} required={required} disabled={classifierState.loading} onChange={(event) => onChange(event.currentTarget.value)}>
      {classifierState.loading && <wa-option value="">Загрузка типов…</wa-option>}
      {classifierState.error && <wa-option value="">{classifierState.error}</wa-option>}
      {services.flatMap(([serviceId, incidents]) => [
        <strong key={`${serviceId}-label`}>{serviceLabels[serviceId] || serviceId}</strong>,
        ...Object.values(incidents).map((incident) => <wa-option key={incident.id} value={incident.id}>{incident.name}</wa-option>)
      ])}
    </wa-select>
  );
}
