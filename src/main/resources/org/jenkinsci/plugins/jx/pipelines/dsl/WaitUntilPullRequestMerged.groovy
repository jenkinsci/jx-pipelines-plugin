package org.jenkinsci.plugins.jx.pipelines.dsl

import org.jenkinsci.plugins.jx.pipelines.arguments.WaitUntilPullRequestMergedArguments
import org.jenkinsci.plugins.workflow.cps.CpsScript

class WaitUntilPullRequestMerged {
  private CpsScript script

  WaitUntilPullRequestMerged(CpsScript script) {
    this.script = script
  }

  def call(WaitUntilPullRequestMergedArguments config) {
    def flow = new CommonFunctions(script)
    def githubToken = flow.getGitHubToken()

    def id = config.id
    def project = config.project
    echo "pull request id ${id}"

    def branchName
    def notified = false

    // wait until the PR is merged, if there's a merge conflict the notify and wait until PR is finally merged
    return flow.doStepExecution(config.stepExtension) {
      script.waitUntil {
        echo "https://api.github.com/repos/${project}/pulls/${id}"

        def apiUrl = new URL("https://api.github.com/repos/${project}/pulls/${id}")

        def rs = script.restGetURL {
          authString = githubToken
          url = apiUrl
        }

        if (rs.merged == true) {
          echo "PR ${id} merged"
          return true
        }

        if (rs.state == 'closed') {
          echo "PR ${id} closed"
          return true
        }

        branchName = rs.head.ref
        def sha = rs.head.sha
        echo "checking status of commit ${sha}"

        apiUrl = new URL("https://api.github.com/repos/${project}/commits/${sha}/status")
        rs = script.restGetURL {
          authString = githubToken
          url = apiUrl
        }

        echo "${project} Pull request ${id} state ${rs.state}"

        def values = project.split('/')
        def prj = values[1]

        if (rs.state == 'failure' && !notified) {
          flow.sendChat """
Pull request was not automatically merged.  Please fix and update Pull Request to continue with release...
```
git clone git@github.com:${project}.git
cd ${prj}
git fetch origin pull/${id}/head:fixPR${id}
git checkout fixPR${id}

  [resolve issue]

git commit -a -m 'resolved merge issues caused by release dependency updates'
git push origin fixPR${id}:${branchName}
```
"""

          def shouldWeWait = requestResolve()

          if (!shouldWeWait) {
            return true
          }
          notified = true
        }
        rs.state == 'success'
      }
      try {
        // clean up
        script.deleteGitHubBranch {
          authString = githubToken
          branch = branchName
          project = prj
        }

      } catch (err) {
        echo "not able to delete repo: ${err}"
      }
    }
  }


  def requestResolve() {
    def proceedMessage = '''
Would you like do resolve the conflict?  If so please reply with the proceed command.

Alternatively you can skip this conflict.  This is highly discouraged but maybe necessary if we have a problem quickstart for example.
To do this chose the abort option below, note this particular action will not abort the release and only skip this conflict.
'''

    try {
      script.hubotApprove message: proceedMessage, failOnError: false
      return true
    } catch (err) {
      echo 'Skipping conflict'
      return false
    }
  }
}