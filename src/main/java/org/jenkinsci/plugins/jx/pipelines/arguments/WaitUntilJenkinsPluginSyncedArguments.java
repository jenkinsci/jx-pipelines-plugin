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
import org.jenkinsci.plugins.jx.pipelines.model.ServiceConstants;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.validation.constraints.NotEmpty;

/**
 */
public class WaitUntilJenkinsPluginSyncedArguments extends JXPipelinesArguments<WaitUntilJenkinsPluginSyncedArguments> {
    private static final long serialVersionUID = 1L;

    @Argument
    private String repo = ServiceConstants.JENKINS_ARCHIVE_REPO;
    @Argument
    @NotEmpty
    private String name = "";
    @Argument
    @NotEmpty
    private String version = "";

    @Override
    public String toString() {
        return "Arguments{" +
                "repo='" + repo + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    @DataBoundConstructor
    public WaitUntilJenkinsPluginSyncedArguments() {

    }

    public String getRepo() {
        return repo;
    }

    @DataBoundSetter
    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    @Extension @Symbol("waitUntilJenkinsPluginSynced")
    public static class DescriptorImpl extends JXPipelinesArgumentsDescriptor<WaitUntilJenkinsPluginSyncedArguments> {

    }
}
