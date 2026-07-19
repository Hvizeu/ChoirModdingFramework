# Changelog

## Unreleased - Choir 0.9.0 household resources

- Added a public, additive household-resource API so content mods can make a
  normal resource available to a race and household class without replacing the
  game's home system.
- Household requirements compose by maximum, preserve existing resource order,
  and use the game's normal shopping, wear, and replacement behaviour.
- Advanced the current development Maven coordinate to `choir-api:0.9.0`.

## Unreleased - Choir 0.8.2 stockpile section model

- Divided each base stockpile shelf's real 80-item capacity into eight
  user-assigned 10-item storage sections; plus and minus now change one section.
- Persisted exact section counts per resource and migrated older boolean
  assignments without silently deleting stored units.
- Enforced the same per-resource quota in UI, tally, pathfinding, reservation,
  hauling, save/reload, and global resource displays.
- Removed a re-entrant room scan that could rebind V71.44's mutable crate endpoint
  between an AI capacity check and its reservation.
- Packed repeated resource sections onto one shelf first and made scattered
  hauling prefer partly filled compatible shelves.
- Routed copied/rebuilt warehouse states through the same bounded section
  allocator and repartitioned every resource safely when shelf upgrades change.
- Added retained diagnostics for section allocation, rejected reservations, and
  capacity reduction/spill behavior.
- Restored the vanilla-safe counter inset in warehouse resource rows so the
  stored/capacity label cannot intercept the slider's plus and input controls.
- Advanced the current development Maven coordinate to `choir-api:0.8.2`.

## Choir 0.8.1 stockpile assignment QA fix (superseded after owner test)

- Replaced the vanilla singular crate-allocation loop with bounded, exact-resource
  assignment updates so removing a mixed-resource assignment cannot hang the game.
- Made each physical stockpile shelf expose eight assignment slots by default
  without multiplying its real item capacity.
- Changed resource rows to show stored/effective capacity, such as `60/80`, and
  relabelled the room counter as `Storage slots`.
- Owner testing found a remaining UI/data quota mismatch and re-entrant
  reservation crash; use 0.8.2 instead.

## Unreleased - Choir 0.8 multi-resource storage

- Replaced supported one-resource shelves with sparse, resource-scoped entries
  that share one physical item capacity.
- Added exact resource-aware pathing, reservations, stockpile tallies, rendering,
  logistics selection, save migration, and lifecycle diagnostics.
- Routed registered workshop/refiner secondary outputs into internal shelves
  before creating ordinary loose overflow.
- Added `choir.api.storage` API v1 with a default of eight and a configurable
  hard cap of sixteen resource kinds per tile.
- Advanced the current development Maven coordinate to `choir-api:0.8.0`.

## Unreleased - Choir 0.7 multi-output production

- Added `choir.api.production` API v1 for opt-in, normal-data-backed workshop and
  refiner recipes with one input and two to five distinct outputs.
- Kept vanilla input consumption and primary-output storage unchanged while
  emitting later outputs as ordinary scattered, haulable resources.
- Added exact live recipe validation, deterministic registration, reconstruction
  safety, runtime diagnostics, and friendly consumer examples.
- Advanced the current development Maven coordinate to `choir-api:0.7.0`.

## Unreleased - Choir 0.6 documentation refresh

- Added a unified first-time onboarding path for manifests, lifecycle events,
  deterministic patch composition, Mod Options, races, passive rooms, named
  resource display groups, tactical damage, optional dependencies, packaging,
  and troubleshooting.
- Added copyable Java examples for every public API domain.
- Updated the Maven examples to the current `choir-api:0.6.0` development
  coordinate while retaining the rule that consumers use `provided` scope.
- Documented runtime maturity separately for features whose static implementation
  is complete but whose owner runtime gate is still pending.

## Unreleased — public repository setup

- Added a clean public source and documentation projection.
- Added the standalone `choir-api` Maven module with source and Javadoc support.
- Added public Maven-consumption and API usage guidance.
- Kept generated runtime JARs, tests, fixtures, internal diagnostics, and
  third-party-derived source replacements out of Git history.

## Compatibility

The current V71.44 runtime line is fingerprint-gated for Songs of Syx `0.71.44`.
Official runtime binaries are released separately after validation.
