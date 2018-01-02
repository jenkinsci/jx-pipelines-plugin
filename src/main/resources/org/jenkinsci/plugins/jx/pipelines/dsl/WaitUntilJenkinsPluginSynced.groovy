package org.jenkinsci.plugins.jx.pipelines.dsl

import org.jenkinsci.plugins.jx.pipelines.arguments.WaitUntilJenkinsPluginSyncedArguments
import org.jenkinsci.plugins.workflow.cps.CpsScript

class WaitUntilJenkinsPluginSynced {
  private CpsScript script

  WaitUntilJenkinsPluginSynced(CpsScript script) {
    this.script = script
  }

  def call(WaitUntilJenkinsPluginSyncedArguments config) {
    def flow = new CommonFunctions(script)

    def repo = config.repo
    if (!repo) {
      repo = "http://archives.jenkins-ci.org/"
    }
    def name = config.name
    def path = "plugins/" + name
    def artifact = "${name}.hpi"

    script.waitUntil {
      script.retry(3) {
        flow.isFileAvailableInRepo(repo, path, config.version, artifact)
      }
    }

    flow.sendChat "${config.artifactId} ${config.version} released and available in the jenkins plugin archive"
  }
}