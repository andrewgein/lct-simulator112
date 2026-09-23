const COOKIE_NAME = "profileSnapshot";
const MAX_AGE_SECONDS = 15 * 60;

export function toProfileSnapshot(profile) {
    return {
        name: profile.name,
        surname: profile.surname,
        patronymic: profile.patronymic || null,
        trainingTrack: profile.trainingTrack || null,
        ddsService: profile.ddsService || null
    };
}

export function readProfileSnapshot(cookies) {
    const value = cookies.get(COOKIE_NAME)?.value;
    if (!value) return null;
    try {
        return JSON.parse(value);
    } catch {
        cookies.delete(COOKIE_NAME, { path: "/" });
        return null;
    }
}

export function setProfileSnapshot(cookies, profile, secure) {
    const snapshot = toProfileSnapshot(profile);
    cookies.set(COOKIE_NAME, JSON.stringify(snapshot), {
        httpOnly: true,
        secure,
        sameSite: "strict",
        path: "/",
        maxAge: MAX_AGE_SECONDS
    });
    return snapshot;
}

export function clearProfileSnapshot(cookies) {
    cookies.delete(COOKIE_NAME, { path: "/" });
}
