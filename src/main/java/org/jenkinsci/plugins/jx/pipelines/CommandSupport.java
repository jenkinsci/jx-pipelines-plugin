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
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.jenkinsci.plugins.jx.pipelines.model.ServiceConstants;
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 */
public abstract class CommandSupport implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient Logger logger = Logger.getInstance();
    private transient Map<String, String> env = createEnv();
    private transient ShellFacade shellFacade;
    private transient FileReadFacade fileReadFacade;
    private File currentDir = new File(".");
    private String containerName;

    public CommandSupport() {
    }

    public CommandSupport(CommandSupport parent) {
        setLogger(parent.getLogger());
        setShellFacade(parent.getShellFacade());
        setCurrentDir(parent.getCurrentDir());
        setFileReadFacade(parent.getFileReadFacade());
    }


    public void echo(String message) {
        getLogger().info(message);
    }

    public void warning(String message) {
        echo("WARNING: " + message);
    }

    public void error(String message) {
        getLogger().error(message);
    }

    public void error(String message, Throwable t) {
        getLogger().error(message, t);
    }

    public void updateEnvironment(Object envVar) {
        //echo("===== updateEnvironment passed in " + envVar + " " + (envVar != null ? envVar.getClass().getName() : ""));
        if (envVar instanceof EnvActionImpl) {
            EnvActionImpl envAction = (EnvActionImpl) envVar;
            Map<String, String> map = envAction.getOverriddenEnvironment();
            if (map == null || map.isEmpty()) {
                try {
                    map = envAction.getEnvironment();
                } catch (Exception e) {
                    error("Failed to getEnvironment() from EnvActionImpl " + envAction, e);
                }
            }
            setEnv(map);
        } else if (envVar instanceof Map) {
            setEnv((Map<String, String>) envVar);
        }
    }

    public String getenv(String name) {
        return getEnv().get(name);
    }

    public String getenv(String name, String defaultValue) {
        String answer = getenv(name);
        if (Strings.isNullOrBlank(name)) {
            answer = defaultValue;
        }
        return answer;
    }

    /**
     * Invokes the given command
     */
    public void sh(String command) {
        ShellFacade shell = getShellFacade();
        if (shell == null) {
            throw new IllegalArgumentException("No shellFacade has been injected into " + this + " so cannot invoke sh(" + command + ")");
        }
        shell.apply(command, false, this.containerName);
    }

    /**
     * Invokes the given command
     */
    public void containerSh(String containerName, String command) {
        ShellFacade shell = getShellFacade();
        if (shell == null) {
            throw new IllegalArgumentException("No shellFacade has been injected into " + this + " so cannot invoke sh(" + command + ") in container " + containerName);
        }
        shell.apply(command, false, containerName);
    }

    /**
     * Returns the output of the given command
     */
    public String shOutput(String command) {
        ShellFacade shell = getShellFacade();
        if (shell == null) {
            throw new IllegalArgumentException("No shellFacade has been injected into " + this + " so cannot invoke sh(" + command + ")");
        }
        String answer = shell.apply(command, false, this.containerName);
        if (answer == null) {
            return "";
        }
        return answer.trim();
    }

    /**
     * Returns the output of the given command
     */
    public String containerShOutput(String containerName, String command) {
        ShellFacade shell = getShellFacade();
        if (shell == null) {
            throw new IllegalArgumentException("No shellFacade has been injected into " + this + " so cannot invoke sh(" + command + ") in container " + containerName);
        }
        String answer = shell.apply(command, false, containerName);
        if (answer == null) {
            return "";
        }
        return answer.trim();
    }

    /**
     * Retries the given block until
     *
     * @param count
     * @param block
     * @param <T>
     * @return
     */
    protected <T> T retry(int count, Callable<T> block) {
        Exception lastException = null;
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                getLogger().info("Retrying");
            }
            try {
                return block.call();
            } catch (Exception e) {
                lastException = e;
                getLogger().error(e);
            }
        }
        if (lastException != null) {
            throw new FailedBuildException(lastException);
        }
        return null;
    }

    /**
     * Invokes a pipeline step
     */
    protected <T> Object step(String stepName, Map<String, T> arguments) {
        // TODO
        return null;
    }

    protected List<File> findFiles(String glob) {
        List<File> answer = new ArrayList<>();
        // TODO
        return answer;
    }

    /**
     * Invokes the given block in the given directory then restores to the current directory at the end of the block
     */
    protected <T> T dir(File dir, Callable<T> callable) throws Exception {
        File currentDir = getCurrentDir();
        setCurrentDir(dir);
        try {
            return callable.call();
        } finally {
            setCurrentDir(currentDir);
        }
    }


    /**
     * Specifies the container name to run commands inside
     */
    protected <T> T container(String containerName, Callable<T> callable) {
        String oldContainerName = this.containerName;
        try {
            this.containerName = containerName;
            return callable.call();
        } catch (Exception e) {
            throw new FailedBuildException(e);
        } finally {
            this.containerName = oldContainerName;
        }
    }

    /**
     * Waits until the given criteria is true ignoring any exceptions that occur each time
     */
    public boolean waitUntil(Callable<Boolean> callable) {
        return waitUntil(250, -1, callable);
    }

    /**
     * Waits until the given criteria is true ignoring any exceptions that occur each time
     */
    public boolean waitUntil(long retryTimeout, long maximumTimeout, Callable<Boolean> callable) {
        long endTime = 0L;
        if (maximumTimeout > 0) {
            endTime = System.currentTimeMillis() + maximumTimeout;
        }
        while (true) {
            Boolean value = null;
            try {
                value = callable.call();
            } catch (Exception e) {
                error("Failed waiting for condition", e);
            }
            if (value != null && value.booleanValue()) {
                return true;
            }
            if (endTime > 0L && System.currentTimeMillis() > endTime) {
                error("waitUntil timed out after " + maximumTimeout + " millis");
                return false;
            }
            try {
                Thread.sleep(retryTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public String getDockerRegistryPort() {
        return Systems.getEnvVar("FABRIC8_DOCKER_REGISTRY_SERVICE_PORT", ServiceConstants.FABRIC8_DOCKER_REGISTRY_PORT);
    }

    public String getDockerRegistryHost() {
        return Systems.getEnvVar("FABRIC8_DOCKER_REGISTRY_SERVICE_HOST", ServiceConstants.FABRIC8_DOCKER_REGISTRY);
    }


    // Properties
    //-------------------------------------------------------------------------

    public File getCurrentDir() {
        return currentDir;
    }

    public void setCurrentDir(File currentDir) {
        this.currentDir = currentDir;
    }

    public void setCurrentPath(String path) {
        setCurrentDir(new File(path));
    }

    public Logger getLogger() {
        if (logger == null) {
            logger = Logger.getInstance();
        }
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Map<String, String> getEnv() {
        if (env == null) {
            env = createEnv();
        }
        return env;
    }

    public void setEnv(Map<String, String> env) {
        if (env != null) {
            if (env.isEmpty()) {
                warning("setting the environment to an empty map");
            } else {
                echo("setting the environment to: " + env);
            }
            this.env = env;
        } else {
            error("No environment map is specified");
        }
    }

    public ShellFacade getShellFacade() {
        return shellFacade;
    }

    public void setShellFacade(ShellFacade shellFacade) {
        this.shellFacade = shellFacade;
    }

    public FileReadFacade getFileReadFacade() {
        return fileReadFacade;
    }

    public void setFileReadFacade(FileReadFacade fileReadFacade) {
        this.fileReadFacade = fileReadFacade;
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    @NonCPS
    protected String readFile(String path) throws IOException {
        FileReadFacade fileReadFacade = getFileReadFacade();
        if (fileReadFacade != null) {
            System.out.println("========= CommandSupport.readFile " + path);
            String result = null;
            try {
                result = fileReadFacade.apply(path);
            } catch (Exception e) {
                error("Could not invoke FileReadFacade on " + path + " due to " + e, e);
                return null;
            }
            System.out.println("========= CommandSupport.readFile " + path + " results: " + result);
            return result;
        } else {
            return IOHelpers.readFully(createFile(path));
        }
    }

    protected File createFile(String name) {
        return new File(getCurrentDir(), name);
    }

    /**
     * Sends a message to Hubot
     */
    public void hubotSend(String message) {
        hubotSend(message, null, false);
    }

    /**
     * Sends a message to Hubot
     */
    public void hubotSend(String message, String room, boolean failOnError) {
        Map<String, Object> map = new LinkedHashMap<>(3);
        if (Strings.notEmpty(room)) {
            map.put("room", room);
        }
        map.put("message", message);
        map.put("failOnError", failOnError);
        step("hubotSend", map);
    }

    protected HashMap<String, String> createEnv() {
        return new HashMap<>();
    }


}
