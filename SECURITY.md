# Security policy

## Reporting a vulnerability

Do not publish credentials, private keys, game saves, personal paths, or exploit
details in a public issue. Use GitHub's private vulnerability reporting or open a
private security advisory for this repository.

Useful reports include:

- affected Choir version and build ID;
- affected Songs of Syx version;
- the smallest reproduction that does not contain private user data;
- whether the issue bypasses a fingerprint gate, loads unintended code, writes
  outside Choir-owned storage, corrupts a registry/save, or exposes private paths;
- expected and observed behavior.

Unsupported game builds and fingerprint mismatches are compatibility reports,
not security vulnerabilities, but they are still useful when reported clearly.

## Supported version

Until Choir reaches its first stable public release, only the newest official
pre-release receives security fixes. Development snapshots are unsupported.
