import { refreshToken } from "../../../../features/auth/api/AuthApi";
import { clearProfileSnapshot } from "../../../../features/profile/profileSnapshot";

export const prerender = false;
const isDev = import.meta.env.DEV;

function clearSession(cookies) {
    cookies.delete("accessToken", { path: "/" });
    cookies.delete("refreshToken", { path: "/api/v1/auth" });
    cookies.delete("role", { path: "/" });
    cookies.delete("profileCompleted", { path: "/" });
    clearProfileSnapshot(cookies);
}

export async function GET({ request, cookies, redirect }) {
    const requestedPath = new URL(request.url).searchParams.get("redirectTo");
    const redirectTo = requestedPath?.startsWith("/") && !requestedPath.startsWith("//") ? requestedPath : "/";
    const storedRefreshToken = cookies.get("refreshToken")?.value;

    if (!storedRefreshToken) {
        clearSession(cookies);
        return redirect("/login");
    }

    try {
        const refreshResponse = await refreshToken(storedRefreshToken);
        const refreshData = await refreshResponse.json();

        if (!refreshResponse.ok || !refreshData.success || !refreshData.data?.accessToken) {
            clearSession(cookies);
            return redirect("/login");
        }

        const cookieOptions = {
            httpOnly: true,
            secure: !isDev,
            sameSite: "strict",
            path: "/",
            maxAge: 15 * 60
        };

        const cookieAttributes = `Path=${cookieOptions.path}; HttpOnly; SameSite=Strict; Max-Age=${cookieOptions.maxAge}${cookieOptions.secure ? "; Secure" : ""}`;
        const headers = new Headers({ Location: redirectTo });
        headers.append("Set-Cookie", `accessToken=${encodeURIComponent(refreshData.data.accessToken)}; ${cookieAttributes}`);
        if (refreshData.data.role) {
            headers.append("Set-Cookie", `role=${encodeURIComponent(refreshData.data.role)}; ${cookieAttributes}`);
        }
        return new Response(null, { status: 302, headers });
    } catch (error) {
        console.error("Failed to refresh access token", error);
        clearSession(cookies);
        return redirect("/login");
    }
}
