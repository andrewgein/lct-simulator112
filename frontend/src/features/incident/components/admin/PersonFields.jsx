import { generatePerson, personValue } from "./editorHelpers";

export default function PersonFields({ title, person, gender, incidentAddress = "", showContactFields = true, disabled = false, afterTitle, onChange }) {
  const change = (field) => (event) => onChange({ ...person, [field]: event.currentTarget.value });
  const generate = () => {
    const generated = generatePerson(gender);
    if (!showContactFields) {
      const { lastName, firstName, middleName, age } = generated;
      onChange({ ...person, lastName, firstName, middleName, age });
      return;
    }
    onChange({ ...person, ...generated });
  };
  const hasPersonData = showContactFields
    ? Boolean(personValue(person))
    : Boolean(person.lastName || person.firstName || person.middleName || person.age || person.additionalInfo);
  const toggleIncidentAddress = (event) => {
    const useIncidentAddress = event.currentTarget.checked;
    onChange({ ...person, useIncidentAddress, address: useIncidentAddress ? incidentAddress : person.address });
  };
  return (
    <div class="wa-stack wa-gap-s">
      <div class="wa-split wa-align-items-center">
        <h3 class="wa-heading-l">{title}</h3>
        <wa-button type="button" size="small" appearance="outlined" disabled={disabled} onClick={generate}>
          <wa-icon name="dice" slot="start">
          </wa-icon>
          Сгенерировать
        </wa-button>
      </div>
      {afterTitle}
      <wa-input value={person.lastName} label="Фамилия" required={hasPersonData} disabled={disabled} onInput={change("lastName")}>
      </wa-input>
      <wa-input value={person.firstName} label="Имя" required={hasPersonData} disabled={disabled} onInput={change("firstName")}>
      </wa-input>
      <wa-input value={person.middleName} label="Отчество" disabled={disabled} onInput={change("middleName")}>
      </wa-input>
      <wa-number-input value={person.age} label="Возраст" min="1" disabled={disabled} onInput={change("age")}>
      </wa-number-input>
      {showContactFields && <>
        <wa-input value={person.phone} label="АОН" required={hasPersonData} disabled={disabled} onInput={change("phone")}>
        </wa-input>
        <wa-input value={person.contactPhone} label="Предоставленный телефон" disabled={disabled} onInput={change("contactPhone")}>
        </wa-input>
        <wa-input value={person.onScenePhone} label="Телефон на место" disabled={disabled} onInput={change("onScenePhone")}>
        </wa-input>
        <wa-input value={person.address} label="Адрес" disabled={disabled || person.useIncidentAddress} onInput={change("address")}>
        </wa-input>
        <wa-checkbox checked={person.useIncidentAddress} disabled={disabled || !incidentAddress} onChange={toggleIncidentAddress}>
          Совпадает с адресом происшествия
        </wa-checkbox>
      </>}
      <wa-textarea value={person.additionalInfo} label="Дополнительная информация" disabled={disabled} onInput={change("additionalInfo")}>
      </wa-textarea>
    </div>
  );
}
