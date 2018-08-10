/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Launcher {

    public static File getHome() {
        String launcherPath = Launcher.class.getName().replace('.', '/') + ".class";
        URL jarfile = Launcher.class.getClassLoader().getResource(launcherPath);
        if (jarfile != null) {
            Matcher m = Pattern.compile("jar:(file:.*)!/" + launcherPath).matcher(jarfile.toString());
            if (m.matches()) {
                try {
                    File jarPath = new File(new URI(m.group(1)));
                    File libPath = jarPath.getParentFile().getParentFile();
                    System.out.println("Home directory: " + libPath.toString());
                    if (!libPath.exists()) {
                        System.exit(1);

                    }
                    return libPath;
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            System.err.println("jar file null: " + launcherPath);
        }
        return null;
    }

    public static void main(String[] args) throws Exception {

        File home = getHome();
        File lib = new File(home, "lib");
        if (!lib.exists()) {
            System.err.println("Could not find lib directory: " + lib.toString());
            System.exit(1);
        }
        List<URL> jars = new ArrayList<URL>();
        for (File file : lib.listFiles()) {
            jars.add(file.toURI().toURL());
        }
        URL[] urls = jars.toArray(new URL[jars.size()]);
        URLClassLoader loader = new URLClassLoader(urls, Launcher.class.getClassLoader());

        Class mainClass = loader.loadClass("org.keycloak.proxy.Main");
        Method mainMethod = null;
        for (Method m : mainClass.getMethods()) if (m.getName().equals("main")) { mainMethod = m; break; }
        Object obj = args;
        mainMethod.invoke(null, obj);
    }
}
