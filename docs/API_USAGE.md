# Choir API usage

Consumer mods compile against `choir-api` and use only `choir.api.*` packages.
At runtime, Choir remains a separately installed mod.

## Declare a capability

Place `V71/choir/core-platform.properties` in the consumer mod:

```properties
formatVersion=1
modId=example.mod
displayName=Example Mod
version=1.0.0
requires=choir.framework@>=0.4.3
capabilities=example.mod.options
gameVersion=0.71.44
choirApi=>=0.4.3
```

Use stable, case-sensitive IDs. Do not import `choir.internal`, `choir.adapter`,
or Songs of Syx UI classes.

## Add a Mod Options page

For pre-game discovery, add `V71/choir/options-provider.properties`:

```properties
formatVersion=1
providerId=example.mod
displayName=Example Mod
description=Configures Example Mod.
```

Register settings from the consumer script when its runtime is available:

```java
import choir.api.options.ChoirOptions;
import choir.api.options.OptionRegistrationResult;
import choir.api.options.OptionSchema;
import choir.api.options.OptionSetting;

OptionSchema schema = OptionSchema.builder("example.mod", "Example Mod")
    .description("Configures Example Mod.")
    .schemaVersion(1)
    .add(OptionSetting.section("general", "General"))
    .add(OptionSetting.bool("enabled", "Enabled", true)
        .description("Enables the optional effect.")
        .build())
    .build();

OptionRegistrationResult result = ChoirOptions.register(schema);
if (result != OptionRegistrationResult.ACCEPTED
        && result != OptionRegistrationResult.IDEMPOTENT) {
    throw new IllegalStateException("Choir Options registration failed: " + result);
}

boolean enabled = ChoirOptions.getBoolean("example.mod", "enabled", true);
```

Choir owns the screen, layout, Apply/Cancel/Reset behavior, persistence, and input
routing. Consumer mods declare schemas, read typed values, and keep repeated
registration idempotent.

For optional Choir support, do not put direct Choir references in the game script
entrypoint. Probe and load a small consumer-owned integration adapter only when
Choir is present.
