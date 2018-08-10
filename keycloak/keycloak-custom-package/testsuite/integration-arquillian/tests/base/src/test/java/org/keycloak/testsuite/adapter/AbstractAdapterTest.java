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

package org.keycloak.testsuite.adapter;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.adapter.page.AppServerContextRoot;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer
public abstract class AbstractAdapterTest extends AbstractAuthTest {

    @Page
    protected AppServerContextRoot appServerContextRootPage;

    public static final String JBOSS_DEPLOYMENT_STRUCTURE_XML = "jboss-deployment-structure.xml";
    public static final URL jbossDeploymentStructure = AbstractServletsAdapterTest.class
            .getResource("/adapter-test/" + JBOSS_DEPLOYMENT_STRUCTURE_XML);
    public static final String TOMCAT_CONTEXT_XML = "context.xml";
    public static final URL tomcatContext = AbstractServletsAdapterTest.class
            .getResource("/adapter-test/" + TOMCAT_CONTEXT_XML);

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        addAdapterTestRealms(testRealms);
        for (RealmRepresentation tr : testRealms) {
            log.info("Setting redirect-uris in test realm '" + tr.getRealm() + "' as " + (isRelative() ? "" : "non-") + "relative");

            modifyClientRedirectUris(tr, "http://localhost:8080", "");
            modifyClientUrls(tr, "http://localhost:8080", "");

            if (isRelative()) {
                modifyClientRedirectUris(tr, appServerContextRootPage.toString(), "");
                modifyClientUrls(tr, appServerContextRootPage.toString(), "");
                modifyClientWebOrigins(tr, "8080", System.getProperty("auth.server.http.port", null));
                modifySamlMasterURLs(tr, "/", "http://localhost:" + System.getProperty("auth.server.http.port", null) + "/");
                modifySAMLClientsAttributes(tr, "8080", System.getProperty("auth.server.http.port", "8180"));
            } else {
                modifyClientRedirectUris(tr, "^(/.*/\\*)", appServerContextRootPage.toString() + "$1");
                modifyClientUrls(tr, "^(/.*)", appServerContextRootPage.toString() + "$1");
                modifyClientWebOrigins(tr, "8080", System.getProperty("app.server.http.port", null));
                modifySamlMasterURLs(tr, "8080", System.getProperty("auth.server.http.port", null));
                modifySAMLClientsAttributes(tr, "http://localhost:8080",  appServerContextRootPage.toString());
                modifyClientJWKSUrl(tr, "^(/.*)", appServerContextRootPage.toString() + "$1");
            }
            if ("true".equals(System.getProperty("auth.server.ssl.required"))) {
                tr.setSslRequired("all");
            }
        }
    }

    // TODO: Fix to not require re-import
    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    private void modifyClientJWKSUrl(RealmRepresentation realm, String regex, String replacement) {
        if (realm.getClients() != null) {
            realm.getClients().stream().
                    filter(client -> "client-jwt".equals(client.getClientAuthenticatorType()) && client.getAttributes().containsKey("jwks.url")).
                    forEach(client -> {
                Map<String, String> attr = client.getAttributes();
                attr.put("jwks.url", attr.get("jwks.url").replaceFirst(regex, replacement));
                client.setAttributes(attr);
            });
        }
    }

    public abstract void addAdapterTestRealms(List<RealmRepresentation> testRealms);

    public boolean isRelative() {
        return testContext.isRelativeAdapterTest();
    }

    protected void modifyClientRedirectUris(RealmRepresentation realm, String regex, String replacement) {
        if (realm.getClients() != null) {
            for (ClientRepresentation client : realm.getClients()) {
                List<String> redirectUris = client.getRedirectUris();
                if (redirectUris != null) {
                    List<String> newRedirectUris = new ArrayList<>();
                    for (String uri : redirectUris) {
                        newRedirectUris.add(uri.replaceAll(regex, replacement));
                    }
                    client.setRedirectUris(newRedirectUris);
                }
            }
        }
    }

    protected void modifyClientUrls(RealmRepresentation realm, String regex, String replacement) {
        if (realm.getClients() != null) {
            for (ClientRepresentation client : realm.getClients()) {
                String baseUrl = client.getBaseUrl();
                if (baseUrl != null) {
                    client.setBaseUrl(baseUrl.replaceAll(regex, replacement));
                }
                String adminUrl = client.getAdminUrl();
                if (adminUrl != null) {
                    client.setAdminUrl(adminUrl.replaceAll(regex, replacement));
                }
            }
        }
    }

    protected void modifyClientWebOrigins(RealmRepresentation realm, String regex, String replacement) {
        if (realm.getClients() != null) {
            for (ClientRepresentation client : realm.getClients()) {
                List<String> webOrigins = client.getWebOrigins();
                if (webOrigins != null) {
                    List<String> newWebOrigins = new ArrayList<>();
                    for (String uri : webOrigins) {
                        newWebOrigins.add(uri.replaceAll(regex, replacement));
                    }
                    client.setWebOrigins(newWebOrigins);
                }
            }
        }
    }

    protected void modifySAMLClientsAttributes(RealmRepresentation realm, String regex, String replacement) {
        if (realm.getClients() != null) {
            for (ClientRepresentation client : realm.getClients()) {
                if (client.getProtocol() != null && client.getProtocol().equals("saml")) {
                    log.info("Modifying attributes of SAML client: " + client.getClientId());
                    for (Map.Entry<String, String> entry : client.getAttributes().entrySet()) {
                        client.getAttributes().put(entry.getKey(), entry.getValue().replaceAll(regex, replacement));
                    }
                }
            }
        }
    }

    protected void modifySamlMasterURLs(RealmRepresentation realm, String regex, String replacement) {
        if (realm.getClients() != null) {
            for (ClientRepresentation client : realm.getClients()) {
                if (client.getProtocol() != null && client.getProtocol().equals("saml")) {
                    log.info("Modifying master URL of SAML client: " + client.getClientId());
                    String masterUrl = client.getAdminUrl();
                    if (masterUrl == null) {
                        masterUrl = client.getBaseUrl();
                    }
                    masterUrl = masterUrl.replaceFirst(regex, replacement);
                    client.setAdminUrl(masterUrl + ((!masterUrl.endsWith("/saml")) ? "/saml" : ""));
                }
            }
        }
    }

    /**
     * Modifies baseUrl, adminUrl and redirectUris for client based on real
     * deployment url of the app.
     *
     * @param realm
     * @param clientId
     * @param deploymentUrl
     */
    protected void fixClientUrisUsingDeploymentUrl(RealmRepresentation realm, String clientId, String deploymentUrl) {
        for (ClientRepresentation client : realm.getClients()) {
            if (clientId.equals(client.getClientId())) {
                if (client.getBaseUrl() != null) {
                    client.setBaseUrl(deploymentUrl);
                }
                if (client.getAdminUrl() != null) {
                    client.setAdminUrl(deploymentUrl);
                }
                List<String> redirectUris = client.getRedirectUris();
                if (redirectUris != null) {
                    List<String> newRedirectUris = new ArrayList<>();
                    for (String uri : redirectUris) {
                        newRedirectUris.add(deploymentUrl + "/*");
                    }
                    client.setRedirectUris(newRedirectUris);
                }
            }
        }
    }

    public static void addContextXml(Archive archive, String contextPath) {
        try {
            String contextXmlContent = IOUtils.toString(tomcatContext.openStream())
                    .replace("%CONTEXT_PATH%", contextPath);
            archive.add(new StringAsset(contextXmlContent), "/META-INF/context.xml");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
