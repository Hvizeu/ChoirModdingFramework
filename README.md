# Choir Modding Framework

[![Verify public API](https://github.com/Hvizeu/ChoirModdingFramework/actions/workflows/verify-api.yml/badge.svg)](https://github.com/Hvizeu/ChoirModdingFramework/actions/workflows/verify-api.yml)

Choir is a content-free compatibility framework for Songs of Syx `0.71.44`.
It gives compatible mods a stable public Java API for options, dependency and
capability declarations, lifecycle events, composition, Mod Options, race
declarations and user-facing patches, passive room declarations, presentation-only
resource display groups, tactical combat damage composition, section-based
stockpiles, and shared-capacity production storage. It also supports bounded
two-to-five-output execution for opt-in workshop and refiner recipes, with all
outputs stored internally before ordinary loose overflow.

Current development version: `0.9.0`, targeting Songs of Syx `0.71.44` only.

Choir is an independent community project. It is not affiliated with, endorsed
by, or distributed by the Songs of Syx developers, and it does not include the
game.

## Players

Install Choir as its own mod and keep it enabled whenever a mod declares Choir
as required. Download an official packaged release rather than using GitHub's
**Download ZIP** button: the Git repository contains public source and release
metadata, while generated runtime JARs are intentionally not tracked in Git.
Official packages and their checksums are published through the project's
[GitHub Releases](https://github.com/Hvizeu/ChoirModdingFramework/releases) page
when a release is approved. See [installation](docs/INSTALLATION.md).

Do not copy Choir classes into another mod and do not enable the retired
standalone Mod Menu Framework alongside Choir.

Choir itself adds no rooms, races, resources, recipes, production chains, terrain,
or balance content. Content mods remain responsible for their own data and saves.

## Mod authors

Start with [API usage](docs/API_USAGE.md), [Maven](docs/MAVEN.md), and the
[compatibility boundary](docs/COMPATIBILITY.md). Consumer code may import only
`choir.api.*`; do not import `choir.internal.*`, `choir.adapter.*`, or shadowed
Songs of Syx classes.

Choir must remain a separate player-installed dependency. Compile against
`choir-api` with Maven `provided` scope or an equivalent compile-only classpath;
never shade, embed, rename, or repackage Choir inside a consumer mod.

## Public source boundary

This repository includes the public API, Choir-owned runtime implementation
source, public manifests, and release metadata. Exact V71.44 source replacements
for vanilla classes are deliberately not published here because they are tied to
proprietary game implementation contracts. Generated runtime binaries are
published only as official release artifacts. See [runtime-source
boundary](docs/RUNTIME_SOURCE_BOUNDARY.md).

## Support and contributions

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or pull request.
For security-sensitive reports, follow [SECURITY.md](SECURITY.md). The project is
source-available under [LICENSE](LICENSE), not open source. The plain-language
[license FAQ](docs/LICENSE_FAQ.md) explains common uses; the LICENSE text controls
if the two ever differ.

Copyright (c) 2026 Henrique Vizeu. All rights reserved.
