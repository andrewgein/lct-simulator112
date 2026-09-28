import ClassifierOptions from "./ClassifierOptions.jsx";

export default function IncidentTypeSelect({ classifierState, id = "incident-type", name = "incident_type", value, excludedValues = [], required = false, label = "Тип происшествия", onChange, onInput }) {
  const empty = !classifierState.loading && !classifierState.error && classifierState.classifier.length === 0;

  return (
    <wa-select id={id} name={name} label={label || undefined} aria-label={!label ? "Тип происшествия" : undefined} placeholder="Выберите тип происшествия" value={value} required={required} disabled={classifierState.loading || !!classifierState.error || empty} onChange={(event) => onChange(event.currentTarget.value)} onInput={onInput ? (event) => onInput(event.currentTarget.value) : undefined}>
      {classifierState.loading && <wa-option value="">Загрузка типов…</wa-option>}
      {classifierState.error && <wa-option value="">{classifierState.error}</wa-option>}
      {empty && <wa-option value="">Типы происшествий не найдены</wa-option>}
      <ClassifierOptions classifier={classifierState.classifier} selectedValue={value} excludedValues={excludedValues} />
    </wa-select>
  );
}
