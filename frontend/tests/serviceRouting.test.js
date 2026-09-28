import assert from "node:assert/strict";
import { test } from "node:test";
import { noResponseServiceCodes, orderedServiceCodes } from "../src/features/incident/components/editor/serviceRouting.js";

const decision = (code, resultKind) => ({ service: { code }, resultKind });

test("services without response are gray only if no selected incident requires them", () => {
  assert.deepEqual(noResponseServiceCodes({
    first: [decision("AMBULANCE", "NO_RESPONSE"), decision("POLICE", "SERVICE_TYPE"), decision("ROAD", "NO_RESPONSE")],
    second: [decision("AMBULANCE", "SEND_CARD"), decision("ROAD", "NO_RESPONSE")]
  }), ["ROAD"]);
});

test("responding services precede no-response services without changing saved selections", () => {
  const services = ["ROAD", "POLICE", "AMBULANCE", "MCHS"];
  assert.deepEqual(orderedServiceCodes(services, ["ROAD", "AMBULANCE"]), ["POLICE", "MCHS", "ROAD", "AMBULANCE"]);
  assert.deepEqual(services, ["ROAD", "POLICE", "AMBULANCE", "MCHS"]);
});

test("missing routing decisions preserve the original order", () => {
  assert.deepEqual(noResponseServiceCodes({}), []);
  assert.deepEqual(orderedServiceCodes(["POLICE", "MCHS"], []), ["POLICE", "MCHS"]);
});
