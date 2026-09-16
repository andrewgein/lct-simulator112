import { logout } from "../../../../features/auth/api/AuthApi";

export const prerender = false;

export async function POST({ cookies, redirect }) {
    try {
        if (cookies.has("refreshToken")) {
            await logout(cookies.get("refreshToken").value);
        }
    } catch (error) {
        console.error("Failed to revoke refresh token", error);
    }

    cookies.delete("accessToken", { path: "/" });
    cookies.delete("refreshToken", { path: "/api/v1/auth" });
    cookies.delete("role", { path: "/" });
    cookies.delete("profileCompleted", { path: "/" });
    return redirect("/login");
}
