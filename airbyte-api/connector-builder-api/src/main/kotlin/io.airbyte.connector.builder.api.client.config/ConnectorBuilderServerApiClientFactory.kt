/*
 * Copyright (c) 2020-2025 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.connector.builder.api.client.config

import dev.failsafe.RetryPolicy
import io.airbyte.api.client.auth.InternalApiAuthenticationInterceptor
import io.airbyte.api.client.auth.KeycloakAccessTokenInterceptor
import io.airbyte.connectorbuilderserver.api.client.generated.ConnectorBuilderServerApi
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.Response
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Factory
class ConnectorBuilderServerApiClientFactory {
  @Singleton
  fun connectorBuilderServerApiClient(
    @Value("\${airbyte.connector-builder-server-api.base-path}") connectorBuilderServerApiBasePath: String,
    @Value("\${airbyte.connector-builder-server-api.connect-timeout-seconds}") connectTimeoutSeconds: Long,
    @Value("\${airbyte.connector-builder-server-api.read-timeout-seconds}") readTimeoutSeconds: Long,
    authenticationInterceptor: InternalApiAuthenticationInterceptor,
    keycloakAccessTokenInterceptor: KeycloakAccessTokenInterceptor?,
  ): ConnectorBuilderServerApi {
    val builder: OkHttpClient.Builder =
      OkHttpClient.Builder().apply {
        addInterceptor(authenticationInterceptor)
        readTimeout(readTimeoutSeconds.seconds.toJavaDuration())
        connectTimeout(connectTimeoutSeconds.seconds.toJavaDuration())

        // Add Keycloak access token interceptor if present
        // This will always be present in cloud once we've fully deprecated ESP auth
        keycloakAccessTokenInterceptor?.let {
          addInterceptor(it)
        }
      }

    val okHttpClient: OkHttpClient = builder.build()
    val retryPolicy: RetryPolicy<Response> = RetryPolicy.builder<Response>().withMaxRetries(0).build()

    return ConnectorBuilderServerApi(basePath = connectorBuilderServerApiBasePath, policy = retryPolicy, client = okHttpClient)
  }
}
