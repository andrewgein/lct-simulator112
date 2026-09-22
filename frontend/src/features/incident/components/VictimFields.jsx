export default function VictimFields({ victimCount, required = false, onChange }) {
  const present = victimCount == null ? "" : victimCount > 0 ? "yes" : "no";
  const changePresent = (event) => onChange(event.currentTarget.value === "yes" ? Math.max(1, Number(victimCount) || 1) : 0);
  const changeCount = (event) => onChange(Math.max(1, Number(event.currentTarget.value) || 1));
  return (
    <div class="victim-fields wa-stack wa-gap-s">
      <wa-select label="Пострадавшие" value={present} required={required} onChange={changePresent}>
        <wa-option value="yes">Есть</wa-option>
        <wa-option value="no">Нет</wa-option>
      </wa-select>
      {present === "yes" && <wa-input type="number" min="1" step="1" label="Количество пострадавших" value={victimCount} required={required} onInput={changeCount}></wa-input>}
    </div>
  );
}
