import { actOnService } from "../../../../../../../features/admin/api/AdminOpsApi";

export const prerender = false;

export async function POST({ params, cookies }) {
    if (cookies.get("role")?.value !== "ADMIN") {
        return new Response(null, { status: 403 });
    }
    return actOnService(params.service, params.action, cookies.get("accessToken")?.value);
}
