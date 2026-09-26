import { assignProfessionalProfile } from "../../../../../../features/profile/api/UserProfileApi";
import { isDispatchService } from "../../../../../../features/incident/api/dispatchServiceValidation.js";

export const prerender = false;

export async function PUT({ params, request, cookies }) {
    if (cookies.get("role")?.value !== "ADMIN") {
        return new Response(null, { status: 403 });
    }
    if (!/^[0-9a-fA-F-]{36}$/.test(params.userId || "")) {
        return new Response(null, { status: 400 });
    }

    const { trainingTrack, ddsService = null } = await request.json();
    const validSystem112 = trainingTrack === "SYSTEM_112" && ddsService === null;
    const validDds = trainingTrack === "DDS" && await isDispatchService(ddsService, cookies.get("accessToken")?.value);
    if (!validSystem112 && !validDds) {
        return new Response(JSON.stringify({ message: "Некорректный профиль обучения" }), {
            status: 400,
            headers: { "Content-Type": "application/json" }
        });
    }

    return assignProfessionalProfile(
        params.userId,
        { trainingTrack, ddsService },
        cookies.get("accessToken")?.value
    );
}
