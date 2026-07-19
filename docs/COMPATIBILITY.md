# Compatibility

Choir's current runtime line supports Songs of Syx `0.71.44` only. Version-sensitive
adapters and approved source shadows are fingerprint-gated and fail closed on a
mismatch.

Choir 0.8 owns the V71.44 storage endpoint, finder, stockpile, logistics, and
workshop/refiner shelf surface as one coherent replacement. A framework that
ships competing shadows for those same classes is incompatible by design; there
must be one runtime owner for resource identity, reservations, and save state.

The public API deliberately avoids live vanilla objects. A consumer must treat a
rejected declaration, conflict, missing target, or unsupported version as a real
diagnostic rather than silently continuing.

Public declarations use stable string IDs and immutable values. Consumers must
not retain game-owned objects across registry reconstruction and must not package
Choir classes inside their own JAR. Tactical combat composition is intentionally
limited to settlement combat; it does not patch world-map auto-resolve.

Some APIs can be present in a development build before their retained owner
runtime gate is closed. The release notes and feature guide maturity table are
authoritative for that distinction.

Choir does not make it safe to remove a content mod from a save that uses that
mod's rooms or other registry definitions.
