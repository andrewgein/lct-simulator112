import { refreshToken } from "../../../../features/auth/api/AuthApi";
import { accessCookieOptions } from "../../../../features/auth/sessionCookie";
import { clearProfileSnapshot } from "../../../../features/profile/profileSnapshot";

export const prerender = false;

function clearSession(cookies) {
    cookies.delete("accessToken", { path: "/" });
    cookies.delete("refreshToken", { path: "/api/v1/auth" });
    cookies.delete("role", { path: "/" });
    cookies.delete("profileCompleted", { path: "/" });
    clearProfileSnapshot(cookies);
}

function isNavigation(request) {
    return request.method === "GET" || request.method === "HEAD";
}

function unauthorized(request, cookies, redirect) {
    clearSession(cookies);
    if (isNavigation(request)) {
        return redirect("/login");
    }
    return new Response(JSON.stringify({ success: false, message: "Unauthorized" }), {
        status: 401,
        headers: { "Content-Type": "application/json" }
    });
}

async function refresh({ request, cookies, redirect }) {
    const requestedPath = new URL(request.url).searchParams.get("redirectTo");
    const redirectTo = requestedPath?.startsWith("/") && !requestedPath.startsWith("//") ? requestedPath : "/";
    const storedRefreshToken = cookies.get("refreshToken")?.value;

    if (!storedRefreshToken) {
        return unauthorized(request, cookies, redirect);
    }

    try {
        const refreshResponse = await refreshToken(storedRefreshToken);
        if (refreshResponse.status === 401) {
            return unauthorized(request, cookies, redirect);
        }
        const refreshData = refreshResponse.ok ? await refreshResponse.json() : null;
        if (!refreshData?.success || !refreshData.data?.accessToken) {
            throw new Error(`Unexpected refresh response: ${refreshResponse.status}`);
        }

        const options = accessCookieOptions(refreshData.data.accessToken);
        cookies.set("accessToken", refreshData.data.accessToken, options);
        if (refreshData.data.role) {
            cookies.set("role", refreshData.data.role, options);
        }
        return redirect(redirectTo, isNavigation(request) ? 302 : 307);
    } catch (error) {
        console.error("Failed to refresh access token", error);
        return new Response("Сервис авторизации временно недоступен. Обновите страницу через несколько секунд.", {
            status: 503,
            headers: { "Content-Type": "text/plain; charset=utf-8", "Retry-After": "3" }
        });
    }
}

export const ALL = refresh;
