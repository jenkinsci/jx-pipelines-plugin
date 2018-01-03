package org.jenkinsci.plugins.jx.pipelines.dsl

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurper
import hudson.model.Result
import io.fabric8.kubernetes.api.KubernetesHelper
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.openshift.client.DefaultOpenShiftClient
import io.fabric8.openshift.client.OpenShiftClient
import jenkins.model.Jenkins
import org.jenkinsci.plugins.jx.pipelines.StepExtension
import org.jenkinsci.plugins.jx.pipelines.helpers.MavenHelpers
import org.jenkinsci.plugins.workflow.cps.CpsScript

import java.util.regex.Pattern

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

  def getProjectVersion() {
    def file = script.readFile('pom.xml')
    def project = new XmlSlurper().parseText(file)
    return project.version.text()
  }

  def loadMavenPom(String fileName = "pom.xml") {
    if (script.fileExists(fileName)) {
      def pomFileContent = script.readFile(fileName)
      return MavenHelpers.loadMavenPom(pomFileContent)
    }
    return null
  }


  def getReleaseVersion(String artifact) {
    def modelMetaData = new XmlSlurper().parse("https://oss.sonatype.org/content/repositories/releases/${artifact}/maven-metadata.xml")
    def version = modelMetaData.versioning.release.text()
    return version
  }

  def getMavenCentralVersion(String artifact) {
    def modelMetaData = new XmlSlurper().parse("http://central.maven.org/maven2/${artifact}/maven-metadata.xml")
    def version = modelMetaData.versioning.release.text()
    return version
  }

  def getVersion(String repo, String artifact) {
    repo = removeTrailingSlash(repo)
    artifact = removeTrailingSlash(artifact)

    def modelMetaData = new XmlSlurper().parse(repo + '/' + artifact + '/maven-metadata.xml')
    def version = modelMetaData.versioning.release.text()
    return version
  }

  def isArtifactAvailableInRepo(String repo, String groupId, String artifactId, String version, String ext) {
    repo = removeTrailingSlash(repo)
    groupId = removeTrailingSlash(groupId)
    artifactId = removeTrailingSlash(artifactId)

    def url = new URL("${repo}/${groupId}/${artifactId}/${version}/${artifactId}-${version}.${ext}")
    HttpURLConnection connection = url.openConnection()

    connection.setRequestMethod("GET")
    connection.setDoInput(true)

    try {
      connection.connect()
      new InputStreamReader(connection.getInputStream(), "UTF-8")
      return true
    } catch (FileNotFoundException e1) {
      script.echo "File not yet available: ${url.toString()}"
      return false
    } finally {
      connection.disconnect()
    }
  }


  def isFileAvailableInRepo(String repo, String path, String version, String artifact) {
    repo = removeTrailingSlash(repo)
    path = removeTrailingSlash(path)
    version = removeTrailingSlash(version)

    def url = new URL("${repo}/${path}/${version}/${artifact}")

    HttpURLConnection connection = url.openConnection()

    connection.setRequestMethod("GET")
    connection.setDoInput(true)

    try {
      connection.connect()
      new InputStreamReader(connection.getInputStream(), "UTF-8")
      script.echo "File is available at: ${url.toString()}"
      return true
    } catch (FileNotFoundException e1) {
      script.echo "File not yet available: ${url.toString()}"
      return false
    } finally {
      connection.disconnect()
    }
  }

  def removeTrailingSlash(String myString) {
    if (myString.endsWith("/")) {
      return myString.substring(0, myString.length() - 1)
    }
    return myString
  }

  def getRepoIds() {
    // we could have multiple staging repos created, we need to write the names of all the generated files to a well known
    // filename so we can use the workflow readFile (wildcards wont works and new File wont with slaves as groovy is executed on the master jenkins
    // We write the names of the files that contain the repo ids used for staging.  Each staging repo id is read from each file and returned as a list
    script.sh 'find target/nexus-staging/staging/  -maxdepth 1 -name "*.properties" > target/nexus-staging/staging/repos.txt'
    def repos = script.readFile('target/nexus-staging/staging/repos.txt')
    def list = []
    // workflow closure not working here https://issues.jenkins-ci.org/browse/JENKINS-26481
    def filelines = new String(repos).split('\n')
    for (int i = 0; i < filelines.size(); i++) {
      def matcher = script.readFile(filelines[i]) =~ 'stagingRepository.id=(.+)'
      list << matcher[0][1]
    }
    return list
  }

  def getDockerHubImageTags(String image) {
    try {
      return "https://registry.hub.docker.com/v1/repositories/${image}/tags".toURL().getText()
    } catch (err) {
      return "NO_IMAGE_FOUND"
    }
  }

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
    script.sh "git config user.email fabric8-admin@googlegroups.com"
    script.sh "git config user.name fabric8-release"

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
    }

    script.sh "git tag -d \$(git tag)"
    script.sh "git fetch --tags"

    if (useGitTagForNextVersion) {
      def newVersion = getNewVersionFromTag(currentVersion)
      script.echo "New release version ${newVersion}"
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
      script.echo "no existing tag found using version ${version}"
      return version
    }

    tag = tag.trim()

    script.echo "Testing to see if version ${tag} is semver compatible"

    def semver = tag =~ /(?i)\bv?(?<major>0|[1-9]\d*)(?:\.(?<minor>0|[1-9]\d*)(?:\.(?<patch>0|[1-9]\d*))?)?(?:-(?<prerelease>[\da-z\-]+(?:\.[\da-z\-]+)*))?(?:\+(?<build>[\da-z\-]+(?:\.[\da-z\-]+)*))?\b/

    if (semver.matches()) {
      script.echo "Version ${tag} is semver compatible"

      def majorVersion = semver.group('major') as int
      def minorVersion = (semver.group('minor') ?: 0) as int
      def patchVersion = ((semver.group('patch') ?: 0) as int) + 1

      script.echo "Testing to see if current POM version ${pomVersion} is semver compatible"

      def pomSemver = pomVersion.trim() =~ /(?i)\bv?(?<major>0|[1-9]\d*)(?:\.(?<minor>0|[1-9]\d*)(?:\.(?<patch>0|[1-9]\d*))?)?(?:-(?<prerelease>[\da-z\-]+(?:\.[\da-z\-]+)*))?(?:\+(?<build>[\da-z\-]+(?:\.[\da-z\-]+)*))?\b/
      if (pomSemver.matches()) {
        script.echo "Current POM version ${pomVersion} is semver compatible"

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
      script.echo "New version is ${newVersion}"
      return newVersion
    } else {
      script.echo "Version is not semver compatible"

      // strip the v prefix from the tag so we can use in a maven version number
      def previousReleaseVersion = tag.substring(tag.lastIndexOf('v') + 1)
      script.echo "Previous version found ${previousReleaseVersion}"

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
    script.echo "Not a release so dropping staging repo ${repoId}"
    script.sh "mvn org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:rc-drop -DserverId=oss-sonatype-staging -DnexusUrl=https://oss.sonatype.org -DstagingRepositoryId=${repoId} -Ddescription=\"Dry run\" -DstagingProgressTimeoutMinutes=60"
  }

  def helm() {
    def pluginVersion = getReleaseVersion("io/fabric8/fabric8-maven-plugin")
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
      script.echo "No previous version found"
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

  def createPullRequest(String message, String project, String branch) {
    def githubToken = getGitHubToken()
    def apiUrl = new URL("https://api.github.com/repos/${project}/pulls")
    script.echo "creating PR for ${apiUrl}"
    try {
      HttpURLConnection connection = apiUrl.openConnection()
      if (githubToken.length() > 0) {
        connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
      }
      connection.setRequestMethod("POST")
      connection.setDoOutput(true)
      connection.connect()

      def body = """
    {
      "title": "${message}",
      "head": "${branch}",
      "base": "master"
    }
    """
      script.echo "sending body: ${body}\n"

      OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())
      writer.write(body)
      writer.flush()

      // execute the POST request
      def rs = new JsonSlurper().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))

      connection.disconnect()

      script.echo "Received PR id:  ${rs.number}"
      return rs.number + ''

    } catch (err) {
      script.error "ERROR  ${err}"
    }
  }

  def closePR(project, id, newVersion, newPRID) {
    def githubToken = getGitHubToken()
    def apiUrl = new URL("https://api.github.com/repos/${project}/pulls/${id}")
    script.echo "deleting PR for ${apiUrl}"

    HttpURLConnection connection = apiUrl.openConnection()
    if (githubToken.length() > 0) {
      connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
    }
    connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.connect()

    def body = """
    {
      "state": "closed",
      "body": "Superseded by new version ${newVersion} #${newPRID}"
    }
    """
    script.echo "sending body: ${body}\n"

    OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())
    writer.write(body)
    writer.flush()

    // execute the PATCH     request
    def rs = new JsonSlurper().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))

    def code = connection.getResponseCode()

    if (code != 200) {
      script.error "${project} PR ${id} not merged.  ${connection.getResponseMessage()}"

    } else {
      script.echo "${project} PR ${id} ${rs.message}"
    }
    connection.disconnect()
  }

  def getIssueComments(project, id, githubToken = null) {
    if (!githubToken) {
      githubToken = getGitHubToken()
    }
    def apiUrl = new URL("https://api.github.com/repos/${project}/issues/${id}/comments")
    script.echo "getting comments for ${apiUrl}"

    def HttpURLConnection connection = apiUrl.openConnection()
    if (githubToken != null && githubToken.length() > 0) {
      connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
    }

    connection.setRequestMethod("GET")
    connection.setDoOutput(true)
    connection.connect()

    def rs = new JsonSlurper().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))

    def code = 0
    try {
      code = connection.getResponseCode()
      // } catch (org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException ex){
      //     script.echo "${ex} will try to continue"
    } finally {
      connection.disconnect()
    }

    if (code != 0 && code != 200) {
      script.error "Cannot get ${project} PR ${id} comments.  ${connection.getResponseMessage()}"
    }

    return rs
  }

  def waitUntilSuccessStatus(project, ref) {

    def githubToken = getGitHubToken()

    def apiUrl = new URL("https://api.github.com/repos/${project}/commits/${ref}/status")
    script.waitUntil {
      def HttpURLConnection connection = apiUrl.openConnection()
      if (githubToken != null && githubToken.length() > 0) {
        connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
      }

      connection.setRequestMethod("GET")
      connection.setDoOutput(true)
      connection.connect()

      def rs
      def code

      try {
        rs = new JsonSlurper().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))

        code = connection.getResponseCode()
      } catch (err) {
        script.echo "CI checks have not passed yet so waiting before merging"
      } finally {
        connection.disconnect()
      }

      if (rs == null) {
        script.echo "Error getting commit status, are CI builds enabled for this PR?"
        return false
      }
      if (rs != null && rs.state == 'success') {
        return true
      } else {
        script.echo "Commit status is ${rs.state}.  Waiting to merge"
        return false
      }
    }
  }

  def getGithubBranch(project, id, githubToken) {

    def apiUrl = new URL("https://api.github.com/repos/${project}/pulls/${id}")
    def HttpURLConnection connection = apiUrl.openConnection()
    if (githubToken != null && githubToken.length() > 0) {
      connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
    }

    connection.setRequestMethod("GET")
    connection.setDoOutput(true)
    connection.connect()
    try {
      def rs = new JsonSlurper().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))
      def branch = rs.head.ref
      script.echo "${branch}"
      return branch
    } catch (err) {
      script.echo "Error while fetching the github branch"
    } finally {
      if (connection) {
        connection.disconnect()
      }
    }
  }

  def mergePR(project, id) {
    def githubToken = getGitHubToken()
    def branch = getGithubBranch(project, id, githubToken)
    waitUntilSuccessStatus(project, branch)

    def apiUrl = new URL("https://api.github.com/repos/${project}/pulls/${id}/merge")

    def HttpURLConnection connection = apiUrl.openConnection()
    if (githubToken.length() > 0) {
      connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
    }
    connection.setRequestMethod("PUT")
    connection.setDoOutput(true)
    connection.connect()

    // execute the request
    def rs
    try {
      rs = new JsonSlurper().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))

      def code = connection.getResponseCode()

      if (code != 200) {
        if (code == 405) {
          script.error "${project} PR ${id} not merged.  ${rs.message}"
        } else {
          script.error "${project} PR ${id} not merged.  GitHub API Response code: ${code}"
        }
      } else {
        script.echo "${project} PR ${id} ${rs.message}"
      }
    } catch (err) {
      // if merge failed try to squash and merge
      connection = null
      rs = null
      squashAndMerge(project, id)
    } finally {
      if (connection) {
        connection.disconnect()
        connection = null
      }
      rs = null
    }
  }

  def squashAndMerge(project, id) {
    def githubToken = getGitHubToken()
    def apiUrl = new URL("https://api.github.com/repos/${project}/pulls/${id}/merge")

    def HttpURLConnection connection = apiUrl.openConnection()
    if (githubToken.length() > 0) {
      connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
    }
    connection.setRequestMethod("PUT")
    connection.setDoOutput(true)
    connection.connect()
    def body = "{\"merge_method\":\"squash\"}"

    def rs
    try {
      OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())
      writer.write(body)
      writer.flush()

      rs = new JsonSlurper().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))
      def code = connection.getResponseCode()

      if (code != 200) {
        if (code == 405) {
          script.error "${project} PR ${id} not merged.  ${rs.message}"
        } else {
          script.error "${project} PR ${id} not merged.  GitHub API Response code: ${code}"
        }
      } else {
        script.echo "${project} PR ${id} ${rs.message}"
      }
    } finally {
      connection.disconnect()
      connection = null
      rs = null
    }
  }

  def addCommentToPullRequest(comment, pr, project) {
    def githubToken = getGitHubToken()
    def apiUrl = new URL("https://api.github.com/repos/${project}/issues/${pr}/comments")
    script.echo "adding ${comment} to ${apiUrl}"
    try {
      def HttpURLConnection connection = apiUrl.openConnection()
      if (githubToken.length() > 0) {
        connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
      }
      connection.setRequestMethod("POST")
      connection.setDoOutput(true)
      connection.connect()

      def body = "{\"body\":\"${comment}\"}"

      OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())
      writer.write(body)
      writer.flush()

      // execute the POST request
      new InputStreamReader(connection.getInputStream())

      connection.disconnect()
    } catch (err) {
      script.error "ERROR  ${err}"
    }
  }

  def addMergeCommentToPullRequest(String pr, String project) {
    def githubToken = getGitHubToken()
    def apiUrl = new URL("https://api.github.com/repos/${project}/issues/${pr}/comments")
    script.echo "merge PR using comment sent to ${apiUrl}"
    try {
      def HttpURLConnection connection = apiUrl.openConnection()
      if (githubToken.length() > 0) {
        connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
      }
      connection.setRequestMethod("POST")
      connection.setDoOutput(true)
      connection.connect()

      def body = '{"body":"[merge]"}'

      OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())
      writer.write(body)
      writer.flush()

      // execute the POST request
      new InputStreamReader(connection.getInputStream())

      connection.disconnect()
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

  def isAuthorCollaborator(githubToken, project) {

    if (!githubToken) {

      githubToken = getGitHubToken()

      if (!githubToken) {
        script.echo "No GitHub api key found so trying annonynous GitHub api call"
      }
    }
    if (!project) {
      project = getGitHubProject()
    }

    def changeAuthor = script.getProperty('env').CHANGE_AUTHOR
    if (!changeAuthor) {
      script.error "No commit author found.  Is this a pull request pipeline?"
    }
    script.echo "Checking if user ${changeAuthor} is a collaborator on ${project}"

    def apiUrl = new URL("https://api.github.com/repos/${project}/collaborators/${changeAuthor}")

    def HttpURLConnection connection = apiUrl.openConnection()
    if (githubToken != null && githubToken.length() > 0) {
      connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
    }
    connection.setRequestMethod("GET")
    connection.setDoOutput(true)

    try {
      connection.connect()
      new InputStreamReader(connection.getInputStream(), "UTF-8")
      return true
    } catch (FileNotFoundException e1) {
      return false
    } finally {
      connection.disconnect()
    }

    script.error "Error checking if user ${changeAuthor} is a collaborator on ${project}.  GitHub API Response code: ${code}"

  }

  def getUrlAsString(urlString) {

    def url = new URL(urlString)
    def scan
    def response
    script.echo "getting string from URL: ${url}"
    try {
      scan = new Scanner(url.openStream(), "UTF-8")
      response = scan.useDelimiter("\\A").next()
    } finally {
      scan.close()
    }
    return response
  }


  def drop(String pr, String project) {
    def githubToken = getGitHubToken()
    def apiUrl = new URL("https://api.github.com/repos/${project}/pulls/${pr}")
    def branch
    HttpURLConnection connection
    OutputStreamWriter writer
    script.echo "closing PR ${apiUrl}"

    try {
      connection = apiUrl.openConnection()
      if (githubToken.length() > 0) {
        connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
      }
      connection.setRequestMethod("POST")
      connection.setDoOutput(true)
      connection.connect()

      def body = '''
    {
      "body": "release aborted",
      "state": "closed"
    }
    '''

      writer = new OutputStreamWriter(connection.getOutputStream())
      writer.write(body)
      writer.flush()

      // execute the POST request
      def rs = new JsonSlurper().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))

      connection.disconnect()

      branchName = rs.head.ref

    } catch (err) {
      script.error "ERROR  ${err}"
    }

    try {
      apiUrl = new URL("https://api.github.com/repos/${project}/git/refs/heads/${branchName}")
      connection = apiUrl.openConnection()
      if (githubToken.length() > 0) {
        connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
      }
      connection.setRequestMethod("DELETE")
      connection.setDoOutput(true)
      connection.connect()

      writer = new OutputStreamWriter(connection.getOutputStream())
      writer.write(body)
      writer.flush()

      // execute the POST request
      new InputStreamReader(connection.getInputStream())

      connection.disconnect()

    } catch (err) {
      script.error "ERROR  ${err}"
    }
  }

  def deleteRemoteBranch(String branchName, containerName) {
    script.container(name: containerName) {
      script.sh 'chmod 600 /root/.ssh-git/ssh-key'
      script.sh 'chmod 600 /root/.ssh-git/ssh-key.pub'
      script.sh 'chmod 700 /root/.ssh-git'
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
            script.echo "OpenShift YAML contains an ImageStream"
            return true
          } else {
            script.echo "OpenShift YAML does not contain an ImageStream so not using S2I binary mode"
          }
        }
      } else {
        script.echo "Warning OpenShift YAML ${openshiftYaml} does not exist!"
      }
    } catch (e) {
      script.error "Failed to load ${openshiftYaml[0]} due to ${e}"
    }
    return false
  }

/**
 * Deletes the given namespace if it exists
 *
 * @param name the name of the namespace
 * @return true if the delete was successful
 */
  @NonCPS
  def deleteNamespace(String name) {
    KubernetesClient kubernetes = new DefaultKubernetesClient()
    try {
      def namespace = kubernetes.namespaces().withName(name).get()
      if (namespace != null) {
        script.echo "Deleting namespace ${name}..."
        kubernetes.namespaces().withName(name).delete()
        script.echo "Deleted namespace ${name}"

        // TODO should we wait for the namespace to really go away???
        namespace = kubernetes.namespaces().withName(name).get()
        if (namespace != null) {
          script.echo "Namespace ${name} still exists!"
        }
        return true
      }
      return false
    } catch (e) {
      // ignore errors
      return false
    }
  }

  @NonCPS
  def isOpenShift() {
    return new DefaultOpenShiftClient().isAdaptable(OpenShiftClient.class)
  }

  @NonCPS
  def getCloudConfig() {
    def openshiftCloudConfig = Jenkins.getInstance().getCloud('openshift')
    return (openshiftCloudConfig) ? 'openshift' : 'kubernetes'
  }

/**
 * Should be called after checkout scm
 */
  @NonCPS
  def getScmPushUrl() {
    def url = script.sh(returnStdout: true, script: 'git config --get remote.origin.url').trim()

    if (!url) {
      script.error "no URL found for git config --get remote.origin.url "
    }
    return url
  }

  @NonCPS
  def openShiftImageStreamExists(String name) {
    if (isOpenShift()) {
      try {
        def result = sh(returnStdout: true, script: 'oc describe is ${name} --namespace openshift')
        if (result && result.contains(name)) {
          script.echo "ImageStream  ${name} is already installed globally"
          return true;
        } else {
          //see if its already in our namespace
          def namespace = kubernetes.getNamespace();
          result = script.sh(returnStdout: true, script: 'oc describe is ${name} --namespace ${namespace}')
          if (result && result.contains(name)) {
            script.echo "ImageStream  ${name} is already installed in project ${namespace}"
            return true;
          }
        }
      } catch (e) {
        script.echo "Warning: ${e} "
      }
    }
    return false;
  }

  @NonCPS
  def openShiftImageStreamInstall(String name, String location) {
    if (openShiftImageStreamExists(name)) {
      script.echo "ImageStream ${name} does not exist - installing ..."
      try {
        def result = script.sh(returnStdout: true, script: 'oc create -f  ${location}')
        def namespace = kubernetes.getNamespace();
        script.echo "ImageStream ${name} now installed in project ${namespace}"
        return true;
      } catch (e) {
        script.echo "Warning: ${e} "
      }
    }
    return false;
  }

  @NonCPS
  def dockerRegistryPrefix() {
    def registryHost = dockerRegistryHostAndPort(null)
    def registryPrefix = ""
    if (registryHost) {
      registryPrefix = "${registryHost}/"
    }
    return registryPrefix
  }

  @NonCPS
  def dockerRegistryHostAndPort(String defaultRegistryHost = "fabric8-docker-registry") {
    def registryHost = script.getProperty('env').FABRIC8_DOCKER_REGISTRY_SERVICE_HOST
    if (!registryHost) {
      script.echo "WARNING you don't seem to be running the fabric8-docker-registry service!!!"
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
    println "CHAT: ${room}: ${message}"
  }

/** Invokes a step extension on the given closure body */
  def doStepExecution(StepExtension stepExtension, body) {
    if (stepExtension == null) {
      stepExtension = new StepExtension()
    }
    if (stepExtension.preBlock instanceof Closure) {
      println "StepExtension invoking pre steps"
      invokeStepBlock(stepExtension.preBlock)
    }
    def answer
    if (stepExtension.stepsBlock instanceof Closure) {
      println "StepExtension invoking replacement steps"
      answer = invokeStepBlock(stepExtension.stepsBlock)
    } else if (body != null) {
      if (stepExtension.disabled) {
        println "StepExtension has disabled the steps"
      } else {
        answer = body()
      }
    }
    if (stepExtension.postBlock instanceof Closure) {
      println "StepExtension invoking post steps"
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