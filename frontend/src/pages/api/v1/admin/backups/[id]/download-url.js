import { getBackupDownloadUrl } from "../../../../../../features/admin/api/AdminBackupApi";

export const prerender = false;

export async function GET({ params, cookies }) {
    if (cookies.get("role")?.value !== "ADMIN") {
        return new Response(null, { status: 403 });
    }
    return getBackupDownloadUrl(params.id, cookies.get("accessToken")?.value);
}
