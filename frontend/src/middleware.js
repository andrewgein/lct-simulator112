import { getProfile } from "./features/profile/api/UserProfileApi";

const isDev = import.meta.env.DEV;
const PUBLIC_PATHS = [
    "/login",
    "/register",
    "/verify-email",
    "/api/v1/auth/register",
    "/api/v1/auth/login",
    "/api/v1/auth/refresh",
    "/api/v1/auth/verify-email"
]

export async function onRequest(context, next) {
    const { cookies, redirect, url, locals, request } = context;
    if (PUBLIC_PATHS.includes(url.pathname)) {
        return await next();
    }
    if (!cookies.has("accessToken")) {
        const redirectAfterRefresh = encodeURIComponent(context.url.pathname + context.url.search);
        return context.redirect("/api/v1/auth/refresh?redirectTo=" + redirectAfterRefresh);
    }
    const accessToken = cookies.get("accessToken").value;
    locals.accessToken = accessToken;
    try {
        const shouldCheckProfile = request.method === "GET" && !url.pathname.startsWith("/api/") && url.pathname !== "/profile/create" && !cookies.has("profileCompleted");
        if (shouldCheckProfile) {
            const profileResponse = await getProfile(accessToken);
            if (profileResponse.status === 404) {
                return redirect("/profile/create");
            }
            if (profileResponse.ok) {
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
            return redirect("/login");
        }
        throw error;
    }
}

