#!/usr/bin/env bash

set -e

# Print all commands to console if DEBUG is set
if [[ ! -z "${DEBUG}" ]]; then
    set -x
fi

# Set some helpful variables
export SERVICE_VERSION=$(awk -F "=" '/service.version/ {print $2}' gradle.properties)
export SERVICE_NAME=$(awk -F "=" '/service.name/ {print $2}' gradle.properties)
export SERVICE_CLASS=$(awk -F "=" '/service.class/ {print $2}' gradle.properties)
export SERVICE=${SERVICE_NAME}.${SERVICE_CLASS}@${SERVICE_VERSION}
export SERVICE_PROPERTY_FILE='tmitocar-service/build/resources/main/application.properties'

# Check mandatory variables
[[ -z "${PRIVATE_KEY}" ]] && \
    echo "Mandatory variable PRIVATE_KEY is not set. Add -e PRIVATE_KEY=privatekey to your arguments." && exit 1
[[ -z "${PUBLIC_KEY}" ]] && \
    echo "Mandatory variable PUBLIC_KEY is not set. Add -e PUBLIC_KEY=publickey to your arguments." && exit 1

# Optional variables
[[ -z "${SERVICE_PASSPHRASE}" ]] && export SERVICE_PASSPHRASE='tmitocar'

function set_in_service_config {
    sed -i "s|${1}[[:blank:]]*=.*|${1}=${2}|g" ${SERVICE_PROPERTY_FILE}
}

set_in_service_config spring.security.oauth2.resourceserver.jwt.issuer-uri ${ISSUER_URI}
set_in_service_config spring.security.oauth2.resourceserver.jwt.jwk-set-uri ${SET_URI}
# set_in_service_config lrsURL ${LRS_URL}
# set_in_service_config lrsAuthTokenLeipzig ${LRS_AUTH_TOKEN_LEIPZIG}
# set_in_service_config lrsAuthTokenDresden ${LRS_AUTH_TOKEN_DRESDEN}

set_in_service_config spring.data.mongodb.uri ${SPRING_DATA_MONGODB_URI}
set_in_service_config spring.data.mongodb.database ${SPRING_DATA_MONGODB_DATABASE}

set_in_service_config spring.datasource.url ${SPRING_DATASOURCE_URL}
set_in_service_config spring.datasource.username ${SPRING_DATASOURCE_USERNAME}
set_in_service_config spring.datasource.password ${SPRING_DATASOURCE_PASSWORD}

set_in_service_config xapi.url ${XAPI_URL}
set_in_service_config xapi.homepage ${XAPI_HOMEPAGE}

set_in_service_config publicKey ${PUBLIC_KEY}
set_in_service_config privateKey ${PRIVATE_KEY}

# Prevent glob expansion in lib/*
set -f

# Launch the service using java -jar
exec java -jar /src/services.tmitocar-3.0.0.jar ${SERVICE_EXTRA_ARGS}