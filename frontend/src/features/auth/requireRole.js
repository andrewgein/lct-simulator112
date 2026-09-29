
export function requireRole(Astro, roles) {
    const role = Astro.cookies.get("role")?.value;
    if (!roles.includes(role)) {
        return Astro.redirect("/");
    }
    return null;
}
