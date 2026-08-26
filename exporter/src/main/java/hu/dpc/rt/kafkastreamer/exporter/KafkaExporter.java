/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package hu.dpc.rt.kafkastreamer.exporter;

import io.camunda.zeebe.exporter.api.Exporter;
import io.camunda.zeebe.exporter.api.context.Context;
import io.camunda.zeebe.exporter.api.context.Controller;
import io.camunda.zeebe.protocol.record.Record;

import org.slf4j.Logger;

import java.time.Duration;

public class KafkaExporter implements Exporter {
    private Logger logger;
    private Controller controller;

    private KafkaExporterConfiguration configuration;

    private KafkaExporterClient client;

    private long lastPosition = -1;

    @Override
    public void configure(final Context context) {
        try {
            logger = context.getLogger();
            configuration = context.getConfiguration().instantiate(KafkaExporterConfiguration.class);
            logger.info("Kafka exporter configured with {}", configuration);
        } catch (Exception e) {
            logger.error("Failed to configure KafkaExporter", e);
        }
    }

    @Override
    public void open(final Controller controller) {
        logger.info("Kafka exporter opening");
        this.controller = controller;
        client = new KafkaExporterClient(configuration, logger);
        scheduleDelayedFlush();
        logger.info("Kafka exporter opened");
    }

    @Override
    public void close() {
        try {
            flush();
        } catch (final Exception e) {
            logger.warn("Failed to flush records before closing exporter.", e);
        }

        try {
            client.close();
        } catch (final Exception e) {
            logger.warn("Failed to close Kafka client", e);
        }

        logger.info("Kafka exporter closed");
    }

    @Override
    public void export(Record<?> record) {
        logger.trace("Exporting record {}", record);
        client.index(record);
        lastPosition = record.getPosition();

        if (client.shouldFlush()) {
            flush();
        }
        logger.trace("Finish exporting record {}", record);
    }

    private void flushAndReschedule() {
        try {
            flush();
        } catch (final Exception e) {
            logger.error("Unexpected exception occurred on periodically flushing bulk, will retry later.", e);
        }
        scheduleDelayedFlush();
    }

    private void scheduleDelayedFlush() {
        controller.scheduleCancellableTask(Duration.ofSeconds(configuration.bulk.delay), this::flushAndReschedule);
    }

    private void flush() {
        if (client.flush()) {
            controller.updateLastExportedRecordPosition(lastPosition);
        } else {
            logger.warn("Failed to flush bulk completely");
        }
    }
}
