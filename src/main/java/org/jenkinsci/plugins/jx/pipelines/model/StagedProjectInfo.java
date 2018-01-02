/**
 * Copyright (C) Original Authors 2017
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.jx.pipelines.model;

import java.io.Serializable;
import java.util.List;

/**
 */
public class StagedProjectInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String project;
    private final String releaseVersion;
    private final List<String> repoIds;

    public StagedProjectInfo(String project, String releaseVersion, List<String> repoIds) {
        this.project = project;
        this.releaseVersion = releaseVersion;
        this.repoIds = repoIds;
    }

    @Override
    public String toString() {
        return "StagedProjectInfo{" +
                "project='" + project + '\'' +
                ", releaseVersion='" + releaseVersion + '\'' +
                ", repoIds=" + repoIds +
                '}';
    }

    public String getProject() {
        return project;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public List<String> getRepoIds() {
        return repoIds;
    }
}
