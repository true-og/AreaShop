#!/usr/bin/env bash

VERSION="$1"

# Copy and rename the jar, using $VERSION in the final filename
cp AreaShop-OG/build/libs/AreaShop-OG-${VERSION}.jar "build/libs/AreaShop-OG-${VERSION}.jar"
