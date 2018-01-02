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
package org.jenkinsci.plugins.jx.pipelines.helpers;

import io.fabric8.utils.Strings;

import java.io.Serializable;

/**
 */
public class GitRepositoryInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String host;
    private final String organisation;
    private final String name;

    public GitRepositoryInfo(String host, String organisation, String name) {
        this.host = host;
        this.organisation = organisation;
        this.name = name;
    }

    @Override
    public String toString() {
        return "GitRepoDetails{" +
                "host='" + host + '\'' +
                ", organisation='" + organisation + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public String getHost() {
        return host;
    }

    public String getOrganisation() {
        return organisation;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the organisation and repository name separated by / in the form <code>organisation/name</code>
     */
    public String getProject() {
        if (Strings.notEmpty(organisation)) {
            if (Strings.notEmpty(name)) {
                return organisation + "/" + name;
            } else {
                return organisation;
            }
        }
        return name;
    }
}
