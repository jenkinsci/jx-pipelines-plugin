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

import io.jenkins.functions.Argument;
import org.jenkinsci.plugins.jx.pipelines.StepExtension;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.io.Serializable;

/**
 */
public class WaitUntilPullRequestMergedArguments implements Serializable {
    private static final long serialVersionUID = 1L;

    @Argument
    @Positive
    private int id = 0;
    @Argument
    @NotEmpty
    private String project = "";

    private StepExtension stepExtension;

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

    public void setId(int id) {
        this.id = id;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public StepExtension getStepExtension() {
        return stepExtension;
    }

    public void setStepExtension(StepExtension stepExtension) {
        this.stepExtension = stepExtension;
    }
}
