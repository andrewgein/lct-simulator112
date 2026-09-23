import { apiCall } from "../../../../services/ApiClient";

export const prerender = false;

export async function GET({ params, cookies }) {
  const path = params.path || "";
  if (path && !/^[0-9a-fA-F-]{36}$/.test(path)) return new Response(null, { status: 404 });
  return apiCall(`/api/v1/enrollments${path ? `/${path}` : ""}`, "GET", undefined,
    cookies.get("accessToken")?.value);
}
