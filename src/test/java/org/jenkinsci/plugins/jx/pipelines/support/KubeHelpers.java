/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.jx.pipelines.support;

import org.csanchez.jenkins.plugins.kubernetes.ContainerTemplate;
import org.csanchez.jenkins.plugins.kubernetes.PodTemplate;
import org.csanchez.jenkins.plugins.kubernetes.model.KeyValueEnvVar;
import org.csanchez.jenkins.plugins.kubernetes.model.TemplateEnvVar;
import org.csanchez.jenkins.plugins.kubernetes.volumes.HostPathVolume;
import org.csanchez.jenkins.plugins.kubernetes.volumes.PersistentVolumeClaim;
import org.csanchez.jenkins.plugins.kubernetes.volumes.PodVolume;
import org.csanchez.jenkins.plugins.kubernetes.volumes.SecretVolume;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class KubeHelpers {

    public static boolean setEnvVar(ContainerTemplate containerTemplate, String name, String value) {
        List<TemplateEnvVar> envVars = containerTemplate.getEnvVars();
        if (envVars == null) {
            envVars = new ArrayList<>();
        }
        boolean answer = setEnvVar(envVars, name, value);
        containerTemplate.setEnvVars(envVars);
        return answer;
    }

    public static boolean setEnvVar(List<TemplateEnvVar> envVarList, String name, String value) {
        for (TemplateEnvVar envVar : envVarList) {
            String envVarName = envVar.getKey();
            if (io.fabric8.utils.Objects.equal(name, envVarName)) {
                if (envVar instanceof KeyValueEnvVar) {
                    KeyValueEnvVar keyValueEnvVar = (KeyValueEnvVar) envVar;
                    String oldValue = keyValueEnvVar.getValue();
                    if (io.fabric8.utils.Objects.equal(value, oldValue)) {
                        return false;
                    } else {
                        keyValueEnvVar.setValue(value);
                        return true;
                    }
                }
            }
        }
        KeyValueEnvVar env = new KeyValueEnvVar(name, value);
        envVarList.add(env);
        return true;
    }

    public static void addVolume(PodTemplate podTemplate, PodVolume volume) {
        List<PodVolume> volumes = podTemplate.getVolumes();
        if (volumes == null) {
            volumes = new ArrayList<>();
        }
        volumes.add(volume);
        podTemplate.setVolumes(volumes);
    }

    public static void hostPathVolume(PodTemplate podTemplate, String hostPath, String mountPath) {
        addVolume(podTemplate, new HostPathVolume(hostPath, mountPath));
    }

    public static void hostPathVolume(List<PodVolume> list, String hostPath, String mountPath) {
        list.add(new HostPathVolume(hostPath, mountPath));
    }

    public static void persistentVolumeClaim(PodTemplate podTemplate, String claimName, String mountPath) {
        addVolume(podTemplate, new PersistentVolumeClaim(mountPath, claimName, false));
    }

    public static void persistentVolumeClaim(List<PodVolume> list, String claimName, String mountPath) {
        list.add(new PersistentVolumeClaim(mountPath, claimName, false));
    }

    public static void secretVolume(PodTemplate podTemplate, String secretName, String mountPath) {
        addVolume(podTemplate, new SecretVolume(mountPath, secretName));

    }

    public static void secretVolume(List<PodVolume> list, String secretName, String mountPath) {
        list.add(new SecretVolume(mountPath, secretName));
    }
}
