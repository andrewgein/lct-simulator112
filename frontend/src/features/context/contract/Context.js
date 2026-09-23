/** @typedef {"CREATE" | "SAVE" | "LINK" | "UNLINK"} SolutionContextOperation */
/** @typedef {"CREATED" | "FILLED" | "IN_REVIEW" | "DIALOG" | "DONE"} ContextStatus */
/** @typedef {"PENDING" | "ACTIVE" | "COMPLETED" | "FAILED"} IncidentProgressStatus */
/** @typedef {"PENDING" | "ACTIVE" | "SUCCEEDED" | "FAILED" | "TIMED_OUT" | "SKIPPED"} StageStatus */
/** @typedef {"BRIGADE_ASSIGNED" | "BRIGADE_STATUS_CHANGED" | "STATUS_CALL_COMPLETED" | "ADDITIONAL_SERVICE_REQUESTED" | "INCIDENT_COMPLETED"} DdsStageSignal */

/**
 * @typedef {Object} PersonInfo
 * @property {string} phone
 * @property {string} contactPhone
 * @property {string} onScenePhone
 * @property {string} lastName
 * @property {string} firstName
 * @property {string} middleName
 * @property {string} status
 * @property {string} address
 * @property {string} additionalInfo
 */

/**
 * @typedef {Object} SolutionCardRequest
 * @property {PersonInfo | null} applicant
 * @property {number} victimCount
 * @property {Object.<string, string> | null} additionalInfo
 * @property {string[] | null} incidentTypes
 * @property {string[] | null} services
 * @property {string | null} cardId
 * @property {number | null} expectedVersion
 * @property {SolutionContextOperation} operation
 * @property {string | null} mainCardId
 */

/**
 * @typedef {Object} SolutionCardSaveResponse
 * @property {string} revisionId
 * @property {string} cardId
 * @property {number} version
 */

/**
 * @typedef {Object} SolutionCard
 * @property {string} revisionId
 * @property {string} cardId
 * @property {string | null} previousRevisionId
 * @property {number} version
 * @property {string} callId
 * @property {string | null} mainCardId
 * @property {PersonInfo | null} applicant
 * @property {number} victimCount
 * @property {Object.<string, string>} additionalInfo
 * @property {string[]} incidentTypes
 * @property {string[]} services
 * @property {string} createdAt
 */

/**
 * @typedef {Object} System112Progress
 * @property {string | null} activeCallId
 * @property {number} completedCalls
 * @property {number} totalCalls
 */

/**
 * @typedef {Object} DdsStageProgress
 * @property {string} stageId
 * @property {string} type
 * @property {StageStatus} status
 * @property {string | null} startedAt
 * @property {string | null} deadline
 */

/**
 * @typedef {Object} DdsProgress
 * @property {string | null} activeStageId
 * @property {string | null} deadline
 * @property {DdsStageProgress[]} stages
 */

/**
 * @typedef {Object} IncidentProgress
 * @property {string} incidentId
 * @property {IncidentProgressStatus} status
 * @property {System112Progress | null} system112
 * @property {DdsProgress | null} dds
 */

/**
 * @typedef {Object} LevelProgress
 * @property {string} contextId
 * @property {import("../../incident/contract/Incident").IncidentTargetType} targetType
 * @property {import("../../incident/contract/Incident").ExecutionMode} executionMode
 * @property {ContextStatus} status
 * @property {IncidentProgress[]} incidents
 */

export {};
