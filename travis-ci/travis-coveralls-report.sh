#!/usr/bin/env bash

./mvnw test jacoco:report coveralls:report
