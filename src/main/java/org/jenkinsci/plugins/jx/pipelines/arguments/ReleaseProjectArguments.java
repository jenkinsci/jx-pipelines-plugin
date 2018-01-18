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
import io.fabric8.utils.Strings;
import io.jenkins.functions.Argument;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.jx.pipelines.StepExtension;
import org.jenkinsci.plugins.jx.pipelines.helpers.ConfigHelper;
import org.jenkinsci.plugins.jx.pipelines.model.ServiceConstants;
import org.jenkinsci.plugins.jx.pipelines.model.StagedProjectInfo;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class ReleaseProjectArguments extends JXPipelinesArguments<ReleaseProjectArguments> {
    private static final long serialVersionUID = 1L;

    @Argument
    @NotEmpty
    private String project = "";
    @Argument
    @NotEmpty
    private String releaseVersion = "";
    @Argument
    private List<String> repoIds = new ArrayList<>();
    @Argument
    private String containerName = "maven";
    @Argument
    private String dockerOrganisation = "";
    @Argument
    private String promoteToDockerRegistry = "";
    @Argument
    private List<String> promoteDockerImages = new ArrayList<>();
    @Argument
    private List<String> extraImagesToTag = new ArrayList<>();
    @Argument
    private String repositoryToWaitFor = ServiceConstants.MAVEN_CENTRAL;
    @Argument
    private String groupId = "";
    @Argument
    private String artifactExtensionToWaitFor = "";
    @Argument
    private String artifactIdToWaitFor = "";
    @Argument
    private boolean useGitTagForNextVersion;
    @Argument
    private boolean helmPush;
    @Argument
    private boolean updateNextDevelopmentVersion;
    @Argument
    private String updateNextDevelopmentVersionArguments = "";

    private StepExtension promoteArtifactsExtension;
    private StepExtension promoteImagesExtension;
    private StepExtension tagImagesExtension;
    private StepExtension waitUntilPullRequestMergedExtension;
    private StepExtension waitUntilArtifactSyncedExtension;

    @DataBoundConstructor
    public ReleaseProjectArguments() {
    }

    public ReleaseProjectArguments(StagedProjectInfo stagedProject) {
        this.project = stagedProject.getProject();
        this.releaseVersion = stagedProject.getReleaseVersion();
        this.repoIds = stagedProject.getRepoIds();
    }

    public static ReleaseProjectArguments newInstance(Map<String, Object> map) {
        return ConfigHelper.populateBeanFromConfiguration(ReleaseProjectArguments.class, map);
    }

    @Override
    public String toString() {
        return "Arguments{" +
                "project=" + project +
                ", releaseVersion='" + releaseVersion + '\'' +
                ", repoIds='" + repoIds + '\'' +
                ", containerName='" + containerName + '\'' +
                ", dockerOrganisation='" + dockerOrganisation + '\'' +
                ", promoteToDockerRegistry='" + promoteToDockerRegistry + '\'' +
                ", promoteDockerImages=" + promoteDockerImages +
                ", extraImagesToTag=" + extraImagesToTag +
                ", repositoryToWaitFor='" + repositoryToWaitFor + '\'' +
                ", groupId='" + groupId + '\'' +
                ", artifactExtensionToWaitFor='" + artifactExtensionToWaitFor + '\'' +
                ", artifactIdToWaitFor='" + artifactIdToWaitFor + '\'' +
                '}';
    }

    /**
     * Returns the arguments for invoking {@link PromoteArtifactsArguments}
     */
    public PromoteArtifactsArguments createPromoteArtifactsArguments() {
        return new PromoteArtifactsArguments(getProject(), getReleaseVersion(), getRepoIds(), getContainerName(), isHelmPush(), isUpdateNextDevelopmentVersion(), getUpdateNextDevelopmentVersionArguments(), getPromoteArtifactsExtension());
    }

    /**
     * Return the arguments for invoking {@link PromoteImagesArguments} or null if there is not sufficient configuration
     * to promote images
     */
    public PromoteImagesArguments createPromoteImagesArguments() {
        String org = getDockerOrganisation();
        String toRegistry = getPromoteToDockerRegistry();
        List<String> images = getPromoteDockerImages();
        return new PromoteImagesArguments(getReleaseVersion(), org, toRegistry, images, getPromoteImagesExtension());
    }

    /**
     * Returns the arguments for invoking {@link TagImagesArguments} or null if there are no images to tag
     */
    public TagImagesArguments createTagImagesArguments() {
        if (extraImagesToTag != null && !extraImagesToTag.isEmpty()) {
            return new TagImagesArguments(getReleaseVersion(), extraImagesToTag, getTagImagesExtension());
        } else {
            return null;
        }
    }

    /**
     * Returns the arguments for invoking {@link WaitUntilPullRequestMergedArguments}
     *
     * @param pullRequestId
     */
    public WaitUntilPullRequestMergedArguments createWaitUntilPullRequestMergedArguments(GHPullRequest pullRequestId) {
        return new WaitUntilPullRequestMergedArguments(pullRequestId.getId(), getProject(), getWaitUntilPullRequestMergedExtension());
    }

    /**
     * Returns the arguments for invoking {@link WaitUntilArtifactSyncedArguments}
     */
    public WaitUntilArtifactSyncedArguments createWaitUntilArtifactSyncedWithCentralArguments() {
        WaitUntilArtifactSyncedArguments arguments = new WaitUntilArtifactSyncedArguments(groupId, artifactIdToWaitFor, getReleaseVersion(), getWaitUntilArtifactSyncedExtension());
        if (Strings.notEmpty(artifactExtensionToWaitFor)) {
            arguments.setExtension(artifactExtensionToWaitFor);
        }
        if (Strings.notEmpty(repositoryToWaitFor)) {
            arguments.setRepositoryUrl(repositoryToWaitFor);
        }
        return arguments;
    }

    // Properties
    //-------------------------------------------------------------------------

    public String getProject() {
        return project;
    }

    @DataBoundSetter
    public void setProject(String project) {
        this.project = project;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    @DataBoundSetter
    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
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

    public String getDockerOrganisation() {
        return dockerOrganisation;
    }

    @DataBoundSetter
    public void setDockerOrganisation(String dockerOrganisation) {
        this.dockerOrganisation = dockerOrganisation;
    }

    public String getPromoteToDockerRegistry() {
        return promoteToDockerRegistry;
    }

    @DataBoundSetter
    public void setPromoteToDockerRegistry(String promoteToDockerRegistry) {
        this.promoteToDockerRegistry = promoteToDockerRegistry;
    }

    public List<String> getPromoteDockerImages() {
        return promoteDockerImages;
    }

    @DataBoundSetter
    public void setPromoteDockerImages(List<String> promoteDockerImages) {
        this.promoteDockerImages = promoteDockerImages;
    }

    public List<String> getExtraImagesToTag() {
        return extraImagesToTag;
    }

    @DataBoundSetter
    public void setExtraImagesToTag(List<String> extraImagesToTag) {
        this.extraImagesToTag = extraImagesToTag;
    }

    public String getRepositoryToWaitFor() {
        return repositoryToWaitFor;
    }

    @DataBoundSetter
    public void setRepositoryToWaitFor(String repositoryToWaitFor) {
        this.repositoryToWaitFor = repositoryToWaitFor;
    }

    public String getGroupId() {
        return groupId;
    }

    @DataBoundSetter
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactExtensionToWaitFor() {
        return artifactExtensionToWaitFor;
    }

    @DataBoundSetter
    public void setArtifactExtensionToWaitFor(String artifactExtensionToWaitFor) {
        this.artifactExtensionToWaitFor = artifactExtensionToWaitFor;
    }

    public String getArtifactIdToWaitFor() {
        return artifactIdToWaitFor;
    }

    @DataBoundSetter
    public void setArtifactIdToWaitFor(String artifactIdToWaitFor) {
        this.artifactIdToWaitFor = artifactIdToWaitFor;
    }

    public boolean isUseGitTagForNextVersion() {
        return useGitTagForNextVersion;
    }

    @DataBoundSetter
    public void setUseGitTagForNextVersion(boolean useGitTagForNextVersion) {
        this.useGitTagForNextVersion = useGitTagForNextVersion;
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

    public StepExtension getPromoteArtifactsExtension() {
        return promoteArtifactsExtension;
    }

    @DataBoundSetter
    public void setPromoteArtifactsExtension(StepExtension promoteArtifactsExtension) {
        this.promoteArtifactsExtension = promoteArtifactsExtension;
    }

    public StepExtension getPromoteImagesExtension() {
        return promoteImagesExtension;
    }

    @DataBoundSetter
    public void setPromoteImagesExtension(StepExtension promoteImagesExtension) {
        this.promoteImagesExtension = promoteImagesExtension;
    }

    public StepExtension getTagImagesExtension() {
        return tagImagesExtension;
    }

    @DataBoundSetter
    public void setTagImagesExtension(StepExtension tagImagesExtension) {
        this.tagImagesExtension = tagImagesExtension;
    }

    public StepExtension getWaitUntilPullRequestMergedExtension() {
        return waitUntilPullRequestMergedExtension;
    }

    @DataBoundSetter
    public void setWaitUntilPullRequestMergedExtension(StepExtension waitUntilPullRequestMergedExtension) {
        this.waitUntilPullRequestMergedExtension = waitUntilPullRequestMergedExtension;
    }

    public StepExtension getWaitUntilArtifactSyncedExtension() {
        return waitUntilArtifactSyncedExtension;
    }

    @DataBoundSetter
    public void setWaitUntilArtifactSyncedExtension(StepExtension waitUntilArtifactSyncedExtension) {
        this.waitUntilArtifactSyncedExtension = waitUntilArtifactSyncedExtension;
    }

    @Extension @Symbol("releaseProject")
    public static class DescriptorImpl extends JXPipelinesArgumentsDescriptor<ReleaseProjectArguments> {

    }
}
