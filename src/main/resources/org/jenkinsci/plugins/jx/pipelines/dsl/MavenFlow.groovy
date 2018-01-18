package org.jenkinsci.plugins.jx.pipelines.dsl

import io.fabric8.utils.Strings
import org.jenkinsci.plugins.jx.pipelines.FailedBuildException
import org.jenkinsci.plugins.jx.pipelines.ShellFacade
import org.jenkinsci.plugins.jx.pipelines.Utils
import org.jenkinsci.plugins.jx.pipelines.arguments.MavenFlowArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.ReleaseProjectArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.StageProjectArguments
import org.jenkinsci.plugins.jx.pipelines.helpers.GitHelper
import org.jenkinsci.plugins.jx.pipelines.helpers.GitRepositoryInfo
import org.jenkinsci.plugins.jx.pipelines.model.StagedProjectInfo
import org.jenkinsci.plugins.workflow.cps.CpsScript

import static org.jenkinsci.plugins.jx.pipelines.dsl.JXDSLUtils.echo

class MavenFlow {
  private CpsScript script
  private Utils utils

  MavenFlow(CpsScript script) {
    this.script = script
  }

  def call(MavenFlowArguments arguments) {

    echo "mavenFlow ${arguments}"

    try {
      script.checkout script.scm

      this.utils = createUtils()
      def branch = findBranch(arguments.clientsContainerName)
      utils.setBranch(branch)

      if (!arguments.gitCloneUrl) {
        arguments.gitCloneUrl = doFindGitCloneURL()
        echo "gitCloneUrl now is ${arguments.gitCloneUrl}"
      }

      if (isCi(arguments)) {
        ciPipeline(arguments);
      } else if (isCD(arguments)) {
        cdPipeline(arguments);
      } else {
        // for now lets assume a CI pipeline
        ciPipeline(arguments);
      }

      echo("Completed")
      if (arguments.pauseOnSuccess) {
        script.input message: 'The build pod has been paused'
      }

    } catch (err) {
      //hubot room: 'release', message: "${env.JOB_NAME} failed: ${err}"
      logError(err)

      if (arguments.pauseOnFailure) {
        script.input message: 'The build pod has been paused'
      }
    }
  }

  Utils createUtils() {
    def u = new Utils()
    u.updateEnvironment(script.getProperty('env'))

    u.setShellFacade({ String cmd, boolean returnOutput, String containerName ->
      if (containerName) {
        def answer
        script.container(containerName) {
          answer = script.sh(script: cmd, returnStdout: returnOutput).toString().trim()
        }
        return answer
      } else {
        return script.sh(script: cmd, returnStdout: returnOutput).toString().trim()
      }
    } as ShellFacade)

    def path = script.sh(script: "pwd", returnStdout: true)
    if (path) {
      u.setCurrentPath(path.trim())
    }
    return u
  }

  boolean isCD(MavenFlowArguments arguments) {
    Boolean flag = null
    try {
      flag = utils.isCD();
    } catch (e) {
      logError(e)
    }
    if (flag && flag.booleanValue()) {
      return true;
    }
    String organisation = arguments.getCdOrganisation();
    List<String> cdBranches = arguments.getCdBranches();
    //echo("invoked with organisation " + organisation + " branches " + cdBranches);
    if (cdBranches != null && cdBranches.size() > 0 && Strings.notEmpty(organisation)) {
      def branch = utils.getBranch()
      if (cdBranches.contains(branch)) {
        String gitUrl = arguments.getGitCloneUrl()
        if (Strings.isNotBlank(gitUrl)) {
          GitRepositoryInfo info = GitHelper.parseGitRepositoryInfo(gitUrl);
          if (info != null) {
            boolean answer = organisation.equals(info.getOrganisation());
            if (!answer) {
              echo("Not a CD pipeline as the organisation is " + info.getOrganisation() + " instead of " + organisation);
            }
            return answer;
          }
        } else {
          warning("No git URL could be found so assuming not a CD pipeline");
        }
      } else {
        echo("branch ${branch} is not in the cdBranches ${cdBranches} so this is a CI pipeline")
      }
    } else {
      warning("No cdOrganisation or cdBranches configured so assuming not a CD pipeline");
    }
    return false;
  }


  boolean isCi(MavenFlowArguments arguments) {
    boolean value = false
    try {
      value = utils.isCI();
    } catch (e) {
      logError(e)
    }
    if (value) {
      return true;
    }

    // TODO for now should we return true if CD is false?
    return !isCD(arguments);
  }

/**
 * Implements the CI pipeline
 */
  Boolean ciPipeline(MavenFlowArguments arguments) {
    echo("Performing CI pipeline");
    //sh("mvn clean install");
    script.sh("mvn clean install");
    return false;
  }

/**
 * Implements the CD pipeline
 */
  Boolean cdPipeline(MavenFlowArguments arguments) {
    echo("Performing CD pipeline");
    String gitCloneUrl = arguments.getGitCloneUrl();
    if (Strings.isNullOrBlank(gitCloneUrl)) {
      logError("No gitCloneUrl configured for this pipeline!");
      throw new FailedBuildException("No gitCloneUrl configured for this pipeline!");
    }
    GitRepositoryInfo repositoryInfo = GitHelper.parseGitRepositoryInfo(gitCloneUrl);
    if (!arguments.isDisableGitPush()) {
      String remoteGitCloneUrl = remoteGitCloneUrl(repositoryInfo)
      if (remoteGitCloneUrl != null) {
        script.container(arguments.clientsContainerName) {
          echo "setting remote URL to ${remoteGitCloneUrl}"
          script.sh("git remote set-url origin " + remoteGitCloneUrl);
        }
      }
    }
    StageProjectArguments stageProjectArguments = arguments.createStageProjectArguments(repositoryInfo)
    StagedProjectInfo stagedProjectInfo = script.stageProject(stageProjectArguments)

    echo "Staging stagedProjectInfo = ${stagedProjectInfo}"

    ReleaseProjectArguments releaseProjectArguments = arguments.createReleaseProjectArguments(stagedProjectInfo)
    return script.releaseProject(releaseProjectArguments)
  }

  String remoteGitCloneUrl(GitRepositoryInfo info) {
/*
    if (info.getHost().equals("github.com")) {
      return "git@github.com:${info.getOrganisation()}/${info.getName()}.git"
    }
*/
    return null
  }

  String doFindGitCloneURL() {
    String dir
    def p = script.pwd()
    if (p instanceof File) {
      dir = p.path
    } else {
      dir = p.toString()
    }
    String text = getGitConfigFile(dir);
    if (Strings.isNullOrBlank(text)) {
      text = script.readFile(".git/config");
    }
    if (Strings.notEmpty(text)) {
      return GitHelper.extractGitUrl(text);
    }
    return null;
  }

  String getGitConfigFile(String dir) {
    String text = script.readFile("${dir}/.git/config");
    if (text != null) {
      text = text.trim();
      if (text.length() > 0) {
        return text;
      }
    }
    if (script.fileExists("${dir}/..")) {
      return getGitConfigFile("${dir}/..");
    }
    return null;
  }

  String findBranch(containerName) {
    def branch = script.getProperty('env').BRANCH_NAME
    if (!branch) {
      script.container(containerName) {
        try {
          echo("output of git --version: " + script.sh(script: "git --version", returnStdout: true));
          echo("pwd: " + script.sh(script: "pwd", returnStdout: true));
        } catch (e) {
          logError("Failed to invoke git --version: " + e, e);
        }
        if (!branch) {
          def head = null
          try {
            head = script.sh(script: "git rev-parse HEAD", returnStdout: true)
          } catch (e) {
            logError("Failed to load: git rev-parse HEAD: " + e, e)
          }
          if (head) {
            head = head.trim()
            try {
              def text = script.sh(script: "git ls-remote --heads origin | grep ${head} | cut -d / -f 3", returnStdout: true)
              if (text) {
                branch = text.trim();
              }
            } catch (e) {
              logError("\nUnable to get git branch: " + e, e);
            }
          }
        }
        if (!branch) {
          try {
            def text = script.sh(script: "git symbolic-ref --short HEAD", returnStdout: true)
            if (text) {
              branch = text.trim();
            }
          } catch (e) {
            logError("\nUnable to get git branch and in a detached HEAD. You may need to select Pipeline additional behaviour and \'Check out to specific local branch\': " + e, e);
          }
        }
      }
      echo "Found branch ${branch}"
    }
    return branch;
  }

// TODO common stuff
  def logError(Throwable t) {
    echo "ERROR: " + t.getMessage()
    echo JXDSLUtils.getFullStackTrace(t)
  }

  def logError(String message) {
    echo "ERROR: " + message
  }

  def logError(String message, Throwable t) {
    echo "ERROR: " + message + " " + t.getMessage()
    echo JXDSLUtils.getFullStackTrace(t)
  }

  def warning(String message) {
    echo "WARNING: ${message}"
  }

}