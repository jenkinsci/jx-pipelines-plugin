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
package org.jenkinsci.plugins.jx.pipelines.dsl;

import hudson.Extension;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;

import java.io.IOException;

@Extension
public class WaitUntilJenkinsPluginSyncedDSL extends PipelineDSLGlobal {

    @Override
    public String getFunctionName() {
        return "waitUntilJenkinsPluginSynced";
    }

    @Extension
    public static class MiscWhitelist extends ProxyWhitelist {
        public MiscWhitelist() throws IOException {
            super(createStaticWhitelist(), new JXPipelinesWhitelist());
        }
    }

}
