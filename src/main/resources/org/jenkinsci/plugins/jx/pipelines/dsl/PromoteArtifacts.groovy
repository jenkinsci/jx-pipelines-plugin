package org.jenkinsci.plugins.jx.pipelines.dsl

import org.jenkinsci.plugins.jx.pipelines.arguments.PromoteArtifactsArguments
import org.jenkinsci.plugins.workflow.cps.CpsScript

import static org.jenkinsci.plugins.jx.pipelines.dsl.JXDSLUtils.echo

class PromoteArtifacts {
  private CpsScript script

  PromoteArtifacts(CpsScript script) {
    this.script = script
  }

  def call(PromoteArtifactsArguments config) {
    def flow = new CommonFunctions(script)
    def name = config.project
    def version = config.version
    def repoIds = config.repoIds
    def containerName = config.containerName

    return flow.doStepExecution(config.stepExtension) {
      if (repoIds && repoIds.size() > 0) {
        script.container(name: containerName) {
/*
          script.sh 'chmod 600 /root/.ssh-git/ssh-key'
          script.sh 'chmod 600 /root/.ssh-git/ssh-key.pub'
          script.sh 'chmod 700 /root/.ssh-git'
*/

          echo "About to release ${name} repo ids ${repoIds}"
          for (int j = 0; j < repoIds.size(); j++) {
            flow.releaseSonartypeRepo(repoIds[j])
          }

          if (config.helmPush) {
            flow.helm()
          }

          if (version && config.updateNextDevelopmentVersion) {
            flow.updateNextDevelopmentVersion(version, config.updateNextDevelopmentVersionArguments ?: "")
            return flow.createPullRequest("[CD] Release ${version}", "${config.project}", "release-v${version}")
          }
        }
      }
    }
  }
}