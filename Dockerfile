# Install maven
FROM maven:alpine as maven

# Package jenkinsfile runner and plugins
FROM jenkins/jenkinsfile-runner as jenkinsfile-runner
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN cd /app/jenkins && jar -cvf jenkins.war *
RUN java -jar /app/bin/jenkins-plugin-manager.jar --war /app/jenkins/jenkins.war --plugin-file /usr/share/jenkins/ref/plugins.txt && rm /app/jenkins/jenkins.war

# Build final image using jdk8
FROM openjdk:8
RUN apt-get update && apt-get install -y git && mkdir -p /function/app
COPY --from=maven /usr/share/maven /usr/share/maven
COPY --from=jenkinsfile-runner /app /app
COPY target/jenkinsfile-runner-googlecloudrun-1.0-SNAPSHOT.jar /app/jenkinsfile-runner-googlecloudrun.jar
COPY src/main/resources/log.properties /app/log.properties

RUN ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENTRYPOINT [ "java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:MaxRAMFraction=2", "-XX:-UsePerfData", "-XX:+UseSerialGC", "-Xshare:on", "/app/jenkinsfile-runner-googlecloudrun.jar"]
