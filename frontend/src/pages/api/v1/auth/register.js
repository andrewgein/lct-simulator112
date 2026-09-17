import { register} from "../../../../features/auth/api/AuthApi"

export const prerender = false;

export async function POST({ request, cookies, redirect }) {
    const registerForm = await request.formData();
    const email = registerForm.get('email');
    const password = registerForm.get('password');

    if (!email || !password) {
        return redirect("/register?error=" + encodeURIComponent("Не указан email или пароль"));
    }

    const registerResponse = await register(email, password);
    const registerData = await registerResponse.json();
    if (!registerResponse.ok || !registerData.success) {
        const message = registerData.message || "Ошибка";
        return redirect("/register?error=" + encodeURIComponent(message));
    }
    return redirect("/register?success");
}
