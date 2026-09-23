/**
 * @typedef {Object} DispatchService
 * @property {string} id
 * @property {string} code
 * @property {string} name
 */

/**
 * @typedef {Object} ClassifierField
 * @property {string} id
 * @property {string} name
 * @property {string} type
 * @property {boolean} required
 * @property {string | undefined} hint
 */

/**
 * @typedef {Object} ClassifierEntry
 * @property {string} id
 * @property {string} code
 * @property {string} categoryCode
 * @property {string} categoryName
 * @property {string | null} feature1Code
 * @property {string | null} feature1Name
 * @property {string | null} feature2Code
 * @property {string | null} feature2Name
 * @property {string | null} feature3Code
 * @property {string | null} feature3Name
 * @property {string | null} statisticalGroup
 * @property {string | null} additionalFeatures
 * @property {string} finalName
 * @property {string | null} ekp35Name
 * @property {DispatchService[]} primaryServices
 * @property {ClassifierField[]} fields
 */

/**
 * @typedef {Object} ClassifierCategory
 * @property {string} code
 * @property {string} name
 * @property {ClassifierEntry[]} entries
 */

/** @typedef {ClassifierCategory[]} IncidentClassifier */

/**
 * @typedef {Object} RoutingDecision
 * @property {DispatchService} service
 * @property {string} routingTarget
 * @property {string | null} matchedVariant
 * @property {string} resultKind
 * @property {string | null} targetTypeName
 */

/**
 * @typedef {Object} RoutingResult
 * @property {string} classifierCode
 * @property {string} incidentTypeName
 * @property {Object.<string, string>} facts
 * @property {RoutingDecision[]} decisions
 */

export {};
