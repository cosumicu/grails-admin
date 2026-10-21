param(
    [string]$GradleTask = 'runCommand',
    [string[]]$ExtraGradleArgs = @()
)

$wrapper = Join-Path (Get-Location) 'gradlew.bat'
if (-not (Test-Path -LiteralPath $wrapper)) {
    throw 'Run this launcher from the main Grails application directory containing gradlew.bat.'
}

$adminName = Read-Host 'Admin username'
$secret = Read-Host 'Password' -AsSecureString
$confirmation = Read-Host 'Confirm password' -AsSecureString

function Get-PlainText([Security.SecureString]$value) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($value)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

try {
    $env:GRAILS_ADMIN_SETUP_USERNAME = $adminName
    $env:GRAILS_ADMIN_SETUP_PASSWORD = Get-PlainText $secret
    $env:GRAILS_ADMIN_SETUP_CONFIRMATION = Get-PlainText $confirmation
    $proceed = $true
    $passwordBytes = [Text.Encoding]::UTF8.GetByteCount($env:GRAILS_ADMIN_SETUP_PASSWORD)
    if ($env:GRAILS_ADMIN_SETUP_PASSWORD.Length -lt 12 -or $passwordBytes -gt 72) {
        Write-Warning 'Passwords under 12 characters are easier to guess. Passwords over 72 UTF-8 bytes use SHA-256 before bcrypt so the full password is checked.'
        $answer = Read-Host 'Use this password anyway? Type yes or no'
        $proceed = $answer.Trim() -ieq 'yes'
        if ($proceed) { $env:GRAILS_ADMIN_SETUP_PASSWORD_WARNING_ACCEPTED = 'yes' }
    }
    if ($proceed) {
        & $wrapper $GradleTask '-Pargs=admin-setup' @ExtraGradleArgs
        $result = $LASTEXITCODE
    } else {
        Write-Host 'Setup cancelled.'
        $result = 1
    }
} finally {
    Remove-Item Env:GRAILS_ADMIN_SETUP_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:GRAILS_ADMIN_SETUP_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:GRAILS_ADMIN_SETUP_CONFIRMATION -ErrorAction SilentlyContinue
    Remove-Item Env:GRAILS_ADMIN_SETUP_PASSWORD_WARNING_ACCEPTED -ErrorAction SilentlyContinue
}
exit $result
