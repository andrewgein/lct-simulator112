import { createLevel } from "../../../../features/level/api/LevelApi";

export const prerender = false;

export async function POST({ request, cookies }) {
    return createLevel(await request.json(), cookies.get("accessToken")?.value);
}
