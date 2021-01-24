# Jenkinsfile Runner for Project Fn

<img src="images/jenkins-fn.png" width="150">

An [Google Cloud Run](https://cloud.google.com/run) Docker image to run Jenkins pipelines. It will process a GitHub webhook, git clone the repository and execute the Jenkinsfile in that git repository.

This image allows `Jenkinsfile` execution without needing a persistent Jenkins server running in the same way as [Jenkins X Serverless](https://medium.com/@jdrawlings/serverless-jenkins-with-jenkins-x-9134cbfe6870), but using Google Cloud Run instead of Kubernetes.

# Google Cloud Run vs Project Fn vs AWS Lambda

Three flavors of Jenkinsfile Runner

* [Google Cloud Run](https://github.com/carlossg/jenkinsfile-runner-google-cloud-run)
* [AWS lambda](https://github.com/carlossg/jenkinsfile-runner-lambda)
* [Project Fn](https://github.com/carlossg/jenkinsfile-runner-fn)

# Limitations

Current implementation limitations:

* `checkout scm` does not work, change it to `sh 'git clone https://github.com/carlossg/jenkinsfile-runner-example.git'`
* Jenkinsfile must use `/tmp` for any tool that needs writing files, see the [example](https://github.com/carlossg/jenkinsfile-runner-example)

# Example

See the [jenkinsfile-runner-example](https://github.com/carlossg/jenkinsfile-runner-example) project for an example that is tested and works.

# Extending

You can add your plugins to `plugins.txt`.
You could also add the Configuration as Code plugin for configuration.

Other tools can be added to the `Dockerfile`.

# Installation


## Building

Build the package

    mvn clean package

## Publishing

    gcloud run deploy --image csanchez/jenkinsfile-runner-google-cloud-run --platform managed

## Execution

Test

    cat src/test/resources/github.json | fn invoke jenkinsfile-runner jenkinsfile-runner

## Logging

Get the logs for the last execution

    fn get logs jenkinsfile-runner jenkinsfile-runner $(fn ls calls jenkinsfile-runner jenkinsfile-runner | grep 'ID:' | head -n 1 | sed -e 's/ID: //')


## GitHub events

Add a GitHub `json` webhook to your git repo pointing to the function url.
