import { apiCall } from "../../../services/ApiClient";

export function getMyEnrollments(token) {
  return apiCall("/api/v1/enrollments", "GET", undefined, token);
}

export function getEnrollment(courseId, token) {
  return apiCall(`/api/v1/enrollments/${encodeURIComponent(courseId)}`, "GET", undefined, token);
}
