import { getReviewRecording } from "../../../../../../features/review/api/ReviewApi";

export const prerender = false;

export async function GET({ params, cookies }) {
    const contextId = params.context || "";
    const path = params.path || "";
    const [callId, fileName, ...rest] = path.split("/");
    if (!/^[0-9a-fA-F-]{36}$/.test(contextId)
        || !/^[a-zA-Z0-9_.-]+$/.test(callId || "")
        || !/^\d{8}T\d{6}_\d{6}Z\.wav$/.test(fileName || "")
        || rest.length) {
        return new Response(null, { status: 400 });
    }

    const response = await getReviewRecording(
        contextId,
        callId,
        fileName,
        cookies.get("accessToken")?.value,
    );
    const headers = new Headers();
    for (const name of ["content-type", "content-length", "cache-control"]) {
        const value = response.headers.get(name);
        if (value) headers.set(name, value);
    }
    return new Response(response.body, { status: response.status, headers });
}
