package dsl

import org.jenkinsci.plugins.jx.pipelines.arguments.StageProjectArguments
import org.jenkinsci.plugins.jx.pipelines.helpers.MavenHelpers
import org.jenkinsci.plugins.jx.pipelines.model.StagedProjectInfo

def call(StageProjectArguments arguments) {
  echo "stageProject ${arguments}"


  def project = arguments.project
  if (!project) {
    error "ERROR missing `project` property"
  }

  def flow = new CommonFunctions()
  def containerName = arguments.containerName
  def clientsContainerName = arguments.clientsContainerName
  def useMavenForNextVersion = arguments.useMavenForNextVersion
  def useStaging = arguments.useStaging
  def skipTests = arguments.skipTests
  def gitPush = !arguments.disableGitPush
  def mavenProfiles = arguments.mavenProfiles

  // TODO should we default these values from the pom.xml instead and only use the defaults if they are missing?
  def nexusUrl = arguments.stageRepositoryUrl
  def serverId = arguments.stageServerId

  setupStageWorkspace(flow, useMavenForNextVersion, arguments.extraSetVersionArgs, containerName, clientsContainerName, gitPush)

  def repoIds = []
  if (useStaging) {
    echo "using staging to the repository: ${serverId} at ${nexusUrl}"
    repoIds = stageSonartypeRepo(flow, serverId, nexusUrl, containerName, mavenProfiles)
  } else {
    echo "deploying to local artifact-repository"
    mavenDeploy(skipTests, containerName, mavenProfiles)
  }

  def releaseVersion = flow.getProjectVersion()

  container(clientsContainerName) {
    if (fileExists("root/.ssh-git")) {
      sh 'chmod 600 /root/.ssh-git/ssh-key'
      sh 'chmod 600 /root/.ssh-git/ssh-key.pub'
      sh 'chmod 700 /root/.ssh-git'
    }

    if (fileExists("/home/jenkins/.gnupg")) {
      sh 'chmod 700 /home/jenkins/.gnupg'
    }
    if (fileExists("/home/jenkins/.gnupg/pubring.gpg")) {
      sh 'chmod 600 /home/jenkins/.gnupg/pubring.gpg'
    }
    if (fileExists("/home/jenkins/.gnupg/trustdb.gpg")) {
      sh 'chmod 600 /home/jenkins/.gnupg/trustdb.gpg'
    }
    if (fileExists("/home/jenkins/.gnupg/secring.gpg")) {
      sh 'chmod 600 /home/jenkins/.gnupg/secring.gpg'
    }
  }
  // TODO
  //sh "git remote set-url origin git@github.com:${project}.git"

  // lets avoide the stash / unstash for now as we're not using helm ATM
  //stash excludes: '*/src/', includes: '**', name: "staged-${project}-${releaseVersion}".hashCode().toString()

  if (gitPush && arguments.useMavenForNextVersion) {
    container(clientsContainerName) {
      flow.updateGithub()
    }
  }

/*
  if (arguments.extraImagesToStage != null){
    stageExtraImages {
      images = extraStageImages
      tag = releaseVersion
    }
  }
*/

  return new StagedProjectInfo(project, releaseVersion, repoIds)
}


def stageSonartypeRepo(CommonFunctions flow, String serverId, String nexusUrl, String containerName, List<String> mavenProfiles) {
  try {
    def registryHost = flow.dockerRegistryHostAndPort()

    registryHost = "http://${registryHost}"

    def mvnProfileArg = MavenHelpers.mavenProfileCliArgument(mavenProfiles, "release", "openshift")

    echo "using docker registry: ${registryHost}, serverId: ${serverId}, nexusUrl: ${nexusUrl} and container: ${containerName}"

    String mvnArgs = ""
    if (serverId) {
      mvnArgs += " -DserverId=${serverId}"
    }
    if (nexusUrl) {
      mvnArgs += " -DnexusUrl=${nexusUrl}"
    }
    container(containerName) {
      sh "mvn clean -B"
      sh "mvn -V -B -e -U install org.sonatype.plugins:nexus-staging-maven-plugin:1.6.7:deploy ${mvnProfileArg} ${mvnArgs} -Ddocker.push.registry=${registryHost}"
    }

    // lets not archive artifacts until we if we just use nexus or a content repo
    //step([$class: 'ArtifactArchiver', artifacts: '**/target/*.jar', fingerprint: true])

  } catch (err) {
    flow.sendChat room: 'release', message: "Release failed when building and deploying to Nexus ${err}"
    currentBuild.result = 'FAILURE'
    error "ERROR Release failed when building and deploying to Nexus ${err}"
  }
  // the sonartype staging repo id gets written to a file in the workspace
  return flow.getRepoIds()
}

def mavenDeploy(skipTests, String containerName, List<String> mavenProfiles) {
  def mvnProfileArg = MavenHelpers.mavenProfileCliArgument(mavenProfiles, "release", "openshift", "artifact-repository")

  container(containerName) {
    sh "mvn clean -B -e -U deploy -Dmaven.test.skip=${skipTests} ${mvnProfileArg}"
  }
}

def setupStageWorkspace(CommonFunctions flow, boolean useMavenForNextVersion, String mvnExtraArgs, String containerName, String clientsContainerName, boolean gitPush) {
  container(clientsContainerName) {
    sh "git config user.email fabric8-admin@googlegroups.com"
    sh "git config user.name fabric8-release"

    sh 'chmod 600 /root/.ssh-git/ssh-key'
    sh 'chmod 600 /root/.ssh-git/ssh-key.pub'
    sh 'chmod 700 /root/.ssh-git'

    if (fileExists("/home/jenkins/.gnupg/pubring.gpg")) {
      sh 'chmod 600 /home/jenkins/.gnupg/pubring.gpg'
      sh 'chmod 600 /home/jenkins/.gnupg/secring.gpg'
      sh 'chmod 600 /home/jenkins/.gnupg/trustdb.gpg'
      sh 'chmod 700 /home/jenkins/.gnupg'
    }

    sh "git tag -d \$(git tag)"
    if (gitPush) {
      sh "git fetch --tags"
    }

    if (useMavenForNextVersion) {
      container(containerName) {
        sh 'mvn -B build-helper:parse-version versions:set -DnewVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}.\\\${parsedVersion.nextIncrementalVersion} ' + mvnExtraArgs
      }
    } else {
      def newVersion = newVersionUsingSemVer(flow, clientsContainerName)
      echo "New release version ${newVersion}"
      container(containerName) {
        sh "mvn -B -U versions:set -DnewVersion=${newVersion} " + mvnExtraArgs
      }

      sh "git commit -a -m 'release ${newVersion}'"
      if (gitPush) {
        flow.pushTag(newVersion)
      }
    }

    def releaseVersion = flow.getProjectVersion()

    // delete any previous branches of this release
    try {
      sh "git checkout -b release-v${releaseVersion}"
    } catch (err) {
      sh "git branch -D release-v${releaseVersion}"
      sh "git checkout -b release-v${releaseVersion}"
    }
  }
  return false
}

String newVersionUsingSemVer(CommonFunctions flow, String clientsContainerName) {
  container(clientsContainerName) {
    return shOutput("semver-release-version --folder " + pwd()).trim();
  }
}


def warning(String message) {
  println "WARNING: ${message}"
}

/**
 * Returns the trimmed text output of the given command
 */
String shOutput(String command) {
  String answer = sh(script: command, returnStdout: true)
  if (answer != null) {
    return answer.trim();
  }
  return null
}
