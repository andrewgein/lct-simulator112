const contactFields = ["lastName", "firstName", "middleName", "age", "additionalInfo"];

export function availableCallContacts(stages, currentKey) {
  const seen = new Set();
  return stages.flatMap((stage, stageIndex) => (stage.calls || []).flatMap((call, callIndex) => {
    if (call.key === currentKey) return [];
    const person = Object.fromEntries(contactFields.map((field) => [field, call.person?.[field] ?? ""]));
    if (!contactFields.some((field) => person[field]) && !(call.counterparty === "SERVICE" && call.serviceCode)) return [];
    const identity = JSON.stringify([call.counterparty, call.counterparty === "SERVICE" ? call.serviceCode : "", call.gender, person]);
    if (seen.has(identity)) return [];
    seen.add(identity);
    return [{ key: call.key, call, stageIndex, callIndex }];
  }));
}

export function withCallContact(draft, source) {
  return {
    ...draft,
    counterparty: source.counterparty,
    serviceCode: source.counterparty === "SERVICE" ? source.serviceCode : "",
    person: { ...source.person },
    gender: source.gender
  };
}
