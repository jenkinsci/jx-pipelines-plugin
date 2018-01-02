package dsl

import org.jenkinsci.plugins.jx.pipelines.arguments.WaitUntilArtifactSyncedArguments
import org.jenkinsci.plugins.jx.pipelines.model.ServiceConstants

def call(WaitUntilArtifactSyncedArguments config) {
  def flow = new CommonFunctions()

  // mandatory properties
  def groupId = config.groupId
  def artifactId = config.artifactId
  def version = config.version

  def repo = config.repositoryUrl ?: ServiceConstants.MAVEN_CENTRAL
  def ext = config.extension ?: 'jar'

  return flow.doStepExecution(config.stepExtension) {
    if (groupId && artifactId && version) {
      echo "waiting for artifact ${groupId}/${artifactId}/${version}/${ext} to be in repo ${repo}"

      waitUntil {
        retry(3) {
          flow.isArtifactAvailableInRepo(repo, groupId.replaceAll('\\.', '/'), artifactId, version, ext)
        }
      }

      flow.sendChat "${config.artifactId} ${config.version} released and available in maven central"
    } else {
      echo "required properties missing groupId: ${groupId}, artifactId: ${artifactId}, version: ${version}"
    }
  }
}
