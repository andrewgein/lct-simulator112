const API_ENDPOINT = import.meta.env.PUBLIC_API_ENDPOINT;

export class UnauthorizedError extends Error {
    constructor() {
        super("Unauthorized");
        this.status = 401;
    }
}

const PUBLIC_PATHS = [
    "/api/v1/auth/register",
    "/api/v1/auth/login",
    "/api/v1/auth/verify-email",
    "/api/v1/auth/refresh"
];

export async function apiCall(path, method, body, accessToken, cookies) {
    const headers = {};
    const hasBody = (method == "POST" || method == "PUT" || method == "PATCH");
    if (hasBody) {
        headers["Content-Type"] = "application/json";
    }
    if (accessToken) {
        headers["Authorization"] = `Bearer ${accessToken}`;
    }
    if (cookies) {
        headers["Cookie"] = Object.entries(cookies)
            .map(([key, value]) => `${key}=${value}`)
            .join('; ');
    }
    if (!API_ENDPOINT) {
        throw new Error("PUBLIC_API_ENDPOINT is not configured in the frontend build");
    }

    console.log(`Send request: ${method} ${API_ENDPOINT + path}`);
    const response = await fetch(API_ENDPOINT + path, {
        method,
        headers,
        body: hasBody ? JSON.stringify(body) : undefined
    });

    if (response.status == 401 && !PUBLIC_PATHS.includes(path)) {
        throw new UnauthorizedError();
    }

    return response;
}
