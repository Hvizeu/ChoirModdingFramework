# Using the Choir API

Choir `0.9.0` is a compatibility framework for Songs of Syx `0.71.44`. It gives
mods stable Java APIs for features that would otherwise require several mods to
patch the same version-sensitive game code.

The basic idea is simple:

1. Your mod declares what it needs or contributes.
2. Choir validates and stores that declaration.
3. Choir combines declarations deterministically and applies them at its verified
   V71.44 integration point.

Your mod owns its gameplay content and settings. Choir owns the shared plumbing.

## Choose the feature you need

| I want to... | Start with | Public package |
|---|---|---|
| Declare dependencies and capabilities | [Platform manifests](#platform-manifests-and-capabilities) | `choir.api.platform` |
| Run code at a stable lifecycle phase | [Lifecycle events](#lifecycle-events) | `choir.api.lifecycle` |
| Let several mods compose one typed value | [Generic patch composition](#generic-patch-composition) | `choir.api.patch` |
| Add a generated settings page | [Mod Options](#mod-options) | `choir.api.options` |
| Declare or patch a race | [Race API](#race-registration-and-patching) | `choir.api.race` |
| Add a passive decorative room | [Room API](#passive-decoration-rooms) | `choir.api.room` |
| Group resources in supported UI lists | [Resource display groups](#resource-display-groups-experimental) | `choir.api.experimental.resources` |
| Multiply player or enemy tactical damage | [Combat damage](#tactical-combat-damage) | `choir.api.combat` |
| Give one workshop/refiner recipe two to five outputs | [Multi-output production](#multi-output-workshop-and-refiner-production) | `choir.api.production` |
| Let one shelf hold several kinds of resources | [Multi-resource storage](#multi-resource-storage) | `choir.api.storage` |

The public API source is available under [`choir-api`](../choir-api). Consumer
mods should never import `choir.internal`, `choir.adapter`, or `choir.api.spi`.

## Set up a consumer mod

### Compile against the API only

With Maven, use `provided` scope so Choir is available while compiling but is not
copied into your mod JAR:

```xml
<dependency>
  <groupId>io.github.hvizeu</groupId>
  <artifactId>choir-api</artifactId>
  <version>0.9.0</version>
  <scope>provided</scope>
</dependency>
```

If the current API artifact is not yet available from your configured Maven
repository, compile against the official `choir-api-0.9.0.jar` locally. Do not use
the runtime module as a compile dependency. See [Maven usage](MAVEN.md) for the
complete setup.

Players install and enable Choir as a separate mod. Never shade, relocate, or
embed Choir classes in a consumer JAR.

### Use the normal mod layout

```text
ExampleMod/
|-- _Info.txt
`-- V71/
    |-- choir/
    |   |-- core-platform.properties
    |   `-- options-provider.properties   # only when using Mod Options
    |-- script/
    |   `-- ExampleMod.jar
    `-- assets/                            # content owned by ExampleMod
```

Choir does not generate race data, room records, localization, sprites, sounds,
recipes, or other gameplay assets for you.

### Declare Choir and your mod identity

Create `V71/choir/core-platform.properties`:

```properties
formatVersion=1
modId=example.mod
displayName=Example Mod
version=1.0.0
requires=choir.framework@>=0.9.0
optional=
incompatible=
capabilities=example.mod.options
```

These are the exact fields consumed by Choir. Songs of Syx version compatibility
still belongs in `_Info.txt`.

Use lowercase, stable, provider-owned IDs such as `example.mod`. Treat IDs as
saved compatibility contracts: labels may change, but IDs should not.

## A small result-handling pattern

Choir APIs return typed results rather than silently guessing. Across most
domains:

- `ACCEPTED` means a new descriptor was retained.
- `IDEMPOTENT` means the identical descriptor was already retained. This is normal
  when Songs of Syx reconstructs systems in one process.
- a conflict means the same stable identity was reused with different content.
- a late, blocked, unavailable, or missing-target result is a real condition that
  should be logged or rejected.

Use a helper for each domain instead of ignoring results:

```java
private static void checkCombat(CombatDamageRegistrationResult result) {
    if (result != CombatDamageRegistrationResult.ACCEPTED
            && result != CombatDamageRegistrationResult.IDEMPOTENT) {
        throw new IllegalStateException("Combat registration failed: " + result);
    }
}
```

Register immutable process-level descriptors repeatedly if necessary. Do not use
a static flag as proof that a live game registry still exists; Choir rebuilds live
materialization for each new registry generation.

## Platform manifests and capabilities

Use the manifest to express hard dependencies, optional relationships,
incompatibilities, and capabilities supplied by your mod:

```properties
formatVersion=1
modId=example.overhaul
displayName=Example Overhaul
version=2.1.0
requires=choir.framework@>=0.9.0,example.library@>=1.2.0
optional=example.compatibility
incompatible=example.old-overhaul
capabilities=example.overhaul.options,example.overhaul.races
```

Requirement operators are `*`, `=`, `<`, `<=`, `>`, and `>=`. Missing required
mods, incompatible versions, dependency cycles, duplicate mod IDs, and declared
incompatibilities block affected manifests deterministically.

Query the resolved graph when you need diagnostics or another mod's capability:

```java
import choir.api.platform.ChoirPlatform;
import choir.api.platform.PlatformSnapshot;
import choir.api.platform.ResolvedMod;

PlatformSnapshot platform = ChoirPlatform.snapshot();

if (!platform.isActive("example.overhaul")) {
    for (ResolvedMod mod : platform.mods()) {
        if (mod.manifest().modId().equals("example.overhaul")) {
            System.err.println(mod.diagnostics());
        }
    }
}

for (String providerId : platform.providersOf("example.overhaul.races")) {
    System.out.println("Race capability supplied by " + providerId);
}
```

There are two different capability checks:

```java
boolean choirSupportsCombat = Choir.hasCapability(
    Capability.COMBAT_TACTICAL_DAMAGE_MULTIPLIERS);

boolean choirSupportsMultiOutput = Choir.hasCapability(
    Capability.PRODUCTION_MULTI_OUTPUT_ROOMS);

boolean choirSupportsMixedShelves = Choir.hasCapability(
    Capability.STORAGE_MULTI_RESOURCE_TILES);

boolean anotherModIsActive = ChoirPlatform.hasCapability(
    "example.overhaul.races");
```

`Choir.hasCapability(...)` asks whether the installed Choir runtime supports a
framework feature. `ChoirPlatform.hasCapability(...)` asks whether an active mod
manifest advertises a named capability.

The manifest's `optional` field records an optional relationship. It does not
create a load-order edge and it does not make direct Java references safe when the
other mod is absent.

## Lifecycle events

Lifecycle events are useful when you need a stable notification without importing
Songs of Syx lifecycle classes or relying on script discovery order.

```java
import choir.api.lifecycle.ChoirLifecycle;
import choir.api.lifecycle.LifecycleEvents;
import choir.api.lifecycle.LifecycleSubscriptionResult;

LifecycleSubscriptionResult result = ChoirLifecycle.subscribe(
    "example.mod",
    "clear-runtime-cache",
    LifecycleEvents.GAME_DISPOSING,
    0,
    context -> {
        ExampleRuntime.clearCachedHandles();
        System.out.println("Disposed generation " + context.runtimeGeneration());
    });

if (result != LifecycleSubscriptionResult.ACCEPTED
        && result != LifecycleSubscriptionResult.IDEMPOTENT) {
    throw new IllegalStateException("Lifecycle subscription failed: " + result);
}
```

Built-in events are:

| Event | Good use |
|---|---|
| `BEFORE_GAME_CREATED` | Process descriptors and pre-registry preparation |
| `GAME_INITIALIZED` | The new game graph has initialized |
| `INSTANCE_CREATED` | A script instance was observed |
| `GAMEPLAY_REACHED` | A playable settlement was reached |
| `GAME_DISPOSING` | Clear every live game or registry handle |

Built-in events replay their latest context to late subscribers. Listener code
must therefore be idempotent. A context contains stable diagnostics such as the
runtime generation, sequence, session ID, game version, and source marker; it does
not expose a live vanilla object.

Delivery order is priority descending, then provider ID and subscription ID
ascending.

## Generic patch composition

Use the generic patch engine when one mod or framework owns a typed target and
wants other providers to contribute values. It does not discover or mutate game
fields automatically.

The owner registers the target and base value:

```java
import choir.api.patch.*;

PatchRegistrationResult targetResult = ChoirPatches.registerTarget(
    new PatchTarget<Double>(
        "example.owner",
        "example:movement:SPEED",
        Double.class,
        1.0,
        PatchComposers.DOUBLE_MULTIPLY));
```

Other providers contribute independently:

```java
PatchRegistrationResult contributionResult = ChoirPatches.contribute(
    new PatchContribution<Double>(
        "example.balance",
        "faster-movement",
        "example:movement:SPEED",
        10,
        1.20));
```

The target owner resolves and applies the effective value at its own verified
integration point:

```java
PatchResolution<Double> resolution =
    ChoirPatches.resolve("example:movement:SPEED", Double.class);

double effectiveSpeed = resolution.value();
```

Built-in composers include double addition/multiplication, integer addition,
boolean AND/OR, and replacement. Contributions apply by priority ascending, then
provider ID and patch ID ascending. Higher priorities therefore apply later.

Prefer a dedicated domain API for races, rooms, combat, or resource presentation.
Those APIs understand their V71.44 registries and safety rules; generic patches do
not.

## Mod Options

Choir can generate a consistent settings page, persist values, and handle Apply,
Cancel, Reset Page, scrolling, right-click, Escape, and menu input routing. Your
mod declares settings and reacts to committed values; it does not build game UI
widgets.

### Make the provider visible on the main menu

Add `V71/choir/options-provider.properties`:

```properties
formatVersion=1
providerId=example.mod
displayName=Example Mod
description=Configure Example Mod.
```

Before your runtime schema has registered, Choir can show the provider and explain
that its settings become available after entering a settlement.

### Register a friendly settings page

```java
import choir.api.options.*;

public final class ExampleOptions {
    private static final String PROVIDER = "example.mod";
    private static volatile boolean enabled = true;
    private static volatile double strength = 1.0;

    public static void register() {
        OptionSchema schema = OptionSchema.builder(PROVIDER, "Example Mod")
            .description("Tune Example Mod without editing files.")
            .schemaVersion(1)
            .add(OptionSetting.section("section.general", "General"))
            .add(OptionSetting.bool("enabled", "Enabled", true)
                .description("Turns the optional effect on or off.")
                .applyMode(OptionApplyMode.IMMEDIATE)
                .build())
            .add(OptionSetting.floating("strength", "Effect strength",
                    1.0, 0.0, 4.0, 0.05)
                .description("1.0 is vanilla, 0.5 is half, and 2.0 is double.")
                .applyMode(OptionApplyMode.IMMEDIATE)
                .build())
            .add(OptionSetting.integer("limit", "Unit limit", 10, 0, 100)
                .description("Maximum number of units used by this feature.")
                .build())
            .add(OptionSetting.choice("mode", "Mode", "Balanced",
                    "Relaxed", "Balanced", "Strict")
                .build())
            .add(OptionSetting.info("info.note", "Tip",
                "Hover a setting to read its description."))
            .build();

        OptionRegistrationResult registration = ChoirOptions.register(schema);
        if (registration != OptionRegistrationResult.ACCEPTED
                && registration != OptionRegistrationResult.IDEMPOTENT) {
            throw new IllegalStateException("Options registration failed: " + registration);
        }

        enabled = ChoirOptions.getBoolean(PROVIDER, "enabled", true);
        strength = ChoirOptions.getDouble(PROVIDER, "strength", 1.0);

        OptionListenerRegistrationResult listener = ChoirOptions.subscribe(
            PROVIDER,
            (providerId, key, oldValue, newValue) -> {
                if (key.equals("enabled")) {
                    enabled = ((Boolean) newValue).booleanValue();
                } else if (key.equals("strength")) {
                    strength = ((Number) newValue).doubleValue();
                }
            });

        if (listener != OptionListenerRegistrationResult.ACCEPTED
                && listener != OptionListenerRegistrationResult.IDEMPOTENT) {
            throw new IllegalStateException("Options listener failed: " + listener);
        }
    }
}
```

Available rows are boolean, bounded integer, bounded floating point, text, choice,
section header, information, and read-only value. Read values with
`getBoolean`, `getInt`, `getDouble`, or `getString`.

The current persistence scope is global. Values are saved atomically under:

```text
%APPDATA%/songsofsyx/Choir/options/global/<provider-id>.json
```

Apply modes are:

- `IMMEDIATE`: your listener can safely use the new value now.
- `WORLD_RELOAD`: the page tells the player a world reload is required.
- `RESTART`: the page tells the player a complete application restart is required.

The latter two are presentation contracts; Choir does not reconstruct your mod's
runtime state automatically.

Install a gameplay hook once and have it read current configuration. Do not add a
new hook or multiply an already modified value every time the player presses
Apply. Preserve setting keys when changing labels, because keys identify persisted
values.

## Race registration and patching

`ChoirRaces` API version 4 lets independent mods contribute to existing or
data-backed races without replacing each other's complete records.

Use one result helper:

```java
import choir.api.race.*;
import java.util.List;

private static void checkRace(RaceRegistrationResult result) {
    if (result != RaceRegistrationResult.ACCEPTED
            && result != RaceRegistrationResult.IDEMPOTENT) {
        throw new IllegalStateException("Race registration failed: " + result);
    }
}
```

### Declare a data-backed race

After your mod supplies the normal V71 init, text, sprite, and supporting assets,
declare ownership of the race key:

```java
checkRace(ChoirRaces.declareDataBackedRace(
    new RaceDeclaration("example.race", "MOON_ELF", "Moon Elf")));
```

This validates and composes around `MOON_ELF`; it does not create the race assets.

### Multiply a race boost

```java
checkRace(ChoirRaces.patchBoost(new RaceBoostPatch(
    "example.balance",
    "human-speed",
    "HUMAN",
    "PHYSICS_SPEED",
    0,
    1.05)));
```

Boost factors must be positive and finite. Independent factors multiply.

### Add a household resource requirement

Use a household requirement when residents should buy, keep, wear out, and replace
a normal resource through the game's existing home-furnishing system. This does
not create free items and it is not a construction cost. The player still chooses
the desired furnishing target in the normal household UI.

```java
checkRace(ChoirRaces.requireHomeResource(new RaceHomeResourceRequirement(
    "example.mod",
    "human-citizen-pillows",
    "HUMAN",
    RaceHomeResidentClass.CITIZEN,
    "PILLOWS",
    1,
    RaceMissingTargetPolicy.FAIL)));
```

The amount is the maximum per resident. `CITIZEN`, `NOBLE`, and `SLAVE` are
separate household classes, so a mod can give nobles a different maximum. Several
mods may request the same resource safely: Choir keeps the largest requested
maximum, preserves existing household-resource order, and does not remove vanilla
requirements. Amounts must be whole numbers from 1 through 15.

### Change food or drink preferences

```java
checkRace(ChoirRaces.patchPreference(new RacePreferencePatch(
    "example.balance",
    "human-add-fruit",
    "HUMAN",
    RacePreferenceKind.FOOD,
    RacePreferenceOperation.ADD,
    0,
    List.of("FRUIT"))));
```

Preference operations are `ADD`, `REMOVE`, and `REPLACE`. Choir rebuilds both the
ordered preference list and its matching resource mask. The final food or drink
list may not be empty.

### Add lore without erasing another mod's prose

```java
checkRace(ChoirRaces.patchText(new RaceTextPatch(
    "example.race",
    "tilapi-moon-elf-history",
    "TILAPI",
    RaceTextField.LONG_DESCRIPTION,
    RaceCollectionOperation.APPEND,
    0,
    List.of("The Tilapi remember an ancient conflict with the Moon Elves."),
    RaceMissingTargetPolicy.SKIP)));
```

Text patches cover long descriptions, initial challenges, pros, cons, army and
raider names, greetings, farewells, curses, insults, titles, city terms, and
self/other/child phrases. Collections support `PREPEND`, `APPEND`, `REMOVE`, and
`REPLACE`. Scalar prose cannot use `REMOVE`.

### Add a directed relationship

```java
checkRace(ChoirRaces.patchRelationship(new RaceRelationshipPatch(
    "example.race",
    "moon-elf-to-human",
    "MOON_ELF",
    "HUMAN",
    RaceNumericOperation.ADD,
    0,
    0.15,
    RaceMissingTargetPolicy.FAIL)));
```

Relationships are directed. Register the reverse pair too if the relationship is
mutual. Final values are clamped to `[0, 1]`.

### Patch numeric race attributes

```java
checkRace(ChoirRaces.patchNumericAttribute(new RaceNumericPatch(
    "example.balance",
    "human-temperate-climate",
    "HUMAN",
    RaceNumericAttribute.CLIMATE_PREFERENCE,
    "TEMPERATE",
    RaceNumericOperation.ADD,
    0,
    0.10,
    RaceMissingTargetPolicy.FAIL)));
```

Numeric attributes cover population maximum/growth; climate and terrain;
work/building/pool/road preferences; crime and punishment; and resource price
multipliers/caps. Operations are `ADD`, `MULTIPLY`, and `REPLACE`.

### Patch likes and dislikes

```java
checkRace(ChoirRaces.patchStanding(new RaceStandingPatch(
    "example.balance",
    "human-likes-service",
    "HUMAN",
    "SERVICE_EXAMPLE",
    "SUBJECT",
    RaceNumericOperation.ADD,
    0,
    0.20,
    RaceStandingPolarity.LIKE,
    RaceMissingTargetPolicy.SKIP)));
```

Use exact, case-sensitive V71 IDs. `SKIP` is appropriate for genuinely optional
content; use `FAIL` for required targets so a typo does not disappear silently.

Choir does not patch race IDs/order, appearances, sprite layouts, pronoun grammar,
home-furniture material arrays, or unsupported technology graphs through this API.

## Passive decoration rooms

The room API currently supports one intentionally narrow family:
`RoomFamily.PASSIVE_DECORATION`. It has no employment, service, industry,
production, storage, scripted updates, upgrades, technologies, or custom Choir
save payload.

Your mod must provide the data first:

```text
ExampleMod/V71/assets/
|-- init/room/EXAMPLE_DECORATION.txt
|-- text/room/EXAMPLE_DECORATION.txt
|-- audio/config/ambience/ExampleDecoration.txt
`-- audio/config/mono/ExampleDecoration.txt
```

Choir validates the referenced room, sprite, text, and derived sound records. It
does not manufacture missing content.

Register before the first room-registry snapshot:

```java
import choir.api.room.*;

RoomRegistrationResult result = ChoirRooms.register(
    RoomDeclaration.passiveDecoration(
        "example.mod",
        "EXAMPLE_DECORATION",
        "EXAMPLE_DECORATION_1X1"));

if (!result.accepted()) {
    throw new IllegalStateException(
        "Room registration failed: " + result.status() + " " + result.detail());
}
```

Room and sprite keys use uppercase stable IDs. New declarations submitted after
the first snapshot are rejected as late.

You may inspect the current generation without retaining game objects:

```java
for (RoomRegistrationView room : ChoirRooms.snapshot().rooms()) {
    if (room.declaration().qualifiedId()
            .equals("example.mod:EXAMPLE_DECORATION")) {
        System.out.println(room.materializedInCurrentRegistry());
        System.out.println(room.currentRuntimeIndex());
    }
}
```

Runtime indices are diagnostics for one registry generation. Never persist them.

Important save rule: a save created with a room provider enabled requires that
provider and a compatible room registry when loaded. This remains true even when
the player never built the room or deleted every instance. V71.44 does not support
removing building providers from existing saves, and Choir does not bypass that
safety check.

## Resource display groups (experimental)

Resource display groups let mods add named, presentation-only groupings to the
right-side Mini/Full resource panels and the stockpile Crates assignment selector.

They do not change resource registries, indices, native categories, prices,
recipes, stockpile state, saves, hauling, trade, production, or AI.

Define a group:

```java
import choir.api.experimental.resources.*;

ResourceDisplayGroupDefinition raw = ResourceDisplayGroupDefinition.of(
        "example.production",
        "example.production:RAW_MATERIALS",
        "example.production.resource_groups.raw_materials",
        "Raw Materials")
    .withSortOrder(10)
    .withDefinitionPriority(0)
    .withLabelResolver(() -> "Raw Materials");

checkResourceDisplay(ResourceDisplayGroups.registerGroup(raw));
```

Assign exact stable resource IDs:

```java
checkResourceDisplay(ResourceDisplayGroups.registerAssignment(
    ResourceDisplayAssignment.of(
        "example.production",
        "WOOD",
        "example.production:RAW_MATERIALS")
    .withAssignmentPriority(0)
    .withResourceSortOrder(10)));
```

The public V71 aliases are `STONE`, `WOOD`, and `LIVESTOCK`. Do not use their
internal underscore-prefixed map keys.

```java
private static void checkResourceDisplay(ResourceDisplayRegistrationResult result) {
    if (result != ResourceDisplayRegistrationResult.ACCEPTED
            && result != ResourceDisplayRegistrationResult.IDEMPOTENT) {
        throw new IllegalStateException("Resource display registration failed: " + result);
    }
}
```

Queries are safe before the live resource registry and return an explicit state:

```java
ResourceDisplayEffectiveSnapshot snapshot =
    ResourceDisplayGroups.effectiveSnapshot();

if (snapshot.state() == ResourceDisplayRuntimeState.MODEL_READY) {
    snapshot.groupForResource("WOOD").ifPresent(group ->
        System.out.println(group.groupId() + " -> " + group.label()));
}
```

Unassigned resources remain visible in native fallback groups. Missing assignments
are skipped with diagnostics. `requestRefresh()` is available after registration
changes, but should never be called every frame. `setEnabled(false)` returns both
supported interfaces to vanilla presentation.

This package is still experimental. Check runtime capability/state and be prepared
for a future migration to a stable package.

## Multi-output workshop and refiner production

Choir can make one existing, normal-data-backed workshop or refiner recipe
physically produce two to five outputs. Your mod still owns the room and its
`INDUSTRIES` data. Choir validates the exact recipe and supplies the narrow
V71.44 execution hook.

The recipe must have exactly one input. Choir `0.9.0` stores the primary and
secondary products in the same shared internal shelf pool. Only units that do not
fit become ordinary loose, haulable resource piles at the work tile.

```java
import choir.api.production.ChoirProduction;
import choir.api.production.MultiOutputRegistrationResult;
import choir.api.production.MultiOutputRoomDeclaration;
import choir.api.production.ProductionRoomFamily;

MultiOutputRegistrationResult result =
    ChoirProduction.registerMultiOutputRoom(
        MultiOutputRoomDeclaration.dataBacked(
            "example.production",
            "sawmill-byproducts",
            "EXAMPLE_SAWMILL",
            ProductionRoomFamily.WORKSHOP,
            0,
            "WOOD",
            "PLANK",
            "BARK",
            "SAWDUST"));

if (result != MultiOutputRegistrationResult.ACCEPTED
        && result != MultiOutputRegistrationResult.IDEMPOTENT) {
    throw new IllegalStateException(
        "Multi-output registration failed: " + result);
}
```

The IDs must match the live recipe exactly:

- provider and declaration IDs are stable lowercase IDs;
- room and resource IDs are uppercase V71 IDs;
- `industryIndex` is zero-based within that room's industries;
- the input and full ordered output list must match the data;
- output resource IDs must be distinct.

`WORKSHOP` and `REFINER` are the only supported families. Registration of an
identical descriptor is idempotent. Reusing its identity with different content,
or letting a second declaration claim the same room/industry target, is rejected.

At runtime the flow is:

```text
Vanilla consumes input once
-> vanilla works and stores output 0 once
-> Choir works outputs 1..4 once
-> Choir stores outputs 1..4 in the room shelves
-> vanilla loose-resource entities represent overflow only
```

All outputs consume one real shelf capacity; Choir never creates separate capacity
per resource. When every shelf is full, the room's production storage gate closes.
Leave enough walking and hauling capacity around work tiles for genuine overflow.

You can inspect stable diagnostics without retaining any game objects:

```java
var snapshot = ChoirProduction.runtimeSnapshot();
System.out.println("Ready: " + snapshot.adapterReady());
System.out.println("Plan: " + snapshot.planSignature());
System.out.println("Cycles: " + snapshot.completedCycles());
System.out.println("Secondary units: " + snapshot.emittedUnits());
System.out.println("Failures: " + snapshot.failures());
```

API v1 does not create recipes or resources, append missing outputs, support
multi-input recipes, or patch farms, mines, fisheries, orchards, hunters,
woodcutters, pastures, production statistics, regional rates, or AI.

## Multi-resource storage

Choir `0.9.0` replaces the idea that one physical stockpile shelf can hold only
one kind of item. The easiest mental model is one box divided into labelled
sections. Each section belongs to one resource and owns a fixed part of the same
real box.

Most mods need no storage code. When Choir is installed:

- stockpile shelves can mix resources selected for that stockpile;
- plus and minus assign one section, not one whole shelf;
- repeated sections for a resource are packed onto one shelf before another is
  opened, and hauling prefers a partly filled compatible shelf;
- pathfinding asks the shelf for the exact requested resource;
- deliveries, cancellations, and pickups keep resource-specific reservations;
- stockpile totals and shelf graphics include every stored resource;
- registered multi-output workshops and refiners store every output internally
  before creating loose overflow.

The default is eight resource kinds/sections per tile. If your own room needs
another limit, choose a value from 1 through 16.

In a stockpile, one base shelf therefore shows `Storage sections 0/8`, and every
section holds 10 of its 80 total items. Five wood sections and three stone
sections give wood 50 spaces and stone 30. Their resource rows show values such
as `42/50` and `18/30`. A ninth section cannot be assigned, so physical capacity
is never multiplied.

```java
import choir.api.storage.ChoirStorage;
import choir.api.storage.MultiResourceStorageDeclaration;
import choir.api.storage.MultiResourceStorageRegistrationResult;

MultiResourceStorageRegistrationResult result =
    ChoirStorage.registerRoomPolicy(
        MultiResourceStorageDeclaration.forRoom(
            "example.production",      // provider manifest ID
            "sawmill-shelf-kinds",     // stable declaration ID
            "EXAMPLE_SAWMILL",         // exact room key
            5));                        // kinds/sections per tile, not extra capacity

if (result != MultiResourceStorageRegistrationResult.ACCEPTED
        && result != MultiResourceStorageRegistrationResult.IDEMPOTENT) {
    throw new IllegalStateException("Storage policy rejected: " + result);
}
```

`1` recreates a one-kind policy, `8` is the default, and `16` is the hard cap.
For stockpiles, the real shelf capacity is divided proportionally across those
sections. Production-room shelves retain a dynamic shared pool so automatic
outputs can use any remaining physical space. The setting does not increase item
capacity or make a specialized export, transport, or military buffer accept
unrelated goods. Acceptance still belongs to the room.

If a custom/upgraded capacity does not divide evenly, Choir rounds each
resource's quota down. That can leave a few remainder spaces unused on a mixed
shelf, but it keeps the result deterministic and within physical capacity.

Use stable diagnostics when troubleshooting:

```java
var snapshot = ChoirStorage.runtimeSnapshot();
System.out.println("Ready: " + snapshot.adapterReady());
System.out.println("Live cells: " + snapshot.liveCells());
System.out.println("Stored units: " + snapshot.storedUnits());
System.out.println("Pickup reservations: " + snapshot.pickupReservations());
System.out.println("Incoming reservations: " + snapshot.incomingReservations());
System.out.println("Failures: " + snapshot.failures());
```

Old one-resource records and Choir 0.8.0/0.8.1 boolean assignments migrate
automatically. Existing stockpile contents receive enough sections to preserve
their units when possible. Choir saves stable resource IDs and fails explicitly
if a saved resource no longer exists instead of silently deleting its stack.

## Tactical combat damage

Choir `0.6.0` can compose deterministic multipliers for tactical settlement
damage to humanoids. Categories are:

- `MELEE`
- `RANGED`
- `MOUNTED`
- `ARTILLERY`

Mounted humanoids use `MOUNTED` for both contact and projectile attacks. Player
ownership follows the combat army, not a humanoid's social class.

### Player outgoing damage

```java
import choir.api.combat.*;
import java.util.EnumSet;

checkCombat(ChoirCombat.registerDamageModifier(
    CombatDamageModifier.playerOutgoing(
        "example.balance",
        "player-melee-output",
        EnumSet.of(CombatDamageCategory.MELEE),
        0,
        1.25)));
```

This makes player melee damage `1.25x` after vanilla resolves the hit and
mitigation.

### Damage received by player troops

```java
checkCombat(ChoirCombat.registerDamageModifier(
    CombatDamageModifier.playerIncoming(
        "example.balance",
        "player-ranged-intake",
        EnumSet.of(CombatDamageCategory.RANGED),
        0,
        0.80)));
```

This makes player troops receive `0.80x` ranged damage. A factor of `1.0` is
neutral, `0.5` is half, `2.0` is double, and `0.0` suppresses otherwise resolved
tactical damage. Factors must be finite and non-negative.

### Match explicit attacker and defender sides

```java
checkCombat(ChoirCombat.registerDamageModifier(
    new CombatDamageModifier(
        "example.balance",
        "enemy-artillery-vs-player",
        EnumSet.of(CombatDamageCategory.ARTILLERY),
        CombatParticipantSide.NON_PLAYER,
        CombatParticipantSide.PLAYER,
        CombatExecutionMode.TACTICAL_SETTLEMENT,
        0,
        1.10)));
```

Matching factors multiply in priority, provider ID, and modifier ID order. Choir
applies the final composite exactly once, after vanilla hit/mitigation resolution
and immediately before the humanoid receives damage.

Combat descriptors are immutable and process-retained. If a multiplier comes from
Mod Options, mark that option `RESTART` and register the persisted value on the
next process start. Re-registering the same provider/modifier identity with a new
factor in the same process is correctly rejected as a conflict; do not stack a new
modifier every time the player presses Apply.

Inspect non-game-object diagnostics when needed:

```java
CombatDamageRuntimeSnapshot snapshot = ChoirCombat.runtimeSnapshot();
System.out.println("Adapter ready: " + snapshot.adapterReady());
System.out.println("Plan: " + snapshot.planSignature());
System.out.println("Applications: " + snapshot.applications());
System.out.println("Failures: " + snapshot.failures());
```

API v1 does not affect world auto-resolve, animals, environmental momentum damage,
accuracy, defence, projectile physics, morale, or AI. Never describe a tactical
damage setting as an auto-resolve setting.

## Make Choir optional

If Choir is required, direct `choir.api.*` imports are appropriate and your
manifest should require the first framework version containing every API you use.
For shared internal multi-output shelves or the storage API, require
`choir.framework@>=0.9.0`.

If Choir support is optional, keep every Choir type out of the Songs of
Syx-discovered script class, including its fields, parameters, return types,
annotations, and static initializers. The JVM may resolve those types before an
ordinary runtime `if` can protect you.

Keep the entrypoint consumer-only and load a small integration adapter only when
Choir is present:

```java
public final class MainScript {
    public void initBeforeGameInited() {
        tryEnableChoir();
    }

    private static void tryEnableChoir() {
        try {
            Class.forName("choir.api.Choir", false,
                MainScript.class.getClassLoader());
        } catch (ClassNotFoundException absent) {
            return; // Expected neutral fallback.
        }

        try {
            Class.forName("example.mod.choir.ChoirIntegration")
                .getMethod("register")
                .invoke(null);
        } catch (ReflectiveOperationException brokenIntegration) {
            throw new IllegalStateException(
                "Choir is present, but Example Mod integration failed",
                brokenIntegration);
        }
    }
}
```

Only `example.mod.choir.ChoirIntegration` imports Choir. Catch true absence
separately from a broken present integration; swallowing every exception as
"optional" hides real compatibility problems.

## Packaging checklist

Before publishing a consumer mod:

- install Choir separately for runtime testing;
- include your `_Info.txt`, Choir manifest, optional options-provider manifest,
  consumer JAR, and consumer-owned assets;
- use `provided` or compile-only scope;
- open the final JAR and confirm it contains no `choir/api`, `choir/internal`, or
  `choir/adapter` classes;
- confirm it contains no copied Songs of Syx or Choir-owned shadow classes;
- keep stable provider, setting, patch, room, race, group, and modifier IDs;
- verify `ACCEPTED` and `IDEMPOTENT` paths as well as conflicts;
- test more than one world/registry construction in the same process;
- clear live handles on `GAME_DISPOSING`;
- document save dependencies and restart/reload requirements honestly.

Choir should be treated as the foundation dependency. Its deterministic
composition does not use launcher order as the final conflict tie-breaker.

## Quick troubleshooting

| Symptom | First thing to check |
|---|---|
| `NoClassDefFoundError` when Choir is absent | Keep Choir types out of the discovered entrypoint and use the optional adapter pattern |
| Manifest is blocked | Check missing requirements, version constraints, duplicate IDs, incompatibilities, and cycles |
| Registration returns conflict | The same stable identity was submitted with different content |
| Registration returns late | Submit process descriptors from an earlier normal script callback |
| Options page has no settings in gameplay | Confirm `ChoirOptions.register` ran and its result was accepted |
| An option compounds after every Apply | Install the hook once; update configuration instead of stacking hooks |
| Race target is missing | Verify exact case-sensitive IDs and use `FAIL` for required targets |
| Resource stays in a native group | Verify the public stable ID and inspect the effective snapshot |
| Save fails after removing a room provider | Re-enable the provider; V71.44 does not support that removal |
| Combat factor never applies | Verify capability, category, sides, tactical humanoid combat, and the runtime snapshot |
| A mixed shelf is never selected | Confirm the resource is accepted by that stockpile and the storage capability is ready |
| A room policy is rejected | Keep provider/declaration IDs stable and allow only one policy owner per room key |
| Fingerprint mismatch | Use the exact supported game version; never bypass the compatibility gate |

For installation and game-version details, see [Installation](INSTALLATION.md) and
[Compatibility](COMPATIBILITY.md). The [runtime source boundary](RUNTIME_SOURCE_BOUNDARY.md)
explains which source is public and why Choir's exact-version vanilla shadows are
not redistributed here.

## Current boundaries

Choir currently covers manifests and dependency resolution, lifecycle events,
generic typed composition, native Mod Options, selected race declarations and
patches, passive decoration rooms, experimental resource presentation groups,
tactical humanoid damage multipliers, bounded workshop/refiner multi-output
execution, section-based stockpiles, and shared-capacity production shelves.

It does not provide arbitrary reflection, unrestricted UI injection, production
output composition, arbitrary save codecs, technology graph editing, general AI
patching, world auto-resolve damage categories, or a general-purpose room factory.
Those boundaries are deliberate: a small API with a verified integration point is
safer for players and easier for modders to compose.
