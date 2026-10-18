# Release signing and publication

## Current status
v1.5.0-rc.2 is a development-signed sideload release candidate unless build-info.json explicitly says signingMode=permanent. Permanent-signing availability has not been verified for this pass; PR builds deliberately ignore signing secrets. The workflow automatically marks development-signed publications as prereleases.

Debug signing is not a production identity. Each runner may generate a different key, so it does not guarantee future in-place updates. The private key from a previous runner is not recoverable from its APK.

## Configure a permanent key
The owner should create and securely back up a dedicated Android release keystore and its passwords. Keep the keystore outside the repository and never publish it as an artifact or release asset. Use a secure password manager and encrypted backup.

Create these repository Actions secrets through GitHub Settings > Secrets and variables > Actions:
- ANDROID_KEYSTORE_BASE64 — base64-encoded private keystore
- ANDROID_KEYSTORE_PASSWORD — keystore password
- ANDROID_KEY_ALIAS — signing alias
- ANDROID_KEY_PASSWORD — private-key password

Do not paste private signing material into issues, PRs, release notes, or chat. Credential setup is an owner action. Once configured, the workflow restores the key only for trusted branch builds, uses a temporary file, and removes it after building. PR builds use development signing.

Adopting a new permanent key may require one intentional fresh install if the old installed app was signed with a different development key. Future updates must retain the same package name, permanent key, and increasing Android versionCode.

## Version convention
Update app/build.gradle.kts:
- versionName: semantic user-facing version, such as 1.5.0-rc.2
- versionCode: strictly increasing Android integer; this candidate is 15002
- Git tag: v plus versionName
- Release notes: docs/releases/v<versionName>.md

Do not reuse a published version/tag for a different binary. Increase the version and code for subsequent releases.

## Publish
The workflow always tests and builds PRs/main. Publication occurs only for:
1. An authorized main-branch push whose commit message includes [publish], or
2. A manual workflow run on main with publish enabled.

A separate publication job receives contents:write. It downloads the already-built artifacts, creates a draft, attaches the versioned release/debug APKs, SHA-256 checksums, signing certificates, and build-info.json, then publishes. It never overwrites an existing release.

Development signing always forces prerelease status. Promote to a normal stable release only after permanent signing and real-TV acceptance checks are complete.

## Local builds
Use JDK 21 and Android SDK 35. Set RELEASE_KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD for a permanent-key build. Without a key, assembleRelease uses the development fallback and should only be distributed with that limitation disclosed.
