package org.jenkinsci.plugins.jx.pipelines.dsl

import org.jenkinsci.plugins.jx.pipelines.arguments.WaitUntilArtifactSyncedArguments
import org.jenkinsci.plugins.jx.pipelines.model.ServiceConstants
import org.jenkinsci.plugins.workflow.cps.CpsScript

class WaitUntilArtifactSyncedWithCentral {
  private CpsScript script

  WaitUntilArtifactSyncedWithCentral(CpsScript script) {
    this.script = script
  }

  def call(WaitUntilArtifactSyncedArguments config) {
    def flow = new CommonFunctions(script)

    // mandatory properties
    def groupId = config.groupId
    def artifactId = config.artifactId
    def version = config.version

    def repo = config.repositoryUrl ?: ServiceConstants.MAVEN_CENTRAL
    def ext = config.extension ?: 'jar'

    return flow.doStepExecution(config.stepExtension) {
      if (groupId && artifactId && version) {
        echo "waiting for artifact ${groupId}/${artifactId}/${version}/${ext} to be in repo ${repo}"

        script.waitUntil {
          script.retry(3) {
            JXDSLUtils.isArtifactAvailableInRepo(repo, groupId.replaceAll('\\.', '/'), artifactId, version, ext)
          }
        }

        flow.sendChat "${config.artifactId} ${config.version} released and available in maven central"
      } else {
        echo "required properties missing groupId: ${groupId}, artifactId: ${artifactId}, version: ${version}"
      }
    }
  }
}