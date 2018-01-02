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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class GitExtractGitCloneURLTest {

    @Test
    public void testGitRepoUrl() throws Exception {
        String configFileText = "[core]\n" +
                "\trepositoryformatversion = 0\n" +
                "\tfilemode = true\n" +
                "\tbare = false\n" +
                "\tlogallrefupdates = true\n" +
                "[remote \"origin\"]\n" +
                "\turl = https://github.com/jstrachan/test-maven-flow-project.git\n" +
                "\tfetch = +refs/heads/*:refs/remotes/origin/*";
        String url = GitHelper.extractGitUrl(configFileText);
        System.out.println("Found git clone URL: " + url);
        assertThat(url).describedAs("extractGitUrl").isNotEmpty();
    }

}
