/**
 * Copyright (C) Original Authors 2017
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.jx.pipelines;

import io.fabric8.utils.Strings;
import jenkins.plugins.git.GitStep;
import org.jenkinsci.plugins.jx.pipelines.helpers.BooleanHelpers;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;


public class KubernetesMavenFlowDSLTest extends AbstractKubernetesPipelineTest {
/*
    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();
*/

    @Ignore
    public void sampleRepoCIBuild() throws Exception {
        String gitUrl = "https://github.com/jstrachan/test-maven-flow-project.git";
        assertBuildSuccess(gitUrl, "scripted");
    }

    @Test
    public void sampleRepoCDBuild() throws Exception {
        String defaultBranch = "agent-label";
        String gitUrl = System.getProperty("testGitUrl");
        String branchName = System.getProperty("testGitBranch");
        if (Strings.isNullOrBlank(gitUrl)) {
            gitUrl = "https://github.com/jstrachan/test-maven-flow-project.git";
        }
        if (Strings.isNullOrBlank(branchName)) {
            branchName = defaultBranch;
        }
        if (branchName.equals(defaultBranch) && BooleanHelpers.asBoolean(System.getProperty("pausePipeline"))) {
            branchName += "-pause";
        }
        assertBuildSuccess(gitUrl, branchName);
    }

    public void assertBuildSuccess(String gitUrl, String branchName) throws Exception {
        System.out.println("Testing repo " + gitUrl + " branch " + branchName);

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "test-" + branchName);

        GitStep gitStep = new GitStep(gitUrl);
        gitStep.setBranch(branchName);
        p.setDefinition(new CpsScmFlowDefinition(gitStep.createSCM(), "Jenkinsfile"));


        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));
        //r.assertLogContains("Apache Maven 3.3.9", b);
        //r.assertLogContains("INSIDE_CONTAINER_ENV_VAR = " + CONTAINER_ENV_VAR_VALUE + "\n", b);
    }

}
