plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val nousVersion = providers.gradleProperty("nous.version").get()

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
            }
        }
    }
}

// Compose 1.6.1 creates a minimal runtime image. The Windows app launcher needs the
// bundled java/javaw launchers present alongside the JVM DLLs.
tasks.configureEach {
    if (name == "createRuntimeImage") doLast {
        val javaHome = System.getenv("JAVA_HOME")?.takeIf { it.isNotBlank() }
            ?: error("JAVA_HOME must point to the supported JDK when packaging")
        val runtimeBin = layout.buildDirectory.dir("compose/tmp/main/runtime/bin").get().asFile
        runtimeBin.mkdirs()
        listOf("java.exe", "javaw.exe").forEach { launcher ->
            val source = file("$javaHome/bin/$launcher")
            check(source.isFile) { "Missing JDK launcher: $source" }
            source.copyTo(runtimeBin.resolve(launcher), overwrite = true)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.codequest.academy.desktop.MainKt"
        jvmArgs += "-Dnous.version=$nousVersion"
        buildTypes {
            release {
                proguard {
                    configurationFiles.from(project.file("proguard-rules.pro"))
                }
            }
        }

        nativeDistributions {
            modules(
                "java.base",
                "java.desktop",
                "java.sql",
                "java.net.http",
                "java.scripting",
                "java.xml",
                "java.logging",
                "java.management",
                "java.naming",
                "java.prefs",
                "jdk.unsupported",
                "jdk.security.auth"
            )
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "Nous-AI-Academy"
            packageVersion = nousVersion
            windows {
                menuGroup = "Nous AI Academy"
                menu = true
                shortcut = true
                dirChooser = true
                iconFile.set(project.file("src/jvmMain/resources/branding/nous-ai-academy-logo.ico"))
            }
        }
    }
}
