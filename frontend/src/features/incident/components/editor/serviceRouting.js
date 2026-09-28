export function noResponseServiceCodes(decisionsByIncident) {
  const responding = new Set();
  const noResponse = new Set();
  for (const decision of Object.values(decisionsByIncident).flat()) {
    if (decision.resultKind === "NO_RESPONSE" || decision.resultKind === "INFORMATION_ONLY") noResponse.add(decision.service.code);
    else responding.add(decision.service.code);
  }
  return [...noResponse].filter((code) => !responding.has(code));
}

export function orderedServiceCodes(services, noResponseServices) {
  const noResponse = new Set(noResponseServices);
  return [...services.filter((code) => !noResponse.has(code)), ...services.filter((code) => noResponse.has(code))];
}
