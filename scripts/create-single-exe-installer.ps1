[CmdletBinding()]
param([string]$Version = "1.2.0")

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot ".."))
$csc = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"

if (!(Test-Path $csc)) {
    throw "C# Compiler (csc.exe) not found at $csc"
}

$appDir = Join-Path $root "desktopApp\build\compose\binaries\main\app\CodeQuestAcademy"
if (!(Test-Path (Join-Path $appDir "CodeQuestAcademy.exe"))) {
    throw "Packaged application not found at $appDir. Please run Gradle build first."
}

Write-Host "Creating Single EXE Installer for CodeQuest Academy v$Version..."

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
using System.Reflection;
using System.Threading;
using System.Windows.Forms;

namespace CodeQuestInstaller
{
    public class SetupForm : Form
    {
        private ProgressBar progressBar;
        private Label statusLabel;
        private Button installButton;
        private CheckBox launchCheckBox;
        private bool isSilent = false;

        public SetupForm(bool silent = false)
        {
            this.isSilent = silent;
            this.Text = "CodeQuest Academy Setup v$Version";
            this.Size = new Size(520, 320);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.BackColor = Color.FromArgb(30, 27, 46);
            this.ForeColor = Color.White;

            Label titleLabel = new Label();
            titleLabel.Text = "CodeQuest Academy Setup";
            titleLabel.Font = new Font("Segoe UI", 16, FontStyle.Bold);
            titleLabel.ForeColor = Color.FromArgb(108, 92, 231);
            titleLabel.Location = new Point(30, 25);
            titleLabel.AutoSize = true;

            Label descLabel = new Label();
            descLabel.Text = "Installing CodeQuest Academy v$Version with bundled JVM runtime...\nNo manual ZIP extraction required.";
            descLabel.Font = new Font("Segoe UI", 9, FontStyle.Regular);
            descLabel.ForeColor = Color.FromArgb(180, 180, 200);
            descLabel.Location = new Point(30, 65);
            descLabel.Size = new Size(440, 40);

            progressBar = new ProgressBar();
            progressBar.Location = new Point(30, 120);
            progressBar.Size = new Size(440, 24);
            progressBar.Style = ProgressBarStyle.Continuous;

            statusLabel = new Label();
            statusLabel.Text = "Initializing setup...";
            statusLabel.Font = new Font("Segoe UI", 9, FontStyle.Italic);
            statusLabel.ForeColor = Color.FromArgb(200, 200, 220);
            statusLabel.Location = new Point(30, 155);
            statusLabel.Size = new Size(440, 25);

            launchCheckBox = new CheckBox();
            launchCheckBox.Text = "Launch CodeQuest Academy after setup";
            launchCheckBox.Font = new Font("Segoe UI", 9);
            launchCheckBox.ForeColor = Color.White;
            launchCheckBox.Checked = true;
            launchCheckBox.Location = new Point(30, 190);
            launchCheckBox.AutoSize = true;

            installButton = new Button();
            installButton.Text = "Installing...";
            installButton.Font = new Font("Segoe UI", 10, FontStyle.Bold);
            installButton.BackColor = Color.FromArgb(108, 92, 231);
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
                string targetDir = Path.Combine(localAppData, @"Programs\CodeQuest Academy");

                // Kill running processes safely
                try
                {
                    foreach (var process in Process.GetProcessesByName("CodeQuestAcademy"))
                    {
                        try { process.Kill(); } catch { }
                    }
                    Thread.Sleep(500);
                }
                catch { }

                Directory.CreateDirectory(targetDir);

                UpdateStatus("Extracting application files and JVM runtime...", 25);
                Assembly assembly = Assembly.GetExecutingAssembly();
                string tempZip = Path.Combine(Path.GetTempPath(), "codequest_setup_payload.zip");

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

                        if (destinationPath.StartsWith(targetDir, StringComparison.OrdinalIgnoreCase))
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

                try { File.Delete(tempZip); } catch { }

                // Validate JVM installation
                string exePath = Path.Combine(targetDir, "CodeQuestAcademy.exe");
                string runtimeDir = Path.Combine(targetDir, "runtime");

                if (!File.Exists(exePath))
                {
                    throw new Exception("Executable CodeQuestAcademy.exe was not created in " + targetDir);
                }

                if (!Directory.Exists(runtimeDir))
                {
                    throw new Exception("JVM runtime directory was not extracted properly to " + runtimeDir);
                }

                UpdateStatus("Creating Desktop and Start Menu Shortcuts...", 90);
                CreateShortcuts(exePath);

                UpdateStatus("Installation Completed Successfully!", 100);

                Thread.Sleep(600);

                this.Invoke((MethodInvoker)delegate
                {
                    if (!isSilent)
                    {
                        MessageBox.Show("CodeQuest Academy v$Version installed successfully with bundled JVM runtime!", "Setup Complete", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    }

                    if (launchCheckBox.Checked && File.Exists(exePath))
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
                    MessageBox.Show("Installation Error: " + ex.Message, "Setup Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    installButton.Enabled = true;
                    installButton.Text = "Retry";
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

        private void CreateShortcuts(string targetExePath)
        {
            try
            {
                string targetDir = Path.GetDirectoryName(targetExePath);
                string desktop = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);
                string startMenu = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.StartMenu), "Programs");

                CreateShortcutFile(Path.Combine(desktop, "CodeQuest Academy.lnk"), targetExePath, targetDir);
                CreateShortcutFile(Path.Combine(startMenu, "CodeQuest Academy.lnk"), targetExePath, targetDir);
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
            shortcut.Description = "CodeQuest Academy - Learn Coding Through Math";
            shortcut.Save();
        }

        [STAThread]
        public static void Main(string[] args)
        {
            bool silent = false;
            foreach (string arg in args)
            {
                if (arg.Equals("/S", StringComparison.OrdinalIgnoreCase) || arg.Equals("/silent", StringComparison.OrdinalIgnoreCase))
                {
                    silent = true;
                }
            }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new SetupForm(silent));
        }
    }
}
"@

$csharpFile = Join-Path $tempDir "Installer.cs"
$csharpSource | Set-Content -LiteralPath $csharpFile -Encoding UTF8

$outputExeName = "codequest-academy-setup.exe"
$outputExePath = Join-Path $tempDir $outputExeName

$resArg = "/resource:$payloadZip,payload.zip"
$iconPath = Join-Path $root "desktopApp\src\jvmMain\resources\branding\codequest-academy-logo.ico"

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
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $outputExePath).Hash

Write-Host "Single EXE Installer compiled successfully!"
Write-Host "Output: $outputExePath"
Write-Host "Size: $fileSize bytes"
Write-Host "SHA256: $hash"

# Copy output to releases, downloads, and public/installers/
$releasesDir = Join-Path $root "releases"
$downloadsDir = Join-Path $root "downloads"
$publicInstallersDir = Join-Path $root "public\installers"

New-Item -ItemType Directory -Force -Path $releasesDir | Out-Null
New-Item -ItemType Directory -Force -Path $downloadsDir | Out-Null
New-Item -ItemType Directory -Force -Path $publicInstallersDir | Out-Null

Copy-Item -LiteralPath $outputExePath -Destination (Join-Path $releasesDir $outputExeName) -Force
Copy-Item -LiteralPath $outputExePath -Destination (Join-Path $downloadsDir $outputExeName) -Force
Copy-Item -LiteralPath $outputExePath -Destination (Join-Path $publicInstallersDir $outputExeName) -Force
Copy-Item -LiteralPath $outputExePath -Destination (Join-Path $releasesDir "CodeQuest-Academy-$Version-Setup.exe") -Force

Write-Host "Installer copied to releases, downloads, and public/installers!"
