/**
 * Redirects to "/" unless the session's role cookie is one of `roles`. Every admin/teacher page
 * currently repeats this check inline via `Astro.cookies.get("role")` - new pages use this helper
 * instead so the check stays in one place, without touching the pages that already inline it.
 *
 * Usage in an .astro frontmatter: `const redirect = requireRole(Astro, ["ADMIN"]); if (redirect) return redirect;`
 */
export function requireRole(Astro, roles) {
    const role = Astro.cookies.get("role")?.value;
    if (!roles.includes(role)) {
        return Astro.redirect("/");
    }
    return null;
}
