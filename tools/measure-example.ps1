param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$BaselineJar
)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$perfDir = Join-Path $taskRoot 'build/performance'
$java = Join-Path $JavaHome 'bin/java.exe'
$currentClasspath = Get-Content (Join-Path $perfDir 'classpath.txt') -Raw
$versions = [ordered]@{}
if ($BaselineJar) {
    $baselinePath = (Resolve-Path -LiteralPath $BaselineJar).Path
    $baselineClasspath = $currentClasspath.Replace((Join-Path $taskRoot 'build\libs\grails-admin-0.1.0-SNAPSHOT.jar'), $baselinePath)
    if ($baselineClasspath -eq $currentClasspath) { throw 'Baseline jar replacement failed' }
    $versions.baseline = $baselineClasspath
}
$versions.optimized = $currentClasspath
$password = [guid]::NewGuid().ToString()
$dbPath = (Join-Path $perfDir ('host-' + [guid]::NewGuid().ToString('N'))).Replace('\', '/')
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$listener.Start()
$port = $listener.LocalEndpoint.Port
$listener.Stop()
$baseUrl = "http://localhost:$port"
$config = @{ dataSource = @{ url = "jdbc:h2:file:$dbPath"; dbCreate = 'update' }; server = @{port = $port; address = '127.0.0.1'} } | ConvertTo-Json -Depth 5 -Compress

function Start-HostProcess($classpath, $main) {
    $info = [System.Diagnostics.ProcessStartInfo]::new($java)
    $info.WorkingDirectory = Join-Path $taskRoot 'example'
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    foreach ($arg in @('-Xms128m', '-Xmx512m', '-Dgrails.env=development', '-Dgrails.admin.setup.applicationClass=example.Application', '-cp', $classpath, $main)) { $info.ArgumentList.Add($arg) }
    $info.Environment['SPRING_APPLICATION_JSON'] = $config
    if ($main -eq 'grailsadmin.AdminSetupLauncher') {
        $info.Environment['GRAILS_ADMIN_SETUP_USERNAME'] = 'perf-admin'
        $info.Environment['GRAILS_ADMIN_SETUP_PASSWORD'] = $password
        $info.Environment['GRAILS_ADMIN_SETUP_CONFIRMATION'] = $password
    }
    $proc = [System.Diagnostics.Process]::new()
    $proc.StartInfo = $info
    [void]$proc.Start()
    return @{ Process = $proc; Output = $proc.StandardOutput.ReadToEndAsync(); Error = $proc.StandardError.ReadToEndAsync() }
}

function Hidden-Fields($html) {
    $body = @{}
    foreach ($input in [regex]::Matches($html, '<input\b[^>]*>')) {
        $attrs = @{}
        foreach ($attr in [regex]::Matches($input.Value, '([\w-]+)="([^"]*)"')) { $attrs[$attr.Groups[1].Value] = [System.Net.WebUtility]::HtmlDecode($attr.Groups[2].Value) }
        if ($attrs.type -eq 'hidden' -and $attrs.name) { $body[$attrs.name] = $attrs.value }
    }
    return $body
}

$setup = Start-HostProcess $currentClasspath 'grailsadmin.AdminSetupLauncher'
if (!$setup.Process.WaitForExit(60000)) { $setup.Process.Kill($true); throw 'Setup timed out' }
$setup.Output.Result | Set-Content (Join-Path $perfDir 'setup.log')
$setup.Error.Result | Add-Content (Join-Path $perfDir 'setup.log')
if ($setup.Process.ExitCode -ne 0) { throw 'Setup failed; see setup.log' }

$results = @()
foreach ($version in $versions.Keys) {
    $cp = $versions[$version]
    $timer = [System.Diagnostics.Stopwatch]::StartNew()
    $hostRun = Start-HostProcess $cp 'example.Application'
    try {
        $session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
        $login = $null
        while ($timer.Elapsed.TotalSeconds -lt 90 -and !$hostRun.Process.HasExited) {
            try { $login = Invoke-WebRequest "$baseUrl/admin/login" -WebSession $session -TimeoutSec 2; break } catch { Start-Sleep -Milliseconds 200 }
        }
        if (!$login) { throw 'Host did not become ready' }
        $startup = $timer.Elapsed.TotalMilliseconds
        $body = Hidden-Fields $login.Content
        $body.username = 'perf-admin'; $body.password = $password
        $signedIn = Invoke-WebRequest "$baseUrl/admin/authenticate" -Method Post -Body $body -WebSession $session
        if ($signedIn.Content -notmatch 'Site administration') { throw 'Authentication failed' }
        $pages = @{}
        foreach ($route in @('/admin/banking/customer', '/admin/banking/account', '/admin/banking/account/1/edit')) {
            $url = $baseUrl + $route
            [void](Invoke-WebRequest $url -WebSession $session)
            $samples = @(1..10 | ForEach-Object {
                $watch = [System.Diagnostics.Stopwatch]::StartNew()
                $response = Invoke-WebRequest $url -WebSession $session
                if ($response.StatusCode -ne 200) { throw 'Page failed' }
                $watch.Elapsed.TotalMilliseconds
            }) | Sort-Object
            $pages[$route] = [math]::Round(($samples[4] + $samples[5]) / 2, 2)
        }
        $hostRun.Process.Refresh()
        $results += [pscustomobject]@{version=$version; startupMs=[math]::Round($startup); workingSetMiB=[math]::Round($hostRun.Process.WorkingSet64 / 1MB, 1); medianPageMs=$pages}
    } finally {
        if (!$hostRun.Process.HasExited) { $hostRun.Process.Kill($true); $hostRun.Process.WaitForExit() }
        $hostRun.Output.Result | Set-Content (Join-Path $perfDir "$version.log")
        $hostRun.Error.Result | Add-Content (Join-Path $perfDir "$version.log")
    }
}
$results | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $perfDir 'host-results.json')
$results | ConvertTo-Json -Depth 5
