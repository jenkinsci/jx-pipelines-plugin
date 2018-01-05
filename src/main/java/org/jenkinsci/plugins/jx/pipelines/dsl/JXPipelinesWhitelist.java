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
package org.jenkinsci.plugins.jx.pipelines.dsl;

import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 */
public class JXPipelinesWhitelist extends Whitelist {
    @Override
    public boolean permitsMethod(@Nonnull Method method, @Nonnull Object o, @Nonnull Object[] objects) {
        return (method.getName().equals("invokeMethod") ||
                method.getName().equals("setProperty") ||
                method.getName().equals("getProperty")) &&
                // TODO: Get rid of MethodMissingWrapper and the closure translation stuff, probably similar to in Declarative
                MethodMissingWrapper.class.isAssignableFrom(o.getClass());
    }

    @Override
    public boolean permitsConstructor(@Nonnull Constructor<?> constructor, @Nonnull Object[] objects) {
        return false;
    }

    @Override
    public boolean permitsStaticMethod(@Nonnull Method method, @Nonnull Object[] objects) {
        return false;
    }

    @Override
    public boolean permitsFieldGet(@Nonnull Field field, @Nonnull Object o) {
        return false;
    }

    @Override
    public boolean permitsFieldSet(@Nonnull Field field, @Nonnull Object o, @CheckForNull Object o1) {
        return false;
    }

    @Override
    public boolean permitsStaticFieldGet(@Nonnull Field field) {
        return false;
    }

    @Override
    public boolean permitsStaticFieldSet(@Nonnull Field field, @CheckForNull Object o) {
        return false;
    }
}
