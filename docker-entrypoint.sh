#!/usr/bin/env bash

set -e

# print all comands to console if DEBUG is set
if [[ ! -z "${DEBUG}" ]]; then
    set -x
fi

# set some helpful variables
export SERVICE_VERSION=$(awk -F "=" '/service.version/ {print $2}' gradle.properties)
export SERVICE_NAME=$(awk -F "=" '/service.name/ {print $2}' gradle.properties)
export SERVICE_CLASS=$(awk -F "=" '/service.class/ {print $2}' gradle.properties)
export SERVICE=${SERVICE_NAME}.${SERVICE_CLASS}@${SERVICE_VERSION}
export SERVICE_PROPERTY_FILE='etc/services.tmitocar.TmitocarService.properties'


# check mandatory variables
[[ -z "${PRIVATE_KEY}" ]] && \
    echo "Mandatory variable PRIVATE_KEY is not set. Add -e PRIVATE_KEY=privatekey to your arguments." && exit 1
[[ -z "${PUBLIC_KEY}" ]] && \
    echo "Mandatory variable PUBLIC_KEY is not set. Add -e PUBLIC_KEY=publickey to your arguments." && exit 1

# optional variables
[[ -z "${SERVICE_PASSPHRASE}" ]] && export SERVICE_PASSPHRASE='tmitocar'

# wait for any bootstrap host to be available
if [[ ! -z "${BOOTSTRAP}" ]]; then
    echo "Waiting for any bootstrap host to become available..."
    for host_port in ${BOOTSTRAP//,/ }; do
        arr_host_port=(${host_port//:/ })
        host=${arr_host_port[0]}
        port=${arr_host_port[1]}
        if { </dev/tcp/${host}/${port}; } 2>/dev/null; then
            echo "${host_port} is available. Continuing..."
            break
        fi
    done
fi

function set_in_service_config {
    sed -i "s?${1}[[:blank:]]*=.*?${1}=${2}?g" ${SERVICE_PROPERTY_FILE}
}

set_in_service_config privateKey ${PRIVATE_KEY}
set_in_service_config publicKey ${PUBLIC_KEY}
set_in_service_config lrsURL ${LRS_URL}
set_in_service_config lrsAuthTokenLeipzig ${LRS_AUTH_TOKEN_LEIPZIG}
set_in_service_config lrsAuthTokenDresden ${LRS_AUTH_TOKEN_DRESDEN}

set_in_service_config mongoHost ${MONGO_HOST}
set_in_service_config mongoDB ${MONGO_DB}
set_in_service_config mongoUser ${MONGO_USER}
set_in_service_config mongoPassword ${MONGO_PASSWORD}
set_in_service_config mongoAuth ${MONGO_AUTH}

set_in_service_config pgsqlHost ${PGSQL_HOST}
set_in_service_config pgsqlPort ${PGSQL_PORT}
set_in_service_config pgsqlUser ${PGSQL_USER}
set_in_service_config pgsqlPassword ${PGSQL_PASSWORD}
set_in_service_config pgsqlDB ${PGSQL_DB}

set_in_service_config xapiUrl ${XAPI_URL}
set_in_service_config xapiHomepage ${XAPI_HOMEPAGE}

# prevent glob expansion in lib/*
set -f
LAUNCH_COMMAND='java -cp lib/* --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -s service -p '"${SERVICE_EXTRA_ARGS}"
if [[ ! -z "${BOOTSTRAP}" ]]; then
    LAUNCH_COMMAND="${LAUNCH_COMMAND} -b ${BOOTSTRAP}"
fi

