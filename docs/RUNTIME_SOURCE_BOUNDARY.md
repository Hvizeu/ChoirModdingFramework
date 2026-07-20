# Runtime source boundary

Choir has two kinds of V71.44 runtime code:

1. Choir-owned adapter and internal code, included under `runtime-source/choir`.
2. Exact source replacements for selected Songs of Syx classes, excluded from this
   public repository because they are tied to proprietary game implementation
   contracts.

The official runtime JAR contains the validated fingerprint-gated implementation.
Contributors should propose behavioral changes through public API and adapter code;
source-shadow changes require separate compatibility review and proof against the
supported game build.

Public availability of Choir-owned runtime source grants only the permissions in
the repository [LICENSE](../LICENSE). It does not make the project open source and
does not authorize independent builds or redistribution.
