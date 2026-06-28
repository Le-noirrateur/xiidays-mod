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
- [Credits](#credits)
- [License](https://github.com/Le-noirrateur/xiidays-mod?tab=AGPL-3.0-1-ov-file)

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

> **https://github.com/Le-noirrateur/xiidays-mod**

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
> **https://api.mceteams.com**


### Compatibility statement

The mod requires installation on **both the client and the server** (NeoForge on both sides). A vanilla client connecting to a server running XII Days will not work — custom network payloads, registries (blocks/items) and client-side UI require the mod to be present locally.

| Side | Required? |
|------|:---------:|
| Client | ✅ Yes |
| Server | ✅ Yes |

> On Modrinth, the project is tagged as **Client and server / Required on both**. Any other tagging seen in older versions was inaccurate and has been corrected per Modrinth's Content Rules §5.1.

---

## Install

### Manual (NeoForge launcher)

1. Install **NeoForge 21.11.42** (or later) for Minecraft `1.21.11`
2. Download the latest `xiidays-<version>.jar` from [Modrinth](https://modrinth.com/mod/xiidays) or [GitHub Releases](https://github.com/Le-noirrateur/xiidays-mod/releases)
3. Place the JAR in the `mods/` folder of **both** your client and server
4. Launch the game

### Via LNR Launcher

If you've registered for the XII Days event and installed the launcher:

1. Select the "XII Days" profile
2. The launcher will fetch compatible mods, configs and options automatically
3. Click **Play** — updates roll in silently when a new version is published

---

## Features

- **Administration** — custom slash commands for server staff (`/xiidays ...`)
- **Team management** — assign players to teams, track rosters, detect eliminations
- **Restrictions** — block/item pickup rules, crafting filters, interaction deny-lists
- **Scoreboard & stats** — per-player and per-team tracking, end-of-day and end-of-game scoreboards
- **Spectator mode** — teammate watch, base spectate, free spectate with zone clamping
- **Zone visualization** — in-world particle outlines for team zones
- **Core maze** — interactive puzzles when attacking an enemy core
- **Cinematic camera** — intro/final fly-through paths
- **Custom blocks & items** — Team Spawner, Team Core, Core Destroyer, Totem of Revivality

---

## Issue guidelines

<div align="center">

### Reporting bugs & crashes

</div>

Before opening an issue, please check that a similar report doesn't already exist in the list. **Duplicates will be closed.**

### How to report properly

So the team can process your request quickly, please provide **at minimum**:

- **Mod version** (e.g. `1.7.1`)
- **Minecraft / NeoForge version** (e.g. `1.21.11` / `21.11.42`)
- **Other installed mods** (if relevant)
- **Description of the problem**: what you were doing, what happened, what should have happened
- **Full logs** on [mclo.gs](https://mclo.gs) or [pastebin](https://pastebin.com) for any crash or suspicious error
- **Steps to reproduce** the bug (1, 2, 3...)

Do **not** paste raw logs as screenshots or in comments — use a paste service.

> **No logs = automatic issue closure.**

### Proper issue usage

The issue tracker is reserved for **serious reports only**. Any abuse will result in sanctions, including:

- Spamming reports, repeatedly posting duplicates, or intentional off-topic content
- Misusing or repeatedly relabeling issues (we assign labels ourselves)
- Posting logs as screenshots or raw text instead of using a paste service

> **Possible sanctions**: warning, temporary suspension of issue submission privileges, **permanent ban from submitting issues.**

Keep this space constructive and professional.

---

## Labels

Issues are categorized by the team using labels. Here is what each one means:

| Label | Color | Description |
|-------|:-----:|-------------|
| **`Bug`** | 🟠 | The mod behaves incorrectly or unexpectedly |
| **`Crash`** | 🔴 | The mod causes a client or server crash |
| **`Incompatibility`** | 🟣 | The mod conflicts with one or more other mods — please specify the conflicting mod(s) and their versions |
| **`On going`** | 🟡 | The team is currently working on a fix |
| **`On hold`** | 🔵 | Known issue, will be fixed later but not right now (depends on another part of the code or is not a priority) |
| **`Fixed`** | 🟢 | Bug resolved and fix released |
| **`Wontfix`** | 🟣 | Issue identified but will not be fixed (intended behavior or technically too complex) |
| **`Off-topic`** | ⚪ | The subject does not relate to the mod |
| **`Not-applicable`** | ⚪ | The report does not apply to the context (invalid report or user error) |

> Labels are **not to be modified by users**. Use the sections of your report to categorize; the team handles the rest.

---

## Report template

When opening an issue, please use the following template:

```md
**Mod version**:
**Minecraft / NeoForge**:
**Other mods**:

---

### Description
*(Describe the problem you encountered)*

### Steps to reproduce
1.
2.
3.

### Expected behavior
*(What should have happened)*

### Logs
[mclo.gs or pastebin link]
