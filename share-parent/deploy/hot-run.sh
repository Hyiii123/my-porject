#!/bin/sh
set -eu

MODULE="share-modules/share-customer"
PROFILE="hot-reload"

echo "[hot] compiling ${MODULE} and required modules"
mvn -pl "${MODULE}" -am -P"${PROFILE}" -DskipTests -Dcheckstyle.skip=true install

trigger_file="${MODULE}/target/classes/reload.trigger"
mkdir -p "$(dirname "${trigger_file}")"
touch "${trigger_file}"

source_stamp() {
    find \
        "${MODULE}/src" \
        share-common \
        share-api \
        -type f ! -path '*/target/*' \( -name '*.java' -o -name '*.xml' -o -name '*.yml' -o -name '*.yaml' -o -name '*.properties' \) \
        -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -n 1
}

last_stamp="$(source_stamp || true)"

watch_sources() {
    while :; do
        sleep 2
        current_stamp="$(source_stamp || true)"
        if [ "${current_stamp}" != "${last_stamp}" ]; then
            echo "[hot] source change detected; compiling without restarting container"
            if mvn -pl "${MODULE}" -am -P"${PROFILE}" -DskipTests -Dcheckstyle.skip=true compile; then
                last_stamp="${current_stamp}"
                touch "${trigger_file}"
                echo "[hot] compile completed; Spring Boot DevTools will reload the context"
            else
                echo "[hot] compile failed; keeping the previous running classes" >&2
            fi
        fi
    done
}

watch_sources &
watch_pid=$!

cleanup() {
    kill "${watch_pid}" 2>/dev/null || true
}
trap cleanup INT TERM EXIT

echo "[hot] starting customer service on port ${SERVER_PORT:-9206}"
mvn -f "${MODULE}/pom.xml" -P"${PROFILE}" -DskipTests \
    -Dspring-boot.run.fork=true \
    -Dspring-boot.run.jvmArguments="-Xms128m -Xmx384m -Dfile.encoding=UTF-8" \
    spring-boot:run
