import { saveCardRevision } from "../../../../../../../features/context/api/ContextApi";

export const prerender = false;

export async function POST({ params, request, cookies }) {
    const body = await request.json();
    const response = await saveCardRevision(
        params.context,
        params.card,
        body,
        cookies.get("accessToken")?.value
    );
    return new Response(await response.text(), {
        status: response.status,
        headers: { "Content-Type": response.headers.get("Content-Type") || "application/json" }
    });
}
