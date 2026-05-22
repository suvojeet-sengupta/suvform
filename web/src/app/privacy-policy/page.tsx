import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Privacy Policy — SuvForm",
  description: "How SuvForm collects, uses, and protects your data.",
};

const LAST_UPDATED = "May 22, 2026";
const CONTACT_EMAIL = "support@suvojeetsengupta.in";

export default function PrivacyPolicyPage() {
  return (
    <main className="min-h-screen bg-paper">
      <div className="max-w-3xl mx-auto px-6 py-16">
        <Link
          href="/"
          className="text-sm text-accent hover:text-accent-deep transition-colors"
        >
          ← Back to SuvForm
        </Link>

        <h1 className="mt-6 text-4xl font-serif text-ink tracking-tight">
          Privacy Policy
        </h1>
        <p className="mt-2 text-sm text-muted">Last updated: {LAST_UPDATED}</p>

        <div className="mt-10 space-y-8 text-ink leading-relaxed">
          <section>
            <p>
              SuvForm (&quot;the app&quot;, &quot;we&quot;, &quot;us&quot;) is a
              form-building application that lets you create forms, share them,
              and collect responses. This Privacy Policy explains what
              information we collect, how we use it, and the choices you have.
              It applies to the SuvForm Android app and the web dashboard at{" "}
              <span className="font-medium">suvforms.suvojeetsengupta.in</span>.
            </p>
          </section>

          <section>
            <h2 className="text-2xl font-serif text-ink mb-3">
              Information we collect
            </h2>
            <ul className="list-disc pl-6 space-y-2">
              <li>
                <span className="font-medium">Account information.</span> When
                you sign in with Google (via Firebase Authentication), we
                receive your name, email address, and profile photo to identify
                your account.
              </li>
              <li>
                <span className="font-medium">Forms and responses.</span> The
                forms you create and the responses submitted to them are stored
                so you can manage and analyze them.
              </li>
              <li>
                <span className="font-medium">AI prompts.</span> When you use AI
                features, the text you provide (such as a form description) is
                sent to Google&apos;s Gemini API to generate or analyze content.
              </li>
              <li>
                <span className="font-medium">Device &amp; notification
                tokens.</span> If you enable notifications, we store a Firebase
                Cloud Messaging token to deliver push notifications.
              </li>
              <li>
                <span className="font-medium">Optional API key.</span> If you
                choose to supply your own Gemini API key, it is stored locally
                on your device and used only to make AI requests on your behalf.
              </li>
            </ul>
          </section>

          <section>
            <h2 className="text-2xl font-serif text-ink mb-3">
              How we use your information
            </h2>
            <ul className="list-disc pl-6 space-y-2">
              <li>To authenticate you and keep your account secure.</li>
              <li>
                To store, display, and let you manage your forms and their
                responses.
              </li>
              <li>To generate and analyze form content with AI when you request it.</li>
              <li>To send notifications you have opted into.</li>
            </ul>
            <p className="mt-4">
              We do not sell your personal information, and we do not use your
              forms or responses for advertising.
            </p>
          </section>

          <section>
            <h2 className="text-2xl font-serif text-ink mb-3">
              Third-party services
            </h2>
            <p>SuvForm relies on the following providers to operate:</p>
            <ul className="list-disc pl-6 space-y-2 mt-3">
              <li>
                <span className="font-medium">Google Firebase</span> —
                authentication and push notifications (
                <a
                  href="https://firebase.google.com/support/privacy"
                  className="text-accent hover:text-accent-deep underline"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Firebase Privacy
                </a>
                ).
              </li>
              <li>
                <span className="font-medium">Google Gemini API</span> — AI form
                generation and insights (
                <a
                  href="https://ai.google.dev/gemini-api/terms"
                  className="text-accent hover:text-accent-deep underline"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Gemini API Terms
                </a>
                ).
              </li>
              <li>
                <span className="font-medium">Cloudflare</span> — hosting, the
                API backend, and the database that stores your forms and
                responses (
                <a
                  href="https://www.cloudflare.com/privacypolicy/"
                  className="text-accent hover:text-accent-deep underline"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Cloudflare Privacy
                </a>
                ).
              </li>
            </ul>
          </section>

          <section>
            <h2 className="text-2xl font-serif text-ink mb-3">
              Data retention &amp; deletion
            </h2>
            <p>
              Your forms and responses are retained until you delete them or
              delete your account. From the app&apos;s Settings you can export
              all of your data as JSON, or permanently delete your account
              along with all your forms and their responses. Deletion is
              immediate and cannot be undone.
            </p>
          </section>

          <section>
            <h2 className="text-2xl font-serif text-ink mb-3">Security</h2>
            <p>
              Access to your data requires authentication, and data is
              transmitted over encrypted (HTTPS) connections. While no method of
              transmission or storage is completely secure, we take reasonable
              measures to protect your information.
            </p>
          </section>

          <section>
            <h2 className="text-2xl font-serif text-ink mb-3">
              Children&apos;s privacy
            </h2>
            <p>
              SuvForm is not directed to children under 13, and we do not
              knowingly collect personal information from them.
            </p>
          </section>

          <section>
            <h2 className="text-2xl font-serif text-ink mb-3">
              Changes to this policy
            </h2>
            <p>
              We may update this Privacy Policy from time to time. Material
              changes will be reflected by updating the &quot;Last updated&quot;
              date above.
            </p>
          </section>

          <section>
            <h2 className="text-2xl font-serif text-ink mb-3">Contact</h2>
            <p>
              If you have questions about this Privacy Policy or your data,
              contact us at{" "}
              <a
                href={`mailto:${CONTACT_EMAIL}`}
                className="text-accent hover:text-accent-deep underline"
              >
                {CONTACT_EMAIL}
              </a>
              .
            </p>
          </section>
        </div>
      </div>
    </main>
  );
}
