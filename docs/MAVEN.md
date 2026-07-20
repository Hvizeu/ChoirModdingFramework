# Maven for mod authors

The current development API coordinate is:

```xml
<dependency>
  <groupId>io.github.hvizeu</groupId>
  <artifactId>choir-api</artifactId>
  <version>0.9.0</version>
  <scope>provided</scope>
</dependency>
```

`provided` is essential: consumer JARs must not embed, shade, rename, or
redistribute Choir classes.

Choir API versions are immutable once published. If `0.9.0` is not yet available
from Maven Central, build against the official API artifact supplied with the
validated release or install that artifact locally. Maven is a development-time
dependency tool only; players continue to install Choir as a normal Songs of Syx
mod.

Maven Central is an authorized delivery channel only for the unmodified official
`io.github.hvizeu:choir-api` artifacts. It is not permission to republish Choir,
publish modified coordinates as Choir, or bundle the API/runtime into another
mod. See [LICENSE](../LICENSE) and the [license FAQ](LICENSE_FAQ.md).
