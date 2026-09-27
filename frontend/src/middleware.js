import { getProfile } from "./features/profile/api/UserProfileApi";
import { clearProfileSnapshot, readProfileSnapshot, setProfileSnapshot } from "./features/profile/profileSnapshot";
import { refreshToken } from "./features/auth/api/AuthApi";

const isDev = import.meta.env.DEV;
const PUBLIC_PATHS = [
    "/login",
    "/register",
    "/verify-email",
    "/api/v1/auth/register",
    "/api/v1/auth/login",
    "/api/v1/auth/refresh",
    "/api/v1/auth/logout",
    "/api/v1/auth/verify-email"
]

const SESSION_COOKIE_OPTIONS = {
    httpOnly: true,
    secure: !isDev,
    sameSite: "strict",
    path: "/",
    maxAge: 15 * 60
};


async function refreshAccessTokenInline(cookies) {
    const storedRefreshToken = cookies.get("refreshToken")?.value;
    if (!storedRefreshToken) {
        return null;
    }
    try {
        const response = await refreshToken(storedRefreshToken);
        const data = await response.json();
        if (!response.ok || !data.success || !data.data?.accessToken) {
            return null;
        }
        return data.data;
    } catch {
        return null;
    }
}

export async function onRequest(context, next) {
    const { cookies, redirect, url, locals, request } = context;
    if (PUBLIC_PATHS.includes(url.pathname)) {
        return await next();
    }
    if (!cookies.has("accessToken")) {
        if (request.method !== "GET" && request.method !== "HEAD") {
            const refreshed = await refreshAccessTokenInline(cookies);
            if (!refreshed) {
                return new Response(JSON.stringify({ success: false, message: "Unauthorized" }), {
                    status: 401,
                    headers: { "Content-Type": "application/json" }
                });
            }
            cookies.set("accessToken", refreshed.accessToken, SESSION_COOKIE_OPTIONS);
            if (refreshed.role) {
                cookies.set("role", refreshed.role, SESSION_COOKIE_OPTIONS);
            }
        } else {
            const redirectAfterRefresh = encodeURIComponent(context.url.pathname + context.url.search);
            return context.redirect("/api/v1/auth/refresh?redirectTo=" + redirectAfterRefresh);
        }
    }
    const accessToken = cookies.get("accessToken").value;
    locals.accessToken = accessToken;
    locals.profile = readProfileSnapshot(cookies);
    try {
        const shouldLoadProfile = request.method === "GET" && !url.pathname.startsWith("/api/") && url.pathname !== "/profile/create" && !locals.profile;
        if (shouldLoadProfile) {
            const profileResponse = await getProfile(accessToken);
            if (profileResponse.status === 404) {
                return redirect("/profile/create");
            }
            if (profileResponse.ok) {
                locals.profile = setProfileSnapshot(cookies, await profileResponse.json(), !isDev);
                cookies.set("profileCompleted", "true", {
                    httpOnly: true,
                    secure: !isDev,
                    sameSite: "strict",
                    path: "/",
                    maxAge: 30 * 24 * 60 * 60
                });
            }
        }
        return await next();
    } catch (error) {
        if (error?.status == 401) {
            cookies.delete('accessToken');
            cookies.delete('role');
            cookies.delete('profileCompleted');
            clearProfileSnapshot(cookies);
            return redirect("/login");
        }
        throw error;
    }
}

