# Compose bundles optional logging/font integrations that are not required by
# the desktop runtime. Keep release shrinking fail-closed for real classes,
# while suppressing optional references that ProGuard cannot resolve on Windows.
-dontwarn org.slf4j.**
-dontwarn sun.font.**
# Native method names and the Swing editor's reflection-loaded token makers.
-keep interface com.codequest.academy.shared.platform.MotionUser32 { *; }
-keep class com.sun.jna.** { *; }
-keep class org.fife.ui.rsyntaxtextarea.modes.** { *; }
