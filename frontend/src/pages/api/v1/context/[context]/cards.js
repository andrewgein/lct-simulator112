import { getCards } from "../../../../../features/context/api/ContextApi";

export const prerender = false;

export async function GET({ params, cookies }) {
    const response = await getCards(params.context, cookies.get("accessToken")?.value);
    return new Response(await response.text(), {
        status: response.status,
        headers: { "Content-Type": response.headers.get("Content-Type") || "application/json" }
    });
}
