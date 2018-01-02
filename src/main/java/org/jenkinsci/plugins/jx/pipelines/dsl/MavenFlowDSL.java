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
public class MavenFlowDSL extends PipelineDSLGlobal {

    @Override
    public String getFunctionName() {
        return "mavenFlow";
    }

    @Extension
    public static class MiscWhitelist extends ProxyWhitelist {
        public MiscWhitelist() throws IOException {
            super(createStaticWhitelist(), new JXPipelinesWhitelist());

/*            super(new StaticWhitelist(
                    "new org.jenkinsci.plugins.fabric8.Fabric8Commands",
                    "new org.jenkinsci.plugins.fabric8.Utils",

                    // arguments
                    "new org.jenkinsci.plugins.fabric8.steps.MavenFlow$Arguments",
                    "method org.jenkinsci.plugins.fabric8.steps.MavenFlow$Arguments setGitCloneUrl java.lang.String",
                    "method org.jenkinsci.plugins.fabric8.steps.MavenFlow$Arguments setPauseOnFailure boolean",
                    "method org.jenkinsci.plugins.fabric8.steps.MavenFlow$Arguments setPauseOnSuccess boolean",
                    "method org.jenkinsci.plugins.fabric8.steps.MavenFlow$Arguments setCdOrganisation java.lang.String",
                    "method org.jenkinsci.plugins.fabric8.steps.MavenFlow$Arguments setCdBranches java.util.List",
                    "method org.jenkinsci.plugins.fabric8.steps.MavenFlow$Arguments *",

                    "staticMethod org.jenkinsci.plugins.fabric8.steps.MavenFlow$Arguments newInstance java.util.Map",

                    // for exposing sh()
                    "method groovy.lang.GroovyObject invokeMethod java.lang.String java.lang.Object",

                    // for println
                    "staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods println java.lang.Object java.lang.Object",
                    
                    "method org.jenkinsci.plugins.fabric8.CommandSupport updateEnvironment java.lang.Object",
                    "method org.jenkinsci.plugins.fabric8.CommandSupport setEnv java.util.Map",
                    "method org.jenkinsci.plugins.fabric8.CommandSupport setCurrentPath java.lang.String",
                    "method org.jenkinsci.plugins.fabric8.CommandSupport setFileReadFacade org.jenkinsci.plugins.fabric8.FileReadFacade",
                    "method org.jenkinsci.plugins.fabric8.CommandSupport setShellFacade org.jenkinsci.plugins.fabric8.ShellFacade",
                    "method org.jenkinsci.plugins.fabric8.CommandSupport updateSh java.lang.Object",
                    "method org.jenkinsci.plugins.fabric8.CommandSupport *",
                    "method org.jenkinsci.plugins.fabric8.Utils setBranch java.lang.String",
                    "method org.jenkinsci.plugins.fabric8.Utils *",

                    // finding git url
                    "new java.io.File java.lang.String",
                    "new java.io.File java.io.File java.lang.String",
                    "method java.io.File getAbsolutePath",

                    "staticMethod org.jenkinsci.plugins.fabric8.helpers.GitHelper extractGitUrl java.lang.String",
                    "staticMethod org.jenkinsci.plugins.fabric8.helpers.GitHelper parseGitRepositoryInfo java.lang.String",
                    "method org.jenkinsci.plugins.fabric8.helpers.GitRepositoryInfo *",
                    "method org.jenkinsci.plugins.fabric8.helpers.GitRepositoryInfo * *",

                    // string utils
                    "staticMethod io.fabric8.utils.Strings isNotBlank java.lang.String",
                    "staticMethod io.fabric8.utils.Strings isNullOrBlank java.lang.String",
                    "staticMethod io.fabric8.utils.Strings notEmpty java.lang.String",

                    "method java.util.Map$Entry getKey",
                    "method java.util.Map$Entry getValue"
            ));*/
        }
    }

}
