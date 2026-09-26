/** @typedef {"SYSTEM_112" | "DDS"} TrainingTrack */
/** @typedef {string | null} DdsService */

/**
 * @typedef {Object} UserProfileRequest
 * @property {string} name
 * @property {string} surname
 * @property {string | null} patronymic
 * @property {TrainingTrack | null} trainingTrack
 * @property {DdsService} ddsService
 */

/**
 * @typedef {Object} UserProfile
 * @property {string} userId
 * @property {string} authId
 * @property {string} name
 * @property {string} surname
 * @property {string | null} patronymic
 * @property {TrainingTrack | null} trainingTrack
 * @property {DdsService} ddsService
 * @property {string | null} updatedAt
 */

export {};
