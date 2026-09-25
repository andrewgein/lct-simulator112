import { markNotificationRead } from "../../../../../features/notification/api/NotificationApi";

export const prerender = false;

export async function PATCH({ params, locals }) {
    if (!/^[0-9a-fA-F-]{36}$/.test(params.id || "")) {
        return new Response(null, { status: 400 });
    }
    const response = await markNotificationRead(params.id, locals.accessToken);
    if (!response.ok) {
        return new Response(null, { status: response.status });
    }
    return new Response(await response.text(), {
        status: response.status,
        headers: { "Content-Type": "application/json" }
    });
}
