/** @typedef {"REVIEW_COMMENT"} NotificationType */

/**
 * @typedef {Object} Notification
 * @property {string} id
 * @property {NotificationType} type
 * @property {string} title
 * @property {string} text
 * @property {string} targetUrl
 * @property {string} createdAt
 * @property {boolean} read
 */

/**
 * @typedef {Object} Notifications
 * @property {Notification[]} notifications
 */

/**
 * @typedef {Object} UnreadCount
 * @property {number} unreadCount
 */

export {};
