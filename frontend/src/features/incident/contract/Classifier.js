/**
 * @typedef {Object} DispatchService
 * @property {string} id
 * @property {string} code
 * @property {string} name
 */

/** @typedef {"BOOLEAN" | "SINGLE_SELECT"} RoutingFactControlType */

/**
 * @typedef {Object} ClassifierFeature
 * @property {number} level
 * @property {string | null} code
 * @property {string | null} name
 */

/**
 * @typedef {Object} RoutingFactOption
 * @property {string} value
 * @property {string} label
 */

/**
 * @typedef {Object} RoutingFact
 * @property {string} code
 * @property {string} label
 * @property {RoutingFactControlType} controlType
 * @property {RoutingFactOption[]} options
 */

/**
 * @typedef {Object} ClassifierEntry
 * @property {string} id
 * @property {string} code
 * @property {ClassifierFeature[]} features
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
 * @property {string[]} routingFactCodes
 * @property {never[]} fields
 */

/**
 * @typedef {Object} ClassifierCategory
 * @property {string} code
 * @property {string} name
 * @property {ClassifierEntry[]} entries
 */

/** @typedef {ClassifierCategory[]} IncidentClassifier */

/**
 * @typedef {Object} ClassifierView
 * @property {ClassifierCategory[]} categories
 * @property {RoutingFact[]} routingFacts
 */

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
