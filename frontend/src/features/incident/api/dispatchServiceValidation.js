import { getDispatchServices } from "./IncidentApi.js";

export async function isDispatchService(code, token) {
    if (typeof code !== "string" || !code.trim()) return false;
    const response = await getDispatchServices(token);
    if (!response.ok) throw new Error("Не удалось проверить службу в классификаторе");
    return (await response.json()).some((service) => service.code === code);
}
