export default function AdditionalFields({ fields, values, onChange }) {
  if (!fields) return <div class="stage-additional-info wa-grid">Выберите тип происшествия.</div>;
  if (!fields.length) return <div class="stage-additional-info wa-grid">Для выбранного типа нет дополнительных полей.</div>;
  return (
    <div class="stage-additional-info wa-grid">
      {fields.map((field) => field.type.toLowerCase() === "boolean" ? (
        <wa-select key={field.id} value={values[field.id] || ""} label={field.name} required={field.required} onChange={(event) => onChange(field.id, event.currentTarget.value)}>
          <wa-option value="">Не указано</wa-option>
          <wa-option value="true">Да</wa-option>
          <wa-option value="false">Нет</wa-option>
        </wa-select>
      ) : (
        <wa-input key={field.id} value={values[field.id] || ""} type={["number", "email", "tel", "url"].includes(field.type.toLowerCase()) ? field.type.toLowerCase() : "text"} label={field.name} required={field.required} onInput={(event) => onChange(field.id, event.currentTarget.value)}>
        </wa-input>
      ))}
    </div>
  );
}
