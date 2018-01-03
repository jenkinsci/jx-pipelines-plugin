package org.jenkinsci.plugins.jx.pipelines.dsl

import io.fabric8.utils.Strings
import org.jenkinsci.plugins.jx.pipelines.FailedBuildException
import org.jenkinsci.plugins.jx.pipelines.ShellFacade
import org.jenkinsci.plugins.jx.pipelines.StepExtension
import org.jenkinsci.plugins.jx.pipelines.Utils
import org.jenkinsci.plugins.jx.pipelines.arguments.JXPipelinesArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.MavenFlowArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.ReleaseProjectArguments
import org.jenkinsci.plugins.jx.pipelines.arguments.StageProjectArguments
import org.jenkinsci.plugins.jx.pipelines.helpers.ConfigHelper
import org.jenkinsci.plugins.jx.pipelines.helpers.GitHelper
import org.jenkinsci.plugins.jx.pipelines.helpers.GitRepositoryInfo
import org.jenkinsci.plugins.jx.pipelines.model.StagedProjectInfo
import org.jenkinsci.plugins.workflow.cps.CpsScript

class MavenFlow {
  private CpsScript script
  private Utils utils

  MavenFlow(CpsScript script) {
    this.script = script
  }

  def call(body) {
    Map config = [:]

    MavenFlowArguments arguments = new MavenFlowArguments()
    addPropertyFunctions(config, arguments)

    if (body) {
      def bodyBlock = new BodyAssigner(config)
      body.delegate = bodyBlock
      body.resolveStrategy = Closure.DELEGATE_ONLY
      body.call()
      arguments = bodyBlock.argumentInstance(MavenFlowArguments.class)
    }

    script.echo "mavenFlow ${arguments}"

    try {
      script.checkout script.scm

      this.utils = createUtils()
      def branch = findBranch()
      utils.setBranch(branch)

      if (!arguments.gitCloneUrl) {
        arguments.gitCloneUrl = doFindGitCloneURL()
        println "gitCloneUrl now is ${arguments.gitCloneUrl}"
      }

      if (isCi(arguments)) {
        ciPipeline(arguments);
      } else if (isCD(arguments)) {
        cdPipeline(arguments);
      } else {
        // for now lets assume a CI pipeline
        ciPipeline(arguments);
      }

      println("Completed")
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
    //println("invoked with organisation " + organisation + " branches " + cdBranches);
    if (cdBranches != null && cdBranches.size() > 0 && Strings.notEmpty(organisation)) {
      def branch = utils.getBranch()
      if (cdBranches.contains(branch)) {
        String gitUrl = arguments.getGitCloneUrl()
        if (Strings.isNotBlank(gitUrl)) {
          GitRepositoryInfo info = GitHelper.parseGitRepositoryInfo(gitUrl);
          if (info != null) {
            boolean answer = organisation.equals(info.getOrganisation());
            if (!answer) {
              println("Not a CD pipeline as the organisation is " + info.getOrganisation() + " instead of " + organisation);
            }
            return answer;
          }
        } else {
          warning("No git URL could be found so assuming not a CD pipeline");
        }
      } else {
        println("branch ${branch} is not in the cdBranches ${cdBranches} so this is a CI pipeline")
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
    println("Performing CI pipeline");
    //sh("mvn clean install");
    script.sh("mvn clean install");
    return false;
  }

/**
 * Implements the CD pipeline
 */
  Boolean cdPipeline(MavenFlowArguments arguments) {
    println("Performing CD pipeline");
    String gitCloneUrl = arguments.getGitCloneUrl();
    if (Strings.isNullOrBlank(gitCloneUrl)) {
      logError("No gitCloneUrl configured for this pipeline!");
      throw new FailedBuildException("No gitCloneUrl configured for this pipeline!");
    }
    GitRepositoryInfo repositoryInfo = GitHelper.parseGitRepositoryInfo(gitCloneUrl);
    if (!arguments.isDisableGitPush()) {
      String remoteGitCloneUrl = remoteGitCloneUrl(repositoryInfo)
      if (remoteGitCloneUrl != null) {
        script.container("clients") {
          println "setting remote URL to ${remoteGitCloneUrl}"
          script.sh("git remote set-url origin " + remoteGitCloneUrl);
        }
      }
    }
    StageProjectArguments stageProjectArguments = arguments.createStageProjectArguments(repositoryInfo)
    StagedProjectInfo stagedProjectInfo = script.stageProject(stageProjectArguments)

    println "Staging stagedProjectInfo = ${stagedProjectInfo}"

    ReleaseProjectArguments releaseProjectArguments = arguments.createReleaseProjectArguments(stagedProjectInfo)
    return script.releaseProject(releaseProjectArguments)
  }

  String remoteGitCloneUrl(GitRepositoryInfo info) {
    if (info.getHost().equals("github.com")) {
      return "git@github.com:${info.getOrganisation()}/${info.getName()}.git"
    }
    return null
  }

  String doFindGitCloneURL() {
    String text = getGitConfigFile(new File(script.pwd()));
    if (Strings.isNullOrBlank(text)) {
      text = script.readFile(".git/config");
    }
    if (Strings.notEmpty(text)) {
      return GitHelper.extractGitUrl(text);
    }
    return null;
  }


  String getGitConfigFile(File dir) {
    String path = new File(dir, ".git/config").getAbsolutePath();
    String text = script.readFile(path);
    if (text != null) {
      text = text.trim();
      if (text.length() > 0) {
        return text;
      }
    }
    File file = dir.getParentFile();
    if (file != null) {
      return getGitConfigFile(file);
    }
    return null;
  }

  String findBranch() {
    def branch = script.getProperty('env').BRANCH_NAME
    if (!branch) {
      script.container("clients") {
        try {
          script.echo("output of git --version: " + script.sh(script: "git --version", returnStdout: true));
          script.echo("pwd: " + script.sh(script: "pwd", returnStdout: true));
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
      script.echo "Found branch ${branch}"
    }
    return branch;
  }

// TODO common stuff
  def logError(Throwable t) {
    println "ERROR: " + t.getMessage()
    t.printStackTrace()
  }

  def logError(String message) {
    println "ERROR: " + message
  }

  def logError(String message, Throwable t) {
    println "ERROR: " + message + " " + t.getMessage()
    t.printStackTrace()
  }

  def warning(String message) {
    println "WARNING: ${message}"
  }

  def createExtensionFunction(StepExtension extension) {
    return { stepBody ->
      stepBody.resolveStrategy = Closure.DELEGATE_FIRST
      stepBody.delegate = extension
      stepBody()
    }
  }

  def addPropertyFunctions(Map config, JXPipelinesArguments bean) {
    def extensionSuffix = "Extension"

    ConfigHelper.getArgumentFields((Class<? extends JXPipelinesArguments>)bean.getClass()).each { n, k ->
      if (StepExtension.class.isAssignableFrom(k) || StepExtension.class == k) {
        String extKey = n
        if (extKey.endsWith(extensionSuffix)) {
          extKey = extKey.substring(0, extKey.length() - extensionSuffix.length())
        }
        config[extKey] = [n, createExtensionFunction(new StepExtension())]
      } else {
        config[n] = [n, { value -> value }]
      }
    }
  }

  def removeClosures(Map config) {
    def answer = [:]
    for (e in config) {
      if (!(e.value instanceof Closure)) {
        answer[e.key] = e.value
      }
    }
    return answer
  }
}