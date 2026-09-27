import CallControls from "./CallControls.jsx";

export default function LevelCommandBar({ call, now, onAccept, onRestart, onDrop, exitHref, modeLabel = "Учебный режим · АРМ оператора", idleLabel = "Ожидание вызова", idleIcon = "headset", children }) {
  const hasCall = ["incoming", "active"].includes(call.phase);
  const exitLevel = () => {
    if (window.confirm("Выйти из уровня? Текущий прогресс диалога будет потерян.")) window.location.href = exitHref;
  };
  return (
    <div class="level-command-bar">
      <div class="level-search wa-stack wa-gap-xs">
        {children}
      </div>
      <div class="level-operator-status">
        <div class="level-current-date wa-stack wa-gap-xs wa-justify-content-center">
          <strong>{now.toLocaleDateString("ru-RU", { weekday: "long", day: "numeric", month: "long", year: "numeric" })}</strong>
          <span>{modeLabel}</span>
          {exitHref && (
            <wa-button class="level-exit-button" type="button" appearance="outlined" variant="neutral" size="s" onClick={exitLevel}>
              <wa-icon slot="start" name="right-from-bracket" aria-hidden="true"></wa-icon>
              Выйти из уровня
            </wa-button>
          )}
        </div>
        {hasCall ? (
          <CallControls call={call} onAccept={onAccept} onRestart={onRestart} onDrop={onDrop} />
        ) : (
          <div class="level-connection wa-stack wa-gap-xs wa-align-items-center wa-justify-content-center">
            <wa-icon name={idleIcon} aria-hidden="true"></wa-icon>
            <span>{idleLabel}</span>
          </div>
        )}
        <time datetime={now.toISOString()}>
          {now.toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit" })}
          <small>:{String(now.getSeconds()).padStart(2, "0")}</small>
        </time>
      </div>
    </div>
  );
}
