#!/usr/bin/env bash

VERSION="$1"

# Copy and rename the jar, using $VERSION in the final filename
cp AreaShop/build/libs/AreaShop-${VERSION}.jar "build/libs/AreaShop-${VERSION}.jar"
