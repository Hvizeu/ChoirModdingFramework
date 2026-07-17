# Choir Modding Framework

Choir is a content-free compatibility framework for Songs of Syx `0.71.44`.
It gives compatible mods a stable public Java API for options, dependency and
capability declarations, lifecycle events, composition, selected race patches,
room declarations, and presentation-only resource display groups.

## Players

Install Choir as its own mod and keep it enabled whenever a mod declares Choir
as required. Download validated runtime releases from this repository's GitHub
**Releases** page or the official Workshop/Nexus distribution page. Do not copy
Choir classes into another mod and do not enable the retired standalone Mod Menu
Framework alongside Choir.

Choir itself adds no rooms, races, resources, recipes, production chains, terrain,
or balance content. Content mods remain responsible for their own data and saves.

## Mod authors

Start with [API usage](docs/API_USAGE.md), [Maven](docs/MAVEN.md), and the
[compatibility boundary](docs/COMPATIBILITY.md). Consumer code may import only
`choir.api.*`; do not import `choir.internal.*`, `choir.adapter.*`, or shadowed
Songs of Syx classes.

## Public source boundary

This repository includes the public API and Choir-owned runtime implementation
source. Exact V71.44 source replacements for vanilla classes are deliberately
not published here because they are tied to proprietary game implementation
contracts. The compiled, fingerprint-gated official runtime is distributed as a
release artifact. See [runtime-source boundary](docs/RUNTIME_SOURCE_BOUNDARY.md).

## Support and contributions

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or pull request.
For security-sensitive reports, follow [SECURITY.md](SECURITY.md). The project is
source-available under [LICENSE](LICENSE), not open source.
