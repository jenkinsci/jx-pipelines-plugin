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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class PromoteArtifactsArguments implements Serializable {
    private static final long serialVersionUID = 1L;

    @Argument
    private String project = "";
    @Argument
    private String version = "";
    @Argument
    private List<String> repoIds = new ArrayList<>();
    @Argument
    private String containerName = "maven";
    @Argument
    private boolean helmPush;
    @Argument
    private boolean updateNextDevelopmentVersion;
    @Argument
    private String updateNextDevelopmentVersionArguments = "";

    private StepExtension stepExtension;

    public PromoteArtifactsArguments() {
    }

    public PromoteArtifactsArguments(String project, String version) {
        this.project = project;
        this.version = version;
    }

    public PromoteArtifactsArguments(String project, String version, List<String> repoIds) {
        this.project = project;
        this.version = version;
        this.repoIds = repoIds;
    }

    public PromoteArtifactsArguments(String project, String version, List<String> repoIds, String containerName, boolean helmPush, boolean updateNextDevelopmentVersion, String updateNextDevelopmentVersionArguments, StepExtension stepExtension) {
        this.project = project;
        this.version = version;
        this.repoIds = repoIds;
        this.containerName = containerName;
        this.helmPush = helmPush;
        this.updateNextDevelopmentVersion = updateNextDevelopmentVersion;
        this.updateNextDevelopmentVersionArguments = updateNextDevelopmentVersionArguments;
        this.stepExtension = stepExtension;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getRepoIds() {
        return repoIds;
    }

    public void setRepoIds(List<String> repoIds) {
        this.repoIds = repoIds;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public boolean isHelmPush() {
        return helmPush;
    }

    public void setHelmPush(boolean helmPush) {
        this.helmPush = helmPush;
    }

    public boolean isUpdateNextDevelopmentVersion() {
        return updateNextDevelopmentVersion;
    }

    public void setUpdateNextDevelopmentVersion(boolean updateNextDevelopmentVersion) {
        this.updateNextDevelopmentVersion = updateNextDevelopmentVersion;
    }

    public String getUpdateNextDevelopmentVersionArguments() {
        return updateNextDevelopmentVersionArguments;
    }

    public void setUpdateNextDevelopmentVersionArguments(String updateNextDevelopmentVersionArguments) {
        this.updateNextDevelopmentVersionArguments = updateNextDevelopmentVersionArguments;
    }

    public StepExtension getStepExtension() {
        return stepExtension;
    }

    public void setStepExtension(StepExtension stepExtension) {
        this.stepExtension = stepExtension;
    }
}
