package org.jenkinsci.plugins.jx.pipelines.dsl

import groovy.json.JsonSlurper
import groovy.transform.Canonical
import hudson.model.TaskListener
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.openshift.client.DefaultOpenShiftClient
import io.fabric8.openshift.client.OpenShiftClient
import jenkins.model.Jenkins
import org.apache.commons.lang.exception.ExceptionUtils
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.cps.CpsThread

import javax.annotation.Nonnull


class JXDSLUtils {

    @Nonnull
    static CpsThread getCpsThread() {
        CpsThread c = CpsThread.current()
        if (c == null) {
            throw new IllegalStateException("Expected to be called from CpsThread")
        }
        return c
    }

    @Nonnull
    static TaskListener getListener() {
        CpsThread thread = getCpsThread()
        CpsFlowExecution execution = thread.getExecution()
        TaskListener listener = TaskListener.NULL

        if (execution?.getOwner()?.getListener() != null) {
            try {
                listener = execution.getOwner().getListener()
            } catch (IOException e) {
                throw new IllegalStateException("Can't get task listener for run: " + e, e)
            }
        }

        return listener
    }

    /**
     * Echoes the given message to the general Jenkins console for the run. Note that this will not prepend with the current
     * parallel branch, unlike the echo step, so we may end up wanting to revert to using the echo step.
     *
     * @param msg
     */
    @Whitelisted
    static void echo(String msg) {
        getListener().getLogger().println(msg)
    }

    /**
     * Deletes the given namespace if it exists
     *
     * @param name the name of the namespace
     * @return true if the delete was successful
     */
    @Whitelisted
    static boolean deleteNamespace(@Nonnull String name) {
        KubernetesClient kubernetes = new DefaultKubernetesClient()
        try {
            Namespace namespace = kubernetes.namespaces().withName(name).get()
            if (namespace != null) {
                echo("Deleting namespace " + name + "...")
                kubernetes.namespaces().withName(name).delete()
                echo("Deleted namespace " + name)

                // TODO should we wait for the namespace to really go away???
                namespace = kubernetes.namespaces().withName(name).get()
                if (namespace != null) {
                    echo("Namespace " + name + " still exists!")
                }
                return true
            }
            return false
        } catch (Exception e) {
            // ignore errors
            return false
        }
    }

    @Whitelisted
    static boolean isOpenShift() {
        return new DefaultOpenShiftClient().isAdaptable(OpenShiftClient.class)
    }

    @Whitelisted
    static String getCloudConfig() {
        if (Jenkins.getInstance().getCloud("openshift") != null) {
            return "openshift"
        } else {
            return "kubernetes"
        }
    }

    @Whitelisted
    static String getReleaseVersion(String artifact) {
        def modelMetaData = new XmlSlurper().parse("https://oss.sonatype.org/content/repositories/releases/${artifact}/maven-metadata.xml")
        def version = modelMetaData.versioning.release.text()
        return version
    }

    @Whitelisted
    static String getMavenCentralVersion(String artifact) {
        def modelMetaData = new XmlSlurper().parse("http://central.maven.org/maven2/${artifact}/maven-metadata.xml")
        def version = modelMetaData.versioning.release.text()
        return version
    }

    @Whitelisted
    static String getVersion(String repo, String artifact) {
        repo = removeTrailingSlash(repo)
        artifact = removeTrailingSlash(artifact)

        def modelMetaData = new XmlSlurper().parse(repo + '/' + artifact + '/maven-metadata.xml')
        def version = modelMetaData.versioning.release.text()
        return version
    }

    @Whitelisted
    static String removeTrailingSlash(String myString) {
        if (myString.endsWith("/")) {
            return myString.substring(0, myString.length() - 1)
        }
        return myString
    }

    @Whitelisted
    static boolean isArtifactAvailableInRepo(String repo, String groupId, String artifactId, String version, String ext) {
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
            echo("File not yet available: ${url.toString()}")
            return false
        } finally {
            connection.disconnect()
        }
    }

    @Whitelisted
    static boolean isFileAvailableInRepo(String repo, String path, String version, String artifact) {
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
            echo("File is available at: ${url.toString()}")
            return true
        } catch (FileNotFoundException e1) {
            echo("File not yet available: ${url.toString()}")
            return false
        } finally {
            connection.disconnect()
        }
    }

    @Whitelisted
    static String getFullStackTrace(Throwable t) {
        return ExceptionUtils.getFullStackTrace(t)
    }

    @Whitelisted
    static String getDockerHubImageTags(String image) {
        try {
            return "https://registry.hub.docker.com/v1/repositories/${image}/tags".toURL().getText()
        } catch (err) {
            return "NO_IMAGE_FOUND"
        }
    }

    @Canonical
    private static final class GitHubResponse implements Serializable {
        private static final long serialVersionUID = 1

        Object result
        int responseCode
        String responseMessage
    }

    @Nonnull
    private static GitHubResponse talkToGitHub(@Nonnull String method, @Nonnull String githubToken, @Nonnull URL apiUrl, String body = null) {
        GitHubResponse resp = new GitHubResponse()
        HttpURLConnection connection = apiUrl.openConnection()
        if (githubToken.length() > 0) {
            connection.setRequestProperty("Authorization", "Bearer ${githubToken}")
        }
        if (method == "PATCH") {
            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            connection.setRequestMethod("POST")
        } else {
            connection.setRequestMethod(method)
        }
        connection.setDoOutput(true)
        connection.connect()

        try {
            if (body != null) {
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())
                writer.write(body)
                writer.flush()
            }
            
            // execute the POST request
            resp.result = new JsonSlurper().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))
            resp.responseCode = connection.responseCode
            resp.responseMessage = connection.responseMessage
        } finally {
            connection.disconnect()
        }
        return resp
    }
    
    @Whitelisted
    static String createPullRequest(String message, String project, String branch, String githubToken) throws Exception {
        def apiUrl = new URL("https://api.github.com/repos/${project}/pulls")
        echo "creating PR for ${apiUrl}"

        def body = """
    {
      "title": "${message}",
      "head": "${branch}",
      "base": "master"
    }
    """

        echo "sending body: ${body}\n"

        def rs = talkToGitHub("POST", githubToken, apiUrl, body)

        echo "Received PR id:  ${rs.result.number}"
        return rs.result.number + ''

    }

    @Whitelisted
    static void closePR(project, id, newVersion, newPRID, String githubToken) throws Exception {
        def apiUrl = new URL("https://api.github.com/repos/${project}/pulls/${id}")
        echo "deleting PR for ${apiUrl}"

        def body = """
    {
      "state": "closed",
      "body": "Superseded by new version ${newVersion} #${newPRID}"
    }
    """
        echo "sending body: ${body}\n"

        GitHubResponse rs = talkToGitHub("PATCH", githubToken, apiUrl, body)
        if (rs.responseCode != 200) {
            throw new IllegalStateException("${project} PR ${id} not merged.  ${rs.getResponseMessage()}")

        } else {
            echo "${project} PR ${id} ${rs.result.message}"
        }
    }

    @Whitelisted
    static String getIssueComments(project, id, String githubToken) {
        def apiUrl = new URL("https://api.github.com/repos/${project}/issues/${id}/comments")
        echo "getting comments for ${apiUrl}"

        GitHubResponse rs = talkToGitHub("GET", githubToken, apiUrl)

        if (rs.responseCode != 0 && rs.responseCode != 200) {
            throw new IllegalStateException("Cannot get ${project} PR ${id} comments.  ${rs.getResponseMessage()}")
        }

        return rs.result
    }

    @Whitelisted
    static boolean checkIfCommitIsSuccessful(project, ref, String githubToken) {
        def apiUrl = new URL("https://api.github.com/repos/${project}/commits/${ref}/status")

        GitHubResponse rs
        try {
            rs = talkToGitHub("GET", githubToken, apiUrl)
        } catch (err) {
            echo "CI checks have not passed yet so waiting before merging"
        }

        if (rs == null) {
            echo "Error getting commit status, are CI builds enabled for this PR?"
            return false
        }
        if (rs != null && rs.result.state == 'success') {
            return true
        } else {
            echo "Commit status is ${rs.result.state}.  Waiting to merge"
            return false
        }
    }

    @Whitelisted
    static String getGithubBranch(project, id, String githubToken) {
        def apiUrl = new URL("https://api.github.com/repos/${project}/pulls/${id}")

        try {
            GitHubResponse rs = talkToGitHub("GET", githubToken, apiUrl)
            def branch = rs.result.head.ref
            echo "${branch}"
            return branch
        } catch (err) {
            echo "Error while fetching the github branch"
        }
    }

    @Whitelisted
    static void mergePR(project, id, String githubToken) throws Exception {
        def apiUrl = new URL("https://api.github.com/repos/${project}/pulls/${id}/merge")

        // execute the request
        GitHubResponse rs = talkToGitHub("PUT", githubToken, apiUrl)

        if (rs.responseCode != 200) {
            if (rs.responseCode == 405) {
                throw new IllegalStateException("${project} PR ${id} not merged.  ${rs.result.message}")
            } else {
                throw new IllegalStateException("${project} PR ${id} not merged.  GitHub API Response code: ${rs.responseCode}")
            }
        } else {
            echo "${project} PR ${id} ${rs.result.message}"
        }
    }

    @Whitelisted
    static void squashAndMerge(project, id, String githubToken) throws Exception {
        def apiUrl = new URL("https://api.github.com/repos/${project}/pulls/${id}/merge")

        def body = "{\"merge_method\":\"squash\"}"

        def rs = talkToGitHub("POST", githubToken, apiUrl, body)

        if (rs.responseCode != 200) {
            if (rs.responseCode == 405) {
                throw new IllegalStateException("${project} PR ${id} not merged.  ${rs.result.message}")
            } else {
                throw new IllegalStateException("${project} PR ${id} not merged.  GitHub API Response code: ${rs.responseCode}")
            }
        } else {
            echo "${project} PR ${id} ${rs.result.message}"
        }
    }

    @Whitelisted
    static void addCommentToPullRequest(comment, pr, project, String githubToken) {
        def apiUrl = new URL("https://api.github.com/repos/${project}/issues/${pr}/comments")
        echo "adding ${comment} to ${apiUrl}"

        def body = "{\"body\":\"${comment}\"}"

        talkToGitHub("POST", githubToken, apiUrl, body)
    }

    @Whitelisted
    static void addMergeCommentToPullRequest(String pr, String project, String githubToken) {
        def apiUrl = new URL("https://api.github.com/repos/${project}/issues/${pr}/comments")
        echo "merge PR using comment sent to ${apiUrl}"

        def body = '{"body":"[merge]"}'

        talkToGitHub("POST", githubToken, apiUrl, body)
    }

    @Whitelisted
    static boolean isAuthorCollaborator(String changeAuthor, String githubToken, project) throws Exception {

        if (!githubToken) {
            echo "No GitHub api key found so trying annonynous GitHub api call"
        }

        if (!changeAuthor) {
            throw new IllegalStateException("No commit author found.  Is this a pull request pipeline?")
        }
        echo "Checking if user ${changeAuthor} is a collaborator on ${project}"

        def apiUrl = new URL("https://api.github.com/repos/${project}/collaborators/${changeAuthor}")

        try {
            GitHubResponse rs = talkToGitHub("GET", githubToken, apiUrl)
            if (rs.responseCode != 200) {
                return false
            } else {
                return true
            }
        } catch (FileNotFoundException e1) {
            return false
        } catch (Exception e) {
            throw new IllegalStateException("Error checking if user ${changeAuthor} is a collaborator on ${project}.")
        }
    }

    @Whitelisted
    static String getUrlAsString(urlString) {

        def url = new URL(urlString)
        def scan
        def response
        echo "getting string from URL: ${url}"
        try {
            scan = new Scanner(url.openStream(), "UTF-8")
            response = scan.useDelimiter("\\A").next()
        } finally {
            scan.close()
        }
        return response
    }

    @Whitelisted
    static void drop(String pr, String project, String githubToken) throws Exception {
        def apiUrl = new URL("https://api.github.com/repos/${project}/pulls/${pr}")
        def branchName
        echo "closing PR ${apiUrl}"


        def body = '''
    {
      "body": "release aborted",
      "state": "closed"
    }
    '''

        def rs = talkToGitHub("POST", githubToken, apiUrl, body)
        
        branchName = rs.result.head.ref

        apiUrl = new URL("https://api.github.com/repos/${project}/git/refs/heads/${branchName}")
        talkToGitHub("DELETE", githubToken, apiUrl, body)
    }
}
