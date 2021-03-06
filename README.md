![](https://www.ga4gh.org/wp-content/themes/ga4gh-theme/gfx/GA-logo-horizontal-tag-RGB.svg)

A CRUD style implementation of the GA4GH Service Registry specification. Could be used to back any kind of installation
as long as there is a compatible client that can create, update, and remove service instances when necessary.

# Service Registry Reference Implementation [![Build Status](https://travis-ci.org/ga4gh-discovery/ga4gh-service-registry-impl.svg?branch=develop)](https://travis-ci.org/ga4gh-discovery/ga4gh-service-registry-impl) [![](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/ga4gh-discovery/ga4gh-service-registry-impl/develop/LICENSE)

This repository contains a reference implementation of the [Service Registry standard](https://github.com/ga4gh-discovery/ga4gh-service-registry/).

## Development prerequisites

* OpenJDK 11 or newer. [AdoptOpenJDK](https://adoptopenjdk.net/) is a good source.
* A PostgreSQL database v9.2 or newer (we use the JSON data type). You can just install the `postgres` package on
    your OS and configure a user and a database:
    ```
    createuser -d serviceregistry
    createdb -U serviceregistry serviceregistry
    ```
* Or you can use this Docker incantation to get one temporarily:
    ```
    docker run -d -p 5432:5432 --name serviceregistry -e POSTGRES_USER=serviceregistry -e POSTGRES_PASSWORD=serviceregistry postgres
    ```

## Creating or updating the database schema

Mac or Windows:
```
docker run --rm -it -v $(pwd)/ci/predeploy/:/liquibase/changelog/ \
  $LIQUIBASE_DOCKER_IMAGE \
  --driver=org.postgresql.Driver \
  --changeLogFile=/liquibase/changelog/db.changelog.yml \
  --url=jdbc:postgresql://host.docker.internal/serviceregistry \
  --username=serviceregistry \
  --password=serviceregistry \
  update
```

Linux:
```
docker run --rm -it -v $(pwd)/ci/predeploy/:/liquibase/changelog/ \
  $LIQUIBASE_DOCKER_IMAGE \
  --driver=org.postgresql.Driver \
  --changeLogFile=/liquibase/changelog/db.changelog.yml \
  --url=jdbc:postgresql://localhost/serviceregistry \
  --username=serviceregistry \
  --password=serviceregistry \
  update
```

where $LIQUIBASE_DOCKER_IMAGE should be set to your Liquibase Docker Image link. See https://cloud.google.com/container-registry/docs/quickstart for Container Registry setup and look up your image at at https://console.cloud.google.com/gcr/images/. This value should be of form `gcr.io/container-store/liquibase-docker-image:version`.

# Running the service locally

Make sure the database schema is up-to-date, then:

```
./gradlew clean build bootRun
```

## Building a deployable Docker image

Normally the CI server runs this to build the image, but you can do it on your local machine
to test the shell script and Dockerfile.

```shell script
image_name=dnastack-service-registry
image_version=$(git describe)
docker_tag=my.docker.repo/${image_name}:{image_version}
ci/build-docker-image ${docker_tag} ${image_name} ${image_version}
```

## Running E2E tests

To run the tests, execute the following from `e2e-tests` directory:

```
./gradlew endToEndTest
```

Environment variables for development setup as required by the tests can be obtained by sourcing `test-secrets.env`. Use `set -a` to export variables so that they're accessible by `gradlew`.
