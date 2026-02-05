#!/bin/bash
# Run quarkusDev with g-unit-service AWS profile
export AWS_PROFILE=g-unit-service
./gradlew quarkusDev
