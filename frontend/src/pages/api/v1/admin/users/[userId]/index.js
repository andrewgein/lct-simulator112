import { deleteUser } from "../../../../../../features/auth/api/AdminUserApi";

export const prerender = false;

export async function DELETE({ params, cookies, locals }) {
    if (cookies.get("role")?.value !== "ADMIN") return new Response(null, { status: 403 });
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(params.userId || "")) {
        return new Response(null, { status: 400 });
    }
    return deleteUser(params.userId, locals.accessToken || cookies.get("accessToken")?.value);
}
