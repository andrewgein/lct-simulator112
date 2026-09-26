import { apiCall } from "../../../services/ApiClient";

export function getMyCertificates(token) {
  return apiCall("/api/v1/review/certificates", "GET", undefined, token);
}

export function getCertificate(id, token) {
  return apiCall(`/api/v1/review/certificates/${encodeURIComponent(id)}`, "GET", undefined, token);
}
