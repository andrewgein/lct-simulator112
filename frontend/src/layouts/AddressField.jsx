import { useEffect, useRef, useState } from "preact/hooks";

export default function AddressField({ id, value = "", label = "Адрес", required = false, disabled = false, dadataApiKey = "", withMap = false, onChange }) {
  const [inputValue, setInputValue] = useState(value);
  const [suggestions, setSuggestions] = useState([]);
  const [focused, setFocused] = useState(false);
  const rootRef = useRef(null);
  const mapWindow = useRef(null);

  useEffect(() => setInputValue(value), [value]);

  useEffect(() => {
    if (!focused || !dadataApiKey || inputValue.trim().length < 3) {
      setSuggestions([]);
      return;
    }
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        const response = await fetch("https://suggestions.dadata.ru/suggestions/api/4_1/rs/suggest/address", {
          method: "POST",
          signal: controller.signal,
          headers: { "Content-Type": "application/json", Accept: "application/json", Authorization: `Token ${dadataApiKey}` },
          body: JSON.stringify({ query: inputValue, count: 8 })
        });
        const data = response.ok ? await response.json() : { suggestions: [] };
        setSuggestions(data.suggestions || []);
      } catch (error) {
        if (error.name !== "AbortError") console.error("DaData suggest failed:", error);
      }
    }, 300);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [inputValue, dadataApiKey, focused]);

  useEffect(() => {
    const handleMessage = (event) => {
      if (event.data?.type === "LOCATION_PICKED" && event.data.targetInputId === id) updateValue(event.data.address);
    };
    window.addEventListener("message", handleMessage);
    return () => window.removeEventListener("message", handleMessage);
  }, [id, onChange]);

  const emit = (name, detail) => rootRef.current?.dispatchEvent(new CustomEvent(name, { bubbles: true, detail }));
  const updateValue = (nextValue) => {
    setInputValue(nextValue);
    onChange?.(nextValue);
    emit("address-input", { value: nextValue });
  };
  const chooseSuggestion = (suggestion) => {
    setFocused(false);
    setSuggestions([]);
    updateValue(suggestion.value);
    emit("address-selected", { suggestion });
  };
  const openMap = () => {
    if (mapWindow.current && !mapWindow.current.closed) return mapWindow.current.focus();
    mapWindow.current = window.open(`/map?target=${encodeURIComponent(id)}`, `MapPicker_${id}`, "width=800,height=600");
  };

  return (
    <div ref={rootRef} id={id} class="address-field">
      <label for={`${id}-input`}>{label}{required ? " *" : ""}</label>
      <div class="wa-flank:end wa-gap-xs">
        <input id={`${id}-input`} value={inputValue} required={required} disabled={disabled} autocomplete="off" onFocus={() => setFocused(true)} onBlur={() => setFocused(false)} onInput={(event) => { setFocused(true); updateValue(event.currentTarget.value); }} />
        {withMap && (
          <wa-button type="button" appearance="outlined" variant="neutral" size="s" aria-label="Выбрать адрес на карте" disabled={disabled} onClick={openMap}>
            <wa-icon name="location-dot"></wa-icon>
          </wa-button>
        )}
      </div>
      {focused && suggestions.length > 0 && (
        <div class="address-field-suggestions">
          {suggestions.map((suggestion) => <button key={suggestion.value} type="button" onMouseDown={(event) => event.preventDefault()} onClick={() => chooseSuggestion(suggestion)}>{suggestion.value}</button>)}
        </div>
      )}
      <style>{`
        .address-field { position: relative; }
        .address-field-suggestions { position: absolute; z-index: 10; inset-block-start: 100%; inset-inline: 0; overflow-y: auto; max-height: 18rem; border: var(--wa-border-width-s) solid var(--wa-color-surface-border); border-radius: var(--wa-border-radius-m); background: var(--wa-color-surface-default); box-shadow: var(--wa-shadow-l); }
        .address-field-suggestions button { display: block; width: 100%; padding: var(--wa-space-s) var(--wa-space-m); border: 0; background: transparent; color: inherit; text-align: start; cursor: pointer; }
        .address-field-suggestions button:hover, .address-field-suggestions button:focus-visible { background: var(--wa-color-surface-raised); }
      `}</style>
    </div>
  );
}
