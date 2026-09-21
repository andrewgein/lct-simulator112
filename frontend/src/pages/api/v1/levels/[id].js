import { getLevel, updateLevel } from "../../../../features/level/api/LevelApi";

export const prerender = false;

export function GET({ params, cookies }) {
    return getLevel(params.id, cookies.get("accessToken")?.value);
}

export async function PUT({ params, request, cookies }) {
    return updateLevel(
        params.id,
        await request.json(),
        cookies.get("accessToken")?.value,
    );
}
