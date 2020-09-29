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

import com.google.common.base.Strings;
import hudson.Extension;
import io.jenkins.functions.Argument;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class PromoteImagesArguments extends StepContainer<PromoteImagesArguments> {
    private static final long serialVersionUID = 1L;

    @Argument
    private String tag = "";
    @Argument
    private String org = "";
    @Argument
    private String toRegistry = "";
    @Argument
    private List<String> images = new ArrayList<>();
    @Argument
    private String containerName = "maven";

    @DataBoundConstructor
    public PromoteImagesArguments() {
    }

    public PromoteImagesArguments(String tag, String org, String toRegistry, List<String> images, PromoteImagesArguments original) {
        this.tag = tag;
        this.org = org;
        this.toRegistry = toRegistry;
        this.images = images;
        copyFrom(original);
    }

    /**
     * Returns why this step cannot be invoked or null if its valid
     */
    public String validate() {
        if (images != null && !images.isEmpty()) {
            if (Strings.isNullOrEmpty(org)) {
                return "Cannot promote images " + images + " as missing the dockerOrganisation argument: " + this;
            }
            if (Strings.isNullOrEmpty(toRegistry)) {
                return "Cannot promote images " + images + " as missing the promoteToDockerRegistry argument: " + this;
            }
        }
        return null;
    }

    public List<String> getImages() {
        return images;
    }

    @DataBoundSetter
    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getTag() {
        return tag;
    }

    @DataBoundSetter
    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getOrg() {
        return org;
    }

    @DataBoundSetter
    public void setOrg(String org) {
        this.org = org;
    }

    public String getToRegistry() {
        return toRegistry;
    }

    @DataBoundSetter
    public void setToRegistry(String toRegistry) {
        this.toRegistry = toRegistry;
    }

    public String getContainerName() {
        return containerName;
    }

    @DataBoundSetter
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @Extension @Symbol("promoteImages")
    public static class DescriptorImpl extends JXPipelinesArgumentsDescriptor<PromoteImagesArguments> {

    }
}
