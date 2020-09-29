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

import groovy.lang.Closure;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jenkinsci.plugins.jx.pipelines.helpers.StringHelpers.addToStringProperty;

/**
 * Represents an extension point in a psuedo step so that you can override a step completely, disable it
 * or add pre/post blocks
 */
public abstract class StepContainer<T extends StepContainer<T>> extends JXPipelinesArguments<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final List<String> closureNames = Arrays.asList("post", "pre", "steps");

    private Closure steps;
    private Closure pre;
    private Closure post;
    private Boolean disabled;

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();
        addToStringProperty(list, "steps", steps);
        addToStringProperty(list, "pre", pre);
        addToStringProperty(list, "post", post);
        addToStringProperty(list, "disabled", disabled);
        return String.join(", ", list);
    }

    @Whitelisted
    public StepContainer steps(Closure steps) {
        System.out.println("Setting stepsBlock " + steps + " on StepExtension");
        this.steps = steps;
        return this;
    }

    @Whitelisted
    public StepContainer pre(Closure pre) {
        this.pre = pre;
        return this;
    }

    @Whitelisted
    public StepContainer post(Closure post) {
        this.post = post;
        return this;
    }

    public boolean isDisabled() {
        return disabled != null && disabled.booleanValue();
    }

    public Closure getSteps() {
        return steps;
    }

    @DataBoundSetter
    public void setSteps(Closure steps) {
        this.steps = steps;
    }

    public Closure getPre() {
        return pre;
    }

    @DataBoundSetter
    public void setPre(Closure pre) {
        this.pre = pre;
    }

    public Closure getPost() {
        return post;
    }

    @DataBoundSetter
    public void setPost(Closure post) {
        this.post = post;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    @DataBoundSetter
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public void copyFrom(StepContainer s) {
        this.disabled = s.getDisabled();
        this.post = s.getPost();
        this.pre = s.getPre();
        this.steps = s.getSteps();
    }
}
