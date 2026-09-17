import IncidentTypeSelect from "./IncidentTypeSelect.jsx";

export default function ReactionPlanCard({ classifierState, value, required = false, onChange }) {
  return (
    <wa-card style="flex: 1">
      <div class="wa-stack">
        <strong class="wa-heading-l">План реагирования</strong>
        <IncidentTypeSelect classifierState={classifierState} value={value} required={required} onChange={onChange} />
      </div>
    </wa-card>
  );
}
