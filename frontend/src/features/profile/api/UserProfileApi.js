import { apiCall } from "../../../services/ApiClient";

/** @typedef {import("../contract/UserProfile").UserProfileRequest} UserProfileRequest */

const API_PREFIX = "/api/v1/profile";

export async function getProfile(token) {
    return await apiCall(API_PREFIX, "GET", undefined, token);
}

export async function getUserProfile(userId, token) {
    return await apiCall(`${API_PREFIX}/${userId}`, "GET", undefined, token);
}

export async function getAllUserProfiles(token) {
    return await apiCall("/api/v1/admin/profiles", "GET", undefined, token);
}

/** @param {UserProfileRequest} profile */
export async function createProfile(profile, token) {
    return await apiCall(API_PREFIX, "POST", profile, token);
}

/** @param {UserProfileRequest} profile */
export async function updateProfile(profile, token) {
    return await apiCall(API_PREFIX, "PATCH", profile, token);
}

/** @param {UserProfileRequest} profile */
export async function updateUserProfile(userId, profile, token) {
    return await apiCall(`${API_PREFIX}/${userId}`, "PATCH", profile, token);
}

export async function assignProfessionalProfile(userId, professionalProfile, token) {
    return await apiCall(`/api/v1/admin/profiles/${encodeURIComponent(userId)}/professional-profile`, "PUT", professionalProfile, token);
}

export async function deleteUserProfile(userId, token) {
    return await apiCall(`${API_PREFIX}/${userId}`, "DELETE", undefined, token);
}
