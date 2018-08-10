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

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DefaultServletConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.undertow.api.UndertowWebArchive;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.filters.KeycloakSessionServletFilter;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;

import org.keycloak.util.JsonSerialization;
import java.io.IOException;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class KeycloakOnUndertow implements DeployableContainer<KeycloakOnUndertowConfiguration> {

    protected final Logger log = Logger.getLogger(this.getClass());
    
    private UndertowJaxrsServer undertow;
    private KeycloakOnUndertowConfiguration configuration;
    private KeycloakSessionFactory sessionFactory;

    Map<String, String> deployedArchivesToContextPath = new ConcurrentHashMap<>();

    private DeploymentInfo createAuthServerDeploymentInfo() {
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplicationClass(KeycloakApplication.class.getName());

        DeploymentInfo di = undertow.undertowDeployment(deployment);
        di.setClassLoader(getClass().getClassLoader());
        di.setContextPath("/auth");
        di.setDeploymentName("Keycloak");
        di.addInitParameter(KeycloakApplication.KEYCLOAK_EMBEDDED, "true");
        if (configuration.getKeycloakConfigPropertyOverridesMap() != null) {
            try {
                di.addInitParameter(KeycloakApplication.SERVER_CONTEXT_CONFIG_PROPERTY_OVERRIDES,
                  JsonSerialization.writeValueAsString(configuration.getKeycloakConfigPropertyOverridesMap()));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        di.setDefaultServletConfig(new DefaultServletConfig(true));
        di.addWelcomePage("theme/keycloak/welcome/resources/index.html");

        FilterInfo filter = Servlets.filter("SessionFilter", KeycloakSessionServletFilter.class);
        di.addFilter(filter);
        di.addFilterUrlMapping("SessionFilter", "/*", DispatcherType.REQUEST);
        filter.setAsyncSupported(true);

        return di;
    }

    public DeploymentInfo getDeplotymentInfoFromArchive(Archive<?> archive) {
        if (archive instanceof UndertowWebArchive) {
            return ((UndertowWebArchive) archive).getDeploymentInfo();
        } else if (archive instanceof WebArchive) {
            return new UndertowDeployerHelper().getDeploymentInfo(configuration, (WebArchive)archive);
        } else {
            throw new IllegalArgumentException("UndertowContainer only supports UndertowWebArchive or WebArchive.");
        }
    }

    private HTTPContext createHttpContextForDeploymentInfo(DeploymentInfo deploymentInfo) {
        HTTPContext httpContext = new HTTPContext(configuration.getBindAddress(), configuration.getBindHttpPort());
        final Map<String, ServletInfo> servlets = deploymentInfo.getServlets();
        final Collection<ServletInfo> servletsInfo = servlets.values();
        for (ServletInfo servletInfo : servletsInfo) {
            httpContext.add(new Servlet(servletInfo.getName(), deploymentInfo.getContextPath()));
        }
        return httpContext;
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        if (isRemoteMode()) {
            log.infof("Skipped deployment of '%s' as we are in remote mode!", archive.getName());
            return new ProtocolMetaData();
        }

        DeploymentInfo di = getDeplotymentInfoFromArchive(archive);

        ClassLoader parentCl = Thread.currentThread().getContextClassLoader();
        UndertowWarClassLoader classLoader = new UndertowWarClassLoader(parentCl, archive);
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            undertow.deploy(di);
        } finally {
            Thread.currentThread().setContextClassLoader(parentCl);
        }

        deployedArchivesToContextPath.put(archive.getName(), di.getContextPath());

        return new ProtocolMetaData().addContext(
                createHttpContextForDeploymentInfo(di));
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Class<KeycloakOnUndertowConfiguration> getConfigurationClass() {
        return KeycloakOnUndertowConfiguration.class;
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.1");
    }

    @Override
    public void setup(
            KeycloakOnUndertowConfiguration undertowContainerConfiguration) {
        this.configuration = undertowContainerConfiguration;
    }

    @Override
    public void start() throws LifecycleException {
        if (isRemoteMode()) {
            log.info("Skip bootstrap undertow. We are in remote mode");
            return;
        }

        log.infof("Starting auth server on embedded Undertow on: http://%s:%d", configuration.getBindAddress(), configuration.getBindHttpPort());
        long start = System.currentTimeMillis();

        if (undertow == null) {
            undertow = new UndertowJaxrsServer();
        }
        undertow.start(Undertow.builder()
                        .addHttpListener(configuration.getBindHttpPort(), configuration.getBindAddress())
                        .setWorkerThreads(configuration.getWorkerThreads())
                        .setIoThreads(configuration.getWorkerThreads() / 8)
        );

        if (configuration.getRoute() != null) {
            log.info("Using route: " + configuration.getRoute());
        }

        DeploymentInfo di = createAuthServerDeploymentInfo();
        undertow.deploy(di);
        ResteasyDeployment deployment = (ResteasyDeployment) di.getServletContextAttributes().get(ResteasyDeployment.class.getName());
        sessionFactory = ((KeycloakApplication) deployment.getApplication()).getSessionFactory();

        setupDevConfig();

        log.info("Auth server started in " + (System.currentTimeMillis() - start) + " ms\n");
    }


    protected void setupDevConfig() {
        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransactionManager().begin();
            if (new ApplianceBootstrap(session).isNoMasterUser()) {
                new ApplianceBootstrap(session).createMasterRealmUser("admin", "admin");
            }
            session.getTransactionManager().commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void stop() throws LifecycleException {
        if (isRemoteMode()) {
            log.info("Skip stopping undertow. We are in remote mode");
            return;
        }

        log.info("Stopping auth server.");
        sessionFactory.close();
        undertow.stop();
    }

    private boolean isRemoteMode() {
        //return true;
        return configuration.isRemoteMode();
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        if (isRemoteMode()) {
            log.infof("Skipped undeployment of '%s' as we are in remote mode!", archive.getName());
            return;
        }

        Field containerField = Reflections.findDeclaredField(UndertowJaxrsServer.class, "container");
        Reflections.setAccessible(containerField);
        ServletContainer container = (ServletContainer) Reflections.getFieldValue(containerField, undertow);

        DeploymentManager deploymentMgr = container.getDeployment(archive.getName());
        if (deploymentMgr != null) {
            DeploymentInfo deployment = deploymentMgr.getDeployment().getDeploymentInfo();

            try {
                deploymentMgr.stop();
            } catch (ServletException se) {
                throw new DeploymentException(se.getMessage(), se);
            }

            deploymentMgr.undeploy();

            Field rootField = Reflections.findDeclaredField(UndertowJaxrsServer.class, "root");
            Reflections.setAccessible(rootField);
            PathHandler root = (PathHandler) Reflections.getFieldValue(rootField, undertow);

            String path = deployedArchivesToContextPath.get(archive.getName());
            root.removePrefixPath(path);

            container.removeDeployment(deployment);
        } else {
            log.warnf("Deployment '%s' not found", archive.getName());
        }
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

}
