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

package org.keycloak.testsuite.exportimport;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.junit.After;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.dir.DirExportProvider;
import org.keycloak.exportimport.dir.DirExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ExportImportTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealm1 = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealm1.getUsers().add(makeUser("user1"));
        testRealm1.getUsers().add(makeUser("user2"));
        testRealm1.getUsers().add(makeUser("user3"));

        setEventsConfig(testRealm1);
        testRealms.add(testRealm1);

        RealmRepresentation testRealm2 = loadJson(getClass().getResourceAsStream("/model/testrealm.json"), RealmRepresentation.class);
        testRealm2.setId("test-realm");
        testRealms.add(testRealm2);
    }

    private void setEventsConfig(RealmRepresentation realm) {
        realm.setEventsEnabled(true);
        realm.setAdminEventsEnabled(true);
        realm.setAdminEventsDetailsEnabled(true);
        realm.setEventsExpiration(600);
        realm.setEnabledEventTypes(Arrays.asList("REGISTER", "REGISTER_ERROR", "LOGIN", "LOGIN_ERROR", "LOGOUT_ERROR"));
    }

    private void checkEventsConfig(RealmEventsConfigRepresentation config) {
        Assert.assertTrue(config.isEventsEnabled());
        Assert.assertTrue(config.isAdminEventsEnabled());
        Assert.assertTrue(config.isAdminEventsDetailsEnabled());
        Assert.assertEquals((Long) 600L, config.getEventsExpiration());
        Assert.assertNames(new HashSet(config.getEnabledEventTypes()),"REGISTER", "REGISTER_ERROR", "LOGIN", "LOGIN_ERROR", "LOGOUT_ERROR");
    }

    private UserRepresentation makeUser(String userName) {
        return UserBuilder.create()
                .username(userName)
                .email(userName + "@test.com")
                .password("password")
                .build();
    }

    @After
    public void clearExportImportProps() throws LifecycleException {
        clearExportImportProperties();
    }

    @Test
    public void testDirFullExportImport() throws Throwable {
        testingClient.testing().exportImport().setProvider(DirExportProviderFactory.PROVIDER_ID);
        String targetDirPath = testingClient.testing().exportImport().getExportImportTestDirectory()+ File.separator + "dirExport";
        DirExportProvider.recursiveDeleteDir(new File(targetDirPath));
        testingClient.testing().exportImport().setDir(targetDirPath);
        testingClient.testing().exportImport().setUsersPerFile(ExportImportConfig.DEFAULT_USERS_PER_FILE);

        testFullExportImport();

        // There should be 6 files in target directory (3 realm, 3 user)
        assertEquals(6, new File(targetDirPath).listFiles().length);
    }

    @Test
    public void testDirRealmExportImport() throws Throwable {
        testingClient.testing()
                .exportImport()
                .setProvider(DirExportProviderFactory.PROVIDER_ID);
        String targetDirPath = testingClient.testing().exportImport().getExportImportTestDirectory() + File.separator + "dirRealmExport";
        DirExportProvider.recursiveDeleteDir(new File(targetDirPath));
        testingClient.testing().exportImport().setDir(targetDirPath);
        testingClient.testing().exportImport().setUsersPerFile(3);

        testRealmExportImport();

        // There should be 3 files in target directory (1 realm, 4 user)
        File[] files = new File(targetDirPath).listFiles();
        assertEquals(5, files.length);
    }

    @Test
    public void testSingleFileFullExportImport() throws Throwable {
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        String targetFilePath = testingClient.testing().exportImport().getExportImportTestDirectory() + File.separator + "singleFile-full.json";
        testingClient.testing().exportImport().setFile(targetFilePath);

        testFullExportImport();
    }

    @Test
    public void testSingleFileRealmExportImport() throws Throwable {
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        String targetFilePath = testingClient.testing().exportImport().getExportImportTestDirectory() + File.separator + "singleFile-realm.json";
        testingClient.testing().exportImport().setFile(targetFilePath);

        testRealmExportImport();
    }

    @Test
    public void testSingleFileRealmWithoutBuiltinsImport() throws Throwable {
        // Remove test realm
        removeRealm("test-realm");

        // Set the realm, which doesn't have builtin clients/roles inside JSON
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        URL url = ExportImportTest.class.getResource("/model/testrealm.json");
        String targetFilePath = new File(url.getFile()).getAbsolutePath();
        testingClient.testing().exportImport().setFile(targetFilePath);

        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().exportImport().runImport();

        RealmResource testRealmRealm = adminClient.realm("test-realm");

        ExportImportUtil.assertDataImportedInRealm(adminClient, testingClient, testRealmRealm.toRepresentation());
    }
    
    private void testFullExportImport() throws LifecycleException {
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_EXPORT);
        testingClient.testing().exportImport().setRealmName("");

        testingClient.testing().exportImport().runExport();

        removeRealm("test");
        removeRealm("test-realm");
        Assert.assertNames(adminClient.realms().findAll(), "master");

        assertNotAuthenticated("test", "test-user@localhost", "password");
        assertNotAuthenticated("test", "user1", "password");
        assertNotAuthenticated("test", "user2", "password");
        assertNotAuthenticated("test", "user3", "password");

        // Configure import
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().exportImport().runImport();

        // Ensure data are imported back
        Assert.assertNames(adminClient.realms().findAll(), "master", "test", "test-realm");

        assertAuthenticated("test", "test-user@localhost", "password");
        assertAuthenticated("test", "user1", "password");
        assertAuthenticated("test", "user2", "password");
        assertAuthenticated("test", "user3", "password");
    }

    private void testRealmExportImport() throws LifecycleException {
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_EXPORT);
        testingClient.testing().exportImport().setRealmName("test");

        testingClient.testing().exportImport().runExport();

        List<ComponentRepresentation> components = adminClient.realm("test").components().query();
        KeysMetadataRepresentation keyMetadata = adminClient.realm("test").keys().getKeyMetadata();
        String sampleRealmRoleId = adminClient.realm("test").roles().get("sample-realm-role").toRepresentation().getId();
        String testAppId = adminClient.realm("test").clients().findByClientId("test-app").get(0).getId();
        String sampleClientRoleId = adminClient.realm("test").clients().get(testAppId).roles().get("sample-client-role").toRepresentation().getId();

        // Delete some realm (and some data in admin realm)
        adminClient.realm("test").remove();

        Assert.assertNames(adminClient.realms().findAll(), "test-realm", "master");

        assertNotAuthenticated("test", "test-user@localhost", "password");
        assertNotAuthenticated("test", "user1", "password");
        assertNotAuthenticated("test", "user2", "password");
        assertNotAuthenticated("test", "user3", "password");

        // Configure import
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().exportImport().runImport();

        // Ensure data are imported back, but just for "test" realm
        Assert.assertNames(adminClient.realms().findAll(), "master", "test", "test-realm");

        assertAuthenticated("test", "test-user@localhost", "password");
        assertAuthenticated("test", "user1", "password");
        assertAuthenticated("test", "user2", "password");
        assertAuthenticated("test", "user3", "password");

        List<ComponentRepresentation> componentsImported = adminClient.realm("test").components().query();
        assertComponents(components, componentsImported);

        KeysMetadataRepresentation keyMetadataImported = adminClient.realm("test").keys().getKeyMetadata();
        assertEquals(keyMetadata.getActive(), keyMetadataImported.getActive());

        String importedSampleRealmRoleId = adminClient.realm("test").roles().get("sample-realm-role").toRepresentation().getId();
        assertEquals(sampleRealmRoleId, importedSampleRealmRoleId);

        String importedSampleClientRoleId = adminClient.realm("test").clients().get(testAppId).roles().get("sample-client-role").toRepresentation().getId();
        assertEquals(sampleClientRoleId, importedSampleClientRoleId);

        checkEventsConfig(adminClient.realm("test").getRealmEventsConfig());
    }

    private void assertAuthenticated(String realmName, String username, String password) {
        assertAuth(true, realmName, username, password);
    }

    private void assertNotAuthenticated(String realmName, String username, String password) {
        assertAuth(false, realmName, username, password);
    }

    private void assertAuth(boolean expectedResult, String realmName, String username, String password) {
        assertEquals(expectedResult, testingClient.testing().validCredentials(realmName, username, password));
    }

    private void assertComponents(List<ComponentRepresentation> expected, List<ComponentRepresentation> actual) {
        expected.sort((o1, o2) -> o1.getId().compareTo(o2.getId()));
        actual.sort((o1, o2) -> o1.getId().compareTo(o2.getId()));

        assertEquals(expected.size(), actual.size());
        for (int i = 0 ; i < expected.size(); i++) {
            ComponentRepresentation e = expected.get(i);
            ComponentRepresentation a = actual.get(i);

            assertEquals(e.getId(), a.getId());
            assertEquals(e.getName(), a.getName());
            assertEquals(e.getProviderId(), a.getProviderId());
            assertEquals(e.getProviderType(), a.getProviderType());
            assertEquals(e.getParentId(), a.getParentId());
            assertEquals(e.getSubType(), a.getSubType());
            Assert.assertNames(e.getConfig().keySet(), a.getConfig().keySet().toArray(new String[] {}));

            // Compare config values without take order into account
            for (Map.Entry<String, List<String>> entry : e.getConfig().entrySet()) {
                List<String> eList = entry.getValue();
                List<String> aList = a.getConfig().getList(entry.getKey());
                Assert.assertNames(eList, aList.toArray(new String[] {}));
            }
        }
    }

    private void clearExportImportProperties() {
        // Clear export/import properties after test
        Properties systemProps = System.getProperties();
        Set<String> propsToRemove = new HashSet<String>();

        for (Object key : systemProps.keySet()) {
            if (key.toString().startsWith(ExportImportConfig.PREFIX)) {
                propsToRemove.add(key.toString());
            }
        }

        for (String propToRemove : propsToRemove) {
            systemProps.remove(propToRemove);
        }
    }


}
