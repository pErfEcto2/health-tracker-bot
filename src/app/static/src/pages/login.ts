import { login } from "../auth";
import { navigate } from "../router";
import { $, mount, toast } from "../ui";

export async function render(): Promise<void> {
  mount(`
    <div class="auth-page">
      <h1>TrackHub</h1>
      <form id="login-form" class="auth-form">
        <label>
          Username
          <input type="text" name="username" autocomplete="username" required autofocus>
        </label>
        <label>
          Password
          <input type="password" name="password" autocomplete="current-password" required>
        </label>
        <button type="submit">Log in</button>
        <a href="#/recover" class="auth-link">Forgot password?</a>
      </form>
    </div>
  `);

  const form = $("#login-form") as HTMLFormElement;
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const username = String(fd.get("username") ?? "").trim().toLowerCase();
    const password = String(fd.get("password") ?? "");
    if (!username || !password) return;

    const btn = form.querySelector("button")!;
    btn.disabled = true;
    try {
      const result = await login(username, password);
      if (result.mustChangePassword) {
        navigate("change-password");
      } else {
        navigate("home");
      }
    } catch (err) {
      toast((err as Error).message || "Login failed");
      btn.disabled = false;
    }
  });
}
