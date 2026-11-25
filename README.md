# Grails Admin

**A server-rendered administration UI for explicitly registered Grails domains.**

Grails Admin is a Grails 6 web plugin that provides a dashboard, searchable and paginated lists, record pages, generated forms, and optional bulk actions. Register the GORM domains you want to expose in your host application's configuration; the plugin does not generate a controller or view for each domain.

> [!IMPORTANT]
> This project is under development and has been tested with Grails 6.2.3. It is not published to a package repository. Install it from a source checkout and review model permissions before enabling it in a host application.

## Requirements

- Java 17 and a Grails 6.2.3 host application with Hibernate 5 GORM and GSP
- A persistent primary GORM datasource for the plugin's admin account
- Gradle 7.6.4 for this checkout (included wrapper)

The plugin uses the host application's datasources. It has its own administrator login, even when the host has an authentication system.

## Try the example

The `example` application contains synthetic banking, authentication, audit, and lookup domains. From the repository root in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17' # adjust for your JDK
.\scripts\admin-setup.cmd -GradleTask :example:grailsAdminSetup
.\gradlew.bat :example:bootRun
```

The setup script prompts for an admin username and hidden password. Open `http://localhost:8080/admin/login` after the app starts. The development account persists in the example's file-backed H2 database.

The `.cmd` launcher works from Command Prompt and PowerShell even when PowerShell script execution is restricted. To run the example without its optional host Spring Security dependency, pass `-PwithoutHostSecurity` to `:example:bootRun` and `-ExtraGradleArgs '-PwithoutHostSecurity'` to the setup script. See the [example README](example/README.md).

## Install in a host application

Place this repository beside the host application's checkout. In the host's `settings.gradle`, add:

```groovy
include 'grails-admin'
project(':grails-admin').projectDir = file('../grails-admin')
```

Add `implementation project(':grails-admin')` to the host's `build.gradle` dependencies. Adjust the relative path if your checkouts have a different layout.

Start only your host application. The plugin serves `/admin` inside that application's JVM and web server and uses its configured datasources. It does not run a separate admin server. The repository's `example` application is a demonstration host; you do not run it alongside your own application. A Gradle daemon used during development is a build process, not another admin application.

From the **host application's directory**, run `..\grails-admin\scripts\admin-setup.cmd` in Command Prompt, or `& ..\grails-admin\scripts\admin-setup.cmd` in PowerShell. That single command prompts for credentials and creates the one admin account in the host's primary datasource. If the checkouts are not siblings, adjust the path or copy [admin-setup.ps1](scripts/admin-setup.ps1), [admin-setup.cmd](scripts/admin-setup.cmd), and [admin-setup.init.gradle](scripts/admin-setup.init.gradle) into the host application's `scripts` directory. The setup files are also packaged in the plugin jar under `META-INF/grails-admin/setup/`.

The setup command refuses to replace an existing account. Without an account, admin requests remain locked. For deployments without automatic schema updates, apply the [starter account migration](docs/admin-account-schema.sql) through the host's migration process. It covers H2 and PostgreSQL; adapt it for other databases.

> [!NOTE]
> If the host uses URL security rules, allow `/admin` and `/admin/**` to reach the plugin's own authentication and authorization checks. A host filter can intercept these routes first.

## Register domains

Add `grailsAdmin` to the host's `application.yml`. Registration happens at startup; `enabled` defaults to `false` and no domains are discovered automatically.

```yaml
grailsAdmin:
  enabled: true
  maxPageSize: 100
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
      formFields: [firstName, lastName, email, active]
      formGroups:
        Identity: [firstName, lastName, email]
        Status: [active]
      readonlyFields: [email]
      allowCreate: true
      allowChange: true
      allowDelete: false
```

Replace the illustrative domain and fields with your own persistent GORM domain. The `customer` key becomes the model's URL slug; its group forms the other route segment. Invalid fields, duplicate routes, and invalid extension beans fail startup.

| Option                                                  | Purpose                                                                                  |
| ------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `group`, `label`, `displayOrder`, `slug`                | Organize and name the model in the dashboard and routes.                                 |
| `listDisplay`, `searchFields`, `listFilter`, `ordering` | Select list columns, text search, filters, and default sort.                             |
| `formFields`, `formGroups`, `readonlyFields`            | Control generated forms; `readonlyFields: ['*']` makes all fields read only.             |
| `allowCreate`, `allowChange`, `allowDelete`             | Set model operations. Create and change default to enabled; delete defaults to disabled. |
| `scopeBean`, `writeHandlerBean`, `bulkActions`          | Add scoped queries, business writes, and trusted bulk operations.                        |
| `widgetTemplates`, `pageTemplates`                      | Replace specific field controls or entire admin pages with trusted GSP templates.        |

Lists default to 25 records per page and respect `maxPageSize` (default 100). Omitted `listDisplay` uses the first four eligible fields. Search fields must be text fields. Association choices are capped at 100 records and their target domain must also be registered and viewable before editing.

For large tables, set `grailsAdmin.countTotal: false` to skip the exact count query. Lists then fetch one extra row to decide whether to show Next and omit the total record count. Exact totals remain enabled by default. Association selectors also include the current permitted selection when it falls outside the first 100 choices.

You can also call `adminRegistryService.register('customer', [domain: 'com.example.Customer', group: 'Sales'])` during application setup, before serving admin requests.

## Authentication and security

Run `admin-setup` once for each host datasource. The launcher collects credentials without putting the password in process arguments. Passwords shorter than 12 characters or longer than 72 UTF-8 bytes require an explicit warning acknowledgement. For automated provisioning, supply short-lived `GRAILS_ADMIN_SETUP_USERNAME`, `GRAILS_ADMIN_SETUP_PASSWORD`, and `GRAILS_ADMIN_SETUP_CONFIRMATION` environment variables, then run `gradlew.bat --init-script scripts/admin-setup.init.gradle :grailsAdminSetup` in the host app. Also set `GRAILS_ADMIN_SETUP_PASSWORD_WARNING_ACCEPTED=yes` when acknowledging a password outside the recommended range. Clear these variables afterward.

The admin login is independent of the host login; signing out of `/admin` only ends the plugin session. A host may register an `adminAccessPolicy` Spring bean implementing [AdminAccessPolicy](src/main/groovy/grailsadmin/AdminAccessPolicy.groovy) for additional model and field restrictions. Generated writes use an explicit field allowlist, GORM validation, and Grails form tokens. Use HTTPS and secure, HTTP-only session cookies in deployment.

If the host requires an additional session-backed CSRF field on POST requests, configure:

```yaml
grailsAdmin:
  hostCsrf:
    parameterName: csrfToken
    sessionAttribute: csrfToken
```

The plugin includes that field when the session attribute exists. It also includes a Spring Security `_csrf` request token when available.

## Extension points

Configure a Spring bean name in the corresponding `grailsAdmin` setting:

| Setting                        | Interface                                                                 | Use                                                              |
| ------------------------------ | ------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| Model `scopeBean`              | [AdminQueryScope](src/main/groovy/grailsadmin/AdminQueryScope.groovy)     | Restrict lists, record lookups, writes, and association choices. |
| Model `writeHandlerBean`       | [AdminWriteHandler](src/main/groovy/grailsadmin/AdminWriteHandler.groovy) | Apply host business rules to create, update, and delete.         |
| Model `bulkActions`            | [AdminBulkAction](src/main/groovy/grailsadmin/AdminBulkAction.groovy)     | Map action keys to beans for selected records.                   |
| Global `auditSinkBean`         | [AdminAuditSink](src/main/groovy/grailsadmin/AdminAuditSink.groovy)       | Receive successful write events within the write transaction.    |
| Bean named `adminAccessPolicy` | [AdminAccessPolicy](src/main/groovy/grailsadmin/AdminAccessPolicy.groovy) | Add model and field permissions after plugin login.              |

`widgetTemplates` accepts trusted partial paths under `/admin/widgets/`; `pageTemplates` accepts `list`, `show`, `form`, or `delete` views under `/admin/custom/`. The example includes an [email widget](example/grails-app/views/admin/widgets/_emailHint.gsp) and a [customer page](example/grails-app/views/admin/custom/customerShow.gsp). Custom full pages must enforce field-level read permissions before displaying record values.

## Develop and verify

From the repository root:

```powershell
.\gradlew.bat test :example:integrationTest
.\gradlew.bat jar
```

Integration tests use in-memory H2 by default. To test against an isolated PostgreSQL database, set `GRAILS_ADMIN_TEST_DB_DRIVER`, `GRAILS_ADMIN_TEST_DB_URL`, `GRAILS_ADMIN_TEST_DB_USER`, and `GRAILS_ADMIN_TEST_DB_PASSWORD` before running `:example:integrationTest`. The test profile uses `dbCreate: update`, so use a disposable database.

See [performance measurements](docs/performance.md) for the Phase 1 query results and commands to measure the example host's pages, startup, and memory.
