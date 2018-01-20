/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.jx.pipelines.arguments;

import hudson.Extension;
import io.jenkins.functions.Argument;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.jx.pipelines.StepExtension;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;

/**
 */
public class WaitUntilPullRequestMergedArguments extends JXPipelinesArguments<WaitUntilPullRequestMergedArguments> {
    private static final long serialVersionUID = 1L;

    @Argument
    @Positive
    private int id = 0;
    @Argument
    @NotEmpty
    private String project = "";

    private StepExtension stepExtension;

    @DataBoundConstructor
    public WaitUntilPullRequestMergedArguments() {
    }

    public WaitUntilPullRequestMergedArguments(int id, String project, StepExtension stepExtension) {
        this.id = id;
        this.project = project;
        this.stepExtension = stepExtension;
    }

    @Override
    public String toString() {
        return "Arguments{" +
                "id=" + id +
                ", project='" + project + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    @DataBoundSetter
    public void setId(int id) {
        this.id = id;
    }

    public String getProject() {
        return project;
    }

    @DataBoundSetter
    public void setProject(String project) {
        this.project = project;
    }

    public StepExtension getStepExtension() {
        return stepExtension;
    }

    @DataBoundSetter
    public void setStepExtension(StepExtension stepExtension) {
        this.stepExtension = stepExtension;
    }

    @Extension @Symbol("waitUntilPullRequestMerged")
    public static class DescriptorImpl extends JXPipelinesArgumentsDescriptor<WaitUntilPullRequestMergedArguments> {

    }
}
