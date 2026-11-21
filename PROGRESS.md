# PROGRESS
## Milestone 1 (started 2026-11-21) — scaffold, NOT COMPILED
Done: Gradle/Compose project skeleton, Obsidian Blue theme tokens, 4-tab shell, fail-closed ProviderPolicy, providers.json (all disabled), CI workflow for debug APK.
Not done / not tested: any build (Android SDK + Maven hosts blocked in authoring sandbox), launcher icons & emblem (icons/ folder not supplied), Groq streaming, key storage, everything in phases 2-6.
Versions (AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01) unverified against current stable.
Next: build on a machine with SDK access / GitHub Actions; add icons; Milestone 2 (Groq chat).
Gradle wrapper not generated (no network in authoring sandbox); CI uses setup-gradle. Run `gradle wrapper` locally.
Icons (2026-11-21): legacy mipmap PNGs (48-192px) wired as launcher icon; 512px emblem used on Home; master artwork kept in design/source. STILL MISSING: adaptive_foreground/background/monochrome PNG layers (only SVG monochrome received), .ico, design tokens JSON/UI sheets copied only as reference. Adaptive icon + themed icon + mask checks not done. Not compiled.
Adaptive icon layers wired (2026-11-21, bg/fg/mono PNGs). Mask/themed-icon rendering NOT tested. Remaining icon gap: .ico/web favicon.
Icon resources merged to match supplied android_example (color bg, v26 + v33 monochrome). Not tested on device.
Website (static, website/) built 2026-11-21: not deployed, not browser-tested. Needs authorised Vercel preview deploy, then confirm before production.
