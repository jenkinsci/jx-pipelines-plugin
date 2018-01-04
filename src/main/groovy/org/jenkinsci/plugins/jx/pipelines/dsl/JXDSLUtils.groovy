package org.jenkinsci.plugins.jx.pipelines.dsl

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
        TaskListener listener = getListener()
        KubernetesClient kubernetes = new DefaultKubernetesClient()
        try {
            Namespace namespace = kubernetes.namespaces().withName(name).get()
            if (namespace != null) {
                listener.getLogger().println("Deleting namespace " + name + "...")
                kubernetes.namespaces().withName(name).delete()
                listener.getLogger().println("Deleted namespace " + name)

                // TODO should we wait for the namespace to really go away???
                namespace = kubernetes.namespaces().withName(name).get()
                if (namespace != null) {
                    listener.getLogger().println("Namespace " + name + " still exists!")
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
            getListener().getLogger().println("File not yet available: ${url.toString()}")
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
            getListener().getLogger().println("File is available at: ${url.toString()}")
            return true
        } catch (FileNotFoundException e1) {
            getListener().getLogger().println("File not yet available: ${url.toString()}")
            return false
        } finally {
            connection.disconnect()
        }
    }

    @Whitelisted
    static String getFullStackTrace(Throwable t) {
        return ExceptionUtils.getFullStackTrace(t)
    }

}
