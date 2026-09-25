import { apiCall } from "../../../../services/ApiClient";

export const prerender = false;

export async function POST({ request, cookies }) {
  if (!["ADMIN", "SUPERVISOR"].includes(cookies.get("role")?.value)) {
    return Response.json({ message: "Недостаточно прав" }, { status: 403 });
  }
  try {
    const body = await request.json();
    return await apiCall("/api/v1/incidents/generate", "POST", body, cookies.get("accessToken")?.value);
  } catch (error) {
    console.error("Incident generation failed", error);
    return Response.json({ message: "Не удалось подключиться к генератору" }, { status: 502 });
  }
}
