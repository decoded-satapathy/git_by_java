#!/bin/sh

set -e

(
  cd "$(dirname "$0")"
  mvn -B package -Ddir=/tmp/git_lite_build
)

exec java -jar /tmp/git_lite_build/git_lite.jar "$@"
