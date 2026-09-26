import { createProfile, updateProfile } from "../../../../features/profile/api/UserProfileApi";
import { setProfileSnapshot } from "../../../../features/profile/profileSnapshot";
import { isDispatchService } from "../../../../features/incident/api/dispatchServiceValidation.js";

export const prerender = false;
const isDev = import.meta.env.DEV;

export async function POST({ request, locals, cookies, redirect }) {
    const profileForm = await request.formData();
    const name = profileForm.get("name")?.toString().trim();
    const surname = profileForm.get("surname")?.toString().trim();
    const patronymic = profileForm.get("patronymic")?.toString().trim() || null;
    const role = cookies.get("role")?.value;
    const isAdmin = role === "ADMIN";
    const profileRequired = role === "STUDENT";
    const trainingTrack = profileForm.get("trainingTrack")?.toString() || null;
    const ddsService = profileForm.get("ddsService")?.toString() || null;

    if (!name || !surname) {
        return redirect("/profile/create?error=" + encodeURIComponent("Не указаны имя или фамилия"));
    }
    if (profileRequired && !trainingTrack) {
        return redirect("/profile/create?error=" + encodeURIComponent("Не выбран профиль"));
    }
    if (trainingTrack && trainingTrack !== "SYSTEM_112" && trainingTrack !== "DDS") {
        return redirect("/profile/create?error=" + encodeURIComponent("Выбран неизвестный профиль"));
    }
    if (trainingTrack === "DDS" && !(await isDispatchService(ddsService, locals.accessToken))) {
        return redirect("/profile/create?error=" + encodeURIComponent("Не выбрана служба ДДС"));
    }

    const profile = isAdmin
        ? { name, surname, patronymic, trainingTrack: null, ddsService: null }
        : { name, surname, patronymic, trainingTrack, ddsService: trainingTrack === "DDS" ? ddsService : null };
    const profileResponse = await createProfile(profile, locals.accessToken);
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

    setProfileSnapshot(cookies, await profileResponse.json(), !isDev);
    cookies.set("profileCompleted", "true", {
        httpOnly: true,
        secure: !isDev,
        sameSite: "strict",
        path: "/",
        maxAge: 30 * 24 * 60 * 60
    });
    return redirect("/");
}

export async function PATCH({ request, locals, cookies }) {
    const body = await request.json();
    const name = body.name?.toString().trim();
    const surname = body.surname?.toString().trim();
    const patronymic = body.patronymic?.toString().trim() || "";
    const isTeacher = cookies.get("role")?.value === "SUPERVISOR";
    const trainingTrack = isTeacher ? body.trainingTrack?.toString() || null : null;
    const ddsService = isTeacher && trainingTrack === "DDS" ? body.ddsService?.toString() || null : null;
    if (!name || !surname) {
        return Response.json({ message: "Не указаны имя или фамилия" }, { status: 400 });
    }
    if (trainingTrack && trainingTrack !== "SYSTEM_112" && trainingTrack !== "DDS") {
        return Response.json({ message: "Выбран неизвестный профиль" }, { status: 400 });
    }
    if (trainingTrack === "DDS" && !(await isDispatchService(ddsService, locals.accessToken))) {
        return Response.json({ message: "Не выбрана служба ДДС" }, { status: 400 });
    }
    const profileResponse = await updateProfile({ name, surname, patronymic, trainingTrack, ddsService }, locals.accessToken);
    if (!profileResponse.ok) return profileResponse;
    const profile = await profileResponse.json();
    setProfileSnapshot(cookies, profile, !isDev);
    return Response.json(profile);
}
