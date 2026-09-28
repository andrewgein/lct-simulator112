import CallControls from "./CallControls.jsx";

const styles = `
wa-page:has(.level-app)::part(main-content) { padding: 0; background: var(--app-dispatch-workspace); }
wa-page:has(.level-app) > main { padding: 0; }
.level-app { min-height: 100dvh; background: var(--app-dispatch-workspace); }
.level-command-bar { display: grid; grid-template-columns: minmax(28rem, 1fr) minmax(38rem, 0.9fr); min-height: 8.5rem; background: var(--wa-color-surface-default); border-block-end: var(--wa-border-width-s) solid var(--app-dispatch-border); }
.level-command-bar--without-connection { grid-template-columns: minmax(28rem, 1fr) max-content; }
.level-search { justify-content: center; padding: var(--wa-space-l); }
.level-search wa-input { width: 100%; --wa-form-control-border-color: var(--wa-color-neutral-10); --wa-form-control-border-radius: 0; --wa-form-control-background-color: transparent; }
.level-search wa-input::part(input-wrapper) { border-width: 0 0 var(--wa-border-width-s); }
.level-search wa-input::part(input) { padding-inline: 0; font-size: var(--wa-font-size-2xl); }
.level-operator-status { display: grid; grid-template-columns: minmax(12rem, 1fr) clamp(16rem, 27vw, 22rem) minmax(8rem, auto); min-width: 0; background: var(--wa-color-neutral-05); color: var(--wa-color-neutral-on-loud); }
.level-operator-status--without-connection { grid-template-columns: max-content minmax(8rem, auto); }
.level-operator-status--without-connection > time { grid-column: 2; }
.level-current-date, .level-connection, .level-operator-status > time { display: flex; padding: var(--wa-space-m); border-inline-start: var(--wa-border-width-s) solid var(--app-dispatch-border); }
.level-current-date { min-width: 0; }
.level-current-date strong { overflow: hidden; text-overflow: ellipsis; text-transform: capitalize; white-space: nowrap; }
.level-current-date span { color: var(--app-dispatch-text-muted); font-size: var(--wa-font-size-s); }
.level-exit-button { align-self: flex-start; --wa-color-neutral-on-quiet: var(--app-dispatch-text); --wa-color-neutral-on-normal: var(--wa-color-neutral-on-loud); --wa-color-neutral-border-normal: var(--app-dispatch-border); }
.level-connection { color: var(--app-dispatch-text-muted); font-size: var(--wa-font-size-s); }
.level-connection wa-icon { font-size: var(--wa-font-size-xl); }
.level-operator-status time { align-items: center; justify-content: center; font-size: var(--wa-font-size-3xl); font-weight: var(--wa-font-weight-bold); font-variant-numeric: tabular-nums; }
.level-operator-status time small { align-self: flex-start; margin-block-start: var(--wa-space-s); font-size: var(--wa-font-size-m); }
@media (max-width: 70rem) { .level-command-bar { grid-template-columns: 1fr; } .level-operator-status { min-height: 6rem; } }
@media (max-width: 40rem) { .level-command-bar { min-height: auto; } .level-search { padding: var(--wa-space-m); } .level-search wa-input::part(input) { font-size: var(--wa-font-size-xl); } .level-operator-status { grid-template-columns: 1fr auto; } .level-current-date { display: none; } }
`;

export default function LevelCommandBar({ call, now, onAccept, onRestart, onDrop, exitHref, modeLabel = "Учебный режим · АРМ оператора", idleLabel = "Ожидание вызова", idleIcon = "headset", showIdleStatus = true, children }) {
  const hasCall = ["incoming", "active"].includes(call.phase);
  const exitLevel = () => {
    if (window.confirm("Выйти из уровня? Текущий прогресс диалога будет потерян.")) window.location.href = exitHref;
  };
  return (
    <>
      <style>{styles}</style>
      <div class={`level-command-bar${!hasCall && !showIdleStatus ? " level-command-bar--without-connection" : ""}`}>
        <div class="level-search wa-stack wa-gap-xs">
          {children}
        </div>
        <div class={`level-operator-status${!hasCall && !showIdleStatus ? " level-operator-status--without-connection" : ""}`}>
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
          ) : showIdleStatus ? (
            <div class="level-connection wa-stack wa-gap-xs wa-align-items-center wa-justify-content-center">
              <wa-icon name={idleIcon} aria-hidden="true"></wa-icon>
              <span>{idleLabel}</span>
            </div>
          ) : null}
          <time datetime={now.toISOString()}>
            {now.toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit" })}
            <small>:{String(now.getSeconds()).padStart(2, "0")}</small>
          </time>
        </div>
      </div>
    </>
  );
}
