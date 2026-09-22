import { updateUserRole } from "../../../../../../features/auth/api/AdminUserApi";

export const prerender = false;

export async function POST({ params, request, cookies }) {
    if (cookies.get("role")?.value !== "ADMIN") {
        return new Response(null, { status: 403 });
    }
    if (!/^[0-9a-fA-F-]{36}$/.test(params.userId || "")) {
        return new Response(null, { status: 400 });
    }

    const { role } = await request.json();
    if (role !== "ADMIN" && role !== "STUDENT") {
        return new Response(JSON.stringify({ message: "Недопустимая роль" }), {
            status: 400,
            headers: { "Content-Type": "application/json" }
        });
    }

    return updateUserRole(params.userId, role, cookies.get("accessToken")?.value);
}
