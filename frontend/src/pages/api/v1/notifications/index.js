import { getNotifications } from "../../../../features/notification/api/NotificationApi";

export const prerender = false;

export async function GET({ locals }) {
    const response = await getNotifications(locals.accessToken);
    if (!response.ok) {
        return new Response(null, { status: response.status });
    }
    return new Response(await response.text(), {
        status: response.status,
        headers: { "Content-Type": "application/json" }
    });
}
