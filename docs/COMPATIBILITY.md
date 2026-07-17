# Compatibility

Choir's current runtime line supports Songs of Syx `0.71.44` only. Version-sensitive
adapters and approved source shadows are fingerprint-gated and fail closed on a
mismatch.

The public API deliberately avoids live vanilla objects. A consumer must treat a
rejected declaration, conflict, missing target, or unsupported version as a real
diagnostic rather than silently continuing.

Choir does not make it safe to remove a content mod from a save that uses that
mod's rooms or other registry definitions.
