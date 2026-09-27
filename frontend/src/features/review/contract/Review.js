/** @typedef {"IN_REVIEW" | "DONE"} ReviewStatus */

/**
 * @typedef {Object} CallRecording
 * @property {string} callId
 * @property {string} fileName
 * @property {string} startedAt
 */

/**
 * @typedef {Object} CallRecordings
 * @property {CallRecording[]} recordings
 */

/**
 * @typedef {Object} ReviewComment
 * @property {string} id
 * @property {string} reviewContextId
 * @property {string} authorId
 * @property {string} text
 * @property {string} createdAt
 */

/**
 * @typedef {Object} ReviewComments
 * @property {ReviewComment[]} comments
 */

/**
 * @typedef {Object} CriterionResult
 * @property {string} id
 * @property {string} incidentId
 * @property {number} incidentOrder
 * @property {string} criterionName
 * @property {number} score
 * @property {number} maxScore
 * @property {string} feedback
 */

/**
 * @typedef {Object} IncidentSummary
 * @property {string} id
 * @property {number} order
 * @property {string} title
 * @property {number} victimCount
 * @property {string[]} classifierCodes
 */

/**
 * @typedef {Object} DispatcherCardPerson
 * @property {string} firstName
 * @property {string} lastName
 * @property {string} middleName
 * @property {string} phone
 * @property {string} contactPhone
 * @property {string} onScenePhone
 * @property {string} address
 * @property {string} additionalInfo
 */

/**
 * @typedef {Object} DispatcherCard
 * @property {string} cardId
 * @property {string} callId
 * @property {string | null} mainCardId
 * @property {string | null} incidentId
 * @property {DispatcherCardPerson | null} applicant
 * @property {number | null} victimCount
 * @property {string[]} incidentTypes
 * @property {string[]} services
 * @property {Object<string, string>} additionalInfo
 */

/**
 * @typedef {Object} Review
 * @property {string} contextId
 * @property {string} userId
 * @property {string} assignmentId
 * @property {string} createdAt
 * @property {CriterionResult[]} criterionResults
 * @property {number} automaticScore
 * @property {number | null} finalScore
 * @property {number} maxScore
 * @property {number | null} grade
 * @property {boolean | null} passed
 * @property {ReviewStatus} status
 * @property {IncidentSummary[]} incidents
 * @property {DispatcherCard[]} cards
 */

/**
 * @typedef {Object} UserReviews
 * @property {Review[]} reviews
 */

/**
 * @typedef {Object} ErrorStatistic
 * @property {string} criterionName
 * @property {number} attempts
 * @property {number} failedCount
 * @property {number} scoreEarned
 * @property {number} scoreMax
 * @property {number} errorRate
 */

/**
 * @typedef {Object} PersonalStatistics
 * @property {ErrorStatistic[]} errorStatistics
 * @property {string[]} recommendations
 */

export {};
