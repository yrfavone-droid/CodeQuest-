# Compose bundles optional logging/font integrations that are not required by
# the desktop runtime. Keep release shrinking fail-closed for real classes,
# while suppressing optional references that ProGuard cannot resolve on Windows.
-dontwarn org.slf4j.**
-dontwarn sun.font.**
