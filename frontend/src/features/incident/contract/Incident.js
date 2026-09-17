/**
 * @typedef {"EASY" | "NORMAL" | "HARD"} IncidentDifficulty
 */

/**
 * @typedef {"CALM" | "WORRIED" | "PANICKED" | "AGGRESSIVE" | "CONFUSED"} EmotionalState
 */

/**
 * @typedef {Object} IncidentAddress
 * @property {string} city
 * @property {string} street
 * @property {string} house
 * @property {string | null} building
 * @property {string | null} apartment
 * @property {number} floor
 */

/**
 * @typedef {Object} IncidentPerson
 * @property {string} firstName
 * @property {string} lastName
 * @property {string | null} middleName
 * @property {number | null} age
 * @property {string} phone
 * @property {string | null} contactPhone
 * @property {string | null} address
 * @property {string | null} additionalInfo
 */

/**
 * @typedef {Object} DispatcherCriteria
 * @property {string[]} requiredQuestions
 * @property {string[]} expectedActions
 * @property {string[]} criticalMistakes
 */

/**
 * @typedef {Object} IncidentType
 * @property {string} id UUID of the incident type
 * @property {string} serviceType
 * @property {string} typeId
 * @property {string} typeName
 */

/**
 * @typedef {Object} IncidentAdditionalInfo
 * @property {string} id
 * @property {string} additionalInfoId
 * @property {string} fieldCode
 * @property {string} fieldName
 * @property {string} fieldType
 * @property {boolean} required
 * @property {string} fieldValue
 */

/**
 * @typedef {Object} DialupDetails
 * @property {"MAN" | "WOMEN" | null} gender
 * @property {string[]} knownFacts
 * @property {string[]} hiddenFacts
 * @property {string | null} aiContext
 * @property {EmotionalState | null} emotionalState
 */

/**
 * @typedef {Object} IncidentDialup
 * @property {string} id
 * @property {number} position
 * @property {IncidentPerson | null} applicant
 * @property {DialupDetails} dialupDetails
 */

/**
 * @typedef {Object} IncidentStage
 * @property {string} id
 * @property {string | null} title
 * @property {number} position
 * @property {IncidentType} type
 * @property {string | null} description
 * @property {IncidentPerson | null} victim
 * @property {IncidentAdditionalInfo[]} additionalInfo
 * @property {IncidentDialup[]} dialups
 */

/**
 * @typedef {Object} IncidentData
 * @property {string} title
 * @property {IncidentAddress} address
 * @property {DispatcherCriteria} criteria
 */

/**
 * @typedef {IncidentData} IncidentRequest
 */

/**
 * @typedef {IncidentData & { id: string, stages: IncidentStage[] }} Incident
 */

/**
 * @typedef {Object} ClassifierField
 * @property {string} id UUID of the additional information field
 * @property {string} name
 * @property {string} type
 * @property {boolean} required
 */

/**
 * @typedef {Object} ClassifierIncident
 * @property {string} id UUID of the incident type
 * @property {string} name
 * @property {ClassifierField[]} fields
 * @property {string[]} instructions
 */

/**
 * @typedef {Object.<string, Object.<string, ClassifierIncident>>} IncidentClassifier
 */

export {};
