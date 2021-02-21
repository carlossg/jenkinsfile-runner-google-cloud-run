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

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HandlerTest {

    // public static class Initializer implements QuarkusTestResourceLifecycleManager {

    //     @Override
    //     public Map<String, String> start() {
    //         System.out.println("Overwriting system properties");
    //         File tmpDir = new File(System.getProperty("user.dir") + "/target/tmp");
    //         tmpDir.mkdirs();
    //         System.setProperty("app.root", System.getProperty("user.dir") + "/target/app");
    //         System.setProperty("tmp.dir", tmpDir.getAbsolutePath());
    //         System.setProperty("git.path", "/usr/local/bin/git");
    //             return Collections.singletonMap("app.root", System.getProperty("user.dir") + "/target/app");
    //     }


    //     @Override
    //     public void stop() {
    //     }
    // }

    @Test
    public void test() throws Exception {
        GitHubPayload request = new GitHubPayload();
        request.setAfter("main");
        request.setRef("refs/heads/main");
        Map<String, Object> repository = new HashMap<>();
        repository.put("clone_url", "https://github.com/carlossg/jenkinsfile-runner-example.git");
        request.setRepository(repository);

        given().contentType("application/json") //
                .body(new Gson().toJson(request)) //
                .when().post("/handle") //
                .then().statusCode(200) //
                .body(is("{\"exitCode\":0,\"message\":\"Finished\"}"));
    }

}
