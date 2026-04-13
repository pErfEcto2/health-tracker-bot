import { recoverAccount } from "../auth";
import { navigate } from "../router";
import { $, mount, toast } from "../ui";

export async function render(): Promise<void> {
  mount(`
    <div class="auth-page">
      <h1>Recover account</h1>
      <p class="hint">Enter your recovery key (the 64-char hex shown on first sign-in). This wipes your current password and sets a new one; encrypted data stays intact.</p>
      <form id="rec-form" class="auth-form">
        <label>
          Username
          <input type="text" name="username" autocomplete="username" required autofocus>
        </label>
        <label>
          Recovery key
          <textarea name="recovery" rows="3" required
            placeholder="a3f2 9c8d 1b4e ..."></textarea>
        </label>
        <label>
          New password
          <input type="password" name="new1" autocomplete="new-password" minlength="12" required>
        </label>
        <label>
          Confirm
          <input type="password" name="new2" autocomplete="new-password" minlength="12" required>
        </label>
        <button type="submit">Recover</button>
        <a href="#/login" class="auth-link">Back to login</a>
      </form>
    </div>
  `);

  const form = $("#rec-form") as HTMLFormElement;
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const username = String(fd.get("username") ?? "").trim().toLowerCase();
    const rec = String(fd.get("recovery") ?? "");
    const a = String(fd.get("new1") ?? "");
    const b = String(fd.get("new2") ?? "");
    if (a !== b) { toast("Passwords don't match"); return; }
    if (a.length < 12) { toast("Password must be at least 12 characters"); return; }

    const btn = form.querySelector("button")!;
    btn.disabled = true;
    try {
      await recoverAccount(username, rec, a);
      toast("Account recovered");
      navigate("home");
    } catch (err) {
      toast((err as Error).message || "Recovery failed");
      btn.disabled = false;
    }
  });
}
