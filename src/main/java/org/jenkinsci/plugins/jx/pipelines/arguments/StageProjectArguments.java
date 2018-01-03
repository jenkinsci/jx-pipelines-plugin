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
import org.jenkinsci.plugins.jx.pipelines.helpers.ConfigHelper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class StageProjectArguments extends JXPipelinesArguments<StageProjectArguments> {
    private static final long serialVersionUID = 1L;

    @Argument
    @NotEmpty
    private String project = "";
    @Argument
    private boolean useMavenForNextVersion;
    @Argument
    private String extraSetVersionArgs = "";
    @Argument
    private List<String> extraImagesToStage = new ArrayList<>();
    @Argument
    private String containerName = "maven";
    @Argument
    private String clientsContainerName = "clients";
    @Argument
    private boolean useStaging;
    @Argument
    private String stageRepositoryUrl = "https://oss.sonatype.org";
    @Argument
    private String stageServerId = "oss-sonatype-staging";
    @Argument
    private boolean skipTests;
    @Argument
    private boolean disableGitPush = false;
    @Argument
    private List<String> mavenProfiles = new ArrayList<>();

    @DataBoundConstructor
    public StageProjectArguments() {
    }

    public StageProjectArguments(String project) {
        this.project = project;
    }

    public static StageProjectArguments newInstance(Map<String, Object> map) {
        return ConfigHelper.populateBeanFromConfiguration(StageProjectArguments.class, map);
    }

    public boolean isUseMavenForNextVersion() {
        return useMavenForNextVersion;
    }

    @DataBoundSetter
    public void setUseMavenForNextVersion(boolean useMavenForNextVersion) {
        this.useMavenForNextVersion = useMavenForNextVersion;
    }

    public String getExtraSetVersionArgs() {
        return extraSetVersionArgs;
    }

    @DataBoundSetter
    public void setExtraSetVersionArgs(String extraSetVersionArgs) {
        this.extraSetVersionArgs = extraSetVersionArgs;
    }

    public List<String> getExtraImagesToStage() {
        return extraImagesToStage;
    }

    @DataBoundSetter
    public void setExtraImagesToStage(List<String> extraImagesToStage) {
        this.extraImagesToStage = extraImagesToStage;
    }

    public String getProject() {
        return project;
    }

    @DataBoundSetter
    public void setProject(String project) {
        this.project = project;
    }

    public String getContainerName() {
        return containerName;
    }

    @DataBoundSetter
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getClientsContainerName() {
        return clientsContainerName;
    }

    @DataBoundSetter
    public void setClientsContainerName(String clientsContainerName) {
        this.clientsContainerName = clientsContainerName;
    }

    public boolean isUseStaging() {
        return useStaging;
    }

    @DataBoundSetter
    public void setUseStaging(boolean useStaging) {
        this.useStaging = useStaging;
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    @DataBoundSetter
    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    public String getStageRepositoryUrl() {
        return stageRepositoryUrl;
    }

    @DataBoundSetter
    public void setStageRepositoryUrl(String stageRepositoryUrl) {
        this.stageRepositoryUrl = stageRepositoryUrl;
    }

    public String getStageServerId() {
        return stageServerId;
    }

    @DataBoundSetter
    public void setStageServerId(String stageServerId) {
        this.stageServerId = stageServerId;
    }

    public boolean isDisableGitPush() {
        return disableGitPush;
    }

    @DataBoundSetter
    public void setDisableGitPush(boolean disableGitPush) {
        this.disableGitPush = disableGitPush;
    }

    public List<String> getMavenProfiles() {
        return mavenProfiles;
    }

    @DataBoundSetter
    public void setMavenProfiles(List<String> mavenProfiles) {
        this.mavenProfiles = mavenProfiles;
    }

    @Extension @Symbol("stageProject")
    public static class DescriptorImpl extends JXPipelinesArgumentsDescriptor<StageProjectArguments> {

    }
}
