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

import io.fabric8.utils.Strings;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

/**
 */
public class MavenHelpers {
    @Whitelisted
    @CheckForNull
    public static Model loadMavenPom(String pomFileContent) throws IOException, XmlPullParserException {
        if (Strings.notEmpty(pomFileContent)) {
            return new MavenXpp3Reader().read(new StringReader(pomFileContent));
        } else {
            return null;
        }
    }

    @Whitelisted
    @CheckForNull
    public static String getProjectVersion(String pomFileContent) throws IOException, XmlPullParserException {
        Model model = loadMavenPom(pomFileContent);
        if (model != null) {
            return model.getVersion();
        } else {
            return null;
        }
    }

    /**
     * Returns the maven profile CLI argument for the given configured list of profile names or uses the given default profiles
     * if no profiles are explicitly configured
     */
    public static String mavenProfileCliArgument(List<String> configuredProfiles, String... defaultProfiles) {
        if (configuredProfiles == null || configuredProfiles.isEmpty()) {
            configuredProfiles = Arrays.asList(defaultProfiles);
        }
        if (configuredProfiles.isEmpty()) {
            return "";
        }
        return " -P " + String.join(",", configuredProfiles);
    }
}
