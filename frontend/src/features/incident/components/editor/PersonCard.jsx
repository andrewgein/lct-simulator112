import AddressField from "../../../../layouts/AddressField.jsx";

export default function PersonCard({ title, kind, person, addressRequired = false, dadataApiKey, onChange }) {
  const field = (name) => (event) => onChange({ ...person, [name]: event.currentTarget.value });
  return (
    <wa-card style="flex: 1">
      <div class="wa-stack">
        <strong class="wa-heading-l">{title}</strong>
        <wa-input value={person.phone} label="АОН" required disabled={kind === "applicant"} onInput={field("phone")}>
        </wa-input>
        <wa-input value={person.contactPhone} label="Предоставленный телефон" onInput={field("contactPhone")}>
        </wa-input>
        <wa-input value={person.onScenePhone} label="Телефон на место" onInput={field("onScenePhone")}>
        </wa-input>
        <wa-input value={person.lastName} label="Фамилия" required onInput={field("lastName")}>
        </wa-input>
        <wa-input value={person.firstName} label="Имя" required onInput={field("firstName")}>
        </wa-input>
        <wa-input value={person.middleName} label="Отчество" onInput={field("middleName")}>
        </wa-input>
        <AddressField id={`${kind}-address`} value={person.address} required={addressRequired} dadataApiKey={dadataApiKey} withMap onChange={(address) => onChange({ ...person, address })} />
        <wa-textarea value={person.additionalInfo} label="Дополнительная информация" onInput={field("additionalInfo")}>
        </wa-textarea>
      </div>
    </wa-card>
  );
}
