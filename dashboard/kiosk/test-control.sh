#!/usr/bin/env bash
# Host tests for the homeboard control channel's pure-Java classes. Needs JDK 17 on PATH.
set -euo pipefail
cd "$(dirname "$0")"
rm -rf build/test && mkdir -p build/test
javac -nowarn -source 8 -target 8 -d build/test \
  src/com/wesbos/homehq/ControlSse.java src/com/wesbos/homehq/ControlCommand.java \
  src/com/wesbos/homehq/ControlBackoff.java src/com/wesbos/homehq/ControlStatusJson.java \
  src/com/wesbos/homehq/ControlClient.java test/ControlLogicTest.java
java -cp build/test ControlLogicTest
