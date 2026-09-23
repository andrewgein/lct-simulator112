import { useMemo, useState } from "preact/hooks";

const featureProperties = ["feature1Name", "feature2Name", "feature3Name"];
const featureLabels = ["Признак 1", "Признак 2", "Признак 3"];

const styles = `
.incident-classifier { position: relative; z-index: 5; padding: var(--wa-space-m); border-block-end: .5rem solid #c8d1d5; background: #f4f6f6; }
.incident-classifier-label, .incident-classifier-caption { display: block; color: #687880; font-size: var(--wa-font-size-s); }
.incident-classifier-input { box-sizing: border-box; width: 100%; margin-block-start: var(--wa-space-2xs); padding: var(--wa-space-xs) 0; border: 0; border-block-end: 2px solid #aeb8bd; outline: 0; background: transparent; color: #26343b; font: inherit; font-size: var(--wa-font-size-2xl); }
.incident-classifier-input::placeholder { color: #526169; opacity: 1; }
.incident-classifier-input:focus { border-block-end-color: #008dca; }
.incident-classifier-results { position: absolute; z-index: 20; inset-inline: var(--wa-space-m); inset-block-start: 5.4rem; max-height: min(26rem, 55vh); overflow-y: auto; border: var(--wa-border-width-s) solid #9ba8ae; background: #ffffff; box-shadow: var(--wa-shadow-l); }
.incident-classifier-result { display: flex; width: 100%; flex-direction: column; gap: var(--wa-space-3xs); padding: var(--wa-space-s) var(--wa-space-m); border: 0; border-block-end: var(--wa-border-width-s) solid #d4dadd; background: #ffffff; color: #26343b; font: inherit; text-align: left; cursor: pointer; }
.incident-classifier-result:hover, .incident-classifier-result:focus { background: #e5f4fa; outline: 0; }
.incident-classifier-result span { color: #687880; font-size: var(--wa-font-size-s); }
.incident-classifier-message { padding: var(--wa-space-m); color: #687880; }
.incident-classifier-caption { margin-block: var(--wa-space-m) var(--wa-space-s); }
.incident-classifier-options { display: flex; flex-wrap: wrap; gap: var(--wa-space-s); }
.incident-classifier-option { min-height: 2.8rem; padding: var(--wa-space-xs) var(--wa-space-m); border: var(--wa-border-width-s) solid #9ba8ae; background: #ffffff; color: #26343b; font: inherit; font-weight: var(--wa-font-weight-semibold); text-align: left; cursor: pointer; }
.incident-classifier-option:hover, .incident-classifier-option:focus { border-color: #008dca; background: #edf8fc; outline: 0; }
.incident-classifier-navigation { display: flex; align-items: center; gap: var(--wa-space-s); margin-block-start: var(--wa-space-m); }
.incident-classifier-back { display: grid; width: 2.5rem; height: 2.5rem; flex: 0 0 auto; place-items: center; border: var(--wa-border-width-s) solid #9ba8ae; background: #ffffff; color: #35434a; cursor: pointer; }
.incident-classifier-path { display: flex; min-width: 0; flex-wrap: wrap; gap: var(--wa-space-2xs); color: #526169; }
.incident-classifier-path strong { color: #26343b; }
.incident-type-chips { display: flex; min-height: 4.5rem; box-sizing: border-box; align-items: center; gap: var(--wa-space-s); overflow-x: auto; padding: var(--wa-space-s) var(--wa-space-m); border-block-end: .5rem solid #c8d1d5; background: #f4f6f6; }
.incident-type-chip { flex: 0 0 auto; padding: var(--wa-space-xs) var(--wa-space-m); border: var(--wa-border-width-s) solid #9ba8ae; background: #ffffff; color: #26343b; font: inherit; cursor: pointer; }
`;

function searchableText(entry) {
  return [entry.code, entry.finalName, entry.categoryName, entry.feature1Name, entry.feature2Name, entry.feature3Name, entry.additionalFeatures].filter(Boolean).join(" ").toLocaleLowerCase("ru");
}

function uniqueValues(entries, property) {
  return [...new Set(entries.map((entry) => entry[property]?.trim()).filter(Boolean))];
}

function matchingEntries(category, path) {
  return category.entries.filter((entry) => path.every((value, index) => entry[featureProperties[index]] === value));
}

export default function IncidentTypeSearch({ classifierState, selectedCodes, onAdd }) {
  const [query, setQuery] = useState("");
  const [categoryCode, setCategoryCode] = useState("");
  const [path, setPath] = useState([]);
  const entries = useMemo(() => classifierState.classifier.flatMap((category) => category.entries), [classifierState.classifier]);
  const selected = new Set(selectedCodes);
  const category = classifierState.classifier.find((item) => item.code === categoryCode);
  const candidates = category ? matchingEntries(category, path) : [];
  const nextLevel = path.length;
  const options = nextLevel < featureProperties.length ? uniqueValues(candidates, featureProperties[nextLevel]) : [];
  const availableResults = candidates.filter((entry) => !selected.has(entry.code));
  const terminalResults = nextLevel < featureProperties.length ? availableResults.filter((entry) => !entry[featureProperties[nextLevel]]) : availableResults;
  const normalizedQuery = query.trim().toLocaleLowerCase("ru");
  const searchResults = normalizedQuery ? entries.filter((entry) => !selected.has(entry.code) && searchableText(entry).includes(normalizedQuery)).slice(0, 80) : [];
  const reset = () => {
    setCategoryCode("");
    setPath([]);
  };
  const add = (code) => {
    onAdd(code);
    setQuery("");
    reset();
  };
  const back = () => path.length ? setPath(path.slice(0, -1)) : reset();
  const onKeyDown = (event) => {
    if (event.key === "Enter" && searchResults.length) {
      event.preventDefault();
      add(searchResults[0].code);
    }
    if (event.key === "Escape") setQuery("");
  };
  const showFinalTypes = !!category && (!options.length || candidates.length === 1);
  return (
    <>
      <style>{styles}</style>
      <div class="incident-classifier">
        <label class="incident-classifier-label" for="incident-classifier-search">Введите тип происшествия</label>
        <input id="incident-classifier-search" class="incident-classifier-input" type="search" placeholder="Что случилось?" value={query} disabled={classifierState.loading || !!classifierState.error} onInput={(event) => setQuery(event.currentTarget.value)} onKeyDown={onKeyDown} />
        {!!normalizedQuery && <div class="incident-classifier-results">{classifierState.loading ? <div class="incident-classifier-message">Загрузка классификатора…</div> : classifierState.error ? <div class="incident-classifier-message">{classifierState.error}</div> : searchResults.length ? searchResults.map((entry) => <button class="incident-classifier-result" type="button" key={entry.code} onClick={() => add(entry.code)}><strong>{entry.finalName}</strong><span>{entry.code} · {entry.categoryName}</span></button>) : <div class="incident-classifier-message">Подходящие типы не найдены</div>}</div>}
        {!category && <div>
          <span class="incident-classifier-caption">Типы происшествий</span>
          {classifierState.loading && <div class="incident-classifier-message">Загрузка классификатора…</div>}
          {!!classifierState.error && <div class="incident-classifier-message">{classifierState.error}</div>}
          <div class="incident-classifier-options">{classifierState.classifier.map((item) => <button class="incident-classifier-option" type="button" key={item.code} onClick={() => { setCategoryCode(item.code); setPath([]); }}>{item.name}</button>)}</div>
        </div>}
        {!!category && <div>
          <div class="incident-classifier-navigation">
            <button class="incident-classifier-back" type="button" aria-label="Назад" onClick={back}><wa-icon name="arrow-left" aria-hidden="true"></wa-icon></button>
            <div class="incident-classifier-path"><strong>{category.name}</strong>{path.map((value, index) => <span key={`${index}-${value}`}>/ {value}</span>)}</div>
          </div>
          {!showFinalTypes && <span class="incident-classifier-caption">{featureLabels[nextLevel]}</span>}
          {!showFinalTypes && <div class="incident-classifier-options">{options.map((option) => <button class="incident-classifier-option" type="button" key={option} onClick={() => setPath([...path, option])}>{option}</button>)}</div>}
          {!showFinalTypes && !!terminalResults.length && <span class="incident-classifier-caption">Итоговые типы без следующего признака</span>}
          {!showFinalTypes && !!terminalResults.length && <div class="incident-classifier-options">{terminalResults.map((entry) => <button class="incident-classifier-option" type="button" key={entry.code} onClick={() => add(entry.code)}>{entry.finalName}</button>)}</div>}
          {showFinalTypes && <span class="incident-classifier-caption">Итоговый тип происшествия</span>}
          {showFinalTypes && <div class="incident-classifier-options">{availableResults.map((entry) => <button class="incident-classifier-option" type="button" key={entry.code} onClick={() => add(entry.code)}>{entry.finalName}</button>)}</div>}
          {showFinalTypes && !availableResults.length && <div class="incident-classifier-message">Все подходящие типы уже добавлены</div>}
        </div>}
      </div>
      {!!selectedCodes.length && <div class="incident-type-chips">{selectedCodes.map((code) => { const entry = entries.find((item) => item.code === code); return <button class="incident-type-chip" type="button" key={code} onClick={() => document.getElementById(`incident-panel-${code}`)?.scrollIntoView({ behavior: "smooth", block: "start" })}>{entry?.finalName || code}</button>; })}</div>}
    </>
  );
}
