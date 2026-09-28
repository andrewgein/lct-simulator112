import { updateReviewCriteria } from "../../../../../features/review/api/ReviewApi";

export const prerender = false;

export async function POST({ params, request, cookies, redirect }) {
    if (!["ADMIN", "SUPERVISOR"].includes(cookies.get("role")?.value)) {
        return new Response(null, { status: 403 });
    }
    const contextId = params.context || "";
    if (!/^[0-9a-fA-F-]{36}$/.test(contextId)) {
        return new Response(null, { status: 400 });
    }
    const form = await request.formData();
    const corrections = [...form.keys()]
        .filter((key) => key.startsWith("score__"))
        .map((key) => ({
            criterionResultId: key.slice("score__".length),
            score: Number(form.get(key))
        }));
    if (!corrections.length || corrections.some((correction) => !Number.isFinite(correction.score))) {
        return redirect(`/review/${encodeURIComponent(contextId)}?scoresError=${encodeURIComponent("Некорректные значения баллов")}#criteria`, 303);
    }
    const response = await updateReviewCriteria(contextId, corrections, cookies.get("accessToken")?.value);
    if (!response.ok) {
        let message = "Не удалось сохранить баллы";
        try {
            const data = await response.json();
            if (data?.message) message = data.message;
        } catch {}
        return redirect(`/review/${encodeURIComponent(contextId)}?scoresError=${encodeURIComponent(message)}#criteria`, 303);
    }
    return redirect(`/review/${encodeURIComponent(contextId)}?scoresUpdated=1#criteria`, 303);
}
