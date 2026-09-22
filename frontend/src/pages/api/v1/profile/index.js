import { createProfile } from "../../../../features/profile/api/UserProfileApi";

export const prerender = false;
const isDev = import.meta.env.DEV;

export async function POST({ request, locals, cookies, redirect }) {
    const profileForm = await request.formData();
    const name = profileForm.get("name")?.toString().trim();
    const surname = profileForm.get("surname")?.toString().trim();
    const trainingTrack = profileForm.get("trainingTrack")?.toString();
    const ddsService = profileForm.get("ddsService")?.toString() || null;

    if (!name || !surname) {
        return redirect("/profile/create?error=" + encodeURIComponent("Не указаны имя или фамилия"));
    }
    if (trainingTrack !== "SYSTEM_112" && trainingTrack !== "DDS") {
        return redirect("/profile/create?error=" + encodeURIComponent("Не выбран профиль"));
    }
    const ddsServices = new Set(["FIRE", "POLICE", "AMBULANCE", "GAS", "ANTI_TERROR"]);
    if (trainingTrack === "DDS" && !ddsServices.has(ddsService)) {
        return redirect("/profile/create?error=" + encodeURIComponent("Не выбрана служба ДДС"));
    }

    const profileResponse = await createProfile({ name, surname, trainingTrack, ddsService: trainingTrack === "DDS" ? ddsService : null }, locals.accessToken);
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
