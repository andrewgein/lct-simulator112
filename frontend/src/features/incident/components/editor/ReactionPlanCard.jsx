import IncidentTypeSelect from "./IncidentTypeSelect.jsx";

export default function ReactionPlanCard({ classifierState, values, required = false, onChange }) {
  const incidentTypes = values.length ? values : [""];
  const entryCount = classifierState.classifier.reduce((count, category) => count + category.entries.length, 0);
  const changeType = (index, value) => onChange(incidentTypes.map((item, itemIndex) => itemIndex === index ? value : item));
  const removeType = (index) => onChange(incidentTypes.filter((_, itemIndex) => itemIndex !== index));
  const canAdd = incidentTypes.every(Boolean) && incidentTypes.length < entryCount;
  return (
    <wa-card style="flex: 1">
      <div class="wa-stack">
        <strong class="wa-heading-l">План реагирования</strong>
        {incidentTypes.map((value, index) => (
          <div class="incident-type-row wa-cluster wa-gap-xs wa-align-items-end wa-flex-nowrap" key={index}>
            <IncidentTypeSelect classifierState={classifierState} id={`incident-type-${index}`} name={null} value={value} excludedValues={incidentTypes.filter((_, itemIndex) => itemIndex !== index)} required={required} onChange={(nextValue) => changeType(index, nextValue)} />
            {index > 0 && <wa-button type="button" appearance="outlined" variant="danger" aria-label="Удалить тип происшествия" onClick={() => removeType(index)}><wa-icon name="trash" aria-hidden="true"></wa-icon></wa-button>}
          </div>
        ))}
        {canAdd && <wa-button type="button" appearance="plain" variant="brand" onClick={() => onChange([...incidentTypes, ""])}><wa-icon slot="start" name="plus" aria-hidden="true"></wa-icon>Добавить тип</wa-button>}
      </div>
    </wa-card>
  );
}
