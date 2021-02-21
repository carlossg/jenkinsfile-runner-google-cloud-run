# Jenkinsfile Runner for Google Cloud Run

<img src="images/jenkins-google-cloud-run.png" width="150">

A [Google Cloud Run](https://cloud.google.com/run) Docker image to run Jenkins pipelines. It will process a GitHub webhook, git clone the repository and execute the Jenkinsfile in that git repository.

This image allows `Jenkinsfile` execution without needing a persistent Jenkins server running,
using Google Cloud Run.

# Google Cloud Run vs Project Fn vs AWS Lambda

Three flavors of Jenkinsfile Runner

* [Google Cloud Run](https://github.com/carlossg/jenkinsfile-runner-google-cloud-run)
* [AWS lambda](https://github.com/carlossg/jenkinsfile-runner-lambda)
* [Project Fn](https://github.com/carlossg/jenkinsfile-runner-fn)

# Limitations

Current implementation limitations:

* `checkout scm` does not work, change it to `sh 'git clone https://github.com/carlossg/jenkinsfile-runner-example.git'`

# Example

See the [jenkinsfile-runner-example](https://github.com/carlossg/jenkinsfile-runner-example) project for an example that is tested and works.

# Extending

You can add your plugins to `plugins.txt`.
You could also add the Configuration as Code plugin for configuration, example at `jenkins.yaml`.

Other tools can be added to the `Dockerfile`.

# Installation


## Building

Build the package

```
mvn verify
docker build -t jenkinsfile-runner-google-cloud-run .
```

## Publishing

Set `GITHUB_TOKEN_JENKINSFILE_RUNNER` to a token that allows posting PR comments.
A more secure way would be to use Google Cloud Secret Manager.

```
GITHUB_TOKEN_JENKINSFILE_RUNNER=...

PROJECT_ID=$(gcloud config get-value project 2> /dev/null)
docker tag jenkinsfile-runner-google-cloud-run "gcr.io/${PROJECT_ID}/jenkinsfile-runner-google-cloud-run"
docker push "gcr.io/${PROJECT_ID}/jenkinsfile-runner-google-cloud-run"
gcloud run deploy jenkinsfile-runner \
    --image "gcr.io/${PROJECT_ID}/jenkinsfile-runner-google-cloud-run" \
    --platform managed \
    --region us-east1 \
    --allow-unauthenticated \
    --memory 1Gi \
    --set-env-vars=GITHUB_TOKEN=${GITHUB_TOKEN_JENKINSFILE_RUNNER}
```

## Execution

```
URL=$(gcloud run services describe jenkinsfile-runner \
    --platform managed \
    --region us-east1 \
    --format 'value(status.address.url)')
curl -v -H "Content-Type: application/json" ${URL}/handle -d @src/test/resources/github.json
```

Note that GitHub webhooks execution will time out if your the call takes too long, it should run asynchronously
using Google Cloud Tasks.

## Logging

```
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=jenkinsfile-runner" \
    --format "value(textPayload)" --limit 100
```

or

```
gcloud alpha logging tail "resource.type=cloud_run_revision AND resource.labels.service_name=jenkinsfile-runner" \
    --format "value(textPayload)"
```


## GitHub events

Add a GitHub `json` webhook to your git repo pointing to the Google Cloud Run url.

# Testing

```
docker run -ti --rm -p 8080:8080 -e GITHUB_TOKEN=${GITHUB_TOKEN_JENKINSFILE_RUNNER} jenkinsfile-runner-google-cloud-run
curl -v -H "Content-Type: application/json" -X POST http://localhost:8080/handle -d @src/test/resources/github.json
```
