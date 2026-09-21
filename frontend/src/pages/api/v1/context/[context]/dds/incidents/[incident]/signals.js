import { applyDdsStageSignal } from "../../../../../../../../features/context/api/ContextApi";

export const prerender = false;

export async function POST({ params, request, cookies }) {
    const { signal } = await request.json();
    return applyDdsStageSignal(
        params.context,
        params.incident,
        signal,
        cookies.get("accessToken")?.value,
    );
}
