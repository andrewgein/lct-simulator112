import { useEffect, useId, useRef, useState } from "preact/hooks";

const styles = `
.dispatch-services { display: flex; width: 0; min-width: 0; flex: 1 1 0; align-items: stretch; overflow-x: auto; overflow-y: hidden; background: #ff5b2d; color: #ffffff; }
.dispatch-services--readonly { background: #45525a; }
.dispatch-services-label, .dispatch-service { display: flex; flex: 0 0 auto; min-width: 8rem; min-height: 6rem; box-sizing: border-box; align-items: center; justify-content: center; padding: var(--wa-space-m); border-inline-end: var(--wa-border-width-s) solid rgba(255, 255, 255, .45); }
.dispatch-services-label { min-width: 7rem; font-weight: var(--wa-font-weight-bold); }
.dispatch-service { position: relative; flex-direction: column; gap: var(--wa-space-3xs); }
.dispatch-service--with-status { min-width: 14rem; padding-block-start: var(--wa-space-m); }
.dispatch-service strong { max-width: 12rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dispatch-service wa-icon { font-size: var(--wa-font-size-l); }
.dispatch-service-status { display: flex; max-width: 12rem; gap: var(--wa-space-xs); overflow: hidden; color: #d4dadd; font-size: var(--wa-font-size-xs); text-overflow: ellipsis; white-space: nowrap; }
.dispatch-service-status time { flex: 0 0 auto; color: #ffffff; font-variant-numeric: tabular-nums; }
.dispatch-service-status span { overflow: hidden; text-overflow: ellipsis; }
.dispatch-service-remove { position: absolute; inset-block-start: var(--wa-space-2xs); inset-inline-end: var(--wa-space-2xs); }
.dispatch-service-history-toggle, .dispatch-service-status-edit { position: absolute; inset-block-start: var(--wa-space-3xs); }
.dispatch-service-history-toggle { inset-inline-start: 50%; transform: translateX(-50%); }
.dispatch-service-history-toggle wa-icon { font-size: var(--wa-font-size-m); transition: transform var(--wa-transition-fast); }
.dispatch-service-status-edit { inset-inline-start: calc(50% + 2rem); opacity: 0; pointer-events: none; visibility: hidden; }
.dispatch-service:has(.dispatch-service-history:state(open), .dispatch-service-status-popover:state(open)) { background: #157dbd; }
.dispatch-service:has(.dispatch-service-history:state(open)) .dispatch-service-history-toggle wa-icon { transform: rotate(180deg); }
.dispatch-service:has(.dispatch-service-history:state(open), .dispatch-service-status-popover:state(open)) .dispatch-service-status-edit { opacity: 1; pointer-events: auto; visibility: visible; }
.dispatch-service-history-toggle::part(button), .dispatch-service-status-edit::part(button) { width: 1.5rem; min-width: 1.5rem; height: 1.5rem; padding: 0; border-color: transparent; color: #ffffff; }
.dispatch-service-history-toggle::part(button):hover, .dispatch-service-status-edit::part(button):hover { border-color: rgba(255, 255, 255, .55); background: rgba(255, 255, 255, .1); }
.dispatch-service-remove::part(button), .dispatch-service-add::part(button) { border-color: #ffffff; color: #ffffff; }
.dispatch-service-history { --max-width: min(48rem, calc(100vw - 2rem)); }
.dispatch-service-history::part(body) { padding: 0; border: var(--wa-border-width-s) solid #71858f; background: #157dbd; color: #ffffff; box-shadow: 0 .5rem 1.5rem rgba(22, 31, 36, .3); }
.dispatch-service-history-panel { width: min(46rem, calc(100vw - 4rem)); }
.dispatch-service-history-header { display: flex; align-items: center; justify-content: space-between; gap: var(--wa-space-m); padding: var(--wa-space-m); border-block-end: var(--wa-border-width-s) solid rgba(255, 255, 255, .35); }
.dispatch-service-history-header strong { overflow: hidden; font-size: var(--wa-font-size-l); text-overflow: ellipsis; white-space: nowrap; }
.dispatch-service-history-close::part(button) { border-color: transparent; color: #ffffff; }
.dispatch-service-history-close::part(button):hover { background: rgba(255, 255, 255, .1); }
.dispatch-service-history-list { margin: 0; padding: var(--wa-space-s) var(--wa-space-m) var(--wa-space-m); list-style: none; }
.dispatch-service-history-item { display: grid; grid-template-columns: auto auto minmax(0, 1fr); align-items: baseline; gap: var(--wa-space-s); padding-block: var(--wa-space-xs); }
.dispatch-service-history-item wa-icon { color: #c5e5f6; font-size: var(--wa-font-size-s); }
.dispatch-service-history-item time { color: #e8f4fb; font-size: var(--wa-font-size-s); font-variant-numeric: tabular-nums; white-space: nowrap; }
.dispatch-service-history-copy { display: flex; min-width: 0; flex-direction: column; gap: var(--wa-space-3xs); }
.dispatch-service-history-copy span { overflow-wrap: anywhere; }
.dispatch-service-history-copy small { color: #e8f4fb; }
.dispatch-service-history-empty { margin: 0; padding: var(--wa-space-l) var(--wa-space-m); color: #e8f4fb; }
.dispatch-service-status-popover { --max-width: min(46rem, calc(100vw - 2rem)); }
.dispatch-service-status-popover::part(body) { padding: var(--wa-space-m); border: var(--wa-border-width-s) solid #87969d; background: #f4f6f6; color: var(--wa-color-text-normal); box-shadow: 0 .5rem 1.5rem rgba(22, 31, 36, .3); }
.dispatch-service-add { align-self: center; margin-inline: var(--wa-space-m); }
.dispatch-services-dialog { --width: min(90vw, 38rem); }
.dispatch-services-dialog-search { margin-block-end: var(--wa-space-l); }
.dispatch-services-dialog-list { max-height: 24rem; overflow-y: auto; border: var(--wa-border-width-s) solid var(--wa-color-neutral-border-normal); }
.dispatch-services-dialog-option { display: flex; align-items: center; min-height: 3.75rem; padding: var(--wa-space-s) var(--wa-space-m); border-block-end: var(--wa-border-width-s) solid var(--wa-color-neutral-border-normal); cursor: pointer; }
.dispatch-services-dialog-option:last-child { border-block-end: 0; }
.dispatch-services-dialog-option:hover { background: var(--wa-color-neutral-fill-quiet); }
.dispatch-services-dialog-empty { padding: var(--wa-space-l); color: var(--wa-color-text-quiet); text-align: center; }
.dispatch-services-dialog-save::part(button) { border-color: #ff5b2d; color: #ff5b2d; }
`;

export function serviceCatalog(classifier) {
  return [...new Map(classifier.flatMap((category) => category.entries).flatMap((entry) => entry.primaryServices || []).map((service) => [service.code, service])).values()];
}

export function automaticServices(classifier, incidentTypes) {
  const selectedTypes = new Set(incidentTypes.filter(Boolean));
  return [...new Set(classifier.flatMap((category) => category.entries).filter((entry) => selectedTypes.has(entry.code)).flatMap((entry) => entry.primaryServices || []).map((service) => service.code))];
}

export default function DispatchServicesPanel({ classifier, dispatchServices = [], services = [], readonly = false, status, statusHistory = [], statusEditor, onChange }) {
  const dialogRef = useRef(null);
  const historyId = `dispatch-service-history-${useId().replace(/:/g, "")}`;
  const [adding, setAdding] = useState(false);
  const [draft, setDraft] = useState([]);
  const [query, setQuery] = useState("");
  const catalog = [...new Map([...serviceCatalog(classifier), ...dispatchServices].map((service) => [service.code, service])).values()];
  const byCode = new Map(catalog.map((service) => [service.code, service]));
  const latestStatus = statusHistory.at(-1);
  const filtered = catalog.filter((service) => `${service.name} ${service.code}`.toLocaleLowerCase("ru").includes(query.trim().toLocaleLowerCase("ru")));

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    dialog.open = adding;
    const close = () => setAdding(false);
    dialog.addEventListener("wa-after-hide", close);
    return () => dialog.removeEventListener("wa-after-hide", close);
  }, [adding]);

  const openDialog = () => {
    setDraft(services);
    setQuery("");
    setAdding(true);
  };
  const toggle = (code) => setDraft((current) => current.includes(code) ? current.filter((value) => value !== code) : [...current, code]);
  const save = () => {
    onChange(draft);
    setAdding(false);
  };

  return (
    <>
      <style>{styles}</style>
      <div class={`dispatch-services ${readonly ? "dispatch-services--readonly" : ""}`} aria-label="Назначенные службы">
        <div class="dispatch-services-label">Службы:</div>
        {services.map((code, index) => {
          const service = byCode.get(code);
          return (
            <div class={`dispatch-service ${status && index === 0 ? "dispatch-service--with-status" : ""}`} key={code}>
              <strong title={service?.name || code}>{service?.name || code}</strong>
              {status && index === 0 && <span class="dispatch-service-status" title={latestStatus ? `${latestStatus.time} ${latestStatus.label}` : status}>{latestStatus && <time dateTime={latestStatus.dateTime || undefined}>{latestStatus.time}</time>}<span>{latestStatus?.label || status}</span></span>}
              {status && index === 0 && <wa-button id={historyId} class="dispatch-service-history-toggle" type="button" size="s" appearance="plain" variant="neutral" aria-label={`История статусов службы ${service?.name || code}`}><wa-icon name="chevron-up" aria-hidden="true"></wa-icon></wa-button>}
              {status && index === 0 && <wa-popover class="dispatch-service-history" for={historyId} placement="top-start" distance={0} skidding={-100} without-arrow><section class="dispatch-service-history-panel" aria-label={`История статусов службы ${service?.name || code}`}><div class="dispatch-service-history-header"><strong>{service?.name || code}</strong><wa-button class="dispatch-service-history-close" type="button" size="s" appearance="plain" variant="neutral" data-popover="close" aria-label="Закрыть историю статусов"><wa-icon name="xmark" aria-hidden="true"></wa-icon></wa-button></div>{statusHistory.length ? <ol class="dispatch-service-history-list">{statusHistory.map((item, historyIndex) => <li class="dispatch-service-history-item" key={`${item.time}-${historyIndex}`}><wa-icon name="chevron-right" aria-hidden="true"></wa-icon><time dateTime={item.dateTime || undefined}>{item.time}</time><div class="dispatch-service-history-copy"><span>{item.label}</span>{item.comment && <small>{item.comment}</small>}</div></li>)}</ol> : <p class="dispatch-service-history-empty">Изменений статуса пока нет</p>}</section></wa-popover>}
              {status && index === 0 && statusEditor && <wa-button id={`dispatch-service-status-${code}`} class="dispatch-service-status-edit" type="button" size="s" appearance="plain" variant="neutral" aria-label={`Изменить статус службы ${service?.name || code}`}><wa-icon name="pencil" aria-hidden="true"></wa-icon></wa-button>}
              {status && index === 0 && statusEditor && <wa-popover class="dispatch-service-status-popover" for={`dispatch-service-status-${code}`} placement="top-start">{statusEditor}</wa-popover>}
              {!readonly && <wa-button class="dispatch-service-remove" type="button" size="xs" appearance="plain" variant="neutral" aria-label={`Удалить службу ${service?.name || code}`} onClick={() => onChange(services.filter((value) => value !== code))}><wa-icon name="xmark"></wa-icon></wa-button>}
            </div>
          );
        })}
        {!readonly && !!catalog.length && <wa-button class="dispatch-service-add" type="button" size="l" appearance="outlined" variant="neutral" aria-label="Добавить службу" onClick={openDialog}><wa-icon name="plus"></wa-icon></wa-button>}
      </div>
      <wa-dialog ref={dialogRef} class="dispatch-services-dialog" label="Добавьте службы" with-footer>
        <wa-input class="dispatch-services-dialog-search" placeholder="Поиск ..." aria-label="Поиск службы" value={query} onInput={(event) => setQuery(event.currentTarget.value)}><wa-icon slot="start" name="magnifying-glass" aria-hidden="true"></wa-icon></wa-input>
        <div class="dispatch-services-dialog-list">
          {filtered.map((service) => <label class="dispatch-services-dialog-option" key={service.code}><wa-checkbox checked={draft.includes(service.code)} onChange={() => toggle(service.code)}>{service.name}</wa-checkbox></label>)}
          {!filtered.length && <div class="dispatch-services-dialog-empty">Службы не найдены</div>}
        </div>
        <wa-button class="dispatch-services-dialog-save" slot="footer" type="button" size="l" appearance="outlined" variant="neutral" onClick={save}>Сохранить и закрыть</wa-button>
      </wa-dialog>
    </>
  );
}
