import { createContext } from "../../../../features/context/api/ContextApi";

export const prerender = false;

export async function POST({ request, cookies }) {
    const { levelId } = await request.json();
    if (!levelId) {
        return new Response(JSON.stringify({ error: "levelId is required" }), { status: 400 });
    }

    const accessToken = cookies.get("accessToken")?.value;
    const contextResponse = await createContext(levelId, accessToken);
    const body = await contextResponse.text();
    if (!contextResponse.ok) {
        return new Response(body, {
            status: contextResponse.status,
            headers: { "Content-Type": contextResponse.headers.get("Content-Type") || "application/json" }
        });
    }

    return new Response(JSON.stringify({ contextId: JSON.parse(body) }), {
        status: contextResponse.status,
        headers: { "Content-Type": "application/json" }
    });
}
