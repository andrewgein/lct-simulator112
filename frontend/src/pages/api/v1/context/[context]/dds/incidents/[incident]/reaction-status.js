import { applyReactionStatus } from "../../../../../../../../features/context/api/ContextApi";

export const prerender = false;

export async function POST({ params, request, cookies }) {
    const body = await request.json();
    return applyReactionStatus(
        params.context,
        params.incident,
        body,
        cookies.get("accessToken")?.value,
    );
}
