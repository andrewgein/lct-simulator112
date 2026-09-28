/** @typedef {"request_status" | "request_next_call"} DialogSessionCommandType */

/** @typedef {{ type: "request_status" } | { type: "request_next_call" }} DialogSessionCommand */
/** @typedef {{ type: "idle" }} DialogIdleEvent */
/** @typedef {{ type: "no_more_calls" }} DialogNoMoreCallsEvent */
/** @typedef {{ type: "error", message: string }} DialogErrorEvent */

/**
 * @typedef {Object} DialogCallEvent
 * @property {"session_restored" | "call_ready"} type
 * @property {true} callAvailable
 * @property {string} callId
 * @property {string} phoneNumber
 * @property {boolean} [interrupted]
 */

/**
 * @typedef {Object} DialogCallFinishedEvent
 * @property {"call_finished"} type
 * @property {boolean} cardCanBeEdited
 * @property {boolean} nextCallAvailable
 * @property {string} callId
 */

/** @typedef {DialogIdleEvent | DialogNoMoreCallsEvent | DialogErrorEvent | DialogCallEvent | DialogCallFinishedEvent} DialogSessionEvent */

export {};
