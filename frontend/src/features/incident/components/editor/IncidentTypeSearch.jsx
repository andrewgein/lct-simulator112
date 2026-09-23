import { useMemo, useState } from "preact/hooks";

const styles = `
.incident-type-search { position: relative; z-index: 5; background: #f4f6f6; }
.incident-type-search-input { box-sizing: border-box; width: 100%; padding: var(--wa-space-m); border: 0; border-block-end: var(--wa-border-width-s) solid #b8c1c5; outline: 0; background: transparent; color: #35434a; font: inherit; font-size: var(--wa-font-size-xl); }
.incident-type-search-input::placeholder { color: #74858d; }
.incident-type-search-input:focus { border-block-end: 2px solid #008dca; }
.incident-type-results { position: absolute; z-index: 10; inset-inline: 0; inset-block-start: 100%; height: min(24rem, 50vh); overflow-y: auto; border: var(--wa-border-width-s) solid #9ba8ae; background: #ffffff; box-shadow: var(--wa-shadow-l); }
.incident-type-result { display: flex; width: 100%; align-items: flex-start; justify-content: flex-start; flex-direction: column; gap: var(--wa-space-3xs); padding: var(--wa-space-s) var(--wa-space-m); border: 0; border-block-end: var(--wa-border-width-s) solid #d4dadd; background: #ffffff; color: #26343b; font: inherit; text-align: left !important; cursor: pointer; }
.incident-type-result strong, .incident-type-result span { width: 100%; text-align: left; }
.incident-type-result:hover, .incident-type-result:focus { background: #e5f4fa; outline: 0; }
.incident-type-result span { color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-s); }
.incident-type-search-message { padding: var(--wa-space-m); color: var(--wa-color-text-quiet); }
.incident-type-chips { display: flex; min-height: 4.5rem; box-sizing: border-box; align-items: center; gap: var(--wa-space-s); overflow-x: auto; padding: var(--wa-space-s) var(--wa-space-m); border-block-end: .5rem solid #c8d1d5; background: #f4f6f6; }
.incident-type-chip { flex: 0 0 auto; padding: var(--wa-space-xs) var(--wa-space-m); border: var(--wa-border-width-s) solid #9ba8ae; background: #ffffff; color: #26343b; font: inherit; cursor: pointer; }
`;

function searchableText(entry) {
  return [entry.code, entry.finalName, entry.categoryName, entry.feature1Name, entry.feature2Name, entry.feature3Name, entry.additionalFeatures].filter(Boolean).join(" ").toLocaleLowerCase("ru");
}

export default function IncidentTypeSearch({ classifierState, selectedCodes, onAdd }) {
  const [query, setQuery] = useState("");
  const [focused, setFocused] = useState(false);
  const entries = useMemo(() => classifierState.classifier.flatMap((category) => category.entries), [classifierState.classifier]);
  const selected = new Set(selectedCodes);
  const normalizedQuery = query.trim().toLocaleLowerCase("ru");
  const results = entries.filter((entry) => !selected.has(entry.code) && (!normalizedQuery || searchableText(entry).includes(normalizedQuery)));
  const add = (code) => {
    onAdd(code);
    setQuery("");
    setFocused(false);
  };
  const onKeyDown = (event) => {
    if (event.key === "Enter" && results.length) {
      event.preventDefault();
      add(results[0].code);
    }
    if (event.key === "Escape") setFocused(false);
  };
  return (
    <>
      <style>{styles}</style>
      <div class="incident-type-search">
        <input class="incident-type-search-input" type="search" aria-label="Добавить тип происшествия" placeholder="Добавить тип происшествия" value={query} disabled={classifierState.loading || !!classifierState.error} onFocus={() => setFocused(true)} onInput={(event) => setQuery(event.currentTarget.value)} onKeyDown={onKeyDown} onBlur={() => window.setTimeout(() => setFocused(false), 150)} />
        {focused && <div class="incident-type-results">{classifierState.loading ? <div class="incident-type-search-message">Загрузка классификатора…</div> : classifierState.error ? <div class="incident-type-search-message">{classifierState.error}</div> : results.length ? results.map((entry) => <button class="incident-type-result" type="button" key={entry.code} onMouseDown={(event) => event.preventDefault()} onClick={() => add(entry.code)}><strong>{entry.finalName}</strong><span>{entry.code} · {entry.categoryName}</span></button>) : <div class="incident-type-search-message">Подходящие типы не найдены</div>}</div>}
      </div>
      {!!selectedCodes.length && <div class="incident-type-chips">{selectedCodes.map((code) => { const entry = entries.find((item) => item.code === code); return <button class="incident-type-chip" type="button" key={code} onClick={() => document.getElementById(`incident-panel-${code}`)?.scrollIntoView({ behavior: "smooth", block: "start" })}>{entry?.finalName || code}</button>; })}</div>}
    </>
  );
}
