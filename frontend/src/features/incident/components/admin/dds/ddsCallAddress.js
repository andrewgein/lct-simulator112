export function syncCallAddress(call, incidentAddress) {
  return call.person.useIncidentAddress
    ? { ...call, person: { ...call.person, address: incidentAddress } }
    : call;
}
