#!/usr/bin/env bash
# Live source check (see Probe.java). Needs Java 11+; downloads org.json and kxml2 once.
set -euo pipefail
cd "$(dirname "$0")/../.."
mkdir -p build/probe/classes
J=build/probe
[ -f $J/json.jar ] || curl -sSfL -o $J/json.jar https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar
[ -f $J/kxml2.jar ] || curl -sSfL -o $J/kxml2.jar https://repo1.maven.org/maven2/net/sf/kxml/kxml2/2.3.0/kxml2-2.3.0.jar
CP="$J/json.jar:$J/kxml2.jar"
javac -nowarn -encoding UTF-8 -cp "$CP" -d $J/classes $(find src/app/newsbreak/core -name '*.java') tools/probe/Probe.java
java -cp "$J/classes:$CP" Probe .
