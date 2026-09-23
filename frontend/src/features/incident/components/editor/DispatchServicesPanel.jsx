import { useEffect, useRef, useState } from "preact/hooks";

const styles = `
.dispatch-services { display: flex; flex: 1; min-width: 0; align-items: stretch; overflow-x: auto; background: #ff5b2d; color: #ffffff; }
.dispatch-services--readonly { background: #45525a; }
.dispatch-services-label, .dispatch-service { display: flex; flex: 0 0 auto; min-width: 8rem; min-height: 6rem; box-sizing: border-box; align-items: center; justify-content: center; padding: var(--wa-space-m); border-inline-end: var(--wa-border-width-s) solid rgba(255, 255, 255, .45); }
.dispatch-services-label { min-width: 7rem; font-weight: var(--wa-font-weight-bold); }
.dispatch-service { position: relative; flex-direction: column; gap: var(--wa-space-xs); }
.dispatch-service strong { max-width: 12rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dispatch-service wa-icon { font-size: var(--wa-font-size-l); }
.dispatch-service-remove { position: absolute; inset-block-start: var(--wa-space-2xs); inset-inline-end: var(--wa-space-2xs); }
.dispatch-service-remove::part(button), .dispatch-service-add::part(button) { border-color: #ffffff; color: #ffffff; }
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

export default function DispatchServicesPanel({ classifier, services = [], readonly = false, onChange }) {
  const dialogRef = useRef(null);
  const [adding, setAdding] = useState(false);
  const [draft, setDraft] = useState([]);
  const [query, setQuery] = useState("");
  const catalog = serviceCatalog(classifier);
  const byCode = new Map(catalog.map((service) => [service.code, service]));
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
        {services.map((code) => {
          const service = byCode.get(code);
          return (
            <div class="dispatch-service" key={code}>
              <wa-icon name="phone" aria-hidden="true"></wa-icon>
              <strong title={service?.name || code}>{service?.name || code}</strong>
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
