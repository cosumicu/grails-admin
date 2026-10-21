# Grails Admin

Grails Admin is a web plugin that builds a compact administration UI from **explicitly registered** GORM domains. It supplies a grouped dashboard, searchable and paginated lists, record details, generated create and edit forms, and optional row selection with custom bulk actions. The UI is server rendered with GSP and original CSS inspired by classic admin interfaces. Search, sorting, and pagination use ordinary forms and links. It does not create controllers or views for each domain.

**Status:** This plugin is under development and has been tested with a Grails 6.2.3 host application. It has not been published to a package repository. Use a source checkout alongside your host application as described below.

## Requirements

- Java 17 and a Grails 6.2.3 host application (the version tested so far)
- Groovy 3 and Gradle 7.6.4 (wrapper included for this plugin)
- Hibernate 5 GORM and GSP in the host application
- A persistent primary GORM datasource for the plugin's single admin account. The host may have its own authentication, but Grails Admin always requires its separate login.

The plugin uses the host application's GORM datasource mappings and stores one separate admin credential in the primary datasource. It does not own a connection pool, job, or external service.

## Install from source

Clone this repository into the same parent folder as your main Grails application. Replace the URL placeholder with the repository's Git URL:

```text
workspace\
├── your-main-app\
└── grails-admin\
```

```cmd
cd workspace
git clone https://example.com/your-org/grails-admin.git grails-admin
```

In the main application's `settings.gradle`, include the sibling checkout (adjust the relative path if your layout differs):

```groovy
include 'grails-admin'
project(':grails-admin').projectDir = file('../grails-admin')
```

Then add `implementation project(':grails-admin')` to the main application's `build.gradle` dependencies. The included `example` project already consumes the plugin as a project dependency.

Do not copy the plugin's example domains into a production application. The plugin is independent of them.

## Configure domains

The supported syntax is `application.yml` nested maps. Registration occurs once during application startup. All fields named in a configuration must be persistent, supported fields; an invalid or duplicate registration fails startup deliberately.

This example is illustrative: replace `com.example.Customer` and the named service beans with domains and beans in your host application.

```yaml
grailsAdmin:
  enabled: true
  maxPageSize: 100
  auditSinkBean: myAdminAuditSinkService # optional trusted host bean
  models:
    customer:
      domain: com.example.Customer
      group: Sales
      label: Customers
      displayOrder: 10
      listDisplay: [firstName, lastName, email, active]
      searchFields: [firstName, lastName, email]
      listFilter: [active]
      ordering: [lastName]
      readonlyFields: [externalId]
      formFields: [firstName, lastName, email, active, externalId]
      formGroups: {Identity: [firstName, lastName, email], Status: [active]}
      widgetTemplates: {email: /admin/widgets/emailHint}
      pageTemplates: {show: /admin/custom/customerShow}
      allowCreate: true
      allowChange: true
      allowDelete: false
      writeHandlerBean: customerWriteHandlerService # optional trusted host bean
      bulkActions: {activate: customerActivateActionService} # optional trusted host beans
```

`enabled` defaults to `false`. If false, `/admin` returns 404. Domains are never registered automatically. `group` defaults to the last segment of the domain package and then `General`; `label` defaults to a spaced class name. `displayOrder` controls dashboard and sidebar order (default 100). The map key is the stable URL slug unless `slug` is given. `readonlyFields: ['*']` makes all fields readonly. Delete is disabled unless `allowDelete: true` is set. Each model supports `allowCreate`, `allowChange`, and `allowDelete`. `formGroups` sets fieldset labels and order; ungrouped allowed form fields appear under Other. Search fields must be text fields; sorting is limited to readable, non-association fields in `listDisplay`; filters are limited to `listFilter`. Omitted `listDisplay` uses the first four eligible fields. Page size defaults to 25 and cannot exceed `maxPageSize`.

The header posts logout to `/admin/logout`. Set `grailsAdmin.viewSiteUrl` and optionally `grailsAdmin.changePasswordUrl` to local paths for your host application.

Manual registration is available through `adminRegistryService.register('customer', [domain: 'com.example.Customer', group: 'Sales'])` during application setup. Register before serving admin requests. Duplicate keys and routes are rejected.

For domain writes that must use an application service, configure `writeHandlerBean` with a Spring bean implementing [`AdminWriteHandler`](src/main/groovy/grailsadmin/AdminWriteHandler.groovy). The plugin checks model and field permissions and converts only allowed fields before passing an immutable `changes` map to that bean. The handler validates and persists the returned domain instance inside the plugin's transaction. The example account handler prevents account-number changes.

For record-level visibility, configure `scopeBean` with a Spring bean implementing [`AdminQueryScope`](src/main/groovy/grailsadmin/AdminQueryScope.groovy). Its `criteria(model, operation)` method returns a trusted closure accepting the GORM criteria builder, such as `{ builder -> builder.eq('tenantId', currentTenantId) }`. The plugin applies it to lists, record lookup, update and delete lookup, and association choices. The example audit-log scope shows only records whose actor is `demo`.

For bulk actions, map an action key to a trusted Spring bean implementing [`AdminBulkAction`](src/main/groovy/grailsadmin/AdminBulkAction.groovy). The bean supplies a label, the required `change` or `delete` permission, the fields it changes, and `execute(model, records)`. The plugin displays row checkboxes only when at least one configured action is allowed, verifies a form token, caps selection at 100 IDs, resolves every record through its scope, enforces model and declared field permissions, and executes the action in one transaction. The example's `activate` action changes only the `active` field on selected synthetic customers. Audit the trusted bean's implementation; it is responsible for its own business validation.

For audit logging, set `auditSinkBean` to a trusted Spring bean implementing [`AdminAuditSink`](src/main/groovy/grailsadmin/AdminAuditSink.groovy). The plugin calls it for successful add, change, delete, and bulk operations inside the write transaction, passing model, operation, record IDs, and actor. It passes no form values or record contents. The example sink writes to the host application's synthetic `AuditLog` domain. With no bean configured, the plugin performs no audit writes.

For a custom field control, map its allowed form field to a trusted GSP partial under `/admin/widgets/` in `widgetTemplates`. The plugin renders it with `name`, `field`, `record`, and `creating`. The partial should emit a control with the given `name`; the plugin's server-side allowlist, conversion, authorization, and validation still apply. The example supplies [`_emailHint.gsp`](example/grails-app/views/admin/widgets/_emailHint.gsp). Review custom partials for safe output encoding.

For a custom full page, set `pageTemplates` for `list`, `show`, `form`, or `delete` to a trusted host GSP under `/admin/custom/`. The controller keeps its model-level permission checks and supplies the same view model; the custom GSP must check field-level read permissions before displaying record values. The example supplies a [customer show view](example/grails-app/views/admin/custom/customerShow.gsp).

## Built-in admin authentication and authorization

Grails Admin always uses its own login, independent of a host application's login. It stores exactly one credential in the `grails_admin_account` table of the host's primary datasource. Before that account exists, `/admin/login` returns 503 and all admin operations are locked. Copy the bundled [`admin-setup.ps1`](scripts/admin-setup.ps1) and [`admin-setup.cmd`](scripts/admin-setup.cmd) launchers into the host application (they are also packaged under `META-INF/grails-admin/setup/` in the plugin jar), then run from the **host application's directory**:

```cmd
scripts\admin-setup.cmd
```

The CMD launcher calls the bundled PowerShell setup script. It asks for a username, password, and confirmation with hidden password input, then invokes the plugin's `admin-setup` Grails command through Gradle. Passwords under 12 characters or over 72 UTF-8 bytes show a warning and require an explicit `yes` before setup continues. An overlong password is SHA-256 hashed before bcrypt so every byte is checked at login. The password is never passed as a command-line argument. The command refuses to create a second account. After setup, start the host app and visit `/admin/login`. Admin logout ends only the plugin login; it does not sign the user out of the main app. If the account is missing in a noninteractive deployment, admin access stays locked; run the setup command against the same persistent datasource before exposing the admin UI.

For automated provisioning, inject `GRAILS_ADMIN_SETUP_USERNAME`, `GRAILS_ADMIN_SETUP_PASSWORD`, and `GRAILS_ADMIN_SETUP_CONFIRMATION` as short-lived environment secrets and run `gradlew.bat runCommand -Pargs=admin-setup`. If the password is outside the recommended range, also set `GRAILS_ADMIN_SETUP_PASSWORD_WARNING_ACCEPTED=yes` to acknowledge the warning. Do not place passwords in `-Pargs`, process arguments, or configuration files. Clear the environment values after the command. Initial setup never resets an existing account. The bcrypt hash is upgraded on a successful login when the configured work factor increases. Login failures are limited to five per source address in a rolling 15-minute window within each application instance; deployments with multiple instances should also rate-limit at the edge.

The host must allow the plugin's `/admin` and `/admin/**` routes through any existing URL security rules so requests reach the plugin's own checks. The example app shows this with Spring Security Core. A restrictive host filter can still intercept those routes first, so check its rules when installing the plugin.

If the host also requires a session-backed CSRF field on every POST, configure Grails Admin to include that field in each of its forms. This retains the plugin's own `useToken`/`withForm` checks and does not require changing the host interceptor:

```yaml
grailsAdmin:
  hostCsrf:
    parameterName: csrfToken
    sessionAttribute: csrfToken
```

The field is emitted only when the configured session attribute exists. A Spring Security `_csrf` request token is also included when present. Grails Admin maps its GET delete confirmation URL to `confirmDelete`, avoiding host rules that reject GET requests to actions named `delete`. Host authentication still applies if the host interceptor requires it; other host CSRF mechanisms need a matching adapter.

Hosts may additionally register a Spring bean named `adminAccessPolicy` implementing [`AdminAccessPolicy`](src/main/groovy/grailsadmin/AdminAccessPolicy.groovy) for finer model and field permissions. It cannot replace the built-in login. Operations are `view`, `add`, `change`, and `delete`. The controller checks model permission on every action, and writes check field permission again in the service.

The account table must be created by the host's schema migration process when automatic schema updates are disabled. A [starter migration for H2 and PostgreSQL](docs/admin-account-schema.sql) matches the plugin's domain mapping; adapt it to other databases and your migration tool. Keep the datasource persistent and include it in backups. The included example uses file-backed H2 in development; its test datasource remains in memory.

For HTTPS deployments, configure the host's session cookie as `Secure` and `HttpOnly`, and ensure reverse-proxy forwarding makes HTTPS requests visible to the app. The example production profile sets both cookie flags. The plugin changes the session ID after login and uses Grails form tokens for login, logout, and mutations.

Generated forms use an explicit field allowlist and GORM validation. Internal fields, credentials, secrets, tokens, and common timestamp fields are excluded from metadata. POST operations require a Grails form token, and the host's Spring Security CSRF filter should remain enabled. Record IDs are restricted to decimal IDs and domain classes are only resolved through the registry. Keep sensitive models readonly unless their business rules are represented in a custom service.

An association can be edited only when its target domain is also registered and the user can view it. Association dropdowns show at most 100 records; use a dedicated business workflow for large or restricted relationships.

## Example app

The `example` project contains synthetic Customer, Account, Transaction, User, Role, AuditLog, and LookupCode domains. LookupCode is mapped to a second host-managed H2 datasource to exercise multi-datasource CRUD. The default datasource is file-backed H2 in development and in-memory H2 in tests; tests can also use an isolated PostgreSQL database through environment variables. `DEMO_ADMIN_PASSWORD` optionally creates a development-only **host application** user to demonstrate that its authentication is separate from Grails Admin. From the plugin checkout in CMD (adjust the JDK path if needed):

```cmd
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
scripts\admin-setup.cmd -GradleTask :example:runCommand
gradlew.bat :example:bootRun
```

The setup Gradle task uses the example app's datasource; then open `/admin/login`. The synthetic customer can be edited; transactions, users, roles, and audit logs are readonly. The example includes Spring Security Core 6.0.3 and Grails Async 5.0.2 for its host application.

## Development and verification

```cmd
gradlew.bat test :example:integrationTest
gradlew.bat :example:bootRun
gradlew.bat jar
```

The plugin's unit tests cover route slugs, type conversion, disabled mode, unknown models, anonymous access, and a missing form token. The example's integration tests cover account setup, password verification, plugin login/logout, host login isolation, registry loading, permissions, CRUD, audit events, and a second datasource.

To run the example integration suite against an isolated PostgreSQL test database, set `GRAILS_ADMIN_TEST_DB_DRIVER=org.postgresql.Driver`, `GRAILS_ADMIN_TEST_DB_URL` (for example `jdbc:postgresql://localhost:5432/grails_admin_test`), `GRAILS_ADMIN_TEST_DB_USER`, and `GRAILS_ADMIN_TEST_DB_PASSWORD`, then run `gradlew.bat :example:integrationTest`. The test profile uses `dbCreate: update`, so use a disposable database dedicated to this suite. Without these variables, tests use in-memory H2.

## Performance and extension points

Metadata is cached once per domain class. Lists use database pagination and query-level search/filtering; dashboard groups do not issue record counts. Association select lists are capped at 100 records. For large associations, provide a custom widget before allowing edits. Displaying association `toString()` values or model methods can issue extra queries, so keep list columns scalar for large tables.

`AdminAccessPolicy`, `AdminQueryScope`, `AdminWriteHandler`, `AdminBulkAction`, `AdminAuditSink`, `widgetTemplates`, and `pageTemplates` are the current authorization, scoped query, business write, batch operation, audit, and GSP customization extension points.

Grails can map a domain to a non-default datasource through its own `static mapping` block. The plugin does not create datasources; reads and generic writes use the registered domain's GORM datasource and transaction manager. The example tests create and update LookupCode on a second H2 datasource. A host `writeHandlerBean` is still appropriate when a non-default datasource has business-specific write rules. Audit writes to a different datasource are not atomic with the model write unless the host configures a cross-datasource transaction strategy. See the [Grails 6.2.3 multiple datasource guide](https://grails.apache.org/docs/6.2.3/guide/single.html#multipleDatasources).

## Publishing (future work)

The plugin is not published yet. Its Gradle `maven-publish` configuration uses `org.grails.plugins:grails-admin:0.1.0-SNAPSHOT`. For local packaging checks from CMD, run:

```cmd
gradlew.bat jar
gradlew.bat publishToMavenLocal
gradlew.bat :example:integrationTest -PusePublishedAdmin
```

These commands verify that the example application consumes the packaged artifact. A hosted Maven repository and credentials are needed before a future `gradlew.bat publish` run.

## Security notes

Only register domains you intend to administer. Model registration is explicit, but field allowlists should still be reviewed for each model. Avoid write access to balances, audit records, and authentication domains without a dedicated business workflow. Use HTTPS for admin login and keep any host CSRF protection enabled. The plugin does not override the host application's URL security or audit policy.

Grails reference: [Grails 6.2.3 plugin guide](https://grails.apache.org/docs/6.2.3/guide/plugins.html). Spring Security reference: [Spring Security Core 6.0.x](https://apache.github.io/grails-spring-security/6.0.x/index.html).
