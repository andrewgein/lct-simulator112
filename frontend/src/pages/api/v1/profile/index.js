import { createProfile } from "../../../../features/profile/api/UserProfileApi";

export const prerender = false;
const isDev = import.meta.env.DEV;

export async function POST({ request, locals, cookies, redirect }) {
    const profileForm = await request.formData();
    const name = profileForm.get("name")?.toString().trim();
    const surname = profileForm.get("surname")?.toString().trim();

    if (!name || !surname) {
        return redirect("/profile/create?error=" + encodeURIComponent("Не указаны имя или фамилия"));
    }

    const profileResponse = await createProfile({ name, surname }, locals.accessToken);
    if (!profileResponse.ok) {
        let message = "Не удалось создать профиль";
        try {
            const errorData = await profileResponse.json();
            message = errorData.message || message;
        } catch {
            // The backend may return an empty or non-JSON error response.
        }
        return redirect("/profile/create?error=" + encodeURIComponent(message));
    }

    cookies.set("profileCompleted", "true", {
        httpOnly: true,
        secure: !isDev,
        sameSite: "strict",
        path: "/",
        maxAge: 30 * 24 * 60 * 60
    });
    return redirect("/");
}
