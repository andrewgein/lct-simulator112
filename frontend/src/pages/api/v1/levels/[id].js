import { getLevel } from "../../../../features/level/api/LevelApi";

export const prerender = false;

export function GET({ params, cookies }) {
    return getLevel(params.id, cookies.get("accessToken")?.value);
}
