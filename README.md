# XII Days

<div align="center">

**Custom administration tools and gameplay features for the multiplayer event "XII Days"**

`Minecraft 1.21.x` · `NeoForge 21.11.42` · `Java 21`

</div>

---

## Table of contents

- [About](#about)
- [Source code & licensing](#source-code--licensing)
- [Distribution & availability](#distribution--availability)
- [Install](#install)
- [Features](#features)
- [Issue guidelines](#issue-guidelines)
- [Labels](#labels)
- [Report template](#report-template)
- [Technical details](#technical-details)
- [Credits](#credits)
- [License](#license)

---

## About

**XII Days** is a NeoForge modification developed by **Fire Sparks Studio** for the "XII Days" multiplayer event. It provides in-game administration tools, team management, gameplay restrictions, statistics tracking, and event-tailored mechanics intended for organized competitive sessions.

The mod is designed to run as part of a complete ecosystem including the **LNR Launcher** (see [Distribution](#distribution--availability)) and is explicitly scoped to the FSS community.

- **Current version**: `1.7.1`
- **Minecraft**: `1.21.x` (tested on `1.21.11`)
- **NeoForge**: `21.11.42`
- **Mappings**: Parchment `2025.12.20`

---

## Source code & licensing

The source code is **public** and hosted on GitHub:

> 🔗 **https://github.com/Le-noirrateur/xiidays-mod**

The mod is licensed under the **GNU AGPLv3** license. By contributing or redistributing, you agree to the terms of that license. Runnable builds (compiled JARs) are distributed via the channels listed below — source distribution remains subject to AGPLv3 obligations.

| Resource | Location |
|----------|----------|
| Source code | `https://github.com/Le-noirrateur/xiidays-mod` |
| Issue tracker | `https://github.com/Le-noirrateur/xiidays-mod/issues` |
| Releases | `https://github.com/Le-noirrateur/xiidays-mod/releases` |

---

## Distribution & availability

The mod is distributed through two channels:

### 1. Modrinth (unlisted)

The project is present on Modrinth but **unlisted** — it does not appear in public search results and is only reachable via direct link:

> **https://modrinth.com/**

This is intentional: the mod is built for a specific private event and is not aimed at general public consumption. If you reached this page, you were given the link by the FSS team.

### 2. LNR Launcher auto-update

For event registrants, the recommended way to install and keep the mod up-to-date is the **LNR Launcher** (the Fire Sparks Studio custom launcher). The launcher:

- Pulls the latest mod JAR from the FSS content CDN
- Verifies file hashes against a published manifest
- Notifies users when a new version is available
- Tracks `latest` and `recommended` releases independently

The update check endpoint used by NeoForge's built-in update checker is published at:
