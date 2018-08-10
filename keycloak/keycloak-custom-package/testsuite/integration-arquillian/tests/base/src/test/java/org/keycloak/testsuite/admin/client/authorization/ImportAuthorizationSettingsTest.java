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

package org.keycloak.testsuite.admin.client.authorization;

import static org.junit.Assert.assertEquals;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ImportAuthorizationSettingsTest extends AbstractAuthorizationTest {

    @Before
    public void createRole() {
        ClientResource clientResource = getClientResource();

        RoleRepresentation role = new RoleRepresentation();
        role.setName("admin");
        clientResource.roles().create(role);

        testRealmResource().users().create(UserBuilder.create().username("alice").build());
    }

    @After
    public void onAfterAuthzTests() {
        ClientResource clientResource = getClientResource();

        // Needed to disable authz first. TODO: Looks like a bug. Check later...
        ClientRepresentation client = clientResource.toRepresentation();
        client.setAuthorizationServicesEnabled(false);
        clientResource.update(client);

        getClientResource().remove();
    }

    @Test
    public void testImportUnorderedSettings() throws Exception {
        ClientResource clientResource = getClientResource();

        enableAuthorizationServices();

        ResourceServerRepresentation toImport = JsonSerialization.readValue(getClass().getResourceAsStream("/authorization-test/import-authorization-unordered-settings.json"), ResourceServerRepresentation.class);

        realmsResouce().realm(getRealmId()).roles().create(new RoleRepresentation("user", null, false));
        clientResource.roles().create(new RoleRepresentation("manage-albums", null, false));

        AuthorizationResource authorizationResource = clientResource.authorization();

        authorizationResource.importSettings(toImport);

        assertEquals(15, authorizationResource.policies().policies().size());
    }
}