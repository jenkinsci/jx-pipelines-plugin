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
package io.jenkins.functions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate fields which to be injected with function arguments
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface Argument {
    /**
     * Returns the short name of the argument which is usually similar to an identifier (starts with a lowercase and no spaces or special characters)
     */
    String name() default "";

    /**
     * Returns the display name of the argument for rendering in UIs such as the Blue Ocean editor
     */
    String displayName() default "";

    /**
     * Returns the description for help or tooltip
     */
    String description() default "";

    /**
     * Returns The default value of this argument if none is specified
     */
    String defaultValue() default "";

}
