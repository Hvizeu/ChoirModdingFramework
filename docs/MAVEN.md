# Maven for mod authors

The planned public API coordinate is:

```xml
<dependency>
  <groupId>io.github.hvizeu</groupId>
  <artifactId>choir-api</artifactId>
  <version>0.4.3</version>
  <scope>provided</scope>
</dependency>
```

`provided` is essential: consumer JARs must not embed, shade, rename, or
redistribute Choir classes.

The API is prepared for Maven Central publication but is not published there yet.
Until publication is announced, build against the official API artifact supplied
with the release process or use a local Maven installation supplied by the project
owner. Maven is a development-time dependency tool only; players continue to
install Choir as a normal Songs of Syx mod.
