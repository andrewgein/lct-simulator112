import { useEffect, useState } from "preact/hooks";
import AddressField from "../../../../layouts/AddressField.jsx";
import { addressFromSuggestion, emptyAddressDetails, formatAddressDetails } from "../../../../layouts/addressUtils.js";

const styles = `
.applicant-address-panel, .applicant-description-panel { border: var(--wa-border-width-s) solid #b8c1c5; background: #f4f6f6; }
.applicant-address-panel { min-height: 28rem; padding: var(--wa-space-m); }
.applicant-address-panel .address-field > label { color: #53636b; font-size: var(--wa-font-size-m); }
.applicant-address-panel .address-field input, .applicant-address-detail { box-sizing: border-box; width: 100%; padding: var(--wa-space-xs) 0; border: 0; border-block-end: var(--wa-border-width-s) solid #b8c1c5; outline: 0; background: transparent; color: #26343b; font: inherit; }
.applicant-address-panel .address-field input:focus, .applicant-address-detail:focus { border-block-end: 2px solid #008dca; }
.applicant-address-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--wa-space-m); margin-block-start: var(--wa-space-l); }
.address-details-hint { margin: var(--wa-space-s) 0 0; color: var(--wa-color-text-quiet); font-size: var(--wa-font-size-s); }
.applicant-address-detail:disabled { opacity: .6; }
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

export default function PersonCard({ kind, person, addressRequired = false, dadataApiKey, onChange }) {
  const [addressState, setAddressState] = useState(null);
  const address = person.address || "";
  const currentAddress = addressState?.value === address ? addressState : null;
  const addressDetails = currentAddress?.details || emptyAddressDetails();
  const detailsEditable = !!currentAddress || !address.trim();
  useEffect(() => {
    if (addressState && addressState.value !== address) setAddressState(null);
  }, [address, addressState]);
  const changeFullAddress = (value, { source } = {}) => {
    if (source !== "suggestion") setAddressState(null);
    onChange({ ...person, address: value });
  };
  const selectAddress = (suggestion) => {
    setAddressState({ value: suggestion.value, ...addressFromSuggestion(suggestion) });
  };
  const changeAddressDetail = (name, value) => {
    if (!detailsEditable) return;
    const nextDetails = { ...addressDetails, [name]: value };
    const nextAddress = formatAddressDetails(nextDetails, currentAddress?.types);
    setAddressState({ value: nextAddress, details: nextDetails, types: currentAddress?.types });
    onChange({ ...person, address: nextAddress });
  };
  const clearAddress = () => {
    setAddressState(null);
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
        <AddressField id={`${kind}-address`} value={address} required={addressRequired} dadataApiKey={dadataApiKey} withMap onChange={changeFullAddress} onSelect={selectAddress} />
        {!detailsEditable && (
          <p class="address-details-hint">Для редактирования отдельных полей выберите адрес из подсказок или очистите адрес и заполните поля вручную.
          </p>
        )}
        <div class="applicant-address-grid">
          {fields.map(([name, label, wide]) => (
            <label class={`applicant-address-field ${wide ? "applicant-address-field--wide" : ""}`} key={name}>
              <span>{label}:
              </span>
              <input class="applicant-address-detail" value={addressDetails[name]} disabled={!detailsEditable} onInput={(event) => changeAddressDetail(name, event.currentTarget.value)} />
            </label>
          ))}
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
