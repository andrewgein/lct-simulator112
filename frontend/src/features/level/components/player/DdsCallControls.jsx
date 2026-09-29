import ServiceLoadIndicator from "../common/ServiceLoadIndicator.jsx";

const styles = `
.dds-call-controls { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: var(--wa-space-s); min-width: 0; background: #ffffff; color: var(--wa-color-text-normal); }
.dds-call-info { display: flex; min-width: 0; align-items: center; gap: var(--wa-space-s); }
.dds-call-info > wa-icon { flex: 0 0 auto; font-size: var(--wa-font-size-xl); }
.dds-call-copy { display: flex; min-width: 0; flex-direction: column; }
.dds-call-copy strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dds-call-copy span { color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-s); }
.dds-call-actions { display: flex; align-items: center; gap: var(--wa-space-xs); }
.dds-call-time { min-width: 3rem; color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-s); font-variant-numeric: tabular-nums; text-align: center; }
.dds-call-controls[data-phase="incoming"] .dds-accept-call { animation: dds-call-buzz 1.5s ease-in-out infinite; }
@keyframes dds-call-buzz { 0%, 30%, 100% { transform: rotate(0); } 5%, 15%, 25% { transform: rotate(-8deg); } 10%, 20% { transform: rotate(8deg); } }
`;

export default function DdsCallControls({ call, load, seconds, onAccept, onDrop }) {
  const incoming = call.phase === "incoming";
  const time = `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
  return (
    <>
      <style>{styles}</style>
      <div class="dds-call-controls" aria-label="Управление вызовом" data-phase={call.phase}>
        <div class="dds-call-info">
          <wa-icon name={incoming ? "phone" : "phone-volume"} aria-hidden="true"></wa-icon>
          {!incoming && <ServiceLoadIndicator load={load} />}
          <div class="dds-call-copy"><strong>{incoming ? "Входящий вызов" : load ? "На линии" : "Ожидание собеседника"}</strong><span>{incoming ? "Принять или отклонить" : "активное соединение"}</span></div>
        </div>
        <div class="dds-call-actions">
          {!incoming && <span class="dds-call-time" aria-label={`Время звонка: ${time}`}>{time}</span>}
          {incoming && <wa-button class="dds-accept-call" type="button" variant="success" size="s" pill onClick={onAccept}><wa-icon name="phone" label="Принять вызов"></wa-icon></wa-button>}
          <wa-button type="button" variant="danger" size="s" pill onClick={onDrop}><wa-icon name="phone-slash" label={incoming ? "Отклонить вызов" : "Завершить вызов"}></wa-icon></wa-button>
        </div>
      </div>
    </>
  );
}
