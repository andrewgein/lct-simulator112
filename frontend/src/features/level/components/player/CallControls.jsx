import { useEffect, useState } from "preact/hooks";

const styles = `
  [data-phase="incoming"] .accept-call { animation: call-buzz 1.5s ease-in-out infinite; }
  .call-controls-compact { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: var(--wa-space-s); box-sizing: border-box; width: 100%; min-width: 0; padding: var(--wa-space-s) var(--wa-space-m); border-inline-start: var(--wa-border-width-s) solid var(--app-dispatch-border); color: var(--wa-color-neutral-on-loud); }
  .compact-call-info { min-width: 0; }
  .compact-call-info strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-variant-numeric: tabular-nums; }
  .compact-call-status { color: var(--app-dispatch-text-muted); font-size: var(--wa-font-size-s); }
  .compact-call-actions { width: 9.5rem; }
  .compact-call-time { min-width: 3rem; color: var(--app-dispatch-text-secondary); font-size: var(--wa-font-size-s); font-variant-numeric: tabular-nums; text-align: center; }
  [data-phase="incoming"] .compact-call-time { visibility: hidden; }
  @keyframes call-buzz { 0%, 30%, 100% { transform: rotate(0); } 5%, 15%, 25% { transform: rotate(-8deg); } 10%, 20% { transform: rotate(8deg); } }
`;

const statuses = { incoming: "Входящий вызов", active: "На линии" };

function formatTime(seconds) {
  return `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
}

function formatPhone(phone) {
  const digits = phone?.replace(/\D/g, "") || "";
  if (digits.length === 11 && /^[78]/.test(digits)) return `+7 (${digits.slice(1, 4)}) ${digits.slice(4, 7)}-${digits.slice(7, 9)}-${digits.slice(9)}`;
  return phone || "Номер не определён";
}

export default function CallControls({ call, onAccept, onDrop }) {
  const [seconds, setSeconds] = useState(0);

  useEffect(() => {
    if (call.phase !== "active") {
      setSeconds(0);
      return;
    }
    const timer = window.setInterval(() => setSeconds((value) => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [call.phase]);

  const phone = formatPhone(call.phone);
  const time = formatTime(seconds);
  const status = statuses[call.phase];
  if (!status) return null;
  return (
    <>
      <style>{styles}</style>
      <div class="call-controls-compact" data-phase={call.phase} aria-label="Управление вызовом">
        <div class="compact-call-info wa-stack wa-gap-2xs"><strong>{phone}</strong><span class="compact-call-status">{status}</span></div>
        <div class="compact-call-actions wa-cluster wa-gap-xs wa-flex-nowrap wa-justify-content-end">
          <span class="compact-call-time" aria-hidden={call.phase !== "active"} aria-label={call.phase === "active" ? `Время звонка: ${time}` : undefined}>{time}</span>
          <wa-button class="accept-call" type="button" variant="success" size="s" pill disabled={call.phase !== "incoming"} onClick={onAccept}><wa-icon name="phone" label="Принять вызов"></wa-icon></wa-button>
          <wa-button type="button" variant="danger" size="s" pill disabled={call.phase !== "active"} onClick={onDrop}><wa-icon name="phone-slash" label="Завершить вызов"></wa-icon></wa-button>
        </div>
      </div>
    </>
  );
}
