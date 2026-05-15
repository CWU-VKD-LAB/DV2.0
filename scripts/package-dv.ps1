#Requires -Version 5.1
<#
.SYNOPSIS
    Builds DV.jar, PyInstaller analytics bundles, and run/DV.zip for Windows distribution.

.DESCRIPTION
    Produces a zip that matches runtime paths used by the Java app:
      - DV.jar + run.bat at zip root
      - src/*.ptx (CUDA kernels)
      - source/Python/<tool>/ (PyInstaller COLLECT output)
      - source/Python/code_and_pyinstaller_spec/LinearRegression.py
      - documentation/ and Run Instructions.pdf when present under run/

.PARAMETER OutputZip
    Path to the zip to create (default: run/DV.zip).

.PARAMETER SkipJava
    Skip compiling DV.jar (reuse existing build/DV.jar).

.PARAMETER SkipPython
    Dev-only: skip PyInstaller (reuse build/python-dist from a prior run). Default is a full rebuild.

.PARAMETER SkipZip
    Only populate build/package-staging; do not write the zip.

.PARAMETER MavenRepo
    Local Maven repository (default: %USERPROFILE%\.m2\repository).

.EXAMPLE
    .\scripts\package-dv.ps1

.EXAMPLE
    .\scripts\package-dv.ps1 -SkipPython
#>
[CmdletBinding()]
param(
    [string]$OutputZip = "",
    [switch]$SkipJava,
    [switch]$SkipPython,
    [switch]$SkipZip,
    [string]$MavenRepo = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if (-not $OutputZip) {
    $OutputZip = Join-Path $Root "run\DV.zip"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputZip)) {
    $OutputZip = Join-Path $Root $OutputZip
}
if (-not $MavenRepo) {
    $MavenRepo = Join-Path $env:USERPROFILE ".m2\repository"
}

$BuildDir = Join-Path $Root "build"
$ClassesDir = Join-Path $BuildDir "classes"
$JarPath = Join-Path $BuildDir "DV.jar"
$StagingDir = Join-Path $BuildDir "package-staging"
$PythonSpecDir = Join-Path $Root "source\Python\code_and_pyinstaller_spec"
$PythonDistDir = Join-Path $BuildDir "python-dist"
$PackageVenvDir = Join-Path $Root ".venv-package"
$PackageRequirements = Join-Path $PSScriptRoot "requirements-package.txt"
$MinPythonMinor = 10
$MaxPythonMinor = 12

$PythonTools = @(
    "ConfusionMatrixGenerator",
    "kFoldCrossValidation",
    "LinearDiscriminantAnalysis",
    "DecisionTree",
    "SupportVectorMachine"
)

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Require-Command([string]$Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $cmd) {
        throw "Required command not found on PATH: $Name"
    }
    return $cmd.Source
}

function Get-MavenJar([string]$GroupId, [string]$ArtifactId, [string]$Version, [string]$Classifier = "") {
    $jarName = if ($Classifier) { "$ArtifactId-$Version-$Classifier.jar" } else { "$ArtifactId-$Version.jar" }
    $relative = ($GroupId -replace '\.', '\') + "\$ArtifactId\$Version\$jarName"
    $path = Join-Path $MavenRepo $relative
    if (-not (Test-Path -LiteralPath $path)) {
        $urlPath = ($GroupId -replace '\.', '/') + "/$ArtifactId/$Version/$jarName"
        $url = "https://repo1.maven.org/maven2/$urlPath"
        Write-Host "Downloading Maven artifact: $url"
        New-Item -ItemType Directory -Path (Split-Path -Parent $path) -Force | Out-Null
        Invoke-WebRequest -Uri $url -OutFile $path -UseBasicParsing
    }
    return (Resolve-Path -LiteralPath $path).Path
}

function Initialize-JavaToolchain {
    if (Get-Command javac -ErrorAction SilentlyContinue) {
        return
    }

    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME "bin\javac.exe")
    }
    $searchRoots = @(
        "C:\Program Files\Microsoft",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Amazon Corretto"
    )
    foreach ($root in $searchRoots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        $candidates += Get-ChildItem -Path $root -Recurse -Filter "javac.exe" -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match 'jdk-?17|jdk17|17\.' } |
            Select-Object -ExpandProperty FullName
    }

    foreach ($javac in $candidates) {
        if (-not (Test-Path -LiteralPath $javac)) { continue }
        $jdkHome = (Resolve-Path (Join-Path (Split-Path -Parent $javac) "..")).Path
        $env:JAVA_HOME = $jdkHome
        $env:Path = (Join-Path $jdkHome "bin") + ";" + $env:Path
        Write-Host "Using JDK: $jdkHome"
        return
    }

    throw @"
Java 17 JDK not found (javac is not on PATH).
Install a JDK 17 distribution, then re-run. Example:
  winget install Microsoft.OpenJDK.17
"@
}

function Get-PythonVersionMinor([string]$PythonExe) {
    $raw = & $PythonExe -c "import sys; print(sys.version_info[0], sys.version_info[1])"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to query Python version: $PythonExe"
    }
    $parts = $raw.Trim() -split '\s+'
    return [int]$parts[0], [int]$parts[1]
}

function Test-PythonVersionSupported([string]$PythonExe) {
    $major, $minor = Get-PythonVersionMinor $PythonExe
    return ($major -eq 3 -and $minor -ge $MinPythonMinor -and $minor -le $MaxPythonMinor)
}

function Resolve-SystemPythonExe {
    if ($env:DV_PACKAGE_PYTHON) {
        if (-not (Test-Path -LiteralPath $env:DV_PACKAGE_PYTHON)) {
            throw "DV_PACKAGE_PYTHON points to a missing file: $($env:DV_PACKAGE_PYTHON)"
        }
        return (Resolve-Path -LiteralPath $env:DV_PACKAGE_PYTHON).Path
    }

    if (Get-Command py -ErrorAction SilentlyContinue) {
        foreach ($ver in @("3.12", "3.11", "3.10")) {
            $candidate = & py "-$ver" -c "import sys; print(sys.executable)" 2>$null
            if ($LASTEXITCODE -eq 0 -and $candidate -and (Test-Path -LiteralPath $candidate.Trim())) {
                return $candidate.Trim()
            }
        }
    }

    if (Get-Command python -ErrorAction SilentlyContinue) {
        $candidate = (Get-Command python).Source
        if (Test-PythonVersionSupported $candidate) {
            return $candidate
        }
    }

    throw @"
No suitable Python found for packaging (need 3.$MinPythonMinor-3.$MaxPythonMinor for PyInstaller 5.x).
Install Python 3.12 and re-run. Examples:
  winget install Python.Python.3.12
  py -3.12 -m venv .venv-package
Or set DV_PACKAGE_PYTHON to a 3.10-3.12 interpreter path.
"@
}

function Ensure-PackageVenv {
    $venvPython = Join-Path $PackageVenvDir "Scripts\python.exe"
    $recreate = $false

    if (Test-Path -LiteralPath $venvPython) {
        try {
            if (-not (Test-PythonVersionSupported $venvPython)) {
                $recreate = $true
            }
        }
        catch {
            $recreate = $true
        }
    }
    else {
        $recreate = $true
    }

    if ($recreate) {
        Write-Host "Creating packaging venv at $PackageVenvDir"
        Remove-DirectorySafe $PackageVenvDir
        $basePython = Resolve-SystemPythonExe
        if (-not (Test-PythonVersionSupported $basePython)) {
            $major, $minor = Get-PythonVersionMinor $basePython
            throw "Refusing to create venv with Python $major.$minor (need 3.$MinPythonMinor-3.$MaxPythonMinor)."
        }
        & $basePython -m venv $PackageVenvDir
        if ($LASTEXITCODE -ne 0) {
            throw "python -m venv failed with exit code $LASTEXITCODE"
        }
        $venvPython = Join-Path $PackageVenvDir "Scripts\python.exe"
    }

    Write-Host "Installing packaging dependencies into venv..."
    $null = & $venvPython -m pip install --upgrade pip wheel 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "pip upgrade failed with exit code $LASTEXITCODE"
    }
    Write-Host "  pip install -r requirements-package.txt"
    $null = & $venvPython -m pip install --upgrade -r $PackageRequirements 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "pip install -r requirements-package.txt failed with exit code $LASTEXITCODE"
    }

    $pyiVersion = (& $venvPython -c "import PyInstaller; print(PyInstaller.__version__)" 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "PyInstaller is not importable in the packaging venv: $($pyiVersion.Trim())"
    }
    $pyiVersion = $pyiVersion.Trim()

    $major, $minor = Get-PythonVersionMinor $venvPython
    Write-Host "Packaging Python: $venvPython ($major.$minor, PyInstaller $pyiVersion)"
    return $venvPython
}

function Expand-JarInto([string]$JarFile, [string]$Destination) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($JarFile)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName.EndsWith("/")) { continue }
            $destPath = Join-Path $Destination ($entry.FullName -replace '/', '\')
            $destDir = Split-Path -Parent $destPath
            if ($destDir -and -not (Test-Path -LiteralPath $destDir)) {
                New-Item -ItemType Directory -Path $destDir -Force | Out-Null
            }
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destPath, $true)
        }
    }
    finally {
        $zip.Dispose()
    }
}

function Remove-DirectorySafe([string]$Path) {
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
}

function Copy-Directory([string]$Source, [string]$Destination) {
    if (-not (Test-Path -LiteralPath $Source)) {
        throw "Missing directory: $Source"
    }
    New-Item -ItemType Directory -Path $Destination -Force | Out-Null
    Copy-Item -Path (Join-Path $Source "*") -Destination $Destination -Recurse -Force
}

function Build-JavaJar {
    Write-Step "Building DV.jar"
    Require-Command "javac" | Out-Null
    Require-Command "jar" | Out-Null

    $jcuda = Get-MavenJar "org.jcuda" "jcuda" "12.0.0"
    $jcudaNatives = Get-MavenJar "org.jcuda" "jcuda-natives" "12.0.0" "windows-x86_64"
    $jfreechart = Get-MavenJar "org.jfree" "jfreechart" "1.5.4"
    $classpath = ($jcuda, $jcudaNatives, $jfreechart) -join ";"

    Remove-DirectorySafe $ClassesDir
    New-Item -ItemType Directory -Path $ClassesDir -Force | Out-Null

    $javaFiles = Get-ChildItem -Path (Join-Path $Root "src") -Filter "*.java" -Recurse |
        ForEach-Object { $_.FullName }
    if ($javaFiles.Count -eq 0) {
        throw "No Java sources found under src/"
    }

    $javacArgs = @("-encoding", "UTF-8", "-d", $ClassesDir, "-cp", $classpath) + $javaFiles
    & javac @javacArgs
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed with exit code $LASTEXITCODE"
    }

    $jarRoot = Join-Path $BuildDir "jar-contents"
    Remove-DirectorySafe $jarRoot
    New-Item -ItemType Directory -Path $jarRoot -Force | Out-Null

    foreach ($depJar in @($jcuda, $jcudaNatives, $jfreechart)) {
        Expand-JarInto $depJar $jarRoot
    }

    # Remove dependency manifests/signatures before merging application classes.
    Get-ChildItem -Path $jarRoot -Directory -Filter "META-INF" -Recurse -ErrorAction SilentlyContinue |
        Remove-Item -Recurse -Force

    Copy-Item -Path (Join-Path $ClassesDir "*") -Destination $jarRoot -Recurse -Force

    $manifestDir = Join-Path $jarRoot "META-INF"
    New-Item -ItemType Directory -Path $manifestDir -Force | Out-Null
    @"
Manifest-Version: 1.0
Main-Class: Main

"@ | Set-Content -LiteralPath (Join-Path $manifestDir "MANIFEST.MF") -Encoding ASCII -NoNewline

    if (Test-Path -LiteralPath $JarPath) {
        Remove-Item -LiteralPath $JarPath -Force
    }

    Push-Location $jarRoot
    try {
        & jar cfm $JarPath META-INF\MANIFEST.MF .
        if ($LASTEXITCODE -ne 0) {
            throw "jar failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }

    Write-Host "Wrote $JarPath"
}

function Assert-PythonBundle([string]$ToolDir, [string]$ToolName) {
    $exe = Join-Path $ToolDir "$ToolName.exe"
    if (-not (Test-Path -LiteralPath $exe)) {
        throw "PyInstaller output is incomplete (missing $exe)."
    }
    $fileCount = (Get-ChildItem -Path $ToolDir -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
    if ($fileCount -lt 10) {
        throw "PyInstaller output for $ToolName looks empty ($fileCount files under $ToolDir)."
    }
}

function Build-PythonTools {
    Write-Step "Building Python analytics bundles (PyInstaller)"
    $venvPython = Ensure-PackageVenv

    Remove-DirectorySafe $PythonDistDir
    Remove-DirectorySafe (Join-Path $PythonSpecDir "build")
    Remove-DirectorySafe (Join-Path $PythonSpecDir "dist")
    New-Item -ItemType Directory -Path $PythonDistDir -Force | Out-Null

    Push-Location $PythonSpecDir
    try {
        foreach ($tool in $PythonTools) {
            $spec = "$tool.spec"
            if (-not (Test-Path -LiteralPath $spec)) {
                throw "Missing PyInstaller spec: $(Join-Path $PythonSpecDir $spec)"
            }

            Write-Host "  PyInstaller: $tool"
            & $venvPython -m PyInstaller --noconfirm --clean $spec
            if ($LASTEXITCODE -ne 0) {
                throw "PyInstaller failed for $tool (exit $LASTEXITCODE)"
            }

            $distDir = Join-Path $PythonSpecDir "dist\$tool"
            if (-not (Test-Path -LiteralPath $distDir)) {
                throw "Expected PyInstaller output directory: $distDir"
            }

            Assert-PythonBundle $distDir $tool
            Copy-Directory $distDir (Join-Path $PythonDistDir $tool)
        }
    }
    finally {
        Pop-Location
    }
}

function Initialize-Staging {
    Write-Step "Preparing package staging directory"
    Remove-DirectorySafe $StagingDir
    New-Item -ItemType Directory -Path $StagingDir -Force | Out-Null
}

function Add-StagingPayload {
    if (-not (Test-Path -LiteralPath $JarPath)) {
        throw "DV.jar not found at $JarPath. Run without -SkipJava or build the jar first."
    }

    Copy-Item -LiteralPath $JarPath -Destination (Join-Path $StagingDir "DV.jar") -Force
    @"
java -jar DV.jar
"@ | Set-Content -LiteralPath (Join-Path $StagingDir "run.bat") -Encoding ASCII -NoNewline

    $runInstructions = Join-Path $Root "run\Run Instructions.pdf"
    if (Test-Path -LiteralPath $runInstructions) {
        Copy-Item -LiteralPath $runInstructions -Destination $StagingDir -Force
    } else {
        Write-Warning "Missing run\Run Instructions.pdf (skipped)"
    }

    $runDocs = Join-Path $Root "run\documentation"
    if (Test-Path -LiteralPath $runDocs) {
        Copy-Directory $runDocs (Join-Path $StagingDir "documentation")
    } else {
        $repoDocs = Join-Path $Root "documentation"
        if (Test-Path -LiteralPath $repoDocs) {
            Copy-Directory $repoDocs (Join-Path $StagingDir "documentation")
        }
    }

    $ptxDir = Join-Path $StagingDir "src"
    New-Item -ItemType Directory -Path $ptxDir -Force | Out-Null
    Get-ChildItem -Path (Join-Path $Root "src") -Filter "*.ptx" | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $ptxDir -Force
    }
    $ptxCount = (Get-ChildItem -Path $ptxDir -Filter "*.ptx").Count
    if ($ptxCount -lt 3) {
        Write-Warning "Expected 3 CUDA .ptx files under src/; found $ptxCount"
    }

    $pythonRoot = Join-Path $StagingDir "source\Python"
    New-Item -ItemType Directory -Path $pythonRoot -Force | Out-Null

    foreach ($tool in $PythonTools) {
        $built = Join-Path $PythonDistDir $tool
        if (-not (Test-Path -LiteralPath $built)) {
            if ($SkipPython) {
                throw "SkipPython set but $built is missing. Run without -SkipPython for a full rebuild."
            }
            throw "Missing built Python bundle: $built"
        }
        Assert-PythonBundle $built $tool
        Copy-Directory $built (Join-Path $pythonRoot $tool)
    }

    $lrSpecDir = Join-Path $pythonRoot "code_and_pyinstaller_spec"
    New-Item -ItemType Directory -Path $lrSpecDir -Force | Out-Null
    $lrScript = Join-Path $PythonSpecDir "LinearRegression.py"
    if (-not (Test-Path -LiteralPath $lrScript)) {
        throw "Missing LinearRegression.py at $lrScript"
    }
    Copy-Item -LiteralPath $lrScript -Destination $lrSpecDir -Force
}

function Write-PackageZip {
    Write-Step "Writing $OutputZip"
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    $zipDir = Split-Path -Parent $OutputZip
    if ($zipDir -and -not (Test-Path -LiteralPath $zipDir)) {
        New-Item -ItemType Directory -Path $zipDir -Force | Out-Null
    }

    if (Test-Path -LiteralPath $OutputZip) {
        Remove-Item -LiteralPath $OutputZip -Force
    }

    [System.IO.Compression.ZipFile]::CreateFromDirectory(
        $StagingDir,
        $OutputZip,
        [System.IO.Compression.CompressionLevel]::Optimal,
        $false
    )

    $sizeMb = [math]::Round((Get-Item -LiteralPath $OutputZip).Length / 1MB, 1)
    Write-Host "Created $OutputZip ($sizeMb MB)"
    Write-Host ""
    Write-Host "Next: commit with Git LFS (run/DV.zip is LFS-tracked):" -ForegroundColor Yellow
    Write-Host "  git add run/DV.zip"
    Write-Host "  git commit -m ""Update Windows runtime bundle (DV.zip)"""
}

Write-Host "DV package build"
Write-Host "  Root:       $Root"
Write-Host "  Output zip: $OutputZip"

Initialize-JavaToolchain

if (-not $SkipJava) {
    Build-JavaJar
} elseif (-not (Test-Path -LiteralPath $JarPath)) {
    throw "SkipJava set but $JarPath does not exist."
} else {
    Write-Host "Skipping Java build; using $JarPath"
}

if (-not $SkipPython) {
    Build-PythonTools
} else {
    Write-Warning "SkipPython: reusing build/python-dist (not rebuilding from source/Python/code_and_pyinstaller_spec)."
    foreach ($tool in $PythonTools) {
        $built = Join-Path $PythonDistDir $tool
        if (-not (Test-Path -LiteralPath $built)) {
            throw "SkipPython set but $built is missing. Run without -SkipPython."
        }
        Assert-PythonBundle $built $tool
    }
}

Initialize-Staging
Add-StagingPayload

if ($SkipZip) {
    Write-Host "SkipZip set; staging directory: $StagingDir"
} else {
    Write-PackageZip
}

Write-Host ""
Write-Host "Done." -ForegroundColor Green
