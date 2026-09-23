/** @typedef {"IN_REVIEW" | "DONE"} ReviewStatus */

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
 * @property {ReviewStatus} status
 */

/**
 * @typedef {Object} UserReviews
 * @property {Review[]} reviews
 */

export {};
