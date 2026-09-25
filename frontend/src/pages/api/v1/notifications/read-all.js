import { markAllNotificationsRead } from "../../../../features/notification/api/NotificationApi";

export const prerender = false;

export async function PATCH({ locals }) {
    const response = await markAllNotificationsRead(locals.accessToken);
    if (!response.ok) {
        return new Response(null, { status: response.status });
    }
    return new Response(await response.text(), {
        status: response.status,
        headers: { "Content-Type": "application/json" }
    });
}
