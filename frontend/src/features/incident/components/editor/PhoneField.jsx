import { formatPhone } from "./editorHelpers.js";

export default function PhoneField({ label, value, aoh, readonly = false, onChange }) {
  return (
    <div class="workspace-phone">
      <wa-icon name="phone" aria-hidden="true"></wa-icon>
      <div class="workspace-phone-content">
        <span class="workspace-call-label">{label}</span>
        {readonly ? <strong>{formatPhone(value)}</strong> : <div class="workspace-phone-control"><input class="workspace-phone-input" type="tel" inputMode="tel" aria-label={label} value={value || ""} placeholder="+7 (___) ___-__-__" onInput={(event) => onChange(event.currentTarget.value)} /><button class="workspace-aoh-button" type="button" disabled={!aoh} onClick={() => onChange(aoh)}>АОН</button></div>}
      </div>
    </div>
  );
}
