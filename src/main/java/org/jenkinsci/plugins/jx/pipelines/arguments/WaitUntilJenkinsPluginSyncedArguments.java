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
import org.jenkinsci.plugins.jx.pipelines.model.ServiceConstants;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 */
public class WaitUntilJenkinsPluginSyncedArguments implements Serializable {
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

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
