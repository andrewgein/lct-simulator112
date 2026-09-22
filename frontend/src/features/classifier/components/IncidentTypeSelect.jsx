import ClassifierOptions from "./ClassifierOptions.jsx";

export default function IncidentTypeSelect({ classifierState, id = "incident-type", name = "incident_type", value, excludedValues = [], required = false, onChange }) {
  const empty = !classifierState.loading && !classifierState.error && classifierState.classifier.length === 0;

  return (
    <wa-select id={id} name={name} label="Тип происшествия" placeholder="Выберите тип происшествия" value={value} required={required} disabled={classifierState.loading || !!classifierState.error || empty} onChange={(event) => onChange(event.currentTarget.value)}>
      {classifierState.loading && <wa-option value="">Загрузка типов…</wa-option>}
      {classifierState.error && <wa-option value="">{classifierState.error}</wa-option>}
      {empty && <wa-option value="">Типы происшествий не найдены</wa-option>}
      <ClassifierOptions classifier={classifierState.classifier} selectedValue={value} excludedValues={excludedValues} />
    </wa-select>
  );
}
