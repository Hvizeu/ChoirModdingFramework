# Contributing

Thank you for helping improve Choir.

## Before opening a pull request

1. Create a focused branch from `main`.
2. Open an issue before proposing a broad API, lifecycle, save, storage, UI, or
   source-shadow change.
3. Keep consumer-facing code under `choir.api` free of vanilla game types and
   Choir implementation packages.
4. Keep V71.44-specific behavior inside the runtime adapter.
5. Do not submit Songs of Syx source files, extracted game data, Workshop assets,
   generated JARs, release archives, credentials, saves, logs, or private paths.
6. Preserve stable IDs and public method contracts. Explain compatibility and
   save implications for every registry-facing change.
7. Build the public API with `mvn -B -f choir-api/pom.xml package`.
8. Describe validation performed and clearly identify any owner in-game test that
   is still required.

Pull requests are proposals. Maintainers reproduce and validate accepted work in
the private authoritative development project before publishing an official
artifact. A public pull request does not authorize the contributor to publish an
independent Choir build.

## Licensing

Choir uses the [Choir Modding Framework Source-Available Contribution License
1.1](LICENSE). GitHub forks are permitted when used to prepare, demonstrate, or
submit contributions to the official project. Independent redistribution,
re-hosting, bundles, and releases from forks require prior written authorization.

By submitting a contribution, you confirm that you have the right to submit it
and grant the contribution rights stated in Section 4 of the license. Accepted
contributors are credited through project history, release notes, contributor
records, or another reasonable project record.

See the [plain-language license FAQ](docs/LICENSE_FAQ.md) for common examples.
