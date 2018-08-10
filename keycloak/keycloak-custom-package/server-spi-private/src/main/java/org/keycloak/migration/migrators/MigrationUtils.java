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

package org.keycloak.migration.migrators;

import java.util.LinkedList;
import java.util.List;
import org.keycloak.Config;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MigrationUtils {

    public static void addAdminRole(RealmModel realm, String roleName) {
        ClientModel client = realm.getMasterAdminClient();
        if (client != null && client.getRole(roleName) == null) {
            RoleModel role = client.addRole(roleName);
            role.setDescription("${role_" + roleName + "}");
            role.setScopeParamRequired(false);

            client.getRealm().getRole(AdminRoles.ADMIN).addCompositeRole(role);
        }

        if (!realm.getName().equals(Config.getAdminRealm())) {
            client = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            if (client != null && client.getRole(roleName) == null) {
                RoleModel role = client.addRole(roleName);
                role.setDescription("${role_" + roleName + "}");
                role.setScopeParamRequired(false);

                client.getRole(AdminRoles.REALM_ADMIN).addCompositeRole(role);
            }
        }
    }

    public static void updateOTPRequiredAction(RequiredActionProviderModel otpAction) {
        if (otpAction == null) return;
        if (!UserModel.RequiredAction.CONFIGURE_TOTP.name().equals(otpAction.getProviderId())) return;
        if (!"Configure Totp".equals(otpAction.getName())) return;

        otpAction.setName("Configure OTP");
    }
    
    public static void updateProtocolMappers(ProtocolMapperContainerModel client) {
        List<ProtocolMapperModel> toUpdate = new LinkedList<>();
        for (ProtocolMapperModel mapper : client.getProtocolMappers()) {
            if (!mapper.getConfig().containsKey("userinfo.token.claim") && mapper.getConfig().containsKey("id.token.claim")) {
                mapper.getConfig().put("userinfo.token.claim", mapper.getConfig().get("id.token.claim"));
                toUpdate.add(mapper);
            }
        }

        for (ProtocolMapperModel mapper : toUpdate) {
            client.updateProtocolMapper(mapper);
        }
    }

}
