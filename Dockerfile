# Install maven
FROM maven:alpine as maven

# Package jenkinsfile runner and plugins
FROM jenkins/jenkinsfile-runner as jenkinsfile-runner
ARG JENKINSFILE_RUNNER_VERSION=1.0-beta-29
ARG PLUGIN_CLI_VERSION=2.5.0

# Custom tools
RUN apt-get update && apt-get install -y git
COPY --from=maven /usr/share/maven /usr/share/maven
RUN ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ARG GITHUBCLI_VERSION=1.6.1

RUN curl -sSLf -o /tmp/gh.tar.gz https://github.com/cli/cli/releases/download/v${GITHUBCLI_VERSION}/gh_${GITHUBCLI_VERSION}_linux_amd64.tar.gz && \
    tar -xf /tmp/gh.tar.gz -C /tmp && \
    mv /tmp/gh_*/bin/gh /usr/local/bin && \
    rm -f /tmp/gh.tar.gz

# CasC
COPY jenkins.yaml /usr/share/jenkins/ref/casc/jenkins.yaml

# Plugin installation
ENV JENKINS_VERSION=2.235.4
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN curl -fsSL -o /app/bin/jenkins-plugin-manager.jar \
    "https://github.com/jenkinsci/plugin-installation-manager-tool/releases/download/${PLUGIN_CLI_VERSION}/jenkins-plugin-manager-${PLUGIN_CLI_VERSION}.jar" && \
    java -jar /app/bin/jenkins-plugin-manager.jar --plugin-file /usr/share/jenkins/ref/plugins.txt -d /usr/share/jenkins/ref/plugins --verbose && \
    rm -f /app/bin/jenkins-plugin-manager.jar

COPY target/jenkinsfile-runner-googlecloudrun-1.0-SNAPSHOT-runner.jar /app/jenkinsfile-runner-googlecloudrun.jar

ENTRYPOINT [ "java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:MaxRAMFraction=2", "-XX:-UsePerfData", "-XX:+UseSerialGC", "-jar", "/app/jenkinsfile-runner-googlecloudrun.jar"]
