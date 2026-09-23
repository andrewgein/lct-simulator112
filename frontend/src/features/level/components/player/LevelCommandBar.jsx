import CallControls from "./CallControls.jsx";

export default function LevelCommandBar({ call, now, onAccept, onDrop, modeLabel = "Учебный режим · АРМ оператора", idleLabel = "Ожидание вызова", idleIcon = "headset", children }) {
  const hasCall = ["incoming", "active"].includes(call.phase);
  return (
    <div class="level-command-bar">
      <div class="level-search wa-stack wa-gap-xs">
        {children}
      </div>
      <div class="level-operator-status">
        <div class="level-current-date wa-stack wa-gap-xs wa-justify-content-center">
          <strong>{now.toLocaleDateString("ru-RU", { weekday: "long", day: "numeric", month: "long", year: "numeric" })}</strong>
          <span>{modeLabel}</span>
        </div>
        {hasCall ? (
          <CallControls call={call} onAccept={onAccept} onDrop={onDrop} />
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
