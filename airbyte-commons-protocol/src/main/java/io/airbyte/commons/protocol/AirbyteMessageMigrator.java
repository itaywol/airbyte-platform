/*
 * Copyright (c) 2020-2025 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.commons.protocol;

import com.google.common.annotations.VisibleForTesting;
import io.airbyte.commons.protocol.migrations.AirbyteMessageMigration;
import io.airbyte.commons.protocol.migrations.MigrationContainer;
import io.airbyte.commons.version.Version;
import io.airbyte.config.ConfiguredAirbyteCatalog;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * AirbyteProtocol Message Migrator.
 *
 * This class is intended to apply the transformations required to go from one version of the
 * AirbyteProtocol to another.
 */
@SuppressWarnings({"MethodTypeParameterName", "LineLength"})
@Singleton
public class AirbyteMessageMigrator {

  private final MigrationContainer<AirbyteMessageMigration<?, ?>> migrationContainer;

  public AirbyteMessageMigrator(final List<AirbyteMessageMigration<?, ?>> migrations) {
    migrationContainer = new MigrationContainer<>(migrations);
  }

  @PostConstruct
  public void initialize() {
    migrationContainer.initialize();
  }

  /**
   * Downgrade a message from the most recent version to the target version by chaining all the
   * required migrations.
   *
   * @param message message to upgrade
   * @param target target version ?
   * @param configuredAirbyteCatalog catalog
   * @param <PreviousVersion> version of message
   * @param <CurrentVersion> version to go to
   * @return downgraded catalog
   */
  public <PreviousVersion, CurrentVersion> PreviousVersion downgrade(final CurrentVersion message,
                                                                     final Version target,
                                                                     final Optional<ConfiguredAirbyteCatalog> configuredAirbyteCatalog) {
    return migrationContainer.downgrade(message, target, (migration, msg) -> applyDowngrade(migration, msg, configuredAirbyteCatalog));
  }

  /**
   * Upgrade a message from the source version to the most recent version by chaining all the required
   * migrations.
   *
   * @param message message to upgrade
   * @param source source's version ?
   * @param configuredAirbyteCatalog catalog
   * @param <PreviousVersion> version of message
   * @param <CurrentVersion> version to go to
   * @return upgraded catalog
   */
  public <PreviousVersion, CurrentVersion> CurrentVersion upgrade(final PreviousVersion message,
                                                                  final Version source,
                                                                  final Optional<ConfiguredAirbyteCatalog> configuredAirbyteCatalog) {
    return migrationContainer.upgrade(message, source, (migration, msg) -> applyUpgrade(migration, msg, configuredAirbyteCatalog));
  }

  /**
   * Get most recent protocol version.
   *
   * @return protocol version
   */
  public Version getMostRecentVersion() {
    return migrationContainer.getMostRecentVersion();
  }

  // Helper function to work around type casting
  private static <PreviousVersion, CurrentVersion> PreviousVersion applyDowngrade(final AirbyteMessageMigration<PreviousVersion, CurrentVersion> migration,
                                                                                  final Object message,
                                                                                  final Optional<ConfiguredAirbyteCatalog> configuredAirbyteCatalog) {
    return migration.downgrade((CurrentVersion) message, configuredAirbyteCatalog);
  }

  // Helper function to work around type casting
  private static <PreviousVersion, CurrentVersion> CurrentVersion applyUpgrade(final AirbyteMessageMigration<PreviousVersion, CurrentVersion> migration,
                                                                               final Object message,
                                                                               final Optional<ConfiguredAirbyteCatalog> configuredAirbyteCatalog) {
    return migration.upgrade((PreviousVersion) message, configuredAirbyteCatalog);
  }

  // Used for inspection of the injection
  @VisibleForTesting
  Set<String> getMigrationKeys() {
    return migrationContainer.getMigrationKeys();
  }

}
