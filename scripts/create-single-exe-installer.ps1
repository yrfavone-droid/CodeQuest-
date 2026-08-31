[CmdletBinding()]
param(
    [string]$Version,
    [switch]$Publish
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot ".."))
$versionProperty = Get-Content (Join-Path $root "gradle.properties") | Where-Object { $_ -match '^nous\.version=' } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($Version)) {
    if ($null -eq $versionProperty) { throw "nous.version is missing from gradle.properties" }
    $Version = ($versionProperty -split '=', 2)[1].Trim()
}
$csc = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"

if (!(Test-Path $csc)) {
    throw "C# Compiler (csc.exe) not found at $csc"
}

$appDir = Join-Path $root "desktopApp\build\compose\binaries\main\app\Nous-AI-Academy"
if (!(Test-Path (Join-Path $appDir "Nous-AI-Academy.exe"))) {
    throw "Packaged application not found at $appDir. Please run Gradle build first."
}

Write-Host "Creating Single EXE Installer for Nous AI Academy v$Version..."

$tempDir = Join-Path $root "build\installer_temp"
if (Test-Path $tempDir) { Remove-Item -LiteralPath $tempDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

$payloadZip = Join-Path $tempDir "payload.zip"
Write-Host "Compressing application payload into zip resource..."
Compress-Archive -Path "$appDir\*" -DestinationPath $payloadZip -CompressionLevel Optimal

if (!(Test-Path $payloadZip)) {
    throw "Payload zip creation failed."
}

$csharpSource = @"
using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.IO.Compression;
using Microsoft.Win32;
using System.Reflection;
using System.Threading;
using System.Windows.Forms;

namespace NousAIAcademyInstaller
{
    public class SetupForm : Form
    {
        private ProgressBar progressBar;
        private Label statusLabel;
        private Button installButton;
        private CheckBox launchCheckBox;
        private bool isSilent = false;
        private bool shouldLaunch = true;

        public SetupForm(bool silent = false, bool launchAfterInstall = true)
        {
            this.isSilent = silent;
            this.shouldLaunch = launchAfterInstall;
            this.Text = "Nous AI Academy Setup v$Version";
            this.Size = new Size(520, 320);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.BackColor = Color.FromArgb(255, 248, 241);
            this.ForeColor = Color.FromArgb(39, 31, 27);
            if (silent)
            {
                this.ShowInTaskbar = false;
                this.Opacity = 0;
                this.FormBorderStyle = FormBorderStyle.None;
                this.Size = new Size(1, 1);
                this.StartPosition = FormStartPosition.Manual;
                this.Location = new Point(-32000, -32000);
            }

            Label titleLabel = new Label();
            titleLabel.Text = "Nous AI Academy Setup";
            titleLabel.Font = new Font("Segoe UI", 16, FontStyle.Bold);
            titleLabel.ForeColor = Color.FromArgb(201, 84, 42);
            titleLabel.Location = new Point(30, 25);
            titleLabel.AutoSize = true;

            Label descLabel = new Label();
            descLabel.Text = "Installing Nous AI Academy v$Version with bundled JVM runtime...\nNo manual ZIP extraction required.";
            descLabel.Font = new Font("Segoe UI", 9, FontStyle.Regular);
            descLabel.ForeColor = Color.FromArgb(87, 74, 67);
            descLabel.Location = new Point(30, 65);
            descLabel.Size = new Size(440, 40);

            progressBar = new ProgressBar();
            progressBar.Location = new Point(30, 120);
            progressBar.Size = new Size(440, 24);
            progressBar.Style = ProgressBarStyle.Continuous;

            statusLabel = new Label();
            statusLabel.Text = "Initializing setup...";
            statusLabel.Font = new Font("Segoe UI", 9, FontStyle.Italic);
            statusLabel.ForeColor = Color.FromArgb(123, 109, 101);
            statusLabel.Location = new Point(30, 155);
            statusLabel.Size = new Size(440, 25);

            launchCheckBox = new CheckBox();
            launchCheckBox.Text = "Launch Nous AI Academy after setup";
            launchCheckBox.Font = new Font("Segoe UI", 9);
            launchCheckBox.ForeColor = Color.FromArgb(39, 31, 27);
            launchCheckBox.Checked = launchAfterInstall;
            launchCheckBox.Location = new Point(30, 190);
            launchCheckBox.AutoSize = true;

            installButton = new Button();
            installButton.Text = "Installing...";
            installButton.Font = new Font("Segoe UI", 10, FontStyle.Bold);
            installButton.BackColor = Color.FromArgb(229, 106, 58);
            installButton.ForeColor = Color.White;
            installButton.FlatStyle = FlatStyle.Flat;
            installButton.FlatAppearance.BorderSize = 0;
            installButton.Location = new Point(340, 220);
            installButton.Size = new Size(130, 38);
            installButton.Enabled = false;

            this.Controls.Add(titleLabel);
            this.Controls.Add(descLabel);
            this.Controls.Add(progressBar);
            this.Controls.Add(statusLabel);
            this.Controls.Add(launchCheckBox);
            this.Controls.Add(installButton);

            // Auto-start installation on window show
            this.Shown += (s, e) =>
            {
                ThreadPool.QueueUserWorkItem((state) => PerformInstall());
            };
        }

        private void PerformInstall()
        {
            try
            {
                UpdateStatus("Preparing installation directory...", 10);
                string localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
                string targetDir = Path.Combine(localAppData, @"Programs\Nous AI Academy");
                string targetPrefix = Path.GetFullPath(targetDir).TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar) + Path.DirectorySeparatorChar;

                // Kill running processes safely
                try
                {
                    foreach (var process in Process.GetProcessesByName("Nous-AI-Academy"))
                    {
                        try { process.Kill(); } catch { }
                    }
                    Thread.Sleep(500);
                }
                catch { }

                if (Directory.Exists(targetDir))
                {
                    UpdateStatus("Cleaning previous application files...", 18);
                    DeleteInstallArtifact(Path.Combine(targetDir, "app"));
                    DeleteInstallArtifact(Path.Combine(targetDir, "runtime"));
                    DeleteInstallArtifact(Path.Combine(targetDir, "Nous-AI-Academy.exe"));
                    DeleteInstallArtifact(Path.Combine(targetDir, "Uninstall Nous AI Academy.exe"));
                }

                Directory.CreateDirectory(targetDir);

                UpdateStatus("Extracting application files and JVM runtime...", 25);
                Assembly assembly = Assembly.GetExecutingAssembly();
                string tempZip = Path.Combine(Path.GetTempPath(), "nous_ai_academy_setup_payload.zip");

                using (Stream stream = assembly.GetManifestResourceStream("payload.zip"))
                {
                    if (stream == null) throw new Exception("Embedded payload resource missing!");
                    using (FileStream fileStream = new FileStream(tempZip, FileMode.Create, FileAccess.Write))
                    {
                        stream.CopyTo(fileStream);
                    }
                }

                // Robust entry-by-entry extraction with overwrite support
                using (ZipArchive archive = ZipFile.OpenRead(tempZip))
                {
                    int total = archive.Entries.Count;
                    int current = 0;

                    foreach (ZipArchiveEntry entry in archive.Entries)
                    {
                        current++;
                        string destinationPath = Path.GetFullPath(Path.Combine(targetDir, entry.FullName));

                        if (destinationPath.StartsWith(targetPrefix, StringComparison.OrdinalIgnoreCase))
                        {
                            if (string.IsNullOrEmpty(entry.Name))
                            {
                                Directory.CreateDirectory(destinationPath);
                            }
                            else
                            {
                                Directory.CreateDirectory(Path.GetDirectoryName(destinationPath));
                                try
                                {
                                    if (File.Exists(destinationPath))
                                    {
                                        File.SetAttributes(destinationPath, FileAttributes.Normal);
                                        File.Delete(destinationPath);
                                    }
                                }
                                catch { }
                                entry.ExtractToFile(destinationPath, true);
                            }
                        }

                        if (current % 20 == 0 || current == total)
                        {
                            int pct = 25 + (int)((current / (double)total) * 60);
                            UpdateStatus("Extracting " + entry.Name + "...", pct);
                        }
                    }
                }

                // Files extracted from a browser-downloaded installer can inherit
                // Windows' Mark-of-the-Web alternate data stream.  Windows Defender
                // and Smart App Control may then block javaw.exe even though the
                // installer itself completed successfully, surfacing as the generic
                // "Failed to launch JVM" dialog.  The installer has already been
                // explicitly run by the user, so remove the marker from the files
                // we just extracted and leave user data untouched.
                ClearDownloadSecurityMarkers(targetDir);

                try { File.Delete(tempZip); } catch { }

                // Validate JVM installation
                string exePath = Path.Combine(targetDir, "Nous-AI-Academy.exe");
                string runtimeDir = Path.Combine(targetDir, "runtime");
                string jvmDll = Path.Combine(runtimeDir, @"bin\server\jvm.dll");
                string javawExe = Path.Combine(runtimeDir, @"bin\javaw.exe");
                string runtimeModules = Path.Combine(runtimeDir, @"lib\modules");
                string appConfig = Path.Combine(targetDir, @"app\Nous-AI-Academy.cfg");

                if (!File.Exists(exePath))
                {
                    throw new Exception("Executable Nous-AI-Academy.exe was not created in " + targetDir);
                }

                if (!Directory.Exists(runtimeDir))
                {
                    throw new Exception("JVM runtime directory was not extracted properly to " + runtimeDir);
                }

                if (!File.Exists(jvmDll) || !File.Exists(javawExe) || !File.Exists(runtimeModules) || !File.Exists(appConfig))
                {
                    throw new Exception("Bundled JVM runtime validation failed. Please run the installer again from the official download.");
                }

                UpdateStatus("Creating shortcuts and uninstaller...", 90);
                CreateShortcuts(exePath);
                CreateUninstallerAndRegistration(targetDir);

                UpdateStatus("Installation Completed Successfully!", 100);

                Thread.Sleep(600);

                this.Invoke((MethodInvoker)delegate
                {
                    if (!isSilent)
                    {
                        MessageBox.Show("Nous AI Academy v$Version installed successfully with bundled JVM runtime!", "Setup Complete", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    }

                    if (shouldLaunch && launchCheckBox.Checked && File.Exists(exePath))
                    {
                        ProcessStartInfo psi = new ProcessStartInfo();
                        psi.FileName = exePath;
                        psi.WorkingDirectory = targetDir; // Critical: Set working directory so relative runtime/ path is found
                        Process.Start(psi);
                    }
                    Application.Exit();
                });
            }
            catch (Exception ex)
            {
                this.Invoke((MethodInvoker)delegate
                {
                    if (!isSilent) MessageBox.Show("Installation Error: " + ex.Message, "Setup Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    installButton.Enabled = true;
                    installButton.Text = "Retry";
                    if (isSilent) Application.Exit();
                });
            }
        }

        private void UpdateStatus(string message, int progress)
        {
            if (this.InvokeRequired)
            {
                this.Invoke((MethodInvoker)delegate { UpdateStatus(message, progress); });
                return;
            }
            statusLabel.Text = message;
            progressBar.Value = Math.Min(100, Math.Max(0, progress));
        }

        private static void DeleteInstallArtifact(string path)
        {
            try
            {
                if (File.Exists(path))
                {
                    File.SetAttributes(path, FileAttributes.Normal);
                    File.Delete(path);
                    return;
                }

                if (!Directory.Exists(path)) return;

                foreach (string file in Directory.GetFiles(path, "*", SearchOption.AllDirectories))
                {
                    try { File.SetAttributes(file, FileAttributes.Normal); } catch { }
                }
                foreach (string dir in Directory.GetDirectories(path, "*", SearchOption.AllDirectories))
                {
                    try { File.SetAttributes(dir, FileAttributes.Normal); } catch { }
                }
                Directory.Delete(path, true);
            }
            catch (Exception ex)
            {
                throw new Exception("Could not clean previous install file: " + path + ". Close Nous AI Academy and retry. " + ex.Message);
            }
        }

        private static void ClearDownloadSecurityMarkers(string root)
        {
            try
            {
                foreach (string file in Directory.GetFiles(root, "*", SearchOption.AllDirectories))
                {
                    try { File.Delete(file + ":Zone.Identifier"); } catch { }
                }
            }
            catch { }
        }

        private void CreateShortcuts(string targetExePath)
        {
            try
            {
                string targetDir = Path.GetDirectoryName(targetExePath);
                string desktop = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);
                string startMenu = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.StartMenu), "Programs");

                CreateShortcutFile(Path.Combine(desktop, "Nous AI Academy.lnk"), targetExePath, targetDir);
                CreateShortcutFile(Path.Combine(startMenu, "Nous AI Academy.lnk"), targetExePath, targetDir);
            }
            catch { }
        }

        private void CreateShortcutFile(string shortcutPath, string targetExePath, string workingDir)
        {
            Type shellType = Type.GetTypeFromProgID("WScript.Shell");
            dynamic shell = Activator.CreateInstance(shellType);
            dynamic shortcut = shell.CreateShortcut(shortcutPath);
            shortcut.TargetPath = targetExePath;
            shortcut.WorkingDirectory = workingDir;
            shortcut.Description = "Nous AI Academy - Read deeply. Build locally.";
            shortcut.Save();
        }

        private void CreateUninstallerAndRegistration(string targetDir)
        {
            string uninstallerPath = Path.Combine(targetDir, "Uninstall Nous AI Academy.exe");
            File.Copy(Application.ExecutablePath, uninstallerPath, true);

            using (RegistryKey key = Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\Windows\CurrentVersion\Uninstall\Nous AI Academy"))
            {
                if (key == null) throw new Exception("Could not register the uninstaller.");
                key.SetValue("DisplayName", "Nous AI Academy");
                key.SetValue("DisplayVersion", "$Version");
                key.SetValue("Publisher", "Nous AI Academy");
                key.SetValue("InstallLocation", targetDir);
                key.SetValue("DisplayIcon", Path.Combine(targetDir, "Nous-AI-Academy.exe"));
                key.SetValue("UninstallString", "\"" + uninstallerPath + "\" /uninstall");
                key.SetValue("NoModify", 1, RegistryValueKind.DWord);
                key.SetValue("NoRepair", 1, RegistryValueKind.DWord);
            }
        }

        private static void DeleteShortcut(string shortcutPath)
        {
            try { if (File.Exists(shortcutPath)) File.Delete(shortcutPath); } catch { }
        }

        private static void Uninstall(bool silent)
        {
            string localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            string targetDir = Path.Combine(localAppData, @"Programs\Nous AI Academy");
            try
            {
                foreach (var process in Process.GetProcessesByName("Nous-AI-Academy"))
                {
                    try { process.Kill(); } catch { }
                }
            }
            catch { }

            string desktop = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);
            string startMenu = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.StartMenu), "Programs");
            DeleteShortcut(Path.Combine(desktop, "Nous AI Academy.lnk"));
            DeleteShortcut(Path.Combine(startMenu, "Nous AI Academy.lnk"));
            try { Registry.CurrentUser.DeleteSubKeyTree(@"Software\Microsoft\Windows\CurrentVersion\Uninstall\Nous AI Academy", false); } catch { }

            string current = Application.ExecutablePath;
            if (Directory.Exists(targetDir))
            {
                try
                {
                    // The active uninstaller cannot delete itself. Use a short-lived cmd process after this process exits.
                    string command = "/c ping 127.0.0.1 -n 2 > nul & rmdir /s /q \"" + targetDir + "\"";
                    Process.Start(new ProcessStartInfo("cmd.exe", command) { CreateNoWindow = true, UseShellExecute = false });
                }
                catch
                {
                    if (!silent) MessageBox.Show("Could not remove all files. Close Nous AI Academy and try again.", "Uninstall incomplete", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                    return;
                }
            }
            if (!silent) MessageBox.Show("Nous AI Academy was removed. Your local reader data was kept.", "Uninstall complete", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }

        [STAThread]
        public static void Main(string[] args)
        {
            bool silent = false;
            bool uninstall = false;
            bool launchAfterInstall = true;
            foreach (string arg in args)
            {
                if (arg.Equals("/S", StringComparison.OrdinalIgnoreCase) || arg.Equals("/silent", StringComparison.OrdinalIgnoreCase))
                {
                    silent = true;
                }
                else if (arg.Equals("/uninstall", StringComparison.OrdinalIgnoreCase))
                {
                    uninstall = true;
                }
                else if (arg.Equals("/no-launch", StringComparison.OrdinalIgnoreCase))
                {
                    launchAfterInstall = false;
                }
            }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            if (uninstall) { Uninstall(silent); return; }
            Application.Run(new SetupForm(silent, launchAfterInstall));
        }
    }
}
"@

$csharpFile = Join-Path $tempDir "Installer.cs"
$csharpSource | Set-Content -LiteralPath $csharpFile -Encoding UTF8

$outputExeName = "Nous-AI-Academy-Setup-$Version.exe"
$outputExePath = Join-Path $tempDir $outputExeName

$resArg = "/resource:$payloadZip,payload.zip"
$iconPath = Join-Path $root "desktopApp\src\jvmMain\resources\branding\nous-ai-academy-logo.ico"

Write-Host "Compiling Single EXE Installer using csc.exe..."
if (Test-Path $iconPath) {
    & $csc /target:winexe /out:"$outputExePath" "$resArg" "/win32icon:$iconPath" /reference:System.dll,System.Drawing.dll,System.Windows.Forms.dll,System.IO.Compression.dll,System.IO.Compression.FileSystem.dll "$csharpFile"
} else {
    & $csc /target:winexe /out:"$outputExePath" "$resArg" /reference:System.dll,System.Drawing.dll,System.Windows.Forms.dll,System.IO.Compression.dll,System.IO.Compression.FileSystem.dll "$csharpFile"
}

if ($LASTEXITCODE -ne 0 -or !(Test-Path $outputExePath)) {
    throw "Failed to compile single EXE installer."
}

$fileSize = (Get-Item $outputExePath).Length
$sha256 = [System.Security.Cryptography.SHA256]::Create()
try {
    $hash = ([System.BitConverter]::ToString($sha256.ComputeHash([System.IO.File]::ReadAllBytes($outputExePath)))).Replace("-", "")
} finally {
    $sha256.Dispose()
}

Write-Host "Single EXE Installer compiled successfully!"
Write-Host "Output: $outputExePath"
Write-Host "Size: $fileSize bytes"
Write-Host "SHA256: $hash"

# Copy output to releases, downloads, and public/installers/
$releasesDir = Join-Path $root "releases"
$downloadsDir = Join-Path $root "downloads"

New-Item -ItemType Directory -Force -Path $releasesDir | Out-Null
New-Item -ItemType Directory -Force -Path $downloadsDir | Out-Null

Copy-Item -LiteralPath $outputExePath -Destination (Join-Path $releasesDir $outputExeName) -Force
Copy-Item -LiteralPath $outputExePath -Destination (Join-Path $downloadsDir $outputExeName) -Force
if ($Publish) {
    $publicInstallersDir = Join-Path $root "public\installers"
    New-Item -ItemType Directory -Force -Path $publicInstallersDir | Out-Null
    Copy-Item -LiteralPath $outputExePath -Destination (Join-Path $publicInstallersDir $outputExeName) -Force
    Write-Host "Installer copied to the public staging directory."
} else {
    Write-Host "Installer retained locally for validation; public staging and download activation are disabled."
}

Write-Host "Installer copied to local releases and downloads directories."
