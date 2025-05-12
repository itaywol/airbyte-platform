ARG AIRBYTE_BASE_JAVA_IMAGE_TAG=dev

FROM airbyte/airbyte-base-java-image:${AIRBYTE_BASE_JAVA_IMAGE_TAG}

USER root
# development tools -> build-essential, zlib-devel -> zlib1g-dev, libbz2-devel -> libbz2-dev, readline-devel -> libreadline-dev, sqlite-devel -> libsqlite3-dev, openssl-devel -> libssl-dev, tk-devel -> tk-dev, libffi-devel -> libffi-dev, xz-devel -> liblzma-dev

# Adding: git (Not part of build-essential), python-3.10 (Echo variant clean of vulnerabilities)

RUN <<EOF
apt-get update
apt-get install -y build-essential
apt-get install -y --no-install-recommends zlib1g-dev libbz2-dev libreadline-dev sqlite3 libsqlite3-dev libssl-dev tk-dev libffi-dev liblzma-dev git
apt-get install -y --no-install-recommends python-3.10
apt-get clean
rm -rf /var/lib/apt/lists/*
EOF

USER airbyte:airbyte

# since we using echo based python, we don't need pyenv
ENV PYTHON_VERSION=3.10.17
ENV VENV_ROOT=/home/airbyte/.venv
ENV PATH=${VENV_ROOT}/bin:$PATH

RUN <<EOF
python3 -m venv ${VENV_ROOT}
python3 -m pip install --upgrade pip setuptools
EOF
