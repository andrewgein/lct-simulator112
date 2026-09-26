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
 * @property {string} incidentId
 * @property {number} incidentOrder
 * @property {string} criterionName
 * @property {number} score
 * @property {number} maxScore
 * @property {string} feedback
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
 */

/**
 * @typedef {Object} UserReviews
 * @property {Review[]} reviews
 */

export {};
