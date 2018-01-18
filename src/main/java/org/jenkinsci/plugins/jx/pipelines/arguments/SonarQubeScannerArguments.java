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

import hudson.Extension;
import io.jenkins.functions.Argument;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 */
public class SonarQubeScannerArguments extends JXPipelinesArguments<SonarQubeScannerArguments> {
    private static final long serialVersionUID = 1L;

    @Argument
    private String serviceName = "sonarqube";
    @Argument
    private int servicePort = 9000;
    @Argument
    private String scannerVersion = "2.8";
    @Argument
    private boolean runSonarScanner = true;

    @DataBoundConstructor
    public SonarQubeScannerArguments() {
    }

    public SonarQubeScannerArguments(boolean runSonarScanner, String scannerVersion, String serviceName, int servicePort) {
        this.serviceName = serviceName;
        this.servicePort = servicePort;
        this.scannerVersion = scannerVersion;
        this.runSonarScanner = runSonarScanner;
    }

    public String getServiceName() {
        return serviceName;
    }

    @DataBoundSetter
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getServicePort() {
        return servicePort;
    }

    @DataBoundSetter
    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    public boolean isRunSonarScanner() {
        return runSonarScanner;
    }

    @DataBoundSetter
    public void setRunSonarScanner(boolean runSonarScanner) {
        this.runSonarScanner = runSonarScanner;
    }

    public String getScannerVersion() {
        return scannerVersion;
    }

    @DataBoundSetter
    public void setScannerVersion(String scannerVersion) {
        this.scannerVersion = scannerVersion;
    }

    @Extension @Symbol("sonarQubeScanner")
    public static class DescriptorImpl extends JXPipelinesArgumentsDescriptor<SonarQubeScannerArguments> {

    }
}
