# Contributing

Thank you for helping improve Choir.

1. Open an issue before proposing a broad API, lifecycle, save, or source-shadow
   change.
2. Keep consumer-facing code under `choir.api` free of vanilla game types and
   Choir implementation packages.
3. Keep V71.44-specific behavior inside the runtime adapter.
4. Do not submit Songs of Syx source files, extracted game data, Workshop assets,
   binaries, or credentials.
5. Preserve stable IDs and deterministic ordering. Explain compatibility and
   save implications for every registry-facing change.
6. Include focused validation evidence and document any runtime test still needed.

Pull requests are proposals. Henrique reviews and integrates accepted changes into
the authoritative development tree before issuing an official release.
