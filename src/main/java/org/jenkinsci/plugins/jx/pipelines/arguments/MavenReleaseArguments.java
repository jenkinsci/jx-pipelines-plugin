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
public class MavenReleaseArguments extends JXPipelinesArguments<MavenReleaseArguments> {
    private static final long serialVersionUID = 1L;

    @Argument
    private boolean skipTests;
    @Argument
    private String version = "";
    @Argument
    private boolean enableArchiveTestResults = true;

    @Argument
    private String analyticsServiceName = "bayesian-link";
    @Argument
    private boolean enableAnalyticsScan = true;

    @Argument
    private String sonarQubeServiceName = "sonarqube";
    @Argument
    private int sonarQubePort = 9000;
    @Argument
    private String sonarQubeScannerVersion = "2.8";
    @Argument
    private boolean enableSonarQubeScan = true;

    @Argument
    private String contentRepositoryServiceName = "content-repository";
    @Argument
    private boolean enableContentRepositorySiteReport = true;

    @Argument
    private String pomFileName = "";

    @DataBoundConstructor
    public MavenReleaseArguments() {

    }

    // Adapters
    //-------------------------------------------------------------------------

    public JUnitResultsArguments createJUnitArguments() {
        return new JUnitResultsArguments(enableArchiveTestResults);
    }

    public BayesianScannerArguments createBayesianScannerArguments() {
        return new BayesianScannerArguments(analyticsServiceName, enableAnalyticsScan);
    }

    public SonarQubeScannerArguments createSonarQubeArguments() {
        return new SonarQubeScannerArguments(enableSonarQubeScan, sonarQubeScannerVersion, sonarQubeServiceName, sonarQubePort);
    }

    public ContentRepositoryArguments createContentRepositoryArguments() {
        return new ContentRepositoryArguments(enableContentRepositorySiteReport, contentRepositoryServiceName);
    }

/*
    public ReadMavenPom.Arguments createReadMavenPomArguments() {
        return new ReadMavenPom.Arguments(pomFileName);
    }
*/


    // Properties
    //-------------------------------------------------------------------------

    public boolean isSkipTests() {
        return skipTests;
    }

    @DataBoundSetter
    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isEnableArchiveTestResults() {
        return enableArchiveTestResults;
    }

    @DataBoundSetter
    public void setEnableArchiveTestResults(boolean enableArchiveTestResults) {
        this.enableArchiveTestResults = enableArchiveTestResults;
    }

    public String getAnalyticsServiceName() {
        return analyticsServiceName;
    }

    @DataBoundSetter
    public void setAnalyticsServiceName(String analyticsServiceName) {
        this.analyticsServiceName = analyticsServiceName;
    }

    public boolean isEnableAnalyticsScan() {
        return enableAnalyticsScan;
    }

    @DataBoundSetter
    public void setEnableAnalyticsScan(boolean enableAnalyticsScan) {
        this.enableAnalyticsScan = enableAnalyticsScan;
    }

    public String getSonarQubeServiceName() {
        return sonarQubeServiceName;
    }

    @DataBoundSetter
    public void setSonarQubeServiceName(String sonarQubeServiceName) {
        this.sonarQubeServiceName = sonarQubeServiceName;
    }

    public int getSonarQubePort() {
        return sonarQubePort;
    }

    @DataBoundSetter
    public void setSonarQubePort(int sonarQubePort) {
        this.sonarQubePort = sonarQubePort;
    }

    public String getSonarQubeScannerVersion() {
        return sonarQubeScannerVersion;
    }

    @DataBoundSetter
    public void setSonarQubeScannerVersion(String sonarQubeScannerVersion) {
        this.sonarQubeScannerVersion = sonarQubeScannerVersion;
    }

    public boolean isEnableSonarQubeScan() {
        return enableSonarQubeScan;
    }

    @DataBoundSetter
    public void setEnableSonarQubeScan(boolean enableSonarQubeScan) {
        this.enableSonarQubeScan = enableSonarQubeScan;
    }

    public String getContentRepositoryServiceName() {
        return contentRepositoryServiceName;
    }

    @DataBoundSetter
    public void setContentRepositoryServiceName(String contentRepositoryServiceName) {
        this.contentRepositoryServiceName = contentRepositoryServiceName;
    }

    public boolean isEnableContentRepositorySiteReport() {
        return enableContentRepositorySiteReport;
    }

    @DataBoundSetter
    public void setEnableContentRepositorySiteReport(boolean enableContentRepositorySiteReport) {
        this.enableContentRepositorySiteReport = enableContentRepositorySiteReport;
    }

    public String getPomFileName() {
        return pomFileName;
    }

    @DataBoundSetter
    public void setPomFileName(String pomFileName) {
        this.pomFileName = pomFileName;
    }

    @Extension @Symbol("mavenRelease")
    public static class DescriptorImpl extends JXPipelinesArgumentsDescriptor<MavenReleaseArguments> {

    }
}
