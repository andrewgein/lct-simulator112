import { addReviewComment } from "../../../../../features/review/api/ReviewApi";

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
    const text = String(form.get("text") || "").trim();
    if (!text || text.length > 4000) {
        const message = !text ? "Введите текст комментария" : "Комментарий не может быть длиннее 4000 символов";
        return redirect(`/review/${encodeURIComponent(contextId)}?commentError=${encodeURIComponent(message)}#comments`, 303);
    }
    const response = await addReviewComment(contextId, text, cookies.get("accessToken")?.value);
    if (!response.ok) {
        let message = "Не удалось добавить комментарий";
        try {
            const data = await response.json();
            if (data?.message) message = data.message;
        } catch {}
        return redirect(`/review/${encodeURIComponent(contextId)}?commentError=${encodeURIComponent(message)}#comments`, 303);
    }
    return redirect(`/review/${encodeURIComponent(contextId)}?commentAdded=1#comments`, 303);
}
