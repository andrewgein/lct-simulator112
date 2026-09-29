import assert from "node:assert/strict";
import { test } from "node:test";
import { syncCallAddress } from "../src/features/incident/components/admin/dds/ddsCallAddress.js";

test("адрес звонка ДДС передаётся ИИ только если его заполнили", () => {
  const call = { person: { address: "", useIncidentAddress: false } };
  assert.equal(syncCallAddress(call, "Москва, Тверская, 8"), call);
  assert.equal(syncCallAddress({ person: { address: "Другой адрес", useIncidentAddress: false } }, "Москва").person.address, "Другой адрес");
});

test("адрес совпадающего звонка следует за адресом происшествия", () => {
  const call = { person: { address: "Москва", useIncidentAddress: true } };
  assert.equal(syncCallAddress(call, "Тверская, 8").person.address, "Тверская, 8");
  assert.equal(syncCallAddress(call, "").person.address, "");
  assert.equal(call.person.address, "Москва");
});
