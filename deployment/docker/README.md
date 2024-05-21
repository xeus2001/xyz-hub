# Naksha PostgresQL
This folder contains docker configurations needed to build Naksha-Hub docker and the PostgresQL database docker that is used by Naksha-Hub.

## Build the image

### Prepare
Before you can start building the image, install docker, for example [Docker Desktop](https://docs.docker.com/desktop/install/).

The first step is to export environment variables, and then login to your docker registry (**DR**), example of HERE registry:

```bash
export DR_USER='<here-user>'
export DR_PWD='<encrypted-password>'
export DR_HOST="hcr.data.here.com"
export DR_NAKSHA_POSTGRES="$DR_HOST/naksha-devops/naksha-postgres"
docker login -u="$DR_USER" -p="$DR_PWD" $DR_HOST
```

**Note**: If you feel saver, enter the password on CLI. The rest of the instructions are no longer environment dependent.

The PostgresQL docker will be build in multiple steps. We will first build the base postgres with the core extensions (`naksha-pg-base`). Then we will build the PLV8 extension and the PL/Java extensions into it (`naksha-pg-plv8`), which takes a lot of time. Finally, we build the real deployment version, which now gets start scripts and other scripts added. We separate these steps, because this allows easily to change the configuration of the PostgresQL database without the need to compile it again.

### Build
The Naksha PostgresQL image is build in steps, follow these instructions:

```bash
# Define postgres version, and revision to be build
# v{pg-major}.{pg-minor}[.{pg-revision}]-r{revision}
export VERSION="v16.2-r1"
cd deployment/docker/postgres

# Build base image and push
cd naksha-pg-base
docker build --platform=linux/arm64 --push -t "$DR_NAKSHA_POSTGRES:base-arm64-$VERSION" .
docker build --platform=linux/amd64 --push -t "$DR_NAKSHA_POSTGRES:base-amd64-$VERSION" .
cd ..

# Build pljava image and push
cd naksha-pg-pljava
docker build --platform=linux/arm64 \
       --build-arg="ARCH=arm64" \
       --build-arg="DR_NAKSHA_POSTGRES=$DR_NAKSHA_POSTGRES" \
       --build-arg="VERSION=$VERSION" \
       --push \
       -t "${DR_NAKSHA_POSTGRES}:pljava-arm64-$VERSION" .
docker build --platform=linux/amd64 \
       --build-arg="ARCH=amd64" \
       --build-arg="DR_NAKSHA_POSTGRES=$DR_NAKSHA_POSTGRES" \
       --build-arg="VERSION=$VERSION" \
       --push \
       -t "${DR_NAKSHA_POSTGRES}:pljava-amd64-$VERSION" .
cd ..

# Build plv8 image and push
cd naksha-pg-plv8
docker build --platform=linux/arm64 \
       --build-arg="ARCH=arm64" \
       --build-arg="DR_NAKSHA_POSTGRES=$DR_NAKSHA_POSTGRES" \
       --build-arg="VERSION=$VERSION" \
       --push \
       -t "${DR_NAKSHA_POSTGRES}:plv8-arm64-$VERSION" .
docker build --platform=linux/amd64 \
       --build-arg="ARCH=amd64" \
       --build-arg="DR_NAKSHA_POSTGRES=$DR_NAKSHA_POSTGRES" \
       --build-arg="VERSION=$VERSION" \
       --push \
       -t "${DR_NAKSHA_POSTGRES}:plv8-amd64-$VERSION" .
cd ..

# Build the final postgres with run-scripts and default configurations
# Note, this can be done multiple times without any need to re-build the previous images
# Therefore we introduce the BASE var
export BASE="v16.2-r0"
export VERSION="v16.2-r1"
cd naksha-pg-release
docker build --platform=linux/arm64 \
       --build-arg="ARCH=arm64" \
       --build-arg="DR_NAKSHA_POSTGRES=$DR_NAKSHA_POSTGRES" \
       --build-arg="VERSION=$BASE" \
       --push \
       -t "${DR_NAKSHA_POSTGRES}:arm64-$VERSION" \
       -t "${DR_NAKSHA_POSTGRES}:arm64-latest" .
docker build --platform=linux/amd64 \
       --build-arg="ARCH=amd64" \
       --build-arg="DR_NAKSHA_POSTGRES=$DR_NAKSHA_POSTGRES" \
       --build-arg="VERSION=$BASE" \
       --push \
       -t "${DR_NAKSHA_POSTGRES}:amd64-$VERSION" \
       -t "${DR_NAKSHA_POSTGRES}:amd64-latest" .
cd ..
```

**Notes**:
- It is totally valid and expected that you only have one revision for the **base** image and the **plv8** image, but multiple revisions for the release.
- To show more output, add `--progress=plain` argument to the `docker build` command.

## Modify the build
Building takes a long time and is then directly pushed to the remote server. This is very unhandy while modifying the build. Therefore, it is recommended to first test the modification in a local docker container before updating the build script.

```bash
# Declare a new version
export BASE="v16.2-r0"
export VERSION="v16.2-r2"
# Build the docker
docker build --platform=linux/arm64 \
       --build-arg="ARCH=arm64" \
       --build-arg="DR_NAKSHA_POSTGRES=${DR_NAKSHA_POSTGRES}" \
       --build-arg="VERSION=$BASE" \
       -t "${DR_NAKSHA_POSTGRES}:arm64-${VERSION}" \
       .
# Run the docker
mkdir -p ~/pg_data
mkdir -p ~/pg_temp
# If the database should be cleared, do
# rm -rf ~/pg_data/*
# rm -rf ~/pg_temp/*
docker run --name naksha_pg \
       -v ~/pg_data:/usr/local/pgsql/data \
       -v ~/pg_temp:/usr/local/pgsql/temp \
       -p 0.0.0.0:5432:5432 \
       -d "hcr.data.here.com/naksha-devops/naksha-postgres:arm64-${VERSION}"
# Show logs
docker logs naksha_pg
# Show generated password
cat ~/pg_data/postgres.pwd
# Test the db
psql "user=postgres sslmode=disable host=localhost dbname=unimap"
# When not okay, delete docker and repeat
docker stop naksha_pg
docker rm naksha_pg
docker image rm "${DR_NAKSHA_POSTGRES}:arm64-${VERSION}"
```

For this purpose, start the docker, if `pg-base` should be built, then start from the [Amazon Linux](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/create-container-image.html) image.

To start a new docker and do some manual builds (for debugging or playing around), do this:

- Start a totally new docker with a bash: `docker run -it amazonlinux bash`
- Or: `docker run -it hcr.data.here.com/naksha-devops/naksha-postgres:plv8-arm64-v16.2-r0 bash`
- If you exited a docker and want to re-enter, first see which one it is doing `docker ps -a`
- Ones found, restart via `docker start -ai <container-id>`, this gets you back to where you stopped

## Start a container
Before a Naksha PostgresQL contains is started it is recommended to create a directory where you want to store the database, if you want to persist it:

```bash
mkdir -p ~/pg_data
mkdir -p ~/pg_temp
```

To create the container do the following:

```bash
docker pull hcr.data.here.com/naksha-devops/naksha-postgres:arm64-v16.2-r1
docker run --name naksha_pg \
       -v ~/pg_data:/usr/local/pgsql/data \
       -v ~/pg_temp:/usr/local/pgsql/temp \
       -p 0.0.0.0:5432:5432 \
       -d hcr.data.here.com/naksha-devops/naksha-postgres:arm64-v16.2-r1
```

When the docker container is started for the first time, it will generate a random password for the `postgres` user and store it inside the docker container in `/home/postgres/postgres.pwd`. You should remember this, because the password is stored in the database. It as well prints it, you can review like:

```bash
docker logs naksha_pg
...
Initialized database with password: bLqzfifYRzfOoXtGvqUsmxQuxCsuhsqT
...
```

**Note**: You may have to change the platform (amd64/arm64) and the version of the container.

## Env-Vars
The docker container accepts the same environment variables that [libpq](https://www.postgresql.org/docs/current/libpq-envars.html) accepts:

- **PGDATABASE**: Database.
- **PGUSER**: User.
- **PGPASSWORD**: Password.

## Performance test
```bash
export NVMEPART=/dev/md0
fio --time_based --name=benchmark --size=100G --runtime=30 \
    --filename=$NVMEPART --ioengine=libaio --randrepeat=0 \
    --iodepth=128 --direct=1 --invalidate=1 --verify=0 --verify_fatal=0 \
    --numjobs=4 --rw=randwrite --blocksize=32k --group_reporting
```