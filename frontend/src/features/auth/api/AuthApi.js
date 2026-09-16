import { apiCall } from "../../../services/ApiClient";

const API_PREFIX = "/api/v1/auth"

export async function register(email, password) {
    return await apiCall(API_PREFIX + '/register', 'POST', { email, password });
}

export async function login(email, password) {
    return await apiCall(API_PREFIX + '/login', 'POST', { email, password });
} 

export async function logout(refreshToken) {
    return await apiCall(API_PREFIX + '/logout', 'POST', undefined, undefined, { refreshToken });
}

export async function verifyEmail(token) {
    return await apiCall(API_PREFIX + '/verify-email', 'POST', { token });
}

export async function refreshToken(refreshToken) {
    return await apiCall(API_PREFIX + '/refresh', 'POST', undefined, undefined, { refreshToken });
}
