import assert from "node:assert/strict";
import { test } from "node:test";
import { availableCallContacts, withCallContact } from "../src/features/incident/components/admin/dds/ddsCallContacts.js";

test("предлагает заполненные контакты из всех этапов, исключая текущий и дубликаты", () => {
  const brigade = { key: "b1", counterparty: "BRIGADE", person: { firstName: "Иван", lastName: "Иванов" }, gender: "MAN" };
  const service = { key: "s1", counterparty: "SERVICE", serviceCode: "03", person: {}, gender: "" };
  const stages = [
    { calls: [brigade, { ...brigade, key: "b2" }, { key: "empty", counterparty: "BRIGADE", person: {} }] },
    { calls: [service, { key: "with-address", counterparty: "BRIGADE", person: { address: "Москва, Лесная, 14" } }, { key: "current", counterparty: "BRIGADE", person: {} }] }
  ];
  assert.deepEqual(availableCallContacts(stages, "current").map(({ key, stageIndex, callIndex }) => [key, stageIndex, callIndex]), [["b1", 0, 0], ["s1", 1, 0], ["with-address", 1, 1]]);
  assert.deepEqual(availableCallContacts(stages, "b1").map(({ key }) => key), ["b2", "s1", "with-address"]);
});

test("копирует только данные контакта, не затрагивая направление и содержание звонка", () => {
  const draft = { counterparty: "BRIGADE", direction: "INBOUND", serviceCode: "", person: {}, gender: "", knownFacts: "новые факты", hiddenFacts: "секрет", aiContext: "отдельный разговор", emotionalState: "CALM" };
  const source = { counterparty: "SERVICE", serviceCode: "02", person: { firstName: "Анна" }, gender: "WOMEN", knownFacts: "старые факты", aiContext: "старый разговор" };
  const result = withCallContact(draft, source);
  assert.deepEqual(result, { ...draft, counterparty: "SERVICE", serviceCode: "02", person: { firstName: "Анна" }, gender: "WOMEN" });
  result.person.firstName = "Другая";
  assert.equal(source.person.firstName, "Анна");
  assert.equal(withCallContact(result, { ...source, counterparty: "BRIGADE" }).serviceCode, "");
});
