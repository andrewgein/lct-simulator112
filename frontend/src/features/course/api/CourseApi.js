import { apiCall } from "../../../services/ApiClient";

const COURSES_PREFIX = "/api/v1/courses";
const GROUPS_PREFIX = "/api/v1/study-groups";

export function getCourses(token) {
    return apiCall(COURSES_PREFIX, "GET", undefined, token);
}

export function getCourse(id, token) {
    return apiCall(`${COURSES_PREFIX}/${encodeURIComponent(id)}`, "GET", undefined, token);
}

export function getStudyGroups(token) {
    return apiCall(GROUPS_PREFIX, "GET", undefined, token);
}

export function getStudyGroup(id, token) {
    return apiCall(`${GROUPS_PREFIX}/${encodeURIComponent(id)}`, "GET", undefined, token);
}

export function getStudyGroupCourses(id, token) {
    return apiCall(`${GROUPS_PREFIX}/${encodeURIComponent(id)}/courses`, "GET", undefined, token);
}
