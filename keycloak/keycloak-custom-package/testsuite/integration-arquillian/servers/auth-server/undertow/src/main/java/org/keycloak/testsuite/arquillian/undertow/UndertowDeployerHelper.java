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
package org.keycloak.testsuite.arquillian.undertow;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ClassAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class UndertowDeployerHelper {

    private static final Logger log = Logger.getLogger(UndertowDeployerHelper.class);

    DeploymentInfo getDeploymentInfo(KeycloakOnUndertowConfiguration config, WebArchive archive) {
        String archiveName = archive.getName();
        String contextPath = "/" + archive.getName().substring(0, archive.getName().lastIndexOf('.'));
        String appContextUrl = "http://" + config.getBindAddress() + ":" + config.getBindHttpPort() + contextPath;

        try {
            DeploymentInfo di = new DeploymentInfo();

            UndertowWarClassLoader classLoader = new UndertowWarClassLoader(UndertowDeployerHelper.class.getClassLoader(), archive);
            di.setClassLoader(classLoader);

            di.setDeploymentName(archiveName);
            di.setContextPath(contextPath);

            ResourceManager undertowResourcesWrapper = getResourceManager(appContextUrl, archive);
            di.setResourceManager(undertowResourcesWrapper);

            if (archive.contains("/WEB-INF/web.xml")) {
                Document webXml = loadXML(archive.get("/WEB-INF/web.xml").getAsset().openStream());
                new SimpleWebXmlParser().parseWebXml(webXml, di);
            }

            addAnnotatedServlets(di, archive);

            return di;
        } catch (Exception ioe) {
            throw new RuntimeException("Error deploying " + archive.getName(), ioe);
        }
    }

    private ResourceManager getResourceManager(final String appServerRoot, final WebArchive archive) throws IOException {
        return new ResourceManager() {

            @Override
            public Resource getResource(String path) throws IOException {
                if (path == null || path.isEmpty()) {
                    return null;
                }

                Node node = archive.get(path);
                if (node == null) {
                    log.warnf("Application '%s' did not found resource on path %s", archive.getName(), path);
                    return null;
                } else {
                    URL contextUrl = new URL(appServerRoot);

                    URL myResourceUrl = new URL(contextUrl.getProtocol(), contextUrl.getHost(), contextUrl.getPort(), path, new URLStreamHandler() {

                        @Override
                        protected URLConnection openConnection(URL u) throws IOException {
                            return new URLConnection(u) {

                                @Override
                                public void connect() throws IOException {
                                }

                                @Override
                                public InputStream getInputStream() throws IOException {
                                    return node.getAsset().openStream();
                                }

                            };
                        }

                    });

                    return new URLResource(myResourceUrl, myResourceUrl.openConnection(), path);
                }
            }

            @Override
            public boolean isResourceChangeListenerSupported() {
                return false;
            }

            @Override
            public void registerResourceChangeListener(ResourceChangeListener listener) {
                throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
            }

            @Override
            public void removeResourceChangeListener(ResourceChangeListener listener) {
                throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
            }

            @Override
            public void close() throws IOException {
                // TODO: Should close open streams?
            }

        };
    }

    private Document loadXML(InputStream is) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addAnnotatedServlets(DeploymentInfo di, Archive<?> archive) {
        Map<ArchivePath, Node> classNodes = archive.getContent((ArchivePath path) -> {

            String stringPath = path.get();
            return (stringPath.startsWith("/WEB-INF/classes") && stringPath.endsWith("class"));

        });

        for (Map.Entry<ArchivePath, Node> entry : classNodes.entrySet()) {
            Node n = entry.getValue();
            if (n.getAsset() instanceof ClassAsset) {
                ClassAsset classAsset = (ClassAsset) n.getAsset();
                Class<?> clazz = classAsset.getSource();

                WebServlet annotation = clazz.getAnnotation(WebServlet.class);
                if (annotation != null) {
                    ServletInfo undertowServlet = new ServletInfo(clazz.getSimpleName(), (Class<? extends Servlet>) clazz);

                    String[] mappings = annotation.value();
                    if (mappings != null) {
                        for (String urlPattern : mappings) {
                            undertowServlet.addMapping(urlPattern);
                        }
                    }

                    di.addServlet(undertowServlet);
                }
            }
        }

    }

}
