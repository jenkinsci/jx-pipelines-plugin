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
package org.jenkinsci.plugins.jx.pipelines.helpers;

import org.jenkinsci.plugins.jx.pipelines.arguments.JXPipelinesArguments;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.jenkinsci.plugins.structs.describable.DescribableParameter;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class ConfigHelper {
    /**
     * Populates any suitable properties in the given configuration on the bean
     */
    @Whitelisted
    public static <T,A extends JXPipelinesArguments> A populateBeanFromConfiguration(@Nonnull Class<A> klazz, Map<String, T> arguments) {
        if (arguments != null) {
            try {
                return new DescribableModel<>(klazz).instantiate(arguments);
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not populate argument for " + klazz.getName() + " from arguments " + arguments + ": " + e, e);
            }
        }
        return null;
    }

    /**
     * Gets a map of field names of an argument and their types.
     */
    @Whitelisted
    public static Map<String,Class> getArgumentFields(@Nonnull Class<? extends JXPipelinesArguments> klazz) {
        Map<String, Class> fieldsAndTypes = new HashMap<>();

        DescribableModel<? extends JXPipelinesArguments> m = new DescribableModel<>(klazz);

        for (DescribableParameter p : m.getParameters()) {
            fieldsAndTypes.put(p.getName(), p.getErasedType());
        }

        return fieldsAndTypes;
    }
}
