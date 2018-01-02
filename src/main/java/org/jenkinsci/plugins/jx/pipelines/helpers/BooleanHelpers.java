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
package org.jenkinsci.plugins.jx.pipelines.helpers;

/**
 */
public class BooleanHelpers {
    /**
     * Returns the given value as a boolean expression
     */
    public static boolean asBoolean(Object object) {
        if (object instanceof Boolean) {
            Boolean b = (Boolean) object;
            return b.booleanValue();
        } else if (object instanceof String) {
            String s = (String) object;
            return s.equalsIgnoreCase("true");
        } else if (object instanceof Number) {
            Number number = (Number) object;
            return number.intValue() != 0;
        } else {
            return object != null;
        }
    }
}
