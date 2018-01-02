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
package org.jenkinsci.plugins.jx.pipelines;

import org.jenkinsci.plugins.jx.pipelines.helpers.StringHelpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.jenkinsci.plugins.jx.pipelines.helpers.StringHelpers.addToStringProperty;

/**
 * Represents an extension point in a psuedo step so that you can override a step completely, disable it
 * or add pre/post blocks
 */
public class StepExtension implements Serializable {
    private static final long serialVersionUID = 1L;

    private Object stepsBlock;
    private Object preBlock;
    private Object postBlock;
    private Boolean disabled;

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();
        addToStringProperty(list, "stepsBlock", stepsBlock);
        addToStringProperty(list, "preBlock", preBlock);
        addToStringProperty(list, "postBlock", postBlock);
        addToStringProperty(list, "disabled", disabled);
        return "StepExtension{" + String.join(", ", list) + "}";
    }

    public StepExtension steps(Object stepsBlock) {
        System.out.println("Setting stepsBlock " + stepsBlock + " on StepExtension");
        this.stepsBlock = stepsBlock;
        return this;
    }

    public StepExtension pre(Object preBlock) {
        this.preBlock = preBlock;
        return this;
    }

    public StepExtension post(Object postBlock) {
        this.postBlock = postBlock;
        return this;
    }

    public boolean isDisabled() {
        return disabled != null && disabled.booleanValue();
    }

    public Object getStepsBlock() {
        return stepsBlock;
    }

    public void setStepsBlock(Object stepsBlock) {
        this.stepsBlock = stepsBlock;
    }

    public Object getPreBlock() {
        return preBlock;
    }

    public void setPreBlock(Object preBlock) {
        this.preBlock = preBlock;
    }

    public Object getPostBlock() {
        return postBlock;
    }

    public void setPostBlock(Object postBlock) {
        this.postBlock = postBlock;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }
}
