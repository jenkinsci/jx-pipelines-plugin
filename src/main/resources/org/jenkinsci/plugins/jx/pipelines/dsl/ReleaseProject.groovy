package org.jenkinsci.plugins.jx.pipelines.dsl

import io.fabric8.utils.Strings
import org.apache.maven.model.Model
import org.jenkinsci.plugins.jx.pipelines.arguments.PromoteArtifactsArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.PromoteImagesArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.ReleaseProjectArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.TagImagesArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.WaitUntilArtifactSyncedArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.WaitUntilPullRequestMergedArguments
import org.jenkinsci.plugins.jx.pipelines.model.ServiceConstants
import org.jenkinsci.plugins.workflow.cps.CpsScript

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
      if (Strings.isNullOrBlank(arguments.groupId)) {
        arguments.groupId = mavenProject.groupId
      }
      if (Strings.isNullOrBlank(arguments.artifactId)) {
        arguments.artifactId = mavenProject.artifactId
      }
      if (Strings.isNullOrBlank(arguments.extension)) {
        arguments.extension = "pom";
      }
      if (Strings.isNullOrBlank(arguments.repositoryUrl)) {
        arguments.repositoryUrl = ServiceConstants.MAVEN_CENTRAL
      }
    }
  }
}

