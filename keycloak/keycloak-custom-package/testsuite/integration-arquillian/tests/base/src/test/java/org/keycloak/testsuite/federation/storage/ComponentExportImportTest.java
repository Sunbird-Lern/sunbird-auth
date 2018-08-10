package org.keycloak.testsuite.federation.storage;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.ws.rs.NotFoundException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.admin.ApiUtil;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import org.keycloak.testsuite.federation.UserMapStorageFactory;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;

/**
 *
 * @author tkyjovsk
 */
public class ComponentExportImportTest extends AbstractAuthTest implements Serializable {

    private File exportFile;

    @Deployment
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(ComponentExportImportTest.class, AbstractAuthTest.class, RealmResource.class)
                .addPackages(true, "org.keycloak.testsuite");
    }

    @Before
    public void setDirs() {
        exportFile = new File (new File(System.getProperty("auth.server.config.dir", "target")), "singleFile-full.json");
        log.infof("Export file: %s", exportFile);
    }

    public void clearExportImportProperties() {
        // Clear export/import properties after test
        Properties systemProps = System.getProperties();
        Set<String> propsToRemove = new HashSet<>();

        for (Object key : systemProps.keySet()) {
            if (key.toString().startsWith(ExportImportConfig.PREFIX)) {
                propsToRemove.add(key.toString());
            }
        }

        for (String propToRemove : propsToRemove) {
            systemProps.remove(propToRemove);
        }
    }

    protected String addComponent(ComponentRepresentation component) {
        return ApiUtil.getCreatedId(testRealmResource().components().add(component));
    }

    @Test
    @Ignore
    public void testSingleFile() {
        clearExportImportProperties();

        String realmId = testRealmResource().toRepresentation().getId();
        String realmName = testRealmResource().toRepresentation().getRealm();

        ComponentRepresentation parentComponent = new ComponentRepresentation();
        parentComponent.setParentId(realmId);
        parentComponent.setName("parent");
        parentComponent.setSubType("subtype");
        parentComponent.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        parentComponent.setProviderType(UserStorageProvider.class.getName());
        parentComponent.setConfig(new MultivaluedHashMap<>());
        parentComponent.getConfig().putSingle("priority", Integer.toString(0));
        parentComponent.getConfig().putSingle("attr", "value");
        String parentComponentId = addComponent(parentComponent);

        ComponentRepresentation subcomponent = new ComponentRepresentation();
        subcomponent.setParentId(parentComponentId);
        subcomponent.setName("child");
        subcomponent.setSubType("subtype2");
        subcomponent.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        subcomponent.setProviderType(UserStorageProvider.class.getName());
        subcomponent.setConfig(new MultivaluedHashMap<>());
        subcomponent.getConfig().putSingle("priority", Integer.toString(0));
        subcomponent.getConfig().putSingle("attr", "value2");
        String subcomponentId = addComponent(subcomponent);

        // export 
        testingClient.server().run(session -> {
            ExportImportConfig.setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
            ExportImportConfig.setFile(exportFile.getAbsolutePath());
            ExportImportConfig.setRealmName(realmName);
            ExportImportConfig.setAction(ExportImportConfig.ACTION_EXPORT);
            new ExportImportManager(session).runExport();
        });

        // import 
        testingClient.server().run(session -> {
            Assert.assertNull(session.realms().getRealmByName(TEST));
            ExportImportConfig.setAction(ExportImportConfig.ACTION_IMPORT);
            new ExportImportManager(session).runImport();
            Assert.assertNotNull(session.realms().getRealmByName(TEST));
        });

        try {
            parentComponent = testRealmResource().components().component(parentComponentId).toRepresentation();
            subcomponent = testRealmResource().components().component(subcomponentId).toRepresentation();
        } catch (NotFoundException nfe) {
            fail("Components not found after import.");
        }

        Assert.assertEquals(parentComponent.getParentId(), realmId);
        Assert.assertEquals(parentComponent.getName(), "parent");
        Assert.assertEquals(parentComponent.getSubType(), "subtype");
        Assert.assertEquals(parentComponent.getProviderId(), UserMapStorageFactory.PROVIDER_ID);
        Assert.assertEquals(parentComponent.getProviderType(), UserStorageProvider.class.getName());
        Assert.assertEquals(parentComponent.getConfig().getFirst("attr"), "value");

        Assert.assertEquals(subcomponent.getParentId(), realmId);
        Assert.assertEquals(subcomponent.getName(), "child");
        Assert.assertEquals(subcomponent.getSubType(), "subtype2");
        Assert.assertEquals(subcomponent.getProviderId(), UserMapStorageFactory.PROVIDER_ID);
        Assert.assertEquals(subcomponent.getProviderType(), UserStorageProvider.class.getName());
        Assert.assertEquals(subcomponent.getConfig().getFirst("attr"), "value2");

    }

}
