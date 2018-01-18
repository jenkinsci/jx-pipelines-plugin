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
import hudson.Extension;
import hudson.ExtensionList;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.jx.pipelines.arguments.JXPipelinesArgumentsDescriptor;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStep;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.CpsThread;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class PipelineDSLGlobal extends GlobalVariable {

    protected static Whitelist createStaticWhitelist(String... lines) throws IOException {
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(
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

    @Extension
    public static class MiscWhitelist extends ProxyWhitelist {
        public MiscWhitelist() throws IOException {
            super(createStaticWhitelist(), new JXPipelinesWhitelist());
        }
    }


    public abstract String getFunctionName();

    @CheckForNull
    public String validate(ModelASTStep step) {
        // No-op by default.
        return null;
    }

    @CheckForNull
    public JXPipelinesArgumentsDescriptor getArgumentsType() {
        return null;
    }

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

        return loadFunction(script, binding, getFunctionName());
    }

    private Object loadFunction(CpsScript script, Binding binding, String functionName) throws Exception {
        script.getClass().getClassLoader().loadClass("org.jenkinsci.plugins.jx.pipelines.dsl.CommonFunctions");
        script.getClass().getClassLoader().loadClass("org.jenkinsci.plugins.jx.pipelines.dsl.BodyAssigner");
        Object pipelineDSL = script.getClass()
                .getClassLoader()
                .loadClass("org.jenkinsci.plugins.jx.pipelines.dsl." + StringUtils.capitalize(functionName))
                .getConstructor(CpsScript.class)
                .newInstance(script);
        binding.setVariable(functionName, pipelineDSL);
        return pipelineDSL;
    }

    @CheckForNull
    public static PipelineDSLGlobal getGlobalForName(@Nonnull String name) {
        for (PipelineDSLGlobal g : ExtensionList.lookup(PipelineDSLGlobal.class)) {
            if (name.equals(g.getFunctionName())) {
                return g;
            }
        }

        return null;
    }
}
