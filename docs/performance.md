# Phase 1 performance

Measured on Windows with Java 17 and the example Grails 6.2.3 host on 2026-11-25. The plugin runs inside the host process. These local measurements describe this fixture, not a guarantee for other host applications or databases.

## Query results

The Hibernate statistics regression tests use in-memory H2, clear the persistence session before measuring lists/details, and exercise real GORM queries. Permission checks simulate a 25-row table with four readable columns. SQL counts are the reliable comparison; elapsed times vary with JVM warmup and machine load.

| Workload | Before | After |
| --- | --- | --- |
| 100 field permission checks within one request | 100 SQL statements | 1 SQL statement |
| 25-row account list, exact total | 2 SQL statements | 2 SQL statements |
| 25-row account list, `grailsAdmin.countTotal: false` | Not available | 1 SQL statement |
| Rendering customer labels after loading the account list | 0 extra statements | 0 extra statements |
| Account detail including its customer label | Existing join retained | 1 statement, 0 extra for label |

The initial permission workload took 176 ms; subsequent optimized runs took 4–10 ms. This is service work, not an end-to-end page measurement. The account lookup is reused only within a request. New requests still verify the session against the database, and login/logout invalidate the request cache. Custom permission policies continue to run.

Forms now load choices only for configured, editable associations. An allowed current selection beyond the first 100 choices is included. A scoped-out or invalid selection is never appended. Tests cover all these cases.

Exact totals remain the default. Set `grailsAdmin.countTotal: false` to fetch at most one extra row and show Previous/Next without a total. This retains the same search, filters, scopes, and page size. Offset pagination and contains searches can still be costly for large tables; this change does not remove those database costs.

The example's association labels use scalar fields. The list and detail queries already join the supported to-one relationships, so this review found no extra label queries to remove. A host's custom `toString()` or page template can introduce more queries; measure those in that host.

## Host measurements

For the first direct host comparison, both versions used the same example classes, dependencies, isolated file-backed H2 database, Java 17, `-Xms128m -Xmx512m`, and host security enabled. The baseline plugin is from commit `0d32d4c`; only its jar was swapped. Each route was warmed once and then requested ten times; values below are medians. The fixture has the example's one customer and one account.

| Measurement | Baseline | Optimized |
| --- | --- | --- |
| Customer list | 62.34 ms | 31.85 ms |
| Account list | 29.74 ms | 24.61 ms |
| Account edit | 24.88 ms | 26.84 ms |
| Launch to successful login-page response | 14.21 s | 12.76 s |
| Whole host working set after page requests | 525.9 MiB | 389.9 MiB |

A second comparison, while the final test suite was also running, produced:

| Measurement | Baseline | Optimized |
| --- | --- | --- |
| Customer list | 45.14 ms | 35.29 ms |
| Account list | 24.15 ms | 21.77 ms |
| Account edit | 23.83 ms | 21.85 ms |
| Launch to successful login-page response | 18.19 s | 18.02 s |
| Whole host working set after page requests | 457.3 MiB | 521.2 MiB |

Startup includes loading the first login page; it excludes compilation and Gradle startup. Working set includes the JVM, Grails, host application, plugin, and native memory. It is neither Java heap usage nor the plugin's standalone footprint. The heap flags are not limits on total process memory. These runs cannot establish a startup or RAM improvement; no startup or memory reduction is claimed.

## Startup and dependency review

The resolved example runtime contains 176 artifacts, including the host's Grails, Hibernate, server, optional security integration, and database drivers. Shared dependencies are resolved into the host's classpath; installing the plugin does not launch another runtime. The plugin registry inspects only explicitly configured models at startup and metadata is already cached. No dependency removal or startup behavior change was justified by these measurements, so none was made.

## Repeat the measurements

Run the query regression tests from the repository root:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat :example:integrationTest --tests example.AdminPerformanceSpec --no-daemon --console=plain
```

The `ADMIN_PERF` lines are saved in `example/build/test-results/integrationTest/TEST-example.AdminPerformanceSpec.xml`.

For direct HTTP, startup, and working-set measurements, use PowerShell 7 on Windows:

```powershell
.\gradlew.bat --init-script tools/performance.init.gradle :example:adminPerformanceClasspath --no-daemon --console=plain
.\tools\measure-example.ps1
```

The tool starts the example directly, provisions a generated benchmark password, uses a new database under `build/performance`, chooses a local port, and stops the JVMs it starts. Output and logs go to `build/performance`. It does not use the example's normal development database. The classpath export also records the resolved runtime artifacts.

To compare a prior compatible plugin jar against the current version:

```powershell
.\tools\measure-example.ps1 -BaselineJar C:\path\to\prior\grails-admin.jar
```

Re-export the classpath after changing dependencies or compilation inputs. Keep the same host and database fixture for comparisons; repeat runs and vary order before attributing changes in startup or memory to the plugin.

Validation: the complete unit and example integration suites passed (31 unit cases and 28 integration cases). PostgreSQL and large production data were not benchmarked in this phase.
