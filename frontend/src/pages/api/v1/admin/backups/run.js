import { runBackup } from "../../../../../features/admin/api/AdminBackupApi";

export const prerender = false;

export async function POST({ cookies }) {
    if (cookies.get("role")?.value !== "ADMIN") {
        return new Response(null, { status: 403 });
    }
    return runBackup(cookies.get("accessToken")?.value);
}
