import { createContext } from "../../../../features/context/api/ContextApi";

export const prerender = false;

export async function POST({ request, cookies }) {
    const { assignmentId } = await request.json();
    if (!assignmentId) {
        return new Response(JSON.stringify({ error: "assignmentId is required" }), { status: 400 });
    }
    const contextResponse = await createContext(assignmentId, cookies.get("accessToken")?.value);
    const body = await contextResponse.text();
    if (!contextResponse.ok) {
        return new Response(body, { status: contextResponse.status,
            headers: { "Content-Type": contextResponse.headers.get("Content-Type") || "application/json" } });
    }
    return new Response(JSON.stringify({ contextId: JSON.parse(body) }), {
        status: contextResponse.status, headers: { "Content-Type": "application/json" }
    });
}
