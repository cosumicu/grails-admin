# Built-in Grails Admin authentication requirements

## Purpose

Allow a Grails application with no authentication system to use Grails Admin without creating application-level user and role classes. Grails Admin will own its administrator account and login. A host application's existing authentication, when present, remains independent.

## Agreed user flow

1. The application operator runs a one-time setup command from CMD, PowerShell, or another interactive terminal. The command prompts for an admin username, password, and password confirmation. Password input is hidden.
2. The command validates the input, creates the admin account in persistent storage, confirms success without printing credentials, and exits. Setup finishes before the web app needs to accept admin requests.
3. The operator starts the main application normally (for example, with `gradlew bootRun`). Visiting `/admin` while signed out redirects to the Grails Admin login page.
4. After successful login, the administrator can use only the registered admin models and operations allowed by the plugin. Logout ends the Grails Admin session.
5. Future application starts find the existing account and do not prompt for setup again. Setup cannot silently replace an existing account.

The setup prompt belongs to a dedicated command rather than `bootRun`. This avoids relying on Gradle forwarding terminal input to the running application and supports unattended startup.

## Functional requirements

### Authentication mode and compatibility

- Built-in authentication is always required, whether or not the host has its own authentication. A host-provided `AdminAccessPolicy` may further restrict model and field access.
- The built-in account is separate from the main application's users, roles, login, and logout. Signing into one does not sign into the other.
- Authentication protects `/admin`, all admin record and mutation routes, and any other route that exposes admin data. The plugin's current model and field authorization checks remain in force.
- The plugin continues to deny admin access when no valid authentication mode or account is available.
- A host app that already has a security filter must be able to coexist with the built-in mode. Integration documentation must identify any host URL-rule changes needed for Grails Admin routes.

### One-time setup command

- Supply a documented command runnable from the consuming application's checkout. Its exact CLI name and Gradle invocation will be decided during implementation.
- Prompt for username, password, and password confirmation in an interactive terminal. Reject blank usernames, invalid usernames, blank passwords, and mismatched confirmation.
- Do not echo passwords, put them in command-line arguments, or write them to logs, Gradle output, or shell history.
- Create the initial account only if none exists. Make that check and creation atomic so simultaneous setup attempts cannot create competing initial accounts.
- If setup is attempted without an interactive terminal, exit with a clear error and leave admin access locked. Provide a documented non-interactive provisioning method for automated deployments that does not expose secrets in command-line arguments or logs.
- A later password reset or account recovery must require an explicit operator action; rerunning initial setup must not reset credentials.

### Account storage

- Grails Admin owns a dedicated admin-account table or equivalent schema in the host application's **existing persistent datasource**. It does not use the host application's ordinary user table.
- Store only a salted, adaptive password hash, never the plaintext password or its confirmation value. Use a supported password-hashing library and define a migration path for hash upgrades.
- Document schema creation and migrations for production applications, where automatic schema creation may be disabled.
- The example application's development datasource must be changed from in-memory H2 to file-backed H2 so its setup account survives restarts. Tests may continue to use disposable databases.

### Login and sessions

- Provide a Grails Admin login page, login action, and logout action under the admin route namespace. Redirect signed-out users to the login page and return signed-in users to the page they requested where safe.
- Use a server-managed authentication session. Logout invalidates the admin authentication state. Do not treat a host-app login session as proof of Grails Admin authentication in built-in mode.
- Protect credential submission and state-changing admin actions against CSRF. Rotate the session identifier on login and use secure cookie settings when served over HTTPS.
- Limit repeated login attempts and avoid revealing whether a username exists.
- An uninitialized instance must not offer a public web setup form or allow anonymous admin operations. It should display a safe "admin setup required" state or return a suitable error without exposing data.

## Acceptance criteria

1. In a host app with no authentication dependency, an operator can provision the initial account through the terminal command and then sign in to `/admin`.
2. A wrong password, an anonymous request, and a signed-in main-app user without a Grails Admin session cannot access admin data or mutations.
3. Correct Grails Admin credentials work after the app restarts; logout removes access; the main app's authentication state is unaffected.
4. Initial setup cannot be claimed through a public URL or repeated to overwrite the account.
5. Non-interactive startup without a provisioned account starts with admin access locked and clear operator guidance.
6. Existing Spring Security Core authentication in the host remains independent. A custom `AdminAccessPolicy` can add restrictions after built-in login.
7. Tests cover setup races, password confirmation, failed logins, CSRF, session changes, logout, route protection, persistence across restart, and coexistence with host authentication.

## Decisions

- Built-in login is always enabled and the first version supports one account.
- The Grails command is `admin-setup`, invoked through the bundled Windows setup launcher, which calls the host application's Gradle `runCommand` task.
- Host URL security rules must pass the `/admin` namespace to the plugin's own controller checks.
- A later account recovery and password rotation procedure remains to be designed; initial setup never resets the account.

## Implementation context

- `AdminSecurityService` requires a plugin admin session and optionally applies a host `adminAccessPolicy` for further restrictions.
- `AdminAccount` stores one credential in the host's primary datasource. The setup launcher invokes the plugin's `admin-setup` Grails command.
- The example app can run with or without Spring Security Core. Its development datasource uses file-backed H2, so the plugin account persists across restarts.
