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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jenkinsci.plugins.jx.pipelines.helpers.BooleanHelpers.asBoolean;

/**
 */
public class MapHelpers {
    public static String getString(Map map, String key) {
        return getString(map, key, "");
    }

    public static String getString(Map map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }

    public static <T> List<T> getList(Map map, String key) {
        return getList(map, key, new ArrayList<>());
    }

    public static <T> List<T> getList(Map map, String key, List<T> defaultValues) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<T>) value;
        }
        return defaultValues;
    }

    public static boolean getBool(Map map, String key) {
        return getBool(map, key, false);
    }

    public static boolean getBool(Map map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value != null) {
            return asBoolean(value);
        }
        return defaultValue;
    }
}
