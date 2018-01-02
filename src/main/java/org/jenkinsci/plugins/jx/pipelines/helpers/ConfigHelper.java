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

import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 */
public class ConfigHelper {
    /**
     * Returns all the property names
     */
    public static Set<String> propertyNames(Object bean) {
        SortedSet<String> answer = new TreeSet<>();

        PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(bean);
        if (propertyDescriptors != null) {
            for (PropertyDescriptor descriptor : propertyDescriptors) {
                if (descriptor.getWriteMethod() != null) {
                    answer.add(descriptor.getName());
                }
            }
        }
        return answer;
    }

    /**
     * Populates any suitable properties in the given configuration on the bean
     */
    public static <T> void populateBeanFromConfiguration(Object bean, Map<String, T> arguments) {
        if (arguments != null) {
            Set<Map.Entry<String, T>> entries = arguments.entrySet();
            for (Map.Entry<String, T> entry : entries) {
                String name = entry.getKey();
                Object value = entry.getValue();
                try {
                    setBeanProperty(bean, name, value);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Could not set property " + name + " on bean " + bean + " to value " + value + " due to: " + e, e);
                }
            }
        }

    }

    protected static void setBeanProperty(Object object, String name, Object value) {
        if (PropertyUtils.isWriteable(object, name)) {
            try {
                PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(object, name);
                Class<?> propertyType = descriptor.getPropertyType();

                // lets do some custom type conversions
                if (propertyType.equals(boolean.class) || propertyType.equals(Boolean.class)) {
                    if (!(value instanceof Boolean)) {
                        value = BooleanHelpers.asBoolean(value);
                    }
                }
                PropertyUtils.setProperty(object, name, value);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to set property " + name + " on function object " + object + " due to: " + e, e);
            }
        }
    }
}
