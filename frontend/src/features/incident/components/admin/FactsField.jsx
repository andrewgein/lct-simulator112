// Значение хранится строкой "факт1\nфакт2", пустые строки отфильтровывает splitLines при сохранении
export default function FactsField({ label, value, required, onChange }) {
  const items = value.split("\n");
  const setItems = (next) => onChange(next.join("\n"));
  return (
    <div class="facts-field wa-stack wa-gap-xs">
      <span class="wa-body-s">{label}{required ? " *" : ""}</span>
      {items.map((item, index) => (
        <div class="wa-cluster wa-gap-xs wa-align-items-center" key={index}>
          <wa-input class="facts-field-input" style="flex: 1" value={item} placeholder="Факт" aria-label={`${label} ${index + 1}`} onInput={(event) => setItems(items.map((current, i) => (i === index ? event.currentTarget.value : current)))}>
          </wa-input>
          <wa-button type="button" size="small" appearance="outlined" variant="danger" aria-label="Удалить факт" disabled={items.length === 1 && !item} onClick={() => setItems(items.length === 1 ? [""] : items.filter((_, i) => i !== index))}>
            <wa-icon name="trash" label="Удалить">
            </wa-icon>
          </wa-button>
        </div>
      ))}
      <div>
        <wa-button type="button" size="small" appearance="outlined" aria-label="Добавить факт" onClick={() => setItems([...items, ""])}>
          <wa-icon name="plus" label="Добавить">
          </wa-icon>
        </wa-button>
      </div>
    </div>
  );
}
