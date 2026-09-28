import { saveDdsComment } from "../../../../../../../../features/context/api/ContextApi";

export const prerender = false;

export async function POST({ params, request, cookies }) {
    const { stageId, comment } = await request.json();
    return saveDdsComment(params.context, params.incident, { stageId, comment }, cookies.get("accessToken")?.value);
}
