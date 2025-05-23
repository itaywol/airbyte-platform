/*
 * Copyright (c) 2020-2025 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.metrics.lib;

import static io.airbyte.metrics.lib.ApmTraceConstants.Tags.ATTEMPT_NUMBER_KEY;
import static io.airbyte.metrics.lib.ApmTraceConstants.Tags.CONNECTION_ID_KEY;
import static io.airbyte.metrics.lib.ApmTraceConstants.Tags.ERROR_ACTUAL_TYPE_KEY;
import static io.airbyte.metrics.lib.ApmTraceConstants.Tags.JOB_ID_KEY;
import static io.airbyte.metrics.lib.ApmTraceConstants.Tags.JOB_ROOT_KEY;

import datadog.trace.api.DDTags;
import datadog.trace.api.interceptor.MutableSpan;
import io.airbyte.metrics.MetricAttribute;
import io.opentracing.Span;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Collection of utility methods to help with performance tracing.
 */
public class ApmTraceUtils {

  /**
   * String format for the name of tags added to spans.
   */
  public static final String TAG_FORMAT = "airbyte.%s.%s";

  /**
   * Standard prefix for tags added to spans.
   */
  public static final String TAG_PREFIX = "metadata";

  /**
   * Converts the provided metric attributes to tags and adds them to the currently active span, if
   * one exists. <br />
   * All tags added via this method will use the default {@link #TAG_PREFIX} namespace.
   *
   * @param attrs A list of attributes to be converted to tags and added to the currently active span.
   */
  public static void addTagsToTrace(final List<MetricAttribute> attrs) {
    final Map<String, Object> tags = attrs.stream()
        .collect(Collectors.toMap(MetricAttribute::getKey, MetricAttribute::getValue));

    addTagsToTrace(tags, TAG_PREFIX);
  }

  /**
   * Adds all the provided tags to the currently active span, if one exists. <br />
   * All tags added via this method will use the default {@link #TAG_PREFIX} namespace.
   *
   * @param tags A map of tags to be added to the currently active span.
   */
  public static void addTagsToTrace(final Map<String, Object> tags) {
    addTagsToTrace(tags, TAG_PREFIX);
  }

  /**
   * Adds all provided tags to the currently active span, if one exists, under the provided tag name
   * namespace.
   *
   * @param tags A map of tags to be added to the currently active span.
   * @param tagPrefix The prefix to be added to each custom tag name.
   */
  public static void addTagsToTrace(final Map<String, Object> tags, final String tagPrefix) {
    addTagsToTrace(GlobalTracer.get().activeSpan(), tags, tagPrefix);
  }

  /**
   * Adds all the provided tags to the provided span, if one exists.
   *
   * @param span The {@link Span} that will be associated with the tags.
   * @param tags A map of tags to be added to the currently active span.
   * @param tagPrefix The prefix to be added to each custom tag name.
   */
  public static void addTagsToTrace(final Span span, final Map<String, Object> tags, final String tagPrefix) {
    if (span != null) {
      tags.entrySet().stream().filter(e -> e.getKey() != null && e.getValue() != null).forEach(entry -> {
        span.setTag(formatTag(entry.getKey(), tagPrefix), entry.getValue().toString());
      });
    }
  }

  /**
   * Adds all the provided values to the currently active span, if one exists. <br />
   * All tags added via this method will use the default {@link #TAG_PREFIX} namespace. Any null
   * values will be ignored.
   */
  public static void addTagsToTrace(final UUID connectionId, final Long attemptNumber, final String jobId, final Path jobRoot) {
    final Map<String, Object> tags = new HashMap<>();

    if (connectionId != null) {
      tags.put(CONNECTION_ID_KEY, connectionId);
    }
    if (attemptNumber != null) {
      tags.put(ATTEMPT_NUMBER_KEY, attemptNumber);
    }
    if (jobId != null) {
      tags.put(JOB_ID_KEY, jobId);
    }
    if (jobRoot != null) {
      tags.put(JOB_ROOT_KEY, jobRoot);
    }
    addTagsToTrace(tags);
  }

  /**
   * Adds an exception to the currently active span, if one exists.
   *
   * @param t The {@link Throwable} to be added to the currently active span.
   */
  public static void addExceptionToTrace(final Throwable t) {
    addExceptionToTrace(GlobalTracer.get().activeSpan(), t);
  }

  /**
   * Adds an exception to the provided span, if one exists.
   *
   * @param span The {@link Span} that will be associated with the exception.
   * @param t The {@link Throwable} to be added to the provided span.
   */
  public static void addExceptionToTrace(final Span span, final Throwable t) {
    if (span != null) {
      span.setTag(Tags.ERROR, true);
      span.log(Map.of(Fields.ERROR_OBJECT, t));
    }
  }

  /**
   * Adds all the provided tags to the root span.
   *
   * @param tags A map of tags to be added to the root span.
   */
  public static void addTagsToRootSpan(final Map<String, Object> tags) {
    final Span activeSpan = GlobalTracer.get().activeSpan();
    if (activeSpan instanceof MutableSpan) {
      final MutableSpan localRootSpan = ((MutableSpan) activeSpan).getLocalRootSpan();
      tags.forEach((key, value) -> localRootSpan.setTag(formatTag(key, TAG_PREFIX), value.toString()));
    }
  }

  /**
   * Adds an exception to the root span, if an active one exists.
   *
   * @param t The {@link Throwable} to be added to the provided span.
   */
  public static void recordErrorOnRootSpan(final Throwable t) {
    final Span activeSpan = GlobalTracer.get().activeSpan();
    if (activeSpan != null) {
      activeSpan.setTag(Tags.ERROR, true);
      activeSpan.log(Map.of(Fields.ERROR_OBJECT, t));
    }
    if (activeSpan instanceof MutableSpan) {
      final MutableSpan localRootSpan = ((MutableSpan) activeSpan).getLocalRootSpan();
      localRootSpan.setError(true);
      localRootSpan.setTag(DDTags.ERROR_MSG, t.getMessage());
      localRootSpan.setTag(DDTags.ERROR_TYPE, t.getClass().getName());
      final StringWriter errorString = new StringWriter();
      t.printStackTrace(new PrintWriter(errorString));
      localRootSpan.setTag(DDTags.ERROR_STACK, errorString.toString());
    }
  }

  /**
   * Formats the tag key using {@link #TAG_FORMAT} provided by this utility, using the default tag
   * prefix {@link #TAG_PREFIX}.
   *
   * @param tagKey The tag key to format.
   * @return The formatted tag key.
   */
  public static String formatTag(final String tagKey) {
    return formatTag(tagKey, TAG_PREFIX);
  }

  /**
   * Formats the tag key using {@link #TAG_FORMAT} provided by this utility with the provided tag
   * prefix.
   *
   * @param tagKey The tag key to format.
   * @param tagPrefix The prefix to be added to each custom tag name.
   * @return The formatted tag key.
   */
  public static String formatTag(final String tagKey, final String tagPrefix) {
    return String.format(TAG_FORMAT, tagPrefix, tagKey);
  }

  public static void addActualRootCauseToTrace(final Exception e) {
    Throwable inner = e;
    while (inner.getCause() != null) {
      inner = inner.getCause();
    }
    ApmTraceUtils.addTagsToTrace(Map.of(ERROR_ACTUAL_TYPE_KEY, inner.getClass().getName()));
  }

}
