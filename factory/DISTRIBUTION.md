# Factory distribution

This archive contains the compiled factory and all of its Java runtime
dependencies. It does not require Maven or the factory source code.

Requirements:

- Bash
- JDK 21 or newer
- Git
- The configured agent harnesses, such as `pi`, available on `PATH`

Run the factory from any working directory after extracting the archive:

```sh
path/to/factory-distribution/bin/factory \
  --run product --target path/to/target --line careful --seed path/to/spec.md

# Later invocations remember those settings.
path/to/factory-distribution/bin/factory --run product
```

Use this command to verify the packaged assembly line without invoking an
agent:

```sh
path/to/factory-distribution/bin/factory \
  --run check --target path/to/target --line careful --seed path/to/spec.md --check-line
```
