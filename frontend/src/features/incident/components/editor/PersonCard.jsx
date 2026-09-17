import { useEffect, useRef } from "preact/hooks";
import AddressField from "../../../../layouts/AddressField.jsx";

export default function PersonCard({ title, kind, person, addressRequired = false, isApplicantVictim = false, dadataApiKey, onChange, onApplicantVictimChange }) {
  const checkboxRef = useRef(null);
  const field = (name) => (event) => onChange({ ...person, [name]: event.currentTarget.value });
  const fieldsDisabled = kind === "victim" && isApplicantVictim;

  useEffect(() => {
    const checkbox = checkboxRef.current;
    if (!checkbox) return;
    let active = true;
    customElements.whenDefined("wa-checkbox").then(() => {
      if (active && checkboxRef.current === checkbox) checkbox.checked = isApplicantVictim;
    });
    return () => { active = false; };
  }, [isApplicantVictim]);
  return (
    <wa-card style="flex: 1">
      <div class="wa-stack">
        <strong class="wa-heading-l">{title}</strong>
        <wa-input value={person.phone} label="Телефон" required disabled={kind === "applicant" || fieldsDisabled} onInput={field("phone")}>
        </wa-input>
        <wa-input value={person.contactPhone} label="Конт. тел." disabled={fieldsDisabled} onInput={field("contactPhone")}>
        </wa-input>
        <wa-input value={person.lastName} label="Фамилия" required disabled={fieldsDisabled} onInput={field("lastName")}>
        </wa-input>
        <wa-input value={person.firstName} label="Имя" required disabled={fieldsDisabled} onInput={field("firstName")}>
        </wa-input>
        <wa-input value={person.middleName} label="Отчество" disabled={fieldsDisabled} onInput={field("middleName")}>
        </wa-input>
        <AddressField id={`${kind}-address`} value={person.address} required={addressRequired} disabled={fieldsDisabled} dadataApiKey={dadataApiKey} withMap onChange={(address) => onChange({ ...person, address })} />
        <wa-textarea value={person.additionalInfo} label="Дополнительная информация" disabled={fieldsDisabled} onInput={field("additionalInfo")}>
        </wa-textarea>
        {kind === "applicant" && (
          <wa-checkbox ref={checkboxRef} onChange={(event) => onApplicantVictimChange(event.currentTarget.checked)}>Заявитель является пострадавшим</wa-checkbox>
        )}
      </div>
    </wa-card>
  );
}
