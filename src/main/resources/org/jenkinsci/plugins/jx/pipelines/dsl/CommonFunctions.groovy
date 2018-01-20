package org.jenkinsci.plugins.jx.pipelines.dsl

import com.cloudbees.groovy.cps.NonCPS
import hudson.model.Result
import io.fabric8.kubernetes.api.KubernetesHelper
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import org.jenkinsci.plugins.jx.pipelines.StepExtension
import org.jenkinsci.plugins.jx.pipelines.helpers.MavenHelpers
import org.jenkinsci.plugins.workflow.cps.CpsScript

import java.util.regex.Pattern

import static org.jenkinsci.plugins.jx.pipelines.dsl.JXDSLUtils.echo

class CommonFunctions {
  private CpsScript script
  
  CommonFunctions(CpsScript script) {
    this.script = script
  }
  
  def swizzleImageName(text, match, replace) {
    return Pattern.compile("image: ${match}:(.*)").matcher(text).replaceFirst("image: ${replace}")
  }

  def getReleaseVersionFromMavenMetadata(url) {
    def cmd = "curl -L ${url} | grep '<latest' | cut -f2 -d'>'|cut -f1 -d'<'"
    return script.sh(script: cmd, returnStdout: true).toString().trim()
  }

  def updatePackageJSONVersion(f, p, v) {
    script.sh "sed -i -r 's/\"${p}\": \"[0-9][0-9]{0,2}.[0-9][0-9]{0,2}(.[0-9][0-9]{0,2})?(.[0-9][0-9]{0,2})?(-development)?\"/\"${p}\": \"${v}\"/g' ${f}"
  }

  def updateDockerfileEnvVar(f, p, v) {
    script.sh "sed -i -r 's/ENV ${p}.*/ENV ${p} ${v}/g' ${f}"
  }

  def getProjectVersion(String fileName = "pom.xml") {
    if (script.fileExists(fileName)) {
      String file = script.readFile(fileName)
      return MavenHelpers.getProjectVersion(file)
    }
    return null
  }

  def loadMavenPom(String fileName = "pom.xml") {
    if (script.fileExists(fileName)) {
      def pomFileContent = script.readFile(fileName)
      return MavenHelpers.loadMavenPom(pomFileContent)
    }
    return null
  }

  // getReleaseVersion moved to JXDSLUtils
  // getMavenCentralVersion moved to JXDSLUtils
  // getVersion moved to JXDSLUtils
  // isArtifactAvailableInRepo moved to JXDSLUtils
  // isFileAvailableInRepo moved to JXDSLUtils
  // removeTrailingSlash moved to JXDSLUtils

  def getRepoIds() {
    // we could have multiple staging repos created, we need to write the names of all the generated files to a well known
    // filename so we can use the workflow readFile (wildcards wont works and new File wont with slaves as groovy is executed on the master jenkins
    // We write the names of the files that contain the repo ids used for staging.  Each staging repo id is read from each file and returned as a list
    script.sh 'find target/nexus-staging/staging/  -maxdepth 1 -name "*.properties" > target/nexus-staging/staging/repos.txt'
    def repos = script.readFile('target/nexus-staging/staging/repos.txt')
    def list = []
    list = repos.split('\n').collect { r ->
      def matcher = script.readFile(r) =~ 'stagingRepository.id=(.+)'
      if (matcher != null) {
        return matcher[0][1]
      }
    }
    return list
  }

  // getDockerHubImageTags moved to JXDSLUtils

  def searchAndReplaceMavenVersionPropertyNoCommit(String property, String newVersion) {
    // example matches <fabric8.version>2.3</fabric8.version> <fabric8.version>2.3.12</fabric8.version> <fabric8.version>2.3.12.5</fabric8.version>
    script.sh "find -type f -name 'pom.xml' | xargs sed -i -r 's/${property}[0-9][0-9]{0,2}.[0-9][0-9]{0,2}(.[0-9][0-9]{0,2})?(.[0-9][0-9]{0,2})?</${property}${newVersion}</g'"
  }

  def searchAndReplaceMavenVersionProperty(String property, String newVersion) {
    // example matches <fabric8.version>2.3</fabric8.version> <fabric8.version>2.3.12</fabric8.version> <fabric8.version>2.3.12.5</fabric8.version>
    script.sh "find -type f -name 'pom.xml' | xargs sed -i -r 's/${property}[0-9][0-9]{0,2}.[0-9][0-9]{0,2}(.[0-9][0-9]{0,2})?(.[0-9][0-9]{0,2})?</${property}${newVersion}</g'"
    script.sh "git commit -a -m 'Bump ${property} version'"
  }

  def searchAndReplaceMavenSnapshotProfileVersionProperty(String property, String newVersion) {
    // example matches <fabric8.version>2.3-SNAPSHOT</fabric8.version> <fabric8.version>2.3.12-SNAPSHOT</fabric8.version> <fabric8.version>2.3.12.5-SNAPSHOT</fabric8.version>
    script.sh "find -type f -name 'pom.xml' | xargs sed -i -r 's/${property}[0-9][0-9]{0,2}.[0-9][0-9]{0,2}(.[0-9][0-9]{0,2})?(.[0-9][0-9]{0,2})?-SNAPSHOT</${property}${newVersion}-SNAPSHOT</g'"
    script.sh "git commit -a -m 'Bump ${property} development profile SNAPSHOT version'"
  }

  def setupWorkspaceForRelease(String project, Boolean useGitTagForNextVersion, String mvnExtraArgs = "", String currentVersion = "", String containerName = "maven") {
    script.sh "git config user.email jenkins-x-admin@googlegroups.com"
    script.sh "git config user.name jenkins-x-bot"
    // TODO disable if no .gitcredentials
    script.sh "git config credential.https://github.com.username jenkins-x-bot"

/*
    if (script.fileExists("root/.ssh-git")) {
      script.sh 'chmod 600 /root/.ssh-git/ssh-key'
      script.sh 'chmod 600 /root/.ssh-git/ssh-key.pub'
      script.sh 'chmod 700 /root/.ssh-git'
    }
    if (script.fileExists("/home/jenkins/.gnupg")) {
      script.sh 'chmod 600 /home/jenkins/.gnupg/pubring.gpg'
      script.sh 'chmod 600 /home/jenkins/.gnupg/secring.gpg'
      script.sh 'chmod 600 /home/jenkins/.gnupg/trustdb.gpg'
      script.sh 'chmod 700 /home/jenkins/.gnupg'
      '
    }
*/

    script.sh 'cp /root/netrc/.netrc ~/.netrc'

    script.sh "git tag -d \$(git tag)"
    script.sh "git fetch --tags"

    if (useGitTagForNextVersion) {
      def newVersion = getNewVersionFromTag(currentVersion)
      echo "New release version ${newVersion}"
      script.container(containerName) {
        script.sh "mvn -B -U versions:set -DnewVersion=${newVersion} " + mvnExtraArgs
      }

      script.sh "git commit -a -m 'release ${newVersion}'"
      pushTag(newVersion)
    } else {
      script.container(containerName) {
        script.sh 'mvn -B build-helper:parse-version versions:set -DnewVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}.\\\${parsedVersion.nextIncrementalVersion} ' + mvnExtraArgs

      }
    }

    def releaseVersion = getProjectVersion()

    // delete any previous branches of this release
    try {
      script.sh "git checkout -b release-v${releaseVersion}"
    } catch (err) {
      script.sh "git branch -D release-v${releaseVersion}"
      script.sh "git checkout -b release-v${releaseVersion}"
    }
  }

// if no previous tag found default 1.0.0 is used, else assume version is in the form major.minor or major.minor.micro version
  def getNewVersionFromTag(pomVersion = null) {
    def version = '1.0.0'

    // Set known prerelease prefixes, needed for the proper sort order
    // in the next command
    script.sh "git config versionsort.prereleaseSuffix -RC"
    script.sh "git config versionsort.prereleaseSuffix -M"

    // if the repo has no tags this command will fail
    script.sh "git tag --sort version:refname | tail -1 > version.tmp"

    def tag = script.readFile 'version.tmp'

    if (tag == null || tag.size() == 0) {
      echo "no existing tag found using version ${version}"
      return version
    }

    tag = tag.trim()

    echo "Testing to see if version ${tag} is semver compatible"

    def semver = tag =~ /(?i)\bv?(?<major>0|[1-9]\d*)(?:\.(?<minor>0|[1-9]\d*)(?:\.(?<patch>0|[1-9]\d*))?)?(?:-(?<prerelease>[\da-z\-]+(?:\.[\da-z\-]+)*))?(?:\+(?<build>[\da-z\-]+(?:\.[\da-z\-]+)*))?\b/

    if (semver.matches()) {
      echo "Version ${tag} is semver compatible"

      def majorVersion = semver.group('major') as int
      def minorVersion = (semver.group('minor') ?: 0) as int
      def patchVersion = ((semver.group('patch') ?: 0) as int) + 1

      echo "Testing to see if current POM version ${pomVersion} is semver compatible"

      def pomSemver = pomVersion.trim() =~ /(?i)\bv?(?<major>0|[1-9]\d*)(?:\.(?<minor>0|[1-9]\d*)(?:\.(?<patch>0|[1-9]\d*))?)?(?:-(?<prerelease>[\da-z\-]+(?:\.[\da-z\-]+)*))?(?:\+(?<build>[\da-z\-]+(?:\.[\da-z\-]+)*))?\b/
      if (pomSemver.matches()) {
        echo "Current POM version ${pomVersion} is semver compatible"

        def pomMajorVersion = pomSemver.group('major') as int
        def pomMinorVersion = (pomSemver.group('minor') ?: 0) as int
        def pomPatchVersion = (pomSemver.group('patch') ?: 0) as int

        if (pomMajorVersion > majorVersion ||
            (pomMajorVersion == majorVersion &&
                (pomMinorVersion > minorVersion) || (pomMinorVersion == minorVersion && pomPatchVersion > patchVersion)
            )
        ) {
          majorVersion = pomMajorVersion
          minorVersion = pomMinorVersion
          patchVersion = pomPatchVersion
        }
      }

      def newVersion = "${majorVersion}.${minorVersion}.${patchVersion}"
      echo "New version is ${newVersion}"
      return newVersion
    } else {
      echo "Version is not semver compatible"

      // strip the v prefix from the tag so we can use in a maven version number
      def previousReleaseVersion = tag.substring(tag.lastIndexOf('v') + 1)
      echo "Previous version found ${previousReleaseVersion}"

      // if there's an int as the version then turn it into a major.minor.micro version
      if (previousReleaseVersion.isNumber()) {
        return previousReleaseVersion + '.0.1'
      } else {
        // if previous tag is not a number and doesnt have a '.' version seperator then error until we have one
        if (previousReleaseVersion.lastIndexOf('.') == 0) {
          script.error "found invalid latest tag [${previousReleaseVersion}] set to major.minor.micro to calculate next release version"
        }
        // increment the release number after the last seperator '.'
        def microVersion = previousReleaseVersion.substring(previousReleaseVersion.lastIndexOf('.') + 1) as int
        return previousReleaseVersion.substring(0, previousReleaseVersion.lastIndexOf('.') + 1) + (microVersion + 1)
      }
    }
  }

  def releaseSonartypeRepo(String repoId) {
    try {
      // release the sonartype staging repo
      script.sh "mvn -B org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:rc-release -DserverId=oss-sonatype-staging -DnexusUrl=https://oss.sonatype.org -DstagingRepositoryId=${repoId} -Ddescription=\"Next release is ready\" -DstagingProgressTimeoutMinutes=60"

    } catch (err) {
      script.sh "mvn org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:rc-drop -DserverId=oss-sonatype-staging -DnexusUrl=https://oss.sonatype.org -DstagingRepositoryId=${repoId} -Ddescription=\"Error during release: ${err}\" -DstagingProgressTimeoutMinutes=60"
      script.getProperty("currentBuild").result = Result.FAILURE
      script.error "ERROR releasing sonartype repo ${repoId}: ${err}"
    }
  }

  def dropStagingRepo(String repoId) {
    echo "Not a release so dropping staging repo ${repoId}"
    script.sh "mvn org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:rc-drop -DserverId=oss-sonatype-staging -DnexusUrl=https://oss.sonatype.org -DstagingRepositoryId=${repoId} -Ddescription=\"Dry run\" -DstagingProgressTimeoutMinutes=60"
  }

  def helm() {
    def pluginVersion = JXDSLUtils.getReleaseVersion("io/fabric8/fabric8-maven-plugin")
    try {
      script.sh "mvn -B io.fabric8:fabric8-maven-plugin:${pluginVersion}:helm"
      script.sh "mvn -B io.fabric8:fabric8-maven-plugin:${pluginVersion}:helm-push"
    } catch (err) {
      script.error "ERROR with helm push ${err}"
    }
  }

  def pushTag(String releaseVersion) {
    script.sh "git tag -fa v${releaseVersion} -m 'Release version ${releaseVersion}'"
    script.sh "git push origin v${releaseVersion}"
  }


  def updateGithub() {
    def releaseVersion = getProjectVersion()
    script.sh "git push origin release-v${releaseVersion}"
  }

  def updateNextDevelopmentVersion(String releaseVersion, String mvnExtraArgs = "") {
    // update poms back to snapshot again
    script.sh 'mvn -B build-helper:parse-version versions:set -DnewVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}.\\\${parsedVersion.nextIncrementalVersion}-SNAPSHOT ' + mvnExtraArgs
    def snapshotVersion = getProjectVersion()
    script.sh "git commit -a -m '[CD] prepare for next development iteration ${snapshotVersion}'"
    script.sh "git push origin release-v${releaseVersion}"
  }

  def hasChangedSinceLastRelease() {
    script.sh "git log --name-status HEAD^..HEAD -1 --grep=\"prepare for next development iteration\" --author='fusesource-ci' >> gitlog.tmp"
    def myfile = script.readFile('gitlog.tmp')
    //sh "rm gitlog.tmp"
    // if the file size is 0 it means the CI user was not the last commit so project has changed
    if (myfile.length() == 0) {
      return true
    } else {
      return false
    }
  }

  def getOldVersion() {
    def matcher = script.readFile('website/src/docs/index.page') =~ '<h1>Documentation for version (.+)</h1>'
    matcher ? matcher[0][1] : null
  }

  def updateDocsAndSite(String newVersion) {
    // get previous version
    def oldVersion = getOldVersion()

    if (oldVersion == null) {
      echo "No previous version found"
      return
    }

    // use perl so that we we can easily turn off regex in the SED query as using dots in version numbers returns unwanted results otherwise
    script.sh "find . -name '*.md' ! -name Changes.md ! -path '*/docs/jube/**.*' | xargs perl -p -i -e 's/\\Q${oldVersion}/${newVersion}/g'"
    script.sh "find . -path '*/website/src/**.*' | xargs perl -p -i -e 's/\\Q${oldVersion}/${newVersion}/g'"

    script.sh "git commit -a -m '[CD] Update docs following ${newVersion} release'"

  }

  def runSystemTests() {
    script.sh 'cd systests && mvn clean && mvn integration-test verify'
  }

  def createPullRequest(String message, String project, String branch, String githubToken = getGitHubToken()) {
    try {
      return JXDSLUtils.createPullRequest(message, project, branch, githubToken)
    } catch (err) {
      script.error "ERROR  ${err}"
    }
  }

  def closePR(project, id, newVersion, newPRID, String githubToken = getGitHubToken()) {
    try {
      JXDSLUtils.closePR(project, id, newVersion, newPRID, githubToken)
    } catch (Exception e) {
      script.error "${e.message}"
    }
  }

  def getIssueComments(project, id, githubToken = getGitHubToken()) {
    try {
      return JXDSLUtils.getIssueComments(project, id, githubToken)
    } catch (Exception e) {
      script.error("${e.message}")
    }
  }

  def waitUntilSuccessStatus(project, ref, String githubToken = getGitHubToken()) {
    script.waitUntil {
      return JXDSLUtils.checkIfCommitIsSuccessful(project, ref, githubToken)
    }
  }

  def getGithubBranch(project, id, String gitHubToken = getGitHubToken()) {
    return JXDSLUtils.getGithubBranch(project, id, githubToken)
  }

  def mergePR(project, id, String githubToken = getGitHubToken()) {
    def branch = getGithubBranch(project, id, githubToken)
    waitUntilSuccessStatus(project, branch, githubToken)

    try {
      JXDSLUtils.mergePR(project, id, githubToken)
    } catch (err) {
      squashAndMerge(project, id)
      script.error("${err.message}")
    }
  }

  def squashAndMerge(project, id, String githubToken = getGitHubToken()) {
    try {
      JXDSLUtils.squashAndMerge(project, id, githubToken)
    } catch (Exception e) {
      script.error("${e.message}")
    }
  }

  def addCommentToPullRequest(comment, pr, project, String githubToken = getGitHubToken()) {
    try {
      JXDSLUtils.addCommentToPullRequest(comment, pr, project, githubToken)
    } catch (err) {
      script.error "ERROR  ${err}"
    }
  }

  def addMergeCommentToPullRequest(String pr, String project, String githubToken = getGitHubToken()) {
    try {
      JXDSLUtils.addMergeCommentToPullRequest(pr, project, githubToken)
    } catch (err) {
      script.error "ERROR  ${err}"
    }
  }

  def getGitHubProject() {
    def url = getScmPushUrl()
    if (!url.contains('github.com')) {
      script.error "${url} is not a GitHub URL"
    }

    if (url.contains("https://github.com/")) {
      url = url.replaceAll("https://github.com/", '')

    } else if (url.contains("git@github.com:")) {
      url = url.replaceAll("git@github.com:", '')
    }

    if (url.contains(".git")) {
      url = url.replaceAll(".git", '')
    }
    return url.trim()
  }

  def isAuthorCollaborator(String githubToken = getGitHubToken(), String project = getGitHubProject()) {
    def changeAuthor = script.getProperty('env').CHANGE_AUTHOR
    try {
      return JXDSLUtils.isAuthorCollaborator(changeAuthor, githubToken, project)
    } catch (Exception e) {
      script.error("${e.message}")
    }
  }

  // getUrlAsString moved to JXDSLUtils

  def drop(String pr, String project, String githubToken = getGitHubToken()) {
    try {
      JXDSLUtils.drop(pr, project, githubToken)
    } catch (err) {
      script.error "ERROR  ${err}"
    }
  }

  def deleteRemoteBranch(String branchName, containerName) {
    script.container(name: containerName) {
/*
      script.sh 'chmod 600 /root/.ssh-git/ssh-key'
      script.sh 'chmod 600 /root/.ssh-git/ssh-key.pub'
      script.sh 'chmod 700 /root/.ssh-git'
*/
      script.sh "git push origin --delete ${branchName}"
    }
  }

  def getGitHubToken() {
    def tokenPath = '/home/jenkins/.apitoken/hub'
    def githubToken = script.readFile tokenPath
    if (!githubToken?.trim()) {
      script.error "No GitHub token found in ${tokenPath}"
    }
    return githubToken.trim()
  }

  @NonCPS
  def isSingleNode() {
    KubernetesClient kubernetes = new DefaultKubernetesClient()
    if (kubernetes.nodes().list().getItems().size() == 1) {
      return true
    } else {
      return false
    }
  }

  @NonCPS
  def hasService(String name) {
    KubernetesClient kubernetes = new DefaultKubernetesClient()

    def service = kubernetes.services().withName(name).get()
    if (service != null) {
      return service.metadata != null
    }
    return false
  }

  @NonCPS
  def getServiceURL(String serviceName, String namespace = null, String protocol = "http", boolean external = true) {
    KubernetesClient kubernetes = new DefaultKubernetesClient()
    if (namespace == null) namespace = kubernetes.getNamespace()
    return KubernetesHelper.getServiceURL(kubernetes, serviceName, namespace, protocol, external)
  }

  def hasOpenShiftYaml() {
    def openshiftYaml = findFiles(glob: '**/openshift.yml')
    try {
      if (openshiftYaml) {
        def contents = script.readFile(openshiftYaml[0].path)
        if (contents != null) {
          if (contents.contains('kind: "ImageStream"') || contents.contains('kind: ImageStream') || contents.contains('kind: \'ImageStream\'')) {
            echo "OpenShift YAML contains an ImageStream"
            return true
          } else {
            echo "OpenShift YAML does not contain an ImageStream so not using S2I binary mode"
          }
        }
      } else {
        echo "Warning OpenShift YAML ${openshiftYaml} does not exist!"
      }
    } catch (e) {
      script.error "Failed to load ${openshiftYaml[0]} due to ${e}"
    }
    return false
  }

  // deleteNamespace moved to JXDSLUtils
  // isOpenShift moved to JXDSLUtils
  // getCloudConfig moved to JXDSLUtils

/**
 * Should be called after checkout scm
 */
  def getScmPushUrl() {
    def url = script.sh(returnStdout: true, script: 'git config --get remote.origin.url').trim()

    if (!url) {
      script.error "no URL found for git config --get remote.origin.url "
    }
    return url
  }

  def openShiftImageStreamExists(String name) {
    if (JXDSLUtils.isOpenShift()) {
      try {
        def result = script.sh(returnStdout: true, script: 'oc describe is ${name} --namespace openshift')
        if (result && result.contains(name)) {
          echo "ImageStream  ${name} is already installed globally"
          return true;
        } else {
          //see if its already in our namespace
          def namespace = script.getProperty('kubernetes').getNamespace();
          result = script.sh(returnStdout: true, script: 'oc describe is ${name} --namespace ${namespace}')
          if (result && result.contains(name)) {
            echo "ImageStream  ${name} is already installed in project ${namespace}"
            return true;
          }
        }
      } catch (e) {
        echo "Warning: ${e} "
      }
    }
    return false;
  }

  def openShiftImageStreamInstall(String name, String location) {
    if (openShiftImageStreamExists(name)) {
      echo "ImageStream ${name} does not exist - installing ..."
      try {
        def result = script.sh(returnStdout: true, script: 'oc create -f  ${location}')
        def namespace = script.getProperty('kubernetes').getNamespace();
        echo "ImageStream ${name} now installed in project ${namespace}"
        return true;
      } catch (e) {
        echo "Warning: ${e} "
      }
    }
    return false;
  }

  def dockerRegistryPrefix() {
    def registryHost = dockerRegistryHostAndPort(null)
    def registryPrefix = ""
    if (registryHost) {
      registryPrefix = "${registryHost}/"
    }
    return registryPrefix
  }

  def dockerRegistryHostAndPort(String defaultRegistryHost = "fabric8-docker-registry") {
    def registryHost = script.getProperty('env').FABRIC8_DOCKER_REGISTRY_SERVICE_HOST
    if (!registryHost) {
      echo "WARNING you don't seem to be running the fabric8-docker-registry service!!!"
      registryHost = defaultRegistryHost
    }
    def registryPort = script.getProperty('env').FABRIC8_DOCKER_REGISTRY_SERVICE_PORT
    if (registryPort && registryHost) {
      registryHost = "${registryHost}:${registryPort}"
    }
    return registryHost
  }

  def sendChat(String message, String room = null, boolean failOnError = false) {
    if (!room) {
      room = "release"
    }
    // TODO call hubotSend now
    echo "CHAT: ${room}: ${message}"
  }

/** Invokes a step extension on the given closure body */
  def doStepExecution(StepExtension stepExtension, body) {
    if (stepExtension == null) {
      stepExtension = new StepExtension()
    }
    if (stepExtension.preBlock instanceof Closure) {
      echo "StepExtension invoking pre steps"
      invokeStepBlock(stepExtension.preBlock)
    }
    def answer
    if (stepExtension.stepsBlock instanceof Closure) {
      echo "StepExtension invoking replacement steps"
      answer = invokeStepBlock(stepExtension.stepsBlock)
    } else if (body != null) {
      if (stepExtension.disabled) {
        echo "StepExtension has disabled the steps"
      } else {
        answer = body()
      }
    }
    if (stepExtension.postBlock instanceof Closure) {
      echo "StepExtension invoking post steps"
      invokeStepBlock(stepExtension.postBlock)
    }
    return answer
  }

  private def invokeStepBlock(Closure stepBlock) {
    stepBlock.delegate = script
    stepBlock.resolveStrategy = Closure.DELEGATE_FIRST
    return stepBlock()
  }
}