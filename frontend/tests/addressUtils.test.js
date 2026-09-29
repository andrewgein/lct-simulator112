import assert from "node:assert/strict";
import { test } from "node:test";
import { addressFromSuggestion, addressSuggestionFromGeocode, emptyAddressDetails, formatAddressDetails, formatIncidentAddress } from "../src/layouts/addressUtils.js";

const suggestion = {
  value: "Московская обл, г Одинцово, д Лапино, ул Полевая, влд 5, стр 2",
  data: {
    country: "Россия", region_with_type: "Московская обл", area_with_type: "Одинцовский р-н",
    city: "Одинцово", city_with_type: "г Одинцово", settlement: "Лапино", settlement_with_type: "д Лапино",
    street: "Полевая", street_with_type: "ул Полевая", house: "5", house_type: "влд",
    block: "2", block_type: "стр", flat: "3", flat_type: "офис", postal_code: "143081", floor: "4"
  }
};

test("map selection provides structured address for operator and constructor fields", () => {
  const result = {
    display_name: "8с1, улица Малая Дмитровка, Москва, Россия",
    address: { country: "Россия", state: "Москва", city: "Москва", borough: "Центральный административный округ",
      suburb: "Тверской район", road: "улица Малая Дмитровка", house_number: "8с1", postcode: "127006" }
  };
  const selected = addressSuggestionFromGeocode(result);
  assert.equal(selected.value, result.display_name);
  const { details, incident } = addressFromSuggestion(selected);
  assert.equal(details.country, "Россия");
  assert.equal(details.region, "Москва");
  assert.equal(details.city, "Москва");
  assert.equal(details.district, "Центральный административный округ");
  assert.equal(details.area, "Тверской район");
  assert.equal(details.street, "улица Малая Дмитровка");
  assert.equal(details.house, "8с1");
  assert.equal(details.postalCode, "127006");
  assert.equal(details.apartment, "");
  assert.equal(details.entrance, "");
  assert.equal(details.floor, "");
  assert.equal(incident.city, "Москва");
  assert.equal(incident.house, "8с1");
  assert.match(formatAddressDetails({ ...details, house: "9" }), /д 9/);
});

test("map address handles settlements and missing components without parsing the label", () => {
  const { details } = addressFromSuggestion(addressSuggestionFromGeocode({
    display_name: "Название объекта, Россия", address: { country: "Россия", village: "Лапино", pedestrian: "Пешеходная улица" }
  }));
  assert.equal(details.city, "Лапино");
  assert.equal(details.street, "Пешеходная улица");
  assert.equal(details.house, "");
  assert.equal(details.region, "");
});

test("map address fills federal district without treating a numbered neighbourhood as a house", () => {
  const { details } = addressFromSuggestion(addressSuggestionFromGeocode({
    display_name: "улица Петровка, 62, Тверской район, Москва, Центральный федеральный округ, Россия",
    address: { road: "улица Петровка", neighbourhood: "62", suburb: "Тверской район", city: "Москва",
      state_district: "Центральный федеральный округ", country: "Россия" }
  }));
  assert.equal(details.district, "Центральный федеральный округ");
  assert.equal(details.house, "");
});

test("map address reads federal district from region and keeps quarter out of house", () => {
  const { details } = addressFromSuggestion(addressSuggestionFromGeocode({
    display_name: "8 к1, Тверская улица, 50, Тверской район, Москва, Центральный федеральный округ, Россия",
    address: { house_number: "8 к1", road: "Тверская улица", quarter: "50", suburb: "Тверской район",
      city: "Москва", state: "Москва", region: "Центральный федеральный округ", country: "Россия" }
  }));
  assert.equal(details.region, "Москва");
  assert.equal(details.district, "Центральный федеральный округ");
  assert.equal(details.area, "Тверской район");
  assert.equal(details.house, "8 к1");
});

test("changing house preserves region, settlement and building types", () => {
  const { details, types } = addressFromSuggestion(suggestion);
  const address = formatAddressDetails({ ...details, house: "7" }, types);
  assert.equal(address, "143081, Россия, Московская обл, Одинцовский р-н, г Одинцово, д Лапино, ул Полевая, влд 7, стр 2, офис 3, этаж 4");
  assert.equal(details.house, "5");
});

test("federal city is not duplicated", () => {
  const selected = addressFromSuggestion({ data: { region_with_type: "г Москва", city: "Москва", city_with_type: "г Москва", street: "Тверская", street_with_type: "ул Тверская", house: "1" } });
  assert.equal(formatAddressDetails(selected.details, selected.types), "г Москва, ул Тверская, д 1");
});

test("constructor preserves geographic context while retaining API field values", () => {
  const selected = addressFromSuggestion(suggestion);
  const fields = { ...selected.incident, house: "9" };
  assert.equal(fields.city, "Одинцово");
  assert.equal(fields.street, "Полевая");
  assert.equal(formatIncidentAddress(fields, selected), "Россия, Московская обл, Одинцовский р-н, г Одинцово, д Лапино, ул Полевая, влд 9, стр 2, офис 3, этаж 4");
});

test("generated or manually entered constructor address works without DaData", () => {
  assert.equal(formatIncidentAddress({ city: "Москва", street: "Тверская", house: "2", building: "", apartment: "", floor: "0" }), "Москва, Тверская, д 2");
});

test("empty or cleared address does not retain any old components", () => {
  assert.equal(formatAddressDetails(emptyAddressDetails()), "");
  assert.equal(formatIncidentAddress({}), "");
});
