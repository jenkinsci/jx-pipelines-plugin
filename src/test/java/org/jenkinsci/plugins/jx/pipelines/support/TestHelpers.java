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
package org.jenkinsci.plugins.jx.pipelines.support;

import io.fabric8.utils.IOHelpers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
public class TestHelpers {
    public static File getBasedir() {
        String basedirName = System.getProperty("basedir", ".");
        return new File(basedirName);
    }

    /**
     * Returns the test resource as a string using a folder based on the given class name
     */
    public static String getTestResource(Class<?> clazz, String name) {
        URL resource = null;
        try {
            resource = clazz.getResource(clazz.getSimpleName() + "/" + name);
            assertTrue("Could not find test resource for " + clazz.getName() + "/" + name, resource != null);
            InputStream is = null;
            is = resource.openStream();
            assertTrue("Could not find test resource for " + resource, is != null);
            return IOHelpers.readFully(is);
        } catch (IOException e) {
            fail("Failed to load " + resource);
            return null;
        }
    }
}
