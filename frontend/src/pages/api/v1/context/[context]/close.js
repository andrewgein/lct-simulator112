import { closeContext } from "../../../../../features/context/api/ContextApi";

export const prerender = false;

export async function POST({ params, cookies }) {
    const response = await closeContext(params.context, cookies.get("accessToken")?.value);
    return new Response(await response.text(), { status: response.status });
}
