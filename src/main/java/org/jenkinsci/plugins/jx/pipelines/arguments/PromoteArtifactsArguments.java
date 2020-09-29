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
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class PromoteArtifactsArguments extends StepContainer<PromoteArtifactsArguments> {
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

    @DataBoundConstructor
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

    public PromoteArtifactsArguments(String project, String version, List<String> repoIds, String containerName, boolean helmPush, boolean updateNextDevelopmentVersion, String updateNextDevelopmentVersionArguments, PromoteArtifactsArguments original) {
        this.project = project;
        this.version = version;
        this.repoIds = repoIds;
        this.containerName = containerName;
        this.helmPush = helmPush;
        this.updateNextDevelopmentVersion = updateNextDevelopmentVersion;
        this.updateNextDevelopmentVersionArguments = updateNextDevelopmentVersionArguments;
        copyFrom(original);
    }

    public String getProject() {
        return project;
    }

    @DataBoundSetter
    public void setProject(String project) {
        this.project = project;
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getRepoIds() {
        return repoIds;
    }

    @DataBoundSetter
    public void setRepoIds(List<String> repoIds) {
        this.repoIds = repoIds;
    }

    public String getContainerName() {
        return containerName;
    }

    @DataBoundSetter
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public boolean isHelmPush() {
        return helmPush;
    }

    @DataBoundSetter
    public void setHelmPush(boolean helmPush) {
        this.helmPush = helmPush;
    }

    public boolean isUpdateNextDevelopmentVersion() {
        return updateNextDevelopmentVersion;
    }

    @DataBoundSetter
    public void setUpdateNextDevelopmentVersion(boolean updateNextDevelopmentVersion) {
        this.updateNextDevelopmentVersion = updateNextDevelopmentVersion;
    }

    public String getUpdateNextDevelopmentVersionArguments() {
        return updateNextDevelopmentVersionArguments;
    }

    @DataBoundSetter
    public void setUpdateNextDevelopmentVersionArguments(String updateNextDevelopmentVersionArguments) {
        this.updateNextDevelopmentVersionArguments = updateNextDevelopmentVersionArguments;
    }

    @Extension @Symbol("promoteArtifacts")
    public static class DescriptorImpl extends JXPipelinesArgumentsDescriptor<PromoteArtifactsArguments> {

    }
}
