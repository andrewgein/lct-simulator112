import { createCardForCall } from "../../../../../../../features/context/api/ContextApi";

export const prerender = false;

export async function POST({ params, request, cookies }) {
    return createCardForCall(
        params.context,
        params.call,
        await request.json(),
        cookies.get("accessToken")?.value,
    );
}
