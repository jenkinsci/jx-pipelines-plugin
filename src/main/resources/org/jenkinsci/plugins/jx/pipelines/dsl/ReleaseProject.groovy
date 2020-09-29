package org.jenkinsci.plugins.jx.pipelines.dsl

import hudson.Util
import org.apache.maven.model.Model
import org.jenkinsci.plugins.jx.pipelines.arguments.PromoteArtifactsArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.PromoteImagesArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.ReleaseProjectArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.TagImagesArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.WaitUntilArtifactSyncedArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.WaitUntilPullRequestMergedArguments
import org.jenkinsci.plugins.jx.pipelines.model.ServiceConstants
import org.jenkinsci.plugins.workflow.cps.CpsScript

import static org.jenkinsci.plugins.jx.pipelines.dsl.JXDSLUtils.echo

class ReleaseProject {
  private CpsScript script

  ReleaseProject(CpsScript script) {
    this.script = script
  }

  def call(ReleaseProjectArguments arguments) {
    echo "releaseProject ${arguments}"

    def flow = new CommonFunctions(script)

    PromoteArtifactsArguments promoteArtifactsArguments = arguments.createPromoteArtifactsArguments()
    String pullRequestId = script.promoteArtifacts(promoteArtifactsArguments)

    PromoteImagesArguments promoteImagesArguments = arguments.createPromoteImagesArguments()
    def promoteDockerImages = promoteImagesArguments.images
    if (promoteDockerImages.size() > 0) {
      def validation = promoteImagesArguments.validate()
      if (validation != null) {
        script.error validation
      } else {
        script.promoteImages(promoteImagesArguments)
      }
    }

    TagImagesArguments tagImagesArguments = arguments.createTagImagesArguments()
    if (tagImagesArguments) {
      def tagDockerImages = tagImagesArguments.images
      if (tagDockerImages && tagDockerImages.size() > 0) {
        script.tagImages(tagImagesArguments)
      }
    }

    if (pullRequestId != null) {
      WaitUntilPullRequestMergedArguments waitUntilPullRequestMergedArguments = arguments.createWaitUntilPullRequestMergedArguments(pullRequestId)
      script.waitUntilPullRequestMerged(waitUntilPullRequestMergedArguments)
    }

    WaitUntilArtifactSyncedArguments waitUntilArtifactSyncedWithCentralArguments = arguments.createWaitUntilArtifactSyncedWithCentralArguments()
    Model mavenProject = flow.loadMavenPom()
    defaultWaitInfoFromPom(waitUntilArtifactSyncedWithCentralArguments, mavenProject)

    if (waitUntilArtifactSyncedWithCentralArguments.isValid()) {
      script.waitUntilArtifactSyncedWithCentral(waitUntilArtifactSyncedWithCentralArguments)
    }
  }

  /**
   * If no properties are configured explicitly lets try default them from the pom.xml
   */
  def defaultWaitInfoFromPom(WaitUntilArtifactSyncedArguments arguments, Model mavenProject) {
    if (mavenProject != null) {
      if (Util.fixEmptyAndTrim(arguments.groupId) == null) {
        arguments.groupId = mavenProject.groupId
      }
      if (Util.fixEmptyAndTrim(arguments.artifactId) == null) {
        arguments.artifactId = mavenProject.artifactId
      }
      if (Util.fixEmptyAndTrim(arguments.extension) == null) {
        arguments.extension = "pom";
      }
      if (Util.fixEmptyAndTrim(arguments.repositoryUrl) == null) {
        arguments.repositoryUrl = ServiceConstants.MAVEN_CENTRAL
      }
    }
  }
}

