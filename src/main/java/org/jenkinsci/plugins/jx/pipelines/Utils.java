/**
 * Copyright (C) Original Authors 2017
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.jx.pipelines;

import com.cloudbees.groovy.cps.NonCPS;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.environments.Environments;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.pipelines.PipelineConfiguration;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamStatus;
import io.fabric8.openshift.api.model.NamedTagEventList;
import io.fabric8.openshift.api.model.TagEvent;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.utils.Strings;
import org.jenkinsci.plugins.jx.pipelines.helpers.GitHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
import org.csanchez.jenkins.plugins.kubernetes.PodAnnotation;
*/

public class Utils extends CommandSupport {
    public static final String CLIENTS = "clients";
    private static final long serialVersionUID = 1L;
    private String branch;

    public Utils() {
    }

    public Utils(CommandSupport parent) {
        super(parent);
    }

    public static String defaultNamespace(KubernetesClient kubernetesClient) {
        String namespace = kubernetesClient.getNamespace();
        if (Strings.isNullOrBlank(namespace)) {
            namespace = KubernetesHelper.defaultNamespace();
        }
        if (Strings.isNullOrBlank(namespace)) {
            namespace = KubernetesHelper.defaultNamespace();
        }
        if (Strings.isNullOrBlank(namespace)) {
            namespace = "default";
        }
        return namespace;
    }

    public static KubernetesClient createKubernetesClient() {
        return new DefaultKubernetesClient();
    }

    public static String getNamespace() {
        return defaultNamespace(createKubernetesClient());
    }

    public void clearCache() {
        this.branch = null;
    }

    public String environmentNamespace(final String environment) {
        KubernetesClient kubernetesClient = createKubernetesClient();
        String answer = Environments.namespaceForEnvironment(kubernetesClient, environment, defaultNamespace(kubernetesClient));
        if (Strings.notEmpty(answer)) {
            return answer;
        }

        String ns = getNamespace();
        if (ns.endsWith("-jenkins")) {
            ns = ns.substring(0, ns.lastIndexOf("-jenkins"));
        }

        return ns + "-" + environment.toLowerCase();
    }

    /**
     * Loads the environments in the default user namespace
     */
    public Environments environments() {
        KubernetesClient kubernetesClient = createKubernetesClient();
        return Environments.load(kubernetesClient, defaultNamespace(kubernetesClient));
    }

    /**
     * Loads the environments from the given namespace
     */
    public Environments environments(String namespace) {
        KubernetesClient kubernetesClient = createKubernetesClient();
        return Environments.load(kubernetesClient, namespace);
    }

    /**
     * Loads the environments from the user namespace
     */
    public PipelineConfiguration pipelineConfiguration() {
        KubernetesClient kubernetesClient = createKubernetesClient();
        return PipelineConfiguration.loadPipelineConfiguration(kubernetesClient, defaultNamespace(kubernetesClient));
    }

    /**
     * Loads the environments from the given namespace
     */
    public PipelineConfiguration pipelineConfiguration(String namespace) {
        KubernetesClient kubernetesClient = createKubernetesClient();
        return PipelineConfiguration.loadPipelineConfiguration(kubernetesClient, namespace);
    }

    /**
     * Returns true if the integration tests should be disabled
     */
    public boolean isDisabledITests() {
        boolean answer = false;
        try {
            final PipelineConfiguration config = pipelineConfiguration();
            echo("Loaded PipelineConfiguration " + config);
            if (isCD()) {
                answer = config.isDisableITestsCD();
            } else if (isCI()) {
                answer = config.isDisableITestsCI();
            }
        } catch (Exception e) {
            echo("WARNING: Failed to find the flag on the PipelineConfiguration object - probably due to the jenkins plugin `kubernetes-pipeline-plugin` version: " + e);
            e.printStackTrace();
        }
        //answer = true;
        return answer;
    }

    /**
     * Returns true if we should use S2I to build docker images
     */
    public boolean isUseOpenShiftS2IForBuilds() {
        return !isUseDockerSocket();
    }

    /**
     * Returns true if the current cluster can support S2I
     */
    public boolean supportsOpenShiftS2I() {
        OpenShiftClient client = new DefaultOpenShiftClient();
        return client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.IMAGE);
    }

    /**
     * Returns true if we should mount the docker socket for docker builds
     */
    public boolean isUseDockerSocket() {
        final PipelineConfiguration config = pipelineConfiguration();
        echo("Loaded PipelineConfiguration " + config);

        Boolean flag = config.getUseDockerSocketFlag();
        if (flag != null) {
            return flag.booleanValue();
        }
        return supportsOpenShiftS2I() ? false : true;
    }

    public String getDockerRegistry() {
        String externalDockerRegistryURL = getUsersPipelineConfig("external-docker-registry-url");
        if (Strings.notEmpty(externalDockerRegistryURL)) {
            return externalDockerRegistryURL;
        }


        // fall back to the old < 4.x when the registry was in the same namespace
        String registryHost = getenv("FABRIC8_DOCKER_REGISTRY_SERVICE_HOST");
        String registryPort = getenv("FABRIC8_DOCKER_REGISTRY_SERVICE_PORT");
        if (Strings.isNullOrBlank(registryHost) || Strings.isNullOrBlank(registryPort)) {
            error("No external-docker-registry-url found in Jenkins configmap or no FABRIC8_DOCKER_REGISTRY_SERVICE_HOST FABRIC8_DOCKER_REGISTRY_SERVICE_PORT environment variables");
        }
        return registryHost + ":" + registryPort;
    }

    public String getUsersPipelineConfig(final String k) {
        // first lets check if we have the new pipelines configmap in the users home namespace
        KubernetesClient client = new DefaultKubernetesClient();
        final String ns = getUsersNamespace();
        ConfigMap r = client.configMaps().inNamespace(ns).withName("fabric8-pipelines").get();
        if (r == null) {
            error("no fabric8-pipelines configmap found in namespace " + ns);
            return null;
        }

        Map<String, String> d = r.getData();
        if (d != null) {
            echo("looking for key " + k + " in " + ns + "/fabric8-pipelines configmap");
            return d.get(k);
        }
        return null;
    }

    public String getConfigMap(String ns, final String cm, String key) {

        // first lets check if we have the new pipeliens configmap in the users home namespace
        KubernetesClient client = new DefaultKubernetesClient();

        ConfigMap r = client.configMaps().inNamespace(ns).withName(cm).get();
        if (r == null) {
            error("no " + cm + " configmap found in namespace " + ns);
            return null;
        }
        Map<String, String> data = r.getData();
        if (data != null && Strings.notEmpty(key)) {
            return data.get(key);
        }
        return null;
    }

    private Map<String, String> parseConfigMapData(final String input) {
        final Map<String, String> map = new HashMap<String, String>();
        for (String pair : input.split("\n")) {
            String[] kv = pair.split(":");
            if (kv.length > 1) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    public String getImageStreamSha(Object imageStreamName) {
        OpenShiftClient oc = new DefaultOpenShiftClient();
        return findTagSha(oc, (String) imageStreamName, getNamespace());
    }

    public String findTagSha(OpenShiftClient client, final String imageStreamName, String namespace) {
        Object currentImageStream = null;
        for (int i = 0; i < 15; i++) {
            if (i > 0) {
                echo("Retrying to find tag on ImageStream " + imageStreamName);
            }
            ;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                echo("interrupted " + e);
            }
            ;
            currentImageStream = client.imageStreams().withName(imageStreamName).get();
            if (currentImageStream == null) {
                continue;
            }

            ImageStreamStatus status = ((ImageStream) currentImageStream).getStatus();
            if (status == null) {
                continue;
            }

            List<NamedTagEventList> tags = status.getTags();
            if (tags == null || tags.isEmpty()) {
                continue;
            }

            // latest tag is the first
            TAG_EVENT_LIST:
            for (NamedTagEventList list : tags) {
                List<TagEvent> items = list.getItems();
                if (items == null) {
                    continue TAG_EVENT_LIST;
                }

                // latest item is the first
                for (TagEvent item : items) {
                    String image = item.getImage();
                    if (image != null && !image.equals("")) {
                        echo("Found tag on ImageStream " + imageStreamName + " tag: " + image);
                        return image;
                    }

                }

            }

            // No image found, even after several retries:
            if (currentImageStream == null) {
                error("Could not find a current ImageStream with name " + imageStreamName + " in namespace " + namespace);
                return null;
            } else {
                error("Could not find a tag in the ImageStream " + imageStreamName);
                return null;
            }
        }
        return null;
    }

    public String getUsersNamespace() {
        String usersNamespace = getNamespace();
        if (usersNamespace.endsWith("-jenkins")) {
            usersNamespace = usersNamespace.substring(0, usersNamespace.lastIndexOf("-jenkins"));
        }
        return usersNamespace;
    }


    public String getBranch() {
        if (branch == null) {
            branch = getenv("BRANCH_NAME");
            if (Strings.isNullOrBlank(branch)) {
                try {
                    echo("output of git --version: " + containerShOutput(CLIENTS, "git --version"));
                    echo("pwd: " + containerShOutput(CLIENTS, "pwd"));
                } catch (Throwable e) {
                    error("Failed to invoke git --version: " + e, e);
                }
                try {
                    branch = containerShOutput(CLIENTS, "git ls-remote --heads origin | grep $(git rev-parse HEAD) | cut -d / -f 3").trim();
                } catch (Throwable e) {
                    error("\nUnable to get git branch: " + e, e);
                }
            }
            if (Strings.isNullOrBlank(branch)) {
                try {
                    branch = containerShOutput(CLIENTS, "git symbolic-ref --short HEAD").trim();
                } catch (Throwable e) {
                    error("\nUnable to get git branch and in a detached HEAD. You may need to select Pipeline additional behaviour and \'Check out to specific local branch\': " + e, e);
                }
            }
            echo("current git branch is " + branch);
        }
        return branch;

    }

    public void setBranch(String branch) {
        echo("the current git branch is " + branch);
        this.branch = branch;
    }

    public boolean isCI() {
        String branch = getBranch();
        return Strings.notEmpty(branch) && branch.startsWith("PR-");
    }

    public Boolean isCD() {
        String branch = getBranch();
        return Strings.notEmpty(branch) && branch.equalsIgnoreCase("master");
    }


    public Object getLatestVersionFromTag() throws IOException {
        sh("git fetch --tags");
        sh("git config versionsort.prereleaseSuffix -RC");
        sh("git config versionsort.prereleaseSuffix -M");

        // if the repo has no tags this command will fail
        try {
            String answer = shOutput("git tag --sort version:refname | tail -1").trim();
            if (Strings.isNullOrBlank(answer)) {
                error("no release tag found");
            } else if (answer.startsWith("v")) {
                return answer.substring(1);
            }
            return answer;
        } catch (Exception err) {
            error("Failed to query tags from git: " + err);
            return null;
        }
    }


    public String getRepoName() {
        String jobName = getenv("JOB_NAME");
        int firstIdx = jobName.indexOf('/');
        int lastIdx = jobName.lastIndexOf('/');
        if (firstIdx > 0 && lastIdx > 0 && firstIdx != lastIdx) {
            // job name from the org plugin
            return jobName.substring(firstIdx + 1, lastIdx);
        } else if (lastIdx > 0) {
            // job name from the branch plugin
            return jobName.substring(0, lastIdx);
        } else {
            // normal job name
            return jobName;
        }
    }

    public String findGitCloneURL() {
        String text = getGitConfigFile(getCurrentDir());
        if (Strings.isNullOrBlank(text)) {
            text = doReadPath(".git/config");
        }
        System.out.println("\nfindGitCloneURL() text: " + text);
        if (Strings.notEmpty(text)) {
            return GitHelper.extractGitUrl(text);
        }
        return null;
    }


    protected String getGitConfigFile(File dir) {
        String path = new File(dir, ".git/config").getAbsolutePath();
        String text = doReadPath(path);
/*
        String command = "cat " + path;
        try {
            echo("trying: " + command);
            text = containerShOutput(CLIENTS, command);

            echo("result: " + text);
        } catch (Throwable e) {
            error("Failed to invoke `" + command + "` due to " + e, e);
        }
*/
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

    private String doReadPath(String path) {
        String text = null;
        try {
            echo("Utils.doReadPath " + path);
            text = readFile(path);
            echo("Utils.doReadPath " + path + " = " + text);
        } catch (Exception e) {
            error("Failed to read path " + path + " due to " + e, e);
        }
        return text;
    }
}
