/*
 * The MIT License
 *
 * Copyright (c) 2016, Carlos Sanchez
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.jx.pipelines;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.IOHelpers;
import jenkins.model.JenkinsLocationConfiguration;
import org.apache.commons.compress.utils.IOUtils;
import org.csanchez.jenkins.plugins.kubernetes.ContainerTemplate;
import org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud;
import org.csanchez.jenkins.plugins.kubernetes.PodTemplate;
import org.csanchez.jenkins.plugins.kubernetes.volumes.PodVolume;
import org.jenkinsci.plugins.jx.pipelines.support.KubeHelpers;
import org.jenkinsci.plugins.jx.pipelines.support.TestHelpers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRuleNonLocalhost;
import org.jvnet.hudson.test.LoggerRule;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jenkinsci.plugins.jx.pipelines.KubernetesTestUtil.TESTING_NAMESPACE;
import static org.jenkinsci.plugins.jx.pipelines.KubernetesTestUtil.assumeKubernetes;
import static org.jenkinsci.plugins.jx.pipelines.KubernetesTestUtil.setupCloud;

public class AbstractKubernetesPipelineTest {
    public static final String JENKINS_MVN_LOCAL_REPO = "jenkins-mvn-local-repo";
    public static final String JENKINS_MAVEN_SETTINGS = "jenkins-maven-settings";
    public static final String JENKINS_DOCKER_CFG = "jenkins-docker-cfg";
    public static final String JENKINS_RELEASE_GPG = "jenkins-release-gpg";
    public static final String JENKINS_HUB_API_TOKEN = "jenkins-hub-api-token";
    public static final String JENKINS_SSH_CONFIG = "jenkins-ssh-config";
    public static final String JENKINS_GIT_SSH = "jenkins-git-ssh";
    public static final String FABRIC8_DOCKER_REGISTRY = "fabric8-docker-registry";
    public static final String NEXUS_STORAGE = "nexus-storage";
    public static final String NEXUS = "nexus";

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public JenkinsRuleNonLocalhost r = new JenkinsRuleNonLocalhost();
    @Rule
    public LoggerRule logs = new LoggerRule().record(Logger.getLogger(KubernetesCloud.class.getPackage().getName()),
            Level.ALL);
    protected KubernetesCloud cloud;

    @BeforeClass
    public static void isKubernetesConfigured() throws Exception {
        assumeKubernetes();

        printSystemProperties("hudson.slaves.NodeProvisioner.initialDelay", "hudson.slaves.NodeProvisioner.MARGIN", "hudson.slaves.NodeProvisioner.MARGIN0");
    }

    private static void printSystemProperties(String... names) {
        for (String name : names) {
            String value = System.getProperty(name);
            System.out.println("  system property: " + name + " = " + value);
        }
    }


    @Before
    public void configureCloud() throws Exception {
        cloud = setupCloud();
        createSecretsAndPVCs(cloud.connect());

        cloud.getTemplates().clear();
        cloud.addTemplate(fabric8MavenTemplate("jx-maven"));

        // Slaves running in Kubernetes (minikube) need to connect to this server, so localhost does not work
        URL url = r.getURL();

        String hostAddress = System.getProperty("jenkins.host.address");
        if (hostAddress == null) {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        }
        URL nonLocalhostUrl = new URL(url.getProtocol(), hostAddress, url.getPort(),
                url.getFile());
        String jenkinsUrl = nonLocalhostUrl.toString();
        JenkinsLocationConfiguration.get().setUrl(jenkinsUrl);

        r.jenkins.clouds.add(cloud);
    }

    protected PodTemplate fabric8MavenTemplate(String label) {
        // Create a busybox template
        PodTemplate podTemplate = new PodTemplate();
        podTemplate.setLabel(label);
        podTemplate.setName(label);

        List<PodVolume> volumes = new ArrayList<>();

        KubeHelpers.secretVolume(volumes, JENKINS_MAVEN_SETTINGS, "/root/.m2");
        KubeHelpers.secretVolume(volumes, JENKINS_DOCKER_CFG, "/home/jenkins/.docker");
        KubeHelpers.secretVolume(volumes, JENKINS_RELEASE_GPG, "/home/jenkins/.gnupg");
        KubeHelpers.secretVolume(volumes, JENKINS_HUB_API_TOKEN, "/home/jenkins/.apitoken");
        KubeHelpers.secretVolume(volumes, JENKINS_SSH_CONFIG, "/root/.ssh");
        KubeHelpers.secretVolume(volumes, JENKINS_GIT_SSH, "/root/.ssh-git");
        KubeHelpers.hostPathVolume(volumes, "/var/run/docker.sock", "/var/run/docker.sock");
        KubeHelpers.persistentVolumeClaim(volumes, JENKINS_MVN_LOCAL_REPO, "/root/.mvnrepository");
        podTemplate.setVolumes(volumes);

        List<ContainerTemplate> containers = new ArrayList<>();

        podTemplate.setContainers(containers);
        ContainerTemplate mavenTemplate = new ContainerTemplate("maven", "fabric8/maven-builder:vd81dedb", "cat", "");
        configureContainer(mavenTemplate);
        KubeHelpers.setEnvVar(mavenTemplate, "MAVEN_OPTS", "-Duser.home=/root/ -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn");
        containers.add(mavenTemplate);

        ContainerTemplate clientsTemplate = new ContainerTemplate("clients", "fabric8/builder-clients:v9c7b90f", "cat", "");
        configureContainer(clientsTemplate);
        containers.add(clientsTemplate);

        podTemplate.setContainers(containers);
        return podTemplate;
    }

    protected void configureContainer(ContainerTemplate container) {
        container.setTtyEnabled(true);
        container.setPrivileged(true);
        container.setWorkingDir("/home/jenkins/");
        KubeHelpers.setEnvVar(container, "DOCKER_HOST", "unix:/var/run/docker.sock");
        KubeHelpers.setEnvVar(container, "DOCKER_CONFIG", "/home/jenkins/.docker/");
        KubeHelpers.setEnvVar(container, "DOCKER_API_VERSION", "1.23");
    }

    private void createSecretsAndPVCs(KubernetesClient client) throws IOException, InterruptedException {
        // lets delete the PVC for nexus storage and bounce the nexus pod to ensure we can re-deploy the same version of the release
        Deployment nexus = client.extensions().deployments().withName(NEXUS).get();
        if (nexus == null) {
            runCommands("kubectl", "apply", "-n", TESTING_NAMESPACE, "-f", "http://central.maven.org/maven2/io/fabric8/apps/nexus-app/1.0.1/nexus-app-1.0.1-kubernetes.yml");
        } else {
            // TODO rather than having to trash the PVC each time we maybe could look at forcing a unique version for each release without doing a git push of new versions?
            System.out.println("Scaling down nexus");
            client.extensions().deployments().withName(NEXUS).scale(0);

            waitForNumberOfPodsWithLabel(client, 0, "project", "nexus-app");

            PersistentVolumeClaim nexusPvc = client.persistentVolumeClaims().withName(NEXUS_STORAGE).get();
            if (nexusPvc != null) {
                System.out.println("Removing nexus storage PVC");
                client.persistentVolumeClaims().withName(NEXUS_STORAGE).delete();
            }

            runCommands("kubectl", "apply", "-n", TESTING_NAMESPACE, "-f", "http://central.maven.org/maven2/io/fabric8/apps/nexus-app/1.0.1/nexus-app-1.0.1-kubernetes.yml");
        }

        waitForNumberOfPodsWithLabel(client, 1, "project", "nexus-app");

        Service registryService = client.services().withName(FABRIC8_DOCKER_REGISTRY).get();
        if (registryService == null) {
            runCommands("kubectl", "apply", "-n", TESTING_NAMESPACE, "-f", "http://central.maven.org/maven2/io/fabric8/apps/docker-registry/1.0.1/docker-registry-1.0.1-kubernetes.yml");
        }
        Deployment exposeController = client.extensions().deployments().withName("exposecontroller").get();
        if (exposeController == null) {
            runCommands("kubectl", "apply", "-n", TESTING_NAMESPACE, "-f", "http://central.maven.org/maven2/io/fabric8/apps/exposecontroller-app/3.0.11/exposecontroller-app-3.0.11-kubernetes.yml");
        }
        PersistentVolumeClaim pvc = client.persistentVolumeClaims().inNamespace(TESTING_NAMESPACE).withName(JENKINS_MVN_LOCAL_REPO).get();
        if (pvc == null) {
            Map<String, Quantity> requestMap = new HashMap<>();
            requestMap.put("storage", new QuantityBuilder().withAmount("200").withFormat("Gi").build());

            pvc = new PersistentVolumeClaimBuilder().
                    withNewMetadata().withName(JENKINS_MVN_LOCAL_REPO).addToLabels("app", "jenkins").endMetadata().
                    withNewSpec().withAccessModes("ReadWriteMany").
                    withNewResources().withRequests(requestMap).endResources().endSpec().build();
            client.persistentVolumeClaims().inNamespace(TESTING_NAMESPACE).create(pvc);
        }

        createOrReplaceSecret(client, JENKINS_MAVEN_SETTINGS, ImmutableMap.of("settings.xml", testClassesFileContent("maven-settings.xml")));
        createOrReplaceSecret(client, JENKINS_DOCKER_CFG, ImmutableMap.of("docker-config.json", testClassesFileContent("docker-config.json")));

        if (client.secrets().withName(JENKINS_RELEASE_GPG).get() == null) {
            createOrReplaceSecret(client, JENKINS_RELEASE_GPG, ImmutableMap.of());
        }
/*
        File gpgDir;
        String gpgPath = System.getProperty("gpgDir");
        if (Strings.notEmpty(gpgPath)) {
            gpgDir = new File(gpgPath);
        } else {
            gpgDir = getHomeSubDir(".gnupg");
        }
        createOrReplaceSecret(client, JENKINS_RELEASE_GPG, createFileMap(gpgDir, "pubring.gpg", "sec-jenkins.gpg", "secring.gpg", "trustdb.gpg"));
*/

        createOrReplaceSecret(client, JENKINS_HUB_API_TOKEN, ImmutableMap.of("hub", System.getProperty("hubToken")));
        createOrReplaceSecret(client, JENKINS_SSH_CONFIG, ImmutableMap.of("config", testClassesFileContent("ssh-config.txt")));

        String sshKeyName = System.getProperty("sshKeyName", "ssh-key");
        Map<String, String> sshFileMap = createUserHomeSubDirFiles(".ssh", sshKeyName, sshKeyName + ".pub");
        createOrReplaceSecret(client, JENKINS_GIT_SSH, ImmutableMap.of("ssh-key", sshFileMap.getOrDefault(sshKeyName, ""), "ssh-key.pub", sshFileMap.getOrDefault(sshKeyName + ".pub", "")));
    }

    private void waitForNumberOfPodsWithLabel(KubernetesClient client, int expectedCount, String label, String labelValue) {
        System.out.println("Waiting for " + expectedCount + " pods with label " + label + "=" + labelValue);
        while (true) {
            PodList podList = client.pods().withLabel(label, labelValue).list();
            if (podList != null) {
                List<Pod> items = podList.getItems();
                if (items != null) {
                    List<Pod> readyPods = filterReadyPods(items);
                    int size = readyPods.size();
                    System.out.println("Has " + size + " ready pod(s)");
                    if (expectedCount == 0) {
                        if (size == 0) {
                            break;
                        }
                    } else {
                        if (size >= expectedCount) {
                            break;
                        }
                    }
                }
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private List<Pod> filterReadyPods(List<Pod> items) {
        List<Pod> answer = new ArrayList<>();
        for (Pod pod : items) {
            if (KubernetesHelper.isPodReady(pod)) {
                answer.add(pod);
            }
        }
        return answer;
    }

    public int runCommands(String... commands) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.inheritIO();
        Process process = builder.start();
        int exitCode = process.waitFor();
        assertThat(exitCode).describedAs("Results of command: " + String.join(" ", commands)).isEqualTo(0);
        return exitCode;
    }

    private Map<String, String> createUserHomeSubDirFiles(String homeSubDir, String... fileNames) throws IOException {
        File dir = getHomeSubDir(homeSubDir);
        return createFileMap(dir, fileNames);
    }

    protected File getHomeSubDir(String homeSubDir) {
        File homeDir = new File(System.getProperty("user.home", "~"));
        return new File(homeDir, homeSubDir);
    }

    protected Map<String, String> createFileMap(File dir, String... fileNames) throws IOException {
        Map<String, String> answer = new HashMap<>();
        for (String name : fileNames) {
            File file = new File(dir, name);
            if (file.isFile() && file.exists()) {
                answer.put(name, IOHelpers.readFully(file));
            }
        }
        return answer;
    }

    protected String testClassesFileContent(String fileName) throws IOException {
        File testClassesDir = new File(TestHelpers.getBasedir(), "target/test-classes");
        File file = new File(testClassesDir, fileName);
        assertThat(file).describedAs("File " + file).exists().isFile();
        return IOHelpers.readFully(file);
    }

    protected void createOrReplaceSecret(KubernetesClient client, String secreteName, Map<String, String> secretData) {
        Secret secret = new SecretBuilder().
                withNewMetadata().withName(secreteName).addToLabels("app", "jenkins").endMetadata().
                withStringData(secretData).withType("Opaque").build();
        client.secrets().createOrReplace(secret);
    }

    protected String loadPipelineScript(String name) {
        try {
            return new String(IOUtils.toByteArray(getClass().getResourceAsStream(name)));
        } catch (Throwable t) {
            throw new RuntimeException("Could not read resource:[" + name + "].");
        }
    }
}
