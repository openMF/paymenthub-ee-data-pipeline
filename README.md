# paymenthub-ee-data-pipeline

Everything that moves Payment Hub workflow data out of the Zeebe broker and into a store you can
query: the broker plugin that publishes the records, the two importers that consume them, and the
operations API over Zeebe itself.

[![License](https://img.shields.io/badge/License-MPL--2.0-blue.svg)](LICENSE)

## Modules

| Module | What it is | Ships |
|---|---|---|
| [`exporter/`](exporter) | Zeebe exporter plugin. The broker loads it from disk and runs it **inside its own JVM**, streaming every record to Kafka. | a fat jar |
| [`importer-es/`](importer-es) | Reads those records off Kafka and indexes them in Elasticsearch. | a Docker image |
| [`importer-rdbms/`](importer-rdbms) | Reads the same records into MySQL, and serves a health endpoint on 5000. | a Docker image |
| [`zeebe-ops/`](zeebe-ops) | Operations REST API over Zeebe: start and cancel workflow instances, list deployed BPMN. | a Docker image |

Each of the three services is a separate Docker image, published as
`<org>/paymenthub-ee-<module>`. The exporter has **no** image: it is a plugin, not a service.

These four were four separate repositories before (`ph-ee-exporter`, `ph-ee-importer-es`,
`ph-ee-importer-rdbms`, `ph-ee-zeebe-ops`).

## Building

One Gradle build for the whole repository. Java 21 is required.

The services are built for Java 21. The exporter is compiled with `--release 17` (Java 17
bytecode) against the Zeebe 8.2.12 exporter API, because it runs inside the Zeebe broker's JVM
and Mifos Gazelle's broker (`camunda/zeebe:8.2.12`) runs on Java 17. See
[exporter/README.md](exporter/README.md) before changing either.

```bash
./gradlew build
```

Per module:

```bash
./gradlew :importer-rdbms:bootJar
```

Library versions come from the `org.mifos:paymenthub-ee-bom` platform, published by
[paymenthub-ee-core](https://github.com/openMF/paymenthub-ee-core). Do not pin managed versions in
a module's `build.gradle`.

To build a service image, use the **repository root** as the build context — the `COPY` path in
each Dockerfile is module-qualified:

```bash
./gradlew :zeebe-ops:bootJar
docker build -f zeebe-ops/Dockerfile -t paymenthub-ee-zeebe-ops .
```

## Branches

- `dev` is the active development branch — all PRs should target `dev`.
- `main` holds released versions.

## Licence

Mozilla Public License 2.0 (see [LICENSE](LICENSE)), except the files listed in [NOTICE](NOTICE),
which carry Apache 2.0 or Zeebe Community License 1.0 headers. NOTICE also credits the original
authors.

## Contributing

See [contributing.md](contributing.md), our [Code of Conduct](CODE_OF_CONDUCT.md) and the [security policy](security.md).
