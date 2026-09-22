import { generatePerson, personValue } from "./editorHelpers";

export default function PersonFields({ title, person, gender, incidentAddress = "", onChange }) {
  const change = (field) => (event) => onChange({ ...person, [field]: event.currentTarget.value });
  const generate = () => onChange({ ...person, ...generatePerson(gender) });
  const hasPersonData = Boolean(personValue(person));
  const toggleIncidentAddress = (event) => {
    const useIncidentAddress = event.currentTarget.checked;
    onChange({ ...person, useIncidentAddress, address: useIncidentAddress ? incidentAddress : person.address });
  };
  return (
    <div class="wa-stack wa-gap-s">
      <div class="wa-split wa-align-items-center">
        <h3 class="wa-heading-l">{title}</h3>
        <wa-button type="button" size="small" appearance="outlined" onClick={generate}>
          <wa-icon name="dice" slot="start">
          </wa-icon>
          Сгенерировать
        </wa-button>
      </div>
      <wa-input value={person.lastName} label="Фамилия" required={hasPersonData} onInput={change("lastName")}>
      </wa-input>
      <wa-input value={person.firstName} label="Имя" required={hasPersonData} onInput={change("firstName")}>
      </wa-input>
      <wa-input value={person.middleName} label="Отчество" onInput={change("middleName")}>
      </wa-input>
      <wa-number-input value={person.age} label="Возраст" min="1" onInput={change("age")}>
      </wa-number-input>
      <wa-input value={person.phone} label="АОН" required={hasPersonData} onInput={change("phone")}>
      </wa-input>
      <wa-input value={person.contactPhone} label="Предоставленный телефон" onInput={change("contactPhone")}>
      </wa-input>
      <wa-input value={person.onScenePhone} label="Телефон на место" onInput={change("onScenePhone")}>
      </wa-input>
      <wa-input value={person.address} label="Адрес" disabled={person.useIncidentAddress} onInput={change("address")}>
      </wa-input>
      <wa-checkbox checked={person.useIncidentAddress} disabled={!incidentAddress} onChange={toggleIncidentAddress}>Совпадает с адресом происшествия</wa-checkbox>
      <wa-textarea value={person.additionalInfo} label="Дополнительная информация" onInput={change("additionalInfo")}>
      </wa-textarea>
    </div>
  );
}
