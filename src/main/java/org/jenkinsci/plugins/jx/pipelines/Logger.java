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

import java.io.PrintStream;

public class Logger {
    private static Logger instance = new Logger(System.out, System.err);
    private final PrintStream out;
    private final PrintStream err;

    public Logger(PrintStream out) {
        this(out, out);
    }

    public Logger(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public static Logger getInstance() {
        return instance;
    }

    public static void setInstance(Logger instance) {
        Logger.instance = instance;
    }

    private PrintStream out() {
        return out;
    }

    public PrintStream err() {
        return err;
    }

    public void info(String message) {
        out().println(message);
    }

    public void warn(String message) {
        out().println("WARNING: " + message);
    }

    public void warn(String message, Throwable t) {
        PrintStream o = out();
        o.println("WARN: " + message + " " + t);
        t.printStackTrace(o);
    }

    public void error(String message) {
        err().println("ERROR: " + message);
    }

    public void error(Throwable t) {
        PrintStream o = out();
        PrintStream e = err();

        e.println("ERROR: " + t);
        t.printStackTrace(e);

        o.println("ERROR: " + t);
        t.printStackTrace(o);
    }

    public void error(String message, Throwable t) {
        PrintStream o = out();
        PrintStream e = err();

        e.println("ERROR: " + message + " " + t);
        t.printStackTrace(e);

        o.println("ERROR: " + message + " " + t);
        t.printStackTrace(o);
    }
}
