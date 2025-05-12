ARG JAVA_PYTHON_BASE_IMAGE_VERSION=2.2.5
FROM airbyte/airbyte-base-java-python-image:${JAVA_PYTHON_BASE_IMAGE_VERSION} AS connector-builder-server

# Set up CDK requirements
ARG CDK_VERSION=6.48.6
ENV CDK_PYTHON=${VENV_ROOT}/bin/python
ENV CDK_ENTRYPOINT ${VENV_ROOT}/lib/python3.10/site-packages/airbyte_cdk/connector_builder/main.py
# Set up CDK
ENV PIP=${VENV_ROOT}/bin/pip
COPY --chown=airbyte:airbyte requirements.txt requirements.txt
RUN ${PIP} install -r requirements.txt

ARG VERSION=dev

ENV APPLICATION airbyte-connector-builder-server
ENV VERSION ${VERSION}

WORKDIR /app

# This is automatically unzipped by Docker
USER root
ADD airbyte-app.tar /app
RUN chown -R airbyte:airbyte /app
USER airbyte:airbyte

# wait for upstream dependencies to become available before starting server
CMD ["/bin/bash", "airbyte-app/bin/airbyte-connector-builder-server"]

LABEL io.airbyte.version=${VERSION}
LABEL io.airbyte.name=airbyte/connector-builder-server
