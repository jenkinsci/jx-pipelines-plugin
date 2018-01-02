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

import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.plugins.git.GitStep;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;


public class MavenFlowDSLTest {


    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();
    @Rule
    public GitSampleRepoRule sampleRepo = new GitSampleRepoRule();

    //@Ignore
    @Test
    public void smokeTests() throws Exception {

        sampleRepo.init();
        sampleRepo.write("Jenkinsfile", "node {\n" +
                "  mavenFlow {\n" +
                "    cdOrganisation 'cheese'\n" +
                "    promoteArtifacts {\n" +
                "      steps {\n" +
                "        echo \"replacement promote steps\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        sampleRepo.git("add", "Jenkinsfile");
        sampleRepo.git("commit", "--message=files");
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");

                p.setDefinition(new CpsScmFlowDefinition(new GitStep(sampleRepo.toString()).createSCM(), "Jenkinsfile"));
                WorkflowRun b = p.scheduleBuild2(0).waitForStart();
                story.j.assertLogContains("Finished: SUCCESS",
                        story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b)));


            }
        });
    }

    @Ignore
    public void sampleRepoCIBuild() throws Exception {
        assertBuildSuccess("scripted");
    }

    @Ignore
    public void sampleRepoCDBuild() throws Exception {
        assertBuildSuccess("scripted-cd");
    }

    public void assertBuildSuccess(String branchName) throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");

                GitStep gitStep = new GitStep("https://github.com/jstrachan/test-maven-flow-project.git");
                gitStep.setBranch(branchName);
                p.setDefinition(new CpsScmFlowDefinition(gitStep.createSCM(), "Jenkinsfile"));
                WorkflowRun b = p.scheduleBuild2(0).waitForStart();
                story.j.assertLogContains("Finished: SUCCESS",
                        story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b)));


            }
        });
    }

}
