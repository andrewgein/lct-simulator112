export default function VictimStatusBar({ victimCount, readonly = false, onChange }) {
  const hasVictims = Number(victimCount) > 0;
  return (
    <div class="workspace-victim-status">
      <span class="workspace-victim-status-label">Пострадавшие:</span>
      {readonly ? <span class="workspace-victim-readonly">{hasVictims ? `Есть · ${victimCount}` : "Нет"}</span> : <><button class={`workspace-victim-button ${victimCount === 0 ? "workspace-victim-button--selected" : ""}`} type="button" onClick={() => onChange(0)}>Нет</button><button class={`workspace-victim-button ${hasVictims ? "workspace-victim-button--selected" : ""}`} type="button" onClick={() => onChange(Math.max(1, Number(victimCount) || 1))}>Есть</button>{hasVictims && <input class="workspace-victim-count" type="number" min="1" step="1" aria-label="Количество пострадавших" value={victimCount} onInput={(event) => onChange(Math.max(1, Number(event.currentTarget.value) || 1))} />}</>}
    </div>
  );
}
