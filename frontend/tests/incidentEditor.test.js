import assert from "node:assert/strict";
import { test } from "node:test";
import { moveItem, normalizeStage, serializeStage } from "../src/features/incident/components/admin/editorHelpers.js";

test("generated stages retain classifier codes and call data", () => {
  const generated = {
    title: "Пожар",
    classifierCodes: ["101", "102"],
    victimCount: 2,
    calls: [{
      person: { firstName: "Анна", lastName: "Иванова", phone: "+79991234567", additionalInfo: "Очевидец" },
      knownFacts: ["Виден дым"],
      hiddenFacts: ["Есть пострадавший"],
      gender: "WOMEN",
      emotionalState: "WORRIED"
    }]
  };
  const saved = serializeStage(normalizeStage(generated), 0);
  assert.deepEqual(saved.classifierCodes, generated.classifierCodes);
  assert.deepEqual(saved.calls[0].knownFacts, generated.calls[0].knownFacts);
  assert.deepEqual(saved.calls[0].hiddenFacts, generated.calls[0].hiddenFacts);
  assert.equal(saved.calls[0].person.firstName, "Анна");
  assert.equal(saved.calls[0].person.additionalInfo, "Очевидец");
  assert.equal(saved.calls[0].gender, "WOMEN");
  assert.deepEqual(serializeStage(normalizeStage(saved), 0), saved);
});

test("moving stages updates positions without changing their data", () => {
  const stages = [
    normalizeStage({ id: "first", classifierCodes: ["101", "102"] }),
    normalizeStage({ id: "second", classifierCodes: ["103"] })
  ];
  const saved = moveItem(stages, 0, 1).map(serializeStage);
  assert.deepEqual(saved.map((stage) => [stage.id, stage.position]), [["second", 0], ["first", 1]]);
  assert.deepEqual(saved[1].classifierCodes, ["101", "102"]);
  assert.equal(stages[0].id, "first");
});
