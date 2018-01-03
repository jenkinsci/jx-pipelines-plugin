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
import org.jenkinsci.plugins.jx.pipelines.model.ServiceConstants;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 */
public class WaitUntilArtifactSyncedArguments extends JXPipelinesArguments<WaitUntilArtifactSyncedArguments> {
    private static final long serialVersionUID = 1L;

    @Argument
    private String repositoryUrl = ServiceConstants.MAVEN_CENTRAL;
    @Argument
    @NotEmpty
    private String groupId = "";
    @Argument
    @NotEmpty
    private String artifactId = "";
    @Argument
    @NotEmpty
    private String version = "";
    @Argument
    private String extension = "jar";

    private StepExtension stepExtension;

    @DataBoundConstructor
    public WaitUntilArtifactSyncedArguments() {
    }

    public WaitUntilArtifactSyncedArguments(String groupId, String artifactId, String version, StepExtension stepExtension) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.stepExtension = stepExtension;
    }

    @Override
    public String toString() {
        return "Arguments{" +
                "repo='" + repositoryUrl + '\'' +
                ", groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", ext='" + extension + '\'' +
                '}';
    }


    /**
     * Returns true if the properties are populated with enough values to watch for an artifact
     */
    public boolean isValid() {
        return io.fabric8.utils.Strings.notEmpty(repositoryUrl) && io.fabric8.utils.Strings.notEmpty(groupId) && io.fabric8.utils.Strings.notEmpty(artifactId) && io.fabric8.utils.Strings.notEmpty(extension);
    }


    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    @DataBoundSetter
    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getGroupId() {
        return groupId;
    }

    @DataBoundSetter
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @DataBoundSetter
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    public String getExtension() {
        return extension;
    }

    @DataBoundSetter
    public void setExtension(String extension) {
        this.extension = extension;
    }

    public StepExtension getStepExtension() {
        return stepExtension;
    }

    @DataBoundSetter
    public void setStepExtension(StepExtension stepExtension) {
        this.stepExtension = stepExtension;
    }

    @Extension @Symbol("waitUntilArtifactSynced")
    public static class DescriptorImpl extends JXPipelinesArgumentsDescriptor<WaitUntilArtifactSyncedArguments> {

    }
}
