# Installation

1. Download the official Choir package for Songs of Syx `0.71.44` from
   [GitHub Releases](https://github.com/Hvizeu/ChoirModdingFramework/releases) or
   another distribution page operated by the project owner.
2. Do not use GitHub's source-code ZIP as a player installation. Generated runtime
   JARs are release artifacts and are intentionally not stored in the Git tree.
3. Extract the official package into the Songs of Syx user mod directory without nesting the
   `ChoirModdingFramework` folder twice.
4. Enable Choir in the game launcher or ChoirLauncher.
5. Keep Choir at the foundation of your profile. In ChoirLauncher, lower priority
   numbers load earlier, so Choir normally belongs at Priority `1`.
6. Keep every content mod that created rooms or other registry definitions enabled
   when loading saves that used it.

Choir is a separate dependency. Never embed its JAR inside another mod.

An official package has this shape:

```text
ChoirModdingFramework/
|-- _Info.txt
`-- V71/
    |-- choir/core-platform.properties
    `-- script/ChoirModdingFramework.jar
```

Verify the checksum supplied with that release before installing a manually
downloaded package.
