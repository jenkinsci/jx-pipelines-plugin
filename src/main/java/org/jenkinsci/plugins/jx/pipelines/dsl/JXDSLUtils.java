package org.jenkinsci.plugins.jx.pipelines.dsl;

import hudson.model.TaskListener;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.CpsThread;

import javax.annotation.Nonnull;
import java.io.IOException;

public class JXDSLUtils {

    @Nonnull
    public static CpsThread getCpsThread() {
        CpsThread c = CpsThread.current();
        if (c == null) {
            throw new IllegalStateException("Expected to be called from CpsThread");
        }
        return c;
    }

    @Nonnull
    public static TaskListener getListener() {
        CpsThread thread = getCpsThread();
        CpsFlowExecution execution = thread.getExecution();
        TaskListener listener = TaskListener.NULL;

        if (execution != null && execution.getOwner() != null) {
            try {
                listener = execution.getOwner().getListener();
            } catch (IOException e) {
                throw new IllegalStateException("Can't get task listener for run: " + e, e);
            }
        }

        return listener;
    }

    /**
     * Deletes the given namespace if it exists
     *
     * @param name the name of the namespace
     * @return true if the delete was successful
     */
    @Whitelisted
    public static boolean deleteNamespace(@Nonnull String name) {
        TaskListener listener = getListener();
        KubernetesClient kubernetes = new DefaultKubernetesClient();
        try {
            Namespace namespace = kubernetes.namespaces().withName(name).get();
            if (namespace != null) {
                listener.getLogger().println("Deleting namespace " + name + "...");
                kubernetes.namespaces().withName(name).delete();
                listener.getLogger().println("Deleted namespace " + name);

                // TODO should we wait for the namespace to really go away???
                namespace = kubernetes.namespaces().withName(name).get();
                if (namespace != null) {
                    listener.getLogger().println("Namespace " + name + " still exists!");
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            // ignore errors
            return false;
        }
    }

    @Whitelisted
    public static boolean isOpenShift() {
        return new DefaultOpenShiftClient().isAdaptable(OpenShiftClient.class);
    }

    @Whitelisted
    public static String getCloudConfig() {
        if (Jenkins.getInstance().getCloud("openshift") != null) {
            return "openshift";
        } else {
            return "kubernetes";
        }
    }
}
