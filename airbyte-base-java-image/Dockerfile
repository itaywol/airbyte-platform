FROM r.echohq.com/amazoncorretto:21

WORKDIR /app

RUN <<EOF
# echo base comes with shadow-utils installed
apt-get update
apt-get install -y tar ca-certificates
apt-get upgrade -y
apt-get clean
rm -rf /var/lib/apt/lists/*
groupadd --gid 1000 airbyte
useradd --uid 1000 --gid airbyte --shell /bin/bash --create-home airbyte
# Create mount point for secrets
mkdir /secrets
chown -R airbyte:airbyte /secrets
chown -R airbyte:airbyte /app
chown -R airbyte:airbyte /etc/ssl/certs
chown -R airbyte:airbyte /usr/local/share/ca-certificates
mkdir -p /tmp/workspace
chown -R airbyte:airbyte /tmp/workspace
mkdir -p /tmp/airbyte_local
chown -R airbyte:airbyte /tmp/airbyte_local
EOF

# Add the Datadog Java APM agent
ADD --chown=airbyte:airbyte https://dtdg.co/latest-java-tracer dd-java-agent.jar

# Add the OpenTelemetry Java APM agent
ADD --chown=airbyte:airbyte https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar opentelemetry-javaagent.jar

ADD --chown=airbyte:airbyte entrypoint.sh /entrypoint.sh

USER airbyte:airbyte

ENTRYPOINT ["/entrypoint.sh"]
