import { bootstrapAccount, changePassword, fetchMe } from "../auth";
import { formatRecoveryKey } from "../crypto";
import { navigate } from "../router";
import { getDek } from "../session";
import { $, escapeHtml, mount, toast } from "../ui";

export async function render(): Promise<void> {
  const me = await fetchMe();
  if (!me) { navigate("login"); return; }
  const firstTime = me.must_change_password;

  mount(`
    <div class="auth-page">
      <h1>${firstTime ? "Set your password" : "Change password"}</h1>
      ${firstTime ? `<p class="hint">Welcome ${escapeHtml(me.username)} — pick a strong password. This is also your encryption key.</p>` : ""}
      <form id="pw-form" class="auth-form">
        <label>
          New password
          <input type="password" name="new1" autocomplete="new-password" minlength="12" required autofocus>
        </label>
        <label>
          Confirm
          <input type="password" name="new2" autocomplete="new-password" minlength="12" required>
        </label>
        <button type="submit">${firstTime ? "Create account" : "Update password"}</button>
      </form>
    </div>
  `);

  const form = $("#pw-form") as HTMLFormElement;
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const a = String(fd.get("new1") ?? "");
    const b = String(fd.get("new2") ?? "");
    if (a !== b) { toast("Passwords don't match"); return; }
    if (a.length < 12) { toast("Password must be at least 12 characters"); return; }

    const btn = form.querySelector("button")!;
    btn.disabled = true;
    try {
      if (firstTime) {
        const { recoveryKey } = await bootstrapAccount(a);
        await showRecoveryKey(formatRecoveryKey(recoveryKey));
        navigate("home");
      } else {
        const dek = getDek();
        if (!dek) { toast("Session expired"); navigate("login"); return; }
        await changePassword(dek, a);
        toast("Password updated");
        navigate("home");
      }
    } catch (err) {
      toast((err as Error).message || "Failed");
      btn.disabled = false;
    }
  });
}

async function showRecoveryKey(formatted: string): Promise<void> {
  return new Promise((resolve) => {
    mount(`
      <div class="auth-page">
        <h1>Save your recovery key</h1>
        <p class="hint">
          This is the <strong>only</strong> way to access your data if you forget your password.
          Write it down or save it in a password manager. It will not be shown again.
        </p>
        <pre id="recovery-key" class="recovery-key">${escapeHtml(formatted)}</pre>
        <div class="auth-actions">
          <button id="copy-key" type="button">Copy</button>
        </div>
        <label class="checkbox-label">
          <input type="checkbox" id="confirm-saved">
          I have saved my recovery key
        </label>
        <button id="continue-btn" type="button" disabled>Continue</button>
      </div>
    `);
    const copyBtn = $("#copy-key") as HTMLButtonElement;
    copyBtn.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(formatted);
        toast("Copied");
      } catch {
        toast("Copy failed — select and copy manually");
      }
    });
    const check = $("#confirm-saved") as HTMLInputElement;
    const cont = $("#continue-btn") as HTMLButtonElement;
    check.addEventListener("change", () => { cont.disabled = !check.checked; });
    cont.addEventListener("click", () => resolve());
  });
}
