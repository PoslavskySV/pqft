FROM maven:3-jdk-14 AS builder-base

RUN mkdir -p /build/q.core && mkdir -p /build/redberry

WORKDIR /build

RUN curl https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.12.0/jmx_prometheus_javaagent-0.12.0.jar -o jmx_prometheus_javaagent.jar
COPY deployment/dockerfiles/jmx-config.yaml jmx-config.yaml

COPY redberry redberry
RUN cd redberry && mvn clean install -DskipTests

COPY q.core/pom.xml q.core/pom.xml
RUN cd q.core && mvn verify clean --fail-never

COPY q.core q.core
RUN cd q.core && mvn clean install -DskipTests

FROM openjdk:14.0.1-jdk AS runtime-base

COPY qgraf-src qgraf-src
RUN yum update -y && yum install -y gcc-gfortran gdb make && cd qgraf-src && gfortran qgraf-3.4.2.f -o qgraf

RUN mkdir /app \
    && mkdir /kafka-state

WORKDIR /app

FROM runtime-base AS runtime

ENTRYPOINT ["java", \
            "--enable-preview", \
            "-XX:InitialRAMPercentage=50", \
            "-XX:MaxRAMPercentage=70", \
            "-javaagent:./jmx_prometheus_javaagent.jar=8090:jmx-config.yaml", \
            "-Dcom.sun.management.jmxremote", \
            "-Dcom.sun.management.jmxremote.port=5555", \
            "-Dcom.sun.management.jmxremote.authenticate=false", \
            "-Dcom.sun.management.jmxremote.ssl=false", \
            "-cp", "app.jar"]

COPY --from=builder-base /build/q.core/target/qPlatform-1.0-SNAPSHOT-distribution.jar /app/app.jar
COPY --from=runtime-base /qgraf-src/qgraf /app/qgraf
COPY --from=runtime-base /qgraf-src/qgrafSty.sty /app/qgrafSty.sty
COPY --from=builder-base /build/jmx_prometheus_javaagent.jar /app/jmx_prometheus_javaagent.jar
COPY --from=builder-base /build/jmx-config.yaml /app/jmx-config.yaml
