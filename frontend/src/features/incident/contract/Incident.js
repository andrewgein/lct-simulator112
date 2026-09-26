/** @typedef {"EASY" | "NORMAL" | "HARD"} Difficulty */
/** @typedef {"SYSTEM_112" | "DDS"} IncidentTargetType */
/** @typedef {"SEQUENTIAL" | "PARALLEL"} ExecutionMode */
/** @typedef {"MAN" | "WOMEN"} Gender */
/** @typedef {"INBOUND" | "OUTBOUND"} CallDirection */
/** @typedef {"CALLER" | "BRIGADE"} CounterpartyType */
/** @typedef {"ASSIGN_BRIGADE" | "WAIT_FOR_BRIGADE_STATUS_CHANGE" | "CALL_BRIGADE_FOR_STATUS" | "REQUEST_ADDITIONAL_SERVICE" | "COMPLETE_INCIDENT"} DdsStageType */

/**
 * @typedef {Object} Address
 * @property {string} city
 * @property {string} street
 * @property {string} house
 * @property {string | null} building
 * @property {string | null} apartment
 * @property {number | null} floor
 */

/**
 * @typedef {Object} Person
 * @property {string} firstName
 * @property {string} lastName
 * @property {string | null} middleName
 * @property {number | null} age
 * @property {string} phone
 * @property {string | null} contactPhone
 * @property {string | null} onScenePhone
 * @property {string | null} address
 * @property {string | null} additionalInfo
 */

/**
 * @typedef {Object} DialogueCriterion
 * @property {string | null} id
 * @property {string} name
 * @property {string} hypothesis
 * @property {number} weight
 */

/** @typedef {{ dialogueCriteria: DialogueCriterion[] }} Criteria */

/**
 * @typedef {Object} CallScenario
 * @property {string | null} id
 * @property {number} position
 * @property {CallDirection} direction
 * @property {CounterpartyType} counterparty
 * @property {Person} person
 * @property {Gender} gender
 * @property {string[]} knownFacts
 * @property {string[]} hiddenFacts
 * @property {string | null} aiContext
 * @property {string | null} emotionalState
 */

/**
 * @typedef {Object} System112Stage
 * @property {string | null} id
 * @property {string} title
 * @property {number} position
 * @property {string} classifierCode
 * @property {number} victimCount
 * @property {string | null} description
 * @property {CallScenario[]} calls
 */

/**
 * @typedef {Object} DdsStage
 * @property {string | null} id
 * @property {string} title
 * @property {string | null} description
 * @property {DdsStageType} type
 * @property {number} timeLimitSeconds
 * @property {CallScenario[]} calls
 */

/**
 * @typedef {Object} PreparedCardTemplate
 * @property {string[]} classifierCodes
 * @property {Person | null} applicant
 * @property {number} victimCount
 * @property {Object.<string, string>} additionalInfo
 */

/**
 * @typedef {Object} InitialAssignment
 * @property {string} emergencyService
 * @property {string} classifierCode
 * @property {string | null} instructions
 */

/**
 * @typedef {Object} DdsStageTransition
 * @property {string} stageId
 * @property {string | null} successStageId
 * @property {string | null} failureStageId
 */

/**
 * @typedef {Object} IncidentBase
 * @property {string | null} id
 * @property {string} title
 * @property {Address} address
 * @property {Difficulty} difficulty
 * @property {IncidentTargetType} targetType
 * @property {Criteria} criteria
 */

/** @typedef {IncidentBase & { targetType: "SYSTEM_112", stages: System112Stage[] }} System112Incident */
/** @typedef {IncidentBase & { targetType: "DDS", stages: DdsStage[], preparedCardTemplate: PreparedCardTemplate | null, initialAssignment: InitialAssignment | null, initialStageId: string, transitions: DdsStageTransition[] }} DdsIncident */
/** @typedef {System112Incident | DdsIncident} Incident */

/**
 * @typedef {Object} IncidentRequest
 * @property {string} title
 * @property {Address} address
 * @property {Difficulty} difficulty
 * @property {IncidentTargetType} targetType
 * @property {(System112Stage | DdsStage)[]} stages
 * @property {DialogueCriterion[]} dialogueCriteria
 * @property {PreparedCardTemplate | null} preparedCardTemplate
 * @property {InitialAssignment | null} initialAssignment
 * @property {string | null} initialStageId
 * @property {DdsStageTransition[]} transitions
 */

/**
 * @typedef {Object} Level
 * @property {string | null} id
 * @property {string} title
 * @property {IncidentTargetType} targetType
 * @property {Difficulty} difficulty
 * @property {ExecutionMode} executionMode
 * @property {string[]} incidentIds
 * @property {number | null} threshold3
 * @property {number | null} threshold4
 * @property {number | null} threshold5
 */

/** @typedef {Omit<Level, "id">} LevelRequest */

export {};
