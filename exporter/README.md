# Zeebe Kafka exporter

This module is the Payment Hub exporter for the Zeebe broker (it used to live in
`openMF/ph-ee-exporter`). Zeebe writes every record it processes — process
instances, jobs, variables, incidents — to its own log. An *exporter* is a small
plugin that reads those records and sends them somewhere else. This one sends
them to Kafka, on the topic `zeebe-export`. The importers (`importer-rdbms`,
`importer-es`) read that topic and write the data into MySQL and Elasticsearch,
which is what the Operations console shows.

## It is not a service

There is no Spring Boot application here and no Docker image. The build produces
one jar that carries its own Kafka client, and the **Zeebe broker loads that jar
into its own JVM**. In Kubernetes an init container downloads the jar into a
volume, and the broker is told where it is:

```yaml
- name: ZEEBE_BROKER_EXPORTERS_KAFKA_JARPATH
  value: "/exporters/ph-ee-kafka-exporter.jar"
- name: ZEEBE_BROKER_EXPORTERS_KAFKA_CLASSNAME
  value: "hu.dpc.rt.kafkastreamer.exporter.KafkaExporter"
```

Two things follow from running inside the broker:

* **The Java version is the broker's, not ours.** A Zeebe 8.4 broker runs on
  Java 21, so the jar is built for Java 21. An older broker (Zeebe 8.2 runs on
  Java 17) cannot load it. Keep the toolchain in `build.gradle` and the Zeebe
  version in the BOM in step with the broker image that is deployed.
* **Only the Kafka client is bundled.** The Zeebe API, the SLF4J logger and the
  Prometheus client are all provided by the broker, so they are `compileOnly`
  here. Shipping a second copy of them would clash with the broker's own.

## Configuration

The broker passes exporter settings as environment variables named
`ZEEBE_BROKER_EXPORTERS_KAFKA_ARGS_<FIELD>`; they are mapped onto the fields of
`KafkaExporterConfiguration`.

| Variable | Default | What it does |
|---|---|---|
| `..._ARGS_KAFKAURL` | `kafka:9092` | Kafka bootstrap servers |
| `..._ARGS_BULK_SIZE` | `1000` | records buffered before a flush |
| `..._ARGS_BULK_DELAY` | `5` | seconds before a forced flush |
| `..._ARGS_INDEX_<TYPE>` | see `IndexConfiguration` | which record types are exported (`EVENT`, `VARIABLE`, `PROCESSINSTANCE`, …) |

`samples/` holds real exported records, useful when you need to know what the
JSON on the topic looks like.

## Build

```bash
./gradlew :exporter:build
```

The jar is `exporter/build/libs/`. The main jar is the fat jar — that is the
file the broker loads and the file that gets published.

```bash
./gradlew :exporter:publish   # needs MIFOS_ARTIFACTORY_USER / _TOKEN
```
