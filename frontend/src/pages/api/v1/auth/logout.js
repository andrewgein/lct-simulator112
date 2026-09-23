import { logout } from "../../../../features/auth/api/AuthApi";
import { clearProfileSnapshot } from "../../../../features/profile/profileSnapshot";

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
    clearProfileSnapshot(cookies);
    return redirect("/login");
}
