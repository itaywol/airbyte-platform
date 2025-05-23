/*
 * Copyright (c) 2020-2025 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.workers.workload

import io.airbyte.commons.json.Jsons
import io.airbyte.commons.storage.StorageClient
import io.airbyte.config.ConnectorJobOutput
import io.airbyte.config.ReplicationOutput
import io.airbyte.metrics.MetricAttribute
import io.airbyte.metrics.MetricClient
import io.airbyte.metrics.OssMetricsRegistry
import io.airbyte.metrics.lib.MetricTags
import io.airbyte.workers.workload.exception.DocStoreAccessException
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.Optional

// TODO This has been duplicated in airbyte-server/src/main/kotlin/io.airbyte.workload.output.WorkloadOutputDocStore
// TODO Delete this once we transitioned to using commands
@Deprecated("Once transitioned to commands, we should use the API version instead of direct DocStore access")
@Singleton
class JobOutputDocStore(
  @Named("outputDocumentStore") val storageClient: StorageClient,
  val metricClient: MetricClient,
) {
  @Throws(DocStoreAccessException::class)
  fun read(workloadId: String): Optional<ConnectorJobOutput> {
    val output: String? =
      try {
        storageClient.read(workloadId)
      } catch (e: Exception) {
        throw DocStoreAccessException("Unable to read output for $workloadId", e)
      }

    return Optional.ofNullable(output?.let { Jsons.deserialize(it, ConnectorJobOutput::class.java) })
  }

  @Throws(DocStoreAccessException::class)
  fun write(
    workloadId: String,
    connectorJobOutput: ConnectorJobOutput,
  ) {
    writeOutput(workloadId = workloadId, output = connectorJobOutput)
  }

  @Throws(DocStoreAccessException::class)
  fun readSyncOutput(workloadId: String): Optional<ReplicationOutput> {
    val output: String? =
      try {
        storageClient.read(workloadId).also { _ ->
          metricClient.count(metric = OssMetricsRegistry.JOB_OUTPUT_READ, attributes = arrayOf(MetricAttribute(MetricTags.STATUS, "success")))
        }
      } catch (e: Exception) {
        metricClient.count(metric = OssMetricsRegistry.JOB_OUTPUT_READ, attributes = arrayOf(MetricAttribute(MetricTags.STATUS, "error")))
        throw DocStoreAccessException("Unable to read output for $workloadId", e)
      }
    return Optional.ofNullable(output?.let { Jsons.deserialize(it, ReplicationOutput::class.java) })
  }

  @Throws(DocStoreAccessException::class)
  fun writeSyncOutput(
    workloadId: String,
    connectorJobOutput: ReplicationOutput,
  ) {
    writeOutput(workloadId = workloadId, output = connectorJobOutput)
  }

  private fun writeOutput(
    workloadId: String,
    output: Any,
  ) {
    try {
      storageClient.write(workloadId, Jsons.serialize(output))
      metricClient.count(metric = OssMetricsRegistry.JOB_OUTPUT_WRITE, attributes = arrayOf(MetricAttribute(MetricTags.STATUS, "success")))
    } catch (e: Exception) {
      metricClient.count(metric = OssMetricsRegistry.JOB_OUTPUT_WRITE, attributes = arrayOf(MetricAttribute(MetricTags.STATUS, "error")))
      throw DocStoreAccessException("Unable to write output for $workloadId", e)
    }
  }
}
