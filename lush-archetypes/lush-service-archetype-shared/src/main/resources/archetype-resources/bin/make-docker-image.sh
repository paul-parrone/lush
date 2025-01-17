#!/bin/bash

source ./bin/rt-env.sh
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=$GROUP_ID/$ARTIFACT_ID:$ARTIFACT_VERSION


