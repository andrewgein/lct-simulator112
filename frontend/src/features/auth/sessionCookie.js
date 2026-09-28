const isDev = import.meta.env.DEV;
const EXPIRY_MARGIN_SECONDS = 60;
const FALLBACK_MAX_AGE_SECONDS = 14 * 60;

export function accessCookieOptions(accessToken) {
    let maxAge = FALLBACK_MAX_AGE_SECONDS;
    try {
        const { exp } = JSON.parse(Buffer.from(accessToken.split(".")[1], "base64url").toString());
        maxAge = exp - Math.floor(Date.now() / 1000) - EXPIRY_MARGIN_SECONDS;
    } catch {
    }
    return {
        httpOnly: true,
        secure: !isDev,
        sameSite: "strict",
        path: "/",
        maxAge: Math.max(maxAge, 1)
    };
}
