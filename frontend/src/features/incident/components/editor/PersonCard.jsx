import { useState } from "preact/hooks";
import AddressField from "../../../../layouts/AddressField.jsx";

const emptyAddress = () => ({ country: "", region: "", city: "", district: "", area: "", street: "", house: "", block: "", apartment: "", entrance: "", floor: "", postalCode: "" });

const styles = `
.applicant-address-panel, .applicant-description-panel { border: var(--wa-border-width-s) solid #b8c1c5; background: #f4f6f6; }
.applicant-address-panel { min-height: 28rem; padding: var(--wa-space-m); }
.applicant-address-panel .address-field > label { color: #53636b; font-size: var(--wa-font-size-m); }
.applicant-address-panel .address-field input, .applicant-address-detail { box-sizing: border-box; width: 100%; padding: var(--wa-space-xs) 0; border: 0; border-block-end: var(--wa-border-width-s) solid #b8c1c5; outline: 0; background: transparent; color: #26343b; font: inherit; }
.applicant-address-panel .address-field input:focus, .applicant-address-detail:focus { border-block-end: 2px solid #008dca; }
.applicant-address-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--wa-space-m); margin-block-start: var(--wa-space-l); }
.applicant-address-field { display: flex; min-width: 0; flex-direction: column; gap: var(--wa-space-3xs); color: #65757d; }
.applicant-address-field--wide { grid-column: span 2; }
.applicant-address-actions { display: flex; justify-content: flex-end; margin-block-start: var(--wa-space-xl); }
.applicant-clear-address { padding: var(--wa-space-xs) var(--wa-space-m); border: var(--wa-border-width-s) solid #9ba8ae; background: #ffffff; color: #35434a; font: inherit; cursor: pointer; }
.applicant-description-panel { min-height: 15rem; padding: var(--wa-space-m); }
.applicant-description-panel label { display: block; margin-block-end: var(--wa-space-s); color: #53636b; }
.applicant-description-input { box-sizing: border-box; width: 100%; min-height: 10rem; resize: vertical; border: 0; border-block-end: var(--wa-border-width-s) solid #b8c1c5; outline: 0; background: transparent; color: #26343b; font: inherit; }
.applicant-description-input:focus { border-block-end: 2px solid #008dca; }
.applicant-description-counter { display: block; margin-block-start: var(--wa-space-xs); color: #65757d; text-align: end; }
@media (max-width: 70rem) { .applicant-address-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
`;

function formatAddress(details) {
  return [details.city, details.street, details.house && `д. ${details.house}`, details.block && `корп. ${details.block}`, details.apartment && `кв. ${details.apartment}`].filter(Boolean).join(", ");
}

export default function PersonCard({ kind, person, addressRequired = false, dadataApiKey, onChange }) {
  const [addressDetails, setAddressDetails] = useState(emptyAddress);
  const selectAddress = (suggestion) => {
    const data = suggestion.data || {};
    setAddressDetails({ country: data.country || "", region: data.region_with_type || data.region || "", city: data.city || data.settlement || "", district: data.city_district || "", area: data.area_with_type || "", street: data.street_with_type || data.street || "", house: data.house || "", block: data.block || "", apartment: data.flat || "", entrance: data.entrance || "", floor: data.floor || "", postalCode: data.postal_code || "" });
  };
  const changeAddressDetail = (name, value) => {
    const nextDetails = { ...addressDetails, [name]: value };
    setAddressDetails(nextDetails);
    onChange({ ...person, address: formatAddress(nextDetails) });
  };
  const clearAddress = () => {
    setAddressDetails(emptyAddress());
    onChange({ ...person, address: "" });
  };
  const fields = [
    ["country", "Страна"], ["region", "Субъект"], ["city", "Населённый пункт"], ["district", "Округ"],
    ["area", "Район"], ["street", "Улица", true], ["house", "Дом/владение"], ["block", "Корпус"],
    ["apartment", "Квартира/офис"], ["entrance", "Подъезд"], ["floor", "Этаж"], ["postalCode", "Индекс"]
  ];
  return (
    <div class="wa-stack wa-gap-m">
      <style>{styles}</style>
      <section class="applicant-address-panel">
        <AddressField id={`${kind}-address`} value={person.address} required={addressRequired} dadataApiKey={dadataApiKey} withMap onChange={(address) => onChange({ ...person, address })} onSelect={selectAddress} />
        <div class="applicant-address-grid">
          {fields.map(([name, label, wide]) => <label class={`applicant-address-field ${wide ? "applicant-address-field--wide" : ""}`} key={name}><span>{label}:</span><input class="applicant-address-detail" value={addressDetails[name]} onInput={(event) => changeAddressDetail(name, event.currentTarget.value)} /></label>)}
        </div>
        <div class="applicant-address-actions">
          <button class="applicant-clear-address" type="button" onClick={clearAddress}>Очистить адрес</button>
        </div>
      </section>
      <section class="applicant-description-panel">
        <label for={`${kind}-description`}>Описание со слов заявителя</label>
        <textarea class="applicant-description-input" id={`${kind}-description`} maxlength="1999" placeholder="Введите описание" value={person.additionalInfo || ""} onInput={(event) => onChange({ ...person, additionalInfo: event.currentTarget.value })}></textarea>
        <span class="applicant-description-counter">{(person.additionalInfo || "").length} / 1999</span>
      </section>
    </div>
  );
}
