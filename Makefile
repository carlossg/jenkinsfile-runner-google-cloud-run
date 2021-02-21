build: jenkinsfile-runner-googlecloudrun-1.0-SNAPSHOT-runner.jar

jenkinsfile-runner-googlecloudrun-1.0-SNAPSHOT-runner.jar:
	mvn verify

deploy:
	docker build -t jenkinsfile-runner-google-cloud-run .
	$(eval PROJECT_ID := $(shell gcloud config get-value project 2> /dev/null))
	docker tag jenkinsfile-runner-google-cloud-run "gcr.io/$(PROJECT_ID)/jenkinsfile-runner-google-cloud-run"
	docker push "gcr.io/$(PROJECT_ID)/jenkinsfile-runner-google-cloud-run"
	gcloud run deploy jenkinsfile-runner \
		--image "gcr.io/$(PROJECT_ID)/jenkinsfile-runner-google-cloud-run" \
		--platform managed \
		--region us-east1 \
		--allow-unauthenticated \
		--memory 1Gi \
		--timeout 15m \
		--set-env-vars=GITHUB_TOKEN=${GITHUB_TOKEN_JENKINSFILE_RUNNER}
