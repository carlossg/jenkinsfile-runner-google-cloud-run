# Install maven
FROM maven:alpine as maven

# Package jenkinsfile runner and plugins
FROM csanchez/jenkinsfile-runner:1.0-beta-6-2.150.2 as jenkinsfile-runner
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
# workaround https://github.com/jenkinsci/docker/pull/587
RUN mkdir /usr/share/jenkins/ref/plugins/tmp.lock && \
  /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

# fn utilities
FROM fnproject/fn-java-fdk:jdk9-1.0.83 as fdk

# Build final image using jdk8
FROM openjdk:8
RUN apt-get update && apt-get install -y git && mkdir -p /function/app
COPY --from=maven /usr/share/maven /usr/share/maven
COPY --from=fdk /function /function
COPY --from=jenkinsfile-runner /app /app
COPY --from=jenkinsfile-runner /usr/share/jenkins/ref/plugins /app/plugins
COPY target/jenkinsfile-runner-fn-1.0-SNAPSHOT.jar /function/app/
COPY src/main/resources/log.properties /app/log.properties

RUN ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

# copied from fnproject/fn-java-fdk
ENTRYPOINT [ "/usr/bin/java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:MaxRAMFraction=2", "-XX:-UsePerfData", "-XX:+UseSerialGC", "-Xshare:on", "-Djava.library.path=/function/runtime/lib", "-cp", "/function/app/*:/function/runtime/*", "com.fnproject.fn.runtime.EntryPoint"]

CMD ["org.csanchez.jenkins.fn.Handler::handleRequest"]
