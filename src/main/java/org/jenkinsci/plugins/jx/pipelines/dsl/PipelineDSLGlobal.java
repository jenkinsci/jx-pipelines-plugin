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

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.CpsThread;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class PipelineDSLGlobal extends GlobalVariable {

    protected static Whitelist createStaticWhitelist(String... lines) throws IOException {
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(
                // for exposing sh()
                "method groovy.lang.GroovyObject invokeMethod java.lang.String java.lang.Object",

                // for nested steps
                "staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods invokeMethod java.lang.Object java.lang.String java.lang.Object",
                "method java.util.Map remove java.lang.Object",
                "method java.lang.Class isInstance java.lang.Object",
                
                "staticMethod org.apache.commons.beanutils.PropertyUtils getPropertyDescriptors java.lang.Object",
                "staticMethod org.apache.commons.beanutils.PropertyUtils getProperty java.lang.Object java.lang.String",
                "staticMethod org.apache.commons.beanutils.PropertyUtils setProperty java.lang.Object java.lang.String java.lang.Object",
                "method java.beans.FeatureDescriptor getName",
                "method java.beans.PropertyDescriptor getWriteMethod",
                "method java.beans.PropertyDescriptor getPropertyType",
                "method java.lang.Class isAssignableFrom java.lang.Class",

                // for println
                "staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods println java.lang.Object java.lang.Object",

                "method java.lang.Exception printStackTrace",
                "method java.lang.Throwable printStackTrace",

                // finding git url
                "new java.io.File java.lang.String",
                "new java.io.File java.io.File java.lang.String",
                "method java.io.File getAbsolutePath",

                "staticMethod org.jenkinsci.plugins.jx.pipelines.helpers.GitHelper extractGitUrl java.lang.String",
                "staticMethod org.jenkinsci.plugins.jx.pipelines.helpers.GitHelper parseGitRepositoryInfo java.lang.String",
                "method org.jenkinsci.plugins.jx.pipelines.helpers.GitRepositoryInfo *",
                "method org.jenkinsci.plugins.jx.pipelines.helpers.GitRepositoryInfo * *",

                // boolean utils
                "method java.lang.Boolean booleanValue",

                // string utils
                "staticMethod io.fabric8.utils.Strings isNotBlank java.lang.String",
                "staticMethod io.fabric8.utils.Strings isNullOrBlank java.lang.String",
                "staticMethod io.fabric8.utils.Strings notEmpty java.lang.String",

                "method java.util.Map$Entry getKey",
                "method java.util.Map$Entry getValue"
        ));

        if (lines != null) {
            list.addAll(Arrays.asList(lines));
        }
        return new StaticWhitelist(list);
    }

    public abstract String getFunctionName();

    @Override
    public String getName() {
        return getFunctionName();
    }

    @Override
    public Object getValue(CpsScript script) throws Exception {
        Binding binding = script.getBinding();

        CpsThread c = CpsThread.current();
        if (c == null) {
            throw new IllegalStateException("Expected to be called from CpsThread");
        }

        ClassLoader cl = getClass().getClassLoader();

        return loadFunction(binding, c, cl, getFunctionName());
    }

    private Object loadFunction(Binding binding, CpsThread c, ClassLoader cl, String functionName) throws IOException, InstantiationException, IllegalAccessException {
        String fileName = functionName + ".groovy";
        String scriptPath = "dsl/" + fileName;
        try (Reader r = new InputStreamReader(cl.getResourceAsStream(scriptPath), "UTF-8")) {
            GroovyCodeSource gsc = new GroovyCodeSource(r, fileName, cl.getResource(scriptPath).getFile());
            gsc.setCachable(true);

            Object pipelineDSL = c.getExecution()
                    .getShell()
                    .getClassLoader()
                    .parseClass(gsc)
                    .newInstance();
            binding.setVariable(functionName, pipelineDSL);
            return pipelineDSL;
        }
    }
}
