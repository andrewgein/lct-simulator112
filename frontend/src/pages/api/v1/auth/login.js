import { login } from "../../../../features/auth/api/AuthApi"
import { accessCookieOptions } from "../../../../features/auth/sessionCookie";
import { clearProfileSnapshot } from "../../../../features/profile/profileSnapshot";

export const prerender = false;

export async function POST({ request, cookies, redirect }) {
    const loginForm = await request.formData();
    const email = loginForm.get('email');
    const password = loginForm.get('password');

    if (!email || !password) {
        return redirect('/login?error');
    }

    const loginResponse = await login(email, password);
    const loginData = await loginResponse.json();
    if (!loginResponse.ok || !loginData.success) {
        const message = loginData.message || "Ошибка";
        return redirect('/login?error=' + encodeURIComponent(message));
    }

    const accessToken = loginData.data.accessToken;
    const role = loginData.data.role;
    const setCookieHeader = loginResponse.headers.get("Set-Cookie");
    const cookieOptions = accessCookieOptions(accessToken);

    cookies.delete('profileCompleted', { path: '/' });
    clearProfileSnapshot(cookies);
    cookies.set('accessToken', accessToken, cookieOptions);
    cookies.set('role', role, cookieOptions);

    return new Response(null, {
        status: 302,
        headers: {
            'Location': '/',
            'Set-Cookie': setCookieHeader
        }
    });
}
