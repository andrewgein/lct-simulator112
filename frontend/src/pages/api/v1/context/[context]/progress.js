import { getLevelProgress } from "../../../../../features/context/api/ContextApi";

export const prerender = false;

export function GET({ params, cookies }) {
    return getLevelProgress(params.context, cookies.get("accessToken")?.value);
}
