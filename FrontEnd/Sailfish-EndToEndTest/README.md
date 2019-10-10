# Sailfish integration test

This project leverages Docker and Docker-compose for integration tests execution. It requires Docker Engine and Docker Compose installed and available in PATH. Aslo Gradle operates by docker-compose with the help of [gradle-docker-compose-plugin](https://github.com/avast/gradle-docker-compose-plugin)

Basically two Docker images are utilized (please refer to docker-compose.yml for actual versions):
* tomcat:9.0.26-jdk8-openjdk-slim
* mysql:5.6

To run the test:

```
$ ./gradlew clean integrationTest
```

# Structure

```
.
├── docker-compose.yml - test environment services
├── docker
|   ├── Catalina       - workspace settings
|   ├── cfg            - hibernate and statistics db settings
|   └── db             - mysql container init script, wrapper of FrontEnd/SailfishFrontEnd/etc/DB/create_mysql_db.sh
├── build
|   ├── webapps        - mounted into container in runtime
|   └── workspace      - mounted into container in runtime
└── src/test/resources - mounted into container in runtime, required by some tests
```

## If docker-compose is not present in PATH

Optionally you can set docker-compose version in URL below
```
$ curl -L "https://github.com/docker/compose/releases/download/1.24.1/docker-compose-$(uname -s)-$(uname -m)" -o /path/to/docker-compose
$ chmod +x /path/to/docker-compose
```


set in build.gradle:
```
dockerCompose {
    ...
    // allow to set the path of the docker-compose executable
    executable = '/path/to/docker-compose'
    ...
}
```
