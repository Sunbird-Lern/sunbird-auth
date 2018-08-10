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

package org.keycloak.adapters.saml.config;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SP implements Serializable {
    public static class PrincipalNameMapping implements Serializable {
        private String policy;
        private String attributeName;

        public String getPolicy() {
            return policy;
        }

        public void setPolicy(String policy) {
            this.policy = policy;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public void setAttributeName(String attributeName) {
            this.attributeName = attributeName;
        }
    }

    private String entityID;
    private String sslPolicy;
    private boolean forceAuthentication;
    private boolean isPassive;
    private boolean turnOffChangeSessionIdOnLogin;
    private String logoutPage;
    private List<Key> keys;
    private String nameIDPolicyFormat;
    private PrincipalNameMapping principalNameMapping;
    private Set<String> roleAttributes;
    private IDP idp;
    private boolean autodetectBearerOnly;

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    public String getSslPolicy() {
        return sslPolicy;
    }

    public void setSslPolicy(String sslPolicy) {
        this.sslPolicy = sslPolicy;
    }

    public boolean isForceAuthentication() {
        return forceAuthentication;
    }

    public void setForceAuthentication(boolean forceAuthentication) {
        this.forceAuthentication = forceAuthentication;
    }

    public boolean isIsPassive() {
        return isPassive;
    }

    public void setIsPassive(boolean isPassive) {
        this.isPassive = isPassive;
    }

    public boolean isTurnOffChangeSessionIdOnLogin() {
        return turnOffChangeSessionIdOnLogin;
    }

    public void setTurnOffChangeSessionIdOnLogin(boolean turnOffChangeSessionIdOnLogin) {
        this.turnOffChangeSessionIdOnLogin = turnOffChangeSessionIdOnLogin;
    }

    public List<Key> getKeys() {
        return keys;
    }

    public void setKeys(List<Key> keys) {
        this.keys = keys;
    }

    public String getNameIDPolicyFormat() {
        return nameIDPolicyFormat;
    }

    public void setNameIDPolicyFormat(String nameIDPolicyFormat) {
        this.nameIDPolicyFormat = nameIDPolicyFormat;
    }

    public PrincipalNameMapping getPrincipalNameMapping() {
        return principalNameMapping;
    }

    public void setPrincipalNameMapping(PrincipalNameMapping principalNameMapping) {
        this.principalNameMapping = principalNameMapping;
    }

    public Set<String> getRoleAttributes() {
        return roleAttributes;
    }

    public void setRoleAttributes(Set<String> roleAttributes) {
        this.roleAttributes = roleAttributes;
    }

    public IDP getIdp() {
        return idp;
    }

    public void setIdp(IDP idp) {
        this.idp = idp;
    }

    public String getLogoutPage() {
        return logoutPage;
    }

    public void setLogoutPage(String logoutPage) {
        this.logoutPage = logoutPage;
    }

    public boolean isAutodetectBearerOnly() {
        return autodetectBearerOnly;
    }

    public void setAutodetectBearerOnly(boolean autodetectBearerOnly) {
        this.autodetectBearerOnly = autodetectBearerOnly;
    }
}
