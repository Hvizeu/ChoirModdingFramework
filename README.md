# Choir Modding Framework

Choir is a content-free compatibility framework for Songs of Syx `0.71.44`.
It gives compatible mods a stable public Java API for options, dependency and
capability declarations, lifecycle events, composition, Mod Options, race
declarations and user-facing patches, passive room declarations, presentation-only
resource display groups, tactical combat damage composition, section-based
stockpiles, and shared-capacity production storage. It also supports bounded
two-to-five-output execution for opt-in workshop and refiner recipes, with all
outputs stored internally before ordinary loose overflow.

Current development version: `0.9.0`, targeting Songs of Syx `0.71.44` only.

## Players

Install Choir as its own mod and keep it enabled whenever a mod declares Choir
as required. This repository is also a ready-to-deploy Workshop mod root: its
`_Info.txt` and `V71` folder contain the validated Choir runtime. The exact JAR
checksum is in [SHA256SUMS.txt](SHA256SUMS.txt). Do not copy Choir classes into
another mod and do not enable the retired standalone Mod Menu Framework alongside
Choir.

Choir itself adds no rooms, races, resources, recipes, production chains, terrain,
or balance content. Content mods remain responsible for their own data and saves.

## Mod authors

Start with [API usage](docs/API_USAGE.md), [Maven](docs/MAVEN.md), and the
[compatibility boundary](docs/COMPATIBILITY.md). Consumer code may import only
`choir.api.*`; do not import `choir.internal.*`, `choir.adapter.*`, or shadowed
Songs of Syx classes.

## Public source boundary

This repository includes the public API, Choir-owned runtime implementation
source, and the validated Workshop runtime payload under `V71`. Exact V71.44
source replacements for vanilla classes are deliberately not published here
because they are tied to proprietary game implementation contracts. See
[runtime-source boundary](docs/RUNTIME_SOURCE_BOUNDARY.md).

## Support and contributions

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or pull request.
For security-sensitive reports, follow [SECURITY.md](SECURITY.md). The project is
source-available under [LICENSE](LICENSE), not open source.
