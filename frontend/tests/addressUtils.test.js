import assert from "node:assert/strict";
import { test } from "node:test";
import { addressFromSuggestion, emptyAddressDetails, formatAddressDetails, formatIncidentAddress } from "../src/layouts/addressUtils.js";

const suggestion = {
  value: "Московская обл, г Одинцово, д Лапино, ул Полевая, влд 5, стр 2",
  data: {
    country: "Россия", region_with_type: "Московская обл", area_with_type: "Одинцовский р-н",
    city: "Одинцово", city_with_type: "г Одинцово", settlement: "Лапино", settlement_with_type: "д Лапино",
    street: "Полевая", street_with_type: "ул Полевая", house: "5", house_type: "влд",
    block: "2", block_type: "стр", flat: "3", flat_type: "офис", postal_code: "143081", floor: "4"
  }
};

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
