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
import io.jenkins.functions.Argument;
import org.jenkinsci.plugins.jx.pipelines.StepExtension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class PromoteImagesArguments implements Serializable {
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
    private String containerName = "clients";

    private StepExtension stepExtension;

    public PromoteImagesArguments() {
    }

    public PromoteImagesArguments(String tag, String org, String toRegistry, List<String> images, StepExtension stepExtension) {
        this.tag = tag;
        this.org = org;
        this.toRegistry = toRegistry;
        this.images = images;
        this.stepExtension = stepExtension;
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

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getToRegistry() {
        return toRegistry;
    }

    public void setToRegistry(String toRegistry) {
        this.toRegistry = toRegistry;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public StepExtension getStepExtension() {
        return stepExtension;
    }

    public void setStepExtension(StepExtension stepExtension) {
        this.stepExtension = stepExtension;
    }
}
