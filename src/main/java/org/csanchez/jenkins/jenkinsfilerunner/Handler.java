/*
 * The MIT License
 *
 * Copyright (c) 2020, Carlos Sanchez
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.csanchez.jenkins.jenkinsfilerunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

import io.jenkins.jenkinsfile.runner.bootstrap.Bootstrap;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * @author Carlos Sanchez
 * @since
 *
 */
@javax.ws.rs.Path("/handle")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Handler {

    static final Logger logger = LogManager.getLogger(Handler.class);

    // just for local testing
    private static final String appRoot = System.getProperty("app.root", "/app");
    private static final String tmpDir = System.getProperty("tmp.dir", "/tmp");
    private static final File tmp = new File(tmpDir);
    private static final File gitWorkdir = new File(tmp, "workspace");
    private static final String gitPath = System.getProperty("git.path", "/usr/bin/git");

    /**
     * Main entry point. Accept GitHub event payload
     */
    @POST
    public String handleRequest(String req) {
        Response response;
        if (req == null || req.isEmpty()) {
            response = new Response(-1, "Empty request");
        } else {
            logger.info("Parsing GitHub payload");
            GitHubPayload request = new Gson().fromJson(req, GitHubPayload.class);
            response = handleRequest(request);
        }
        return new Gson().toJson(response);
    }

    /**
     * Handle the GitHub payload. Clone from Git and run the Jenkinsfile
     */
    Response handleRequest(GitHubPayload request) {

        if (request.getRepository() == null) {
            logger.fatal("Repository not present in payload");
            return new Response(0, "Repository not present in payload");
        }

        String cloneUrl = (String) request.getRepository().get("clone_url");
        String commit = request.getAfter();

        if (cloneUrl == null || commit == null || cloneUrl.isEmpty() || commit.isEmpty()) {
            logger.fatal("repository.clone_url or after not present in payload");
            return new Response(0, "repository.clone_url or after not present in payload");
        }

        System.out.printf("Cloning %s@%s\n", cloneUrl, commit);
        try {
            int gitClone = gitClone(cloneUrl, commit);
            if (gitClone != 0) {
                return new Response(gitClone, "Git clone failed: " + cloneUrl + "@" + commit);
            }
        } catch (IOException | InterruptedException e) {
            logger.fatal("Failed to clone repo");
            throw new RuntimeException(e);
        }

        return runJenkinsfile(gitWorkdir);
    }

    /**
     * Do the Git clone
     */
    public int gitClone(String url, String commit) throws IOException, InterruptedException {
        FileUtils.deleteDirectory(gitWorkdir);
        if (!gitWorkdir.mkdir()) {
            logger.fatal("Failed to create dir: " + gitWorkdir.getAbsolutePath());
            return -1;
        }
        ;
        Process p = new ProcessBuilder(gitPath, "clone", url, gitWorkdir.getAbsolutePath()).inheritIO()
                .directory(gitWorkdir).start();
        int waitFor = p.waitFor();
        if (waitFor == 0) {
            p = new ProcessBuilder(gitPath, "checkout", commit).inheritIO().directory(gitWorkdir).start();
            waitFor = p.waitFor();
        }
        return waitFor;
    }

    /**
     * Execute the Jenkinsfile
     */
    public Response runJenkinsfile(File dir) {
        System.out.println("tmp dir: " + tmpDir);
        System.out.println("App root: " + appRoot);

        if (!tmp.exists() && !tmp.mkdirs()) {
            return new Response(-1, "Unable to create tmp dir: " + tmpDir);
        }

        File jenkinsfile = Paths.get(dir.getAbsolutePath(), "Jenkinsfile").toFile();

        System.setProperty("app.name", "jenkinsfile-runner");
        System.setProperty("app.repo", Paths.get(appRoot, "repo").toString());
        System.setProperty("app.home", appRoot);
        System.setProperty("basedir", appRoot);

        try {
            // link the plugins to the writable filesystem in tmp as they need to be extracted
            Path pluginsPath = Paths.get(tmpDir, "plugins");
            FileUtils.deleteDirectory(pluginsPath.toFile());
            Files.createDirectories(pluginsPath);
            linkFolder(Paths.get(appRoot, "plugins"), pluginsPath);

            // call jenkinsfile runner with the right parameters
            final Bootstrap bootstrap = new Bootstrap();
            bootstrap.warDir = Paths.get(appRoot, "jenkins").toFile();
            bootstrap.pluginsDir = pluginsPath.toFile();
            bootstrap.jenkinsfile = jenkinsfile;
            logger.info(String.format("Executing bootstrap: warDir: %s, pluginsDir: %s, jenkinsfile: %s",
                    bootstrap.warDir, bootstrap.pluginsDir, bootstrap.jenkinsfile));
            final int status = bootstrap.run();

            return new Response(status, "Finished");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void linkFolder(Path src, Path dest) throws IOException {
        Files.walk(src).filter(source -> Files.isRegularFile(source))
                .forEach(source -> link(source, dest.resolve(src.relativize(source))));
    }

    private void link(Path source, Path dest) {
        try {
            if (Files.exists(dest)) {
                Files.delete(dest);
            }
            Files.createSymbolicLink(dest, source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}