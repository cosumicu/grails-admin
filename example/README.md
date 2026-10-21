# Grails Admin example

This separate Grails 6.2.3 application consumes the plugin from the parent Gradle project. It contains only synthetic Customer, Account, Transaction, User, Role, and AuditLog data. It uses file-backed H2 in development and in-memory H2 by default in tests.

From the repository root in PowerShell, run `.\scripts\admin-setup.ps1 -GradleTask ':example:runCommand'` to create the plugin's one admin account. If PowerShell blocks scripts under its execution policy, run `.\scripts\admin-setup.cmd -GradleTask :example:runCommand` instead; this launcher uses a process-only policy override. The launcher warns about passwords under 12 characters or over 72 UTF-8 bytes and proceeds only if you type `yes`. Then run `.\gradlew.bat :example:bootRun` and sign in at `/admin/login`. The development database persists the account across restarts. `DEMO_ADMIN_PASSWORD` optionally creates a separate host-app user named `demo-admin` for `/login/auth`; that account does not grant Grails Admin access. The account write handler and audit-log query scope demonstrate plugin extension points.

To exercise the example without any host authentication dependency, add `-PwithoutHostSecurity` to `:example:bootRun` and pass `-ExtraGradleArgs '-PwithoutHostSecurity'` to the setup launcher. The plugin login continues to protect `/admin` in that mode.

See the [main README](../README.md) for configuration, security, and testing details.
