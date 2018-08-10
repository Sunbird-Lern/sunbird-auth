/*
 * Copyright 2016 Analytical Graphics, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.authentication.authenticators.x509;

import org.keycloak.models.AuthenticatorConfigModel;

import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.*;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/26/2016
 */

public class X509AuthenticatorConfigModel extends AuthenticatorConfigModel {

    private static final long serialVersionUID = 1L;

    public enum IdentityMapperType {
        USER_ATTRIBUTE(USER_ATTRIBUTE_MAPPER),
        USERNAME_EMAIL(USERNAME_EMAIL_MAPPER);

        private String name;
        IdentityMapperType(String name) {
            this.name = name;
        }
        public String getName() {  return this.name; }
        static public IdentityMapperType parse(String name) throws IllegalArgumentException, IndexOutOfBoundsException {
            if (name == null || name.trim().length() == 0)
                throw new IllegalArgumentException("name");

            for (IdentityMapperType value : IdentityMapperType.values()) {
                if (value.getName().equalsIgnoreCase(name))
                    return value;
            }
            throw new IndexOutOfBoundsException("name");
        }
    }

    public enum MappingSourceType {
        SERIALNUMBER(MAPPING_SOURCE_CERT_SERIALNUMBER),
        ISSUERDN_CN(MAPPING_SOURCE_CERT_ISSUERDN_CN),
        ISSUERDN_EMAIL(MAPPING_SOURCE_CERT_ISSUERDN_EMAIL),
        ISSUERDN(MAPPING_SOURCE_CERT_ISSUERDN),
        SUBJECTDN_CN(MAPPING_SOURCE_CERT_SUBJECTDN_CN),
        SUBJECTDN_EMAIL(MAPPING_SOURCE_CERT_SUBJECTDN_EMAIL),
        SUBJECTDN(MAPPING_SOURCE_CERT_SUBJECTDN);

        private String name;
        MappingSourceType(String name) {
            this.name = name;
        }
        public String getName() {  return this.name; }
        static public MappingSourceType parse(String name) throws IllegalArgumentException, IndexOutOfBoundsException {
            if (name == null || name.trim().length() == 0)
                throw new IllegalArgumentException("name");

            for (MappingSourceType value : MappingSourceType.values()) {
                if (value.getName().equalsIgnoreCase(name))
                    return value;
            }
            throw new IndexOutOfBoundsException("name");
        }
    }

    public X509AuthenticatorConfigModel(AuthenticatorConfigModel model) {
        this.setAlias(model.getAlias());
        this.setId(model.getId());
        this.setConfig(model.getConfig());
    }
    public X509AuthenticatorConfigModel() {

    }

    public boolean getCRLEnabled() {
        return Boolean.parseBoolean(getConfig().get(ENABLE_CRL));
    }

    public X509AuthenticatorConfigModel setCRLEnabled(boolean value) {
        getConfig().put(ENABLE_CRL, Boolean.toString(value));
        return this;
    }

    public boolean getOCSPEnabled() {
        return Boolean.parseBoolean(getConfig().get(ENABLE_OCSP));
    }

    public X509AuthenticatorConfigModel setOCSPEnabled(boolean value) {
        getConfig().put(ENABLE_OCSP, Boolean.toString(value));
        return this;
    }

    public boolean getCRLDistributionPointEnabled() {
        return Boolean.parseBoolean(getConfig().get(ENABLE_CRLDP));
    }

    public X509AuthenticatorConfigModel setCRLDistributionPointEnabled(boolean value) {
        getConfig().put(ENABLE_CRLDP, Boolean.toString(value));
        return this;
    }

    public String getCRLRelativePath() {
        return getConfig().getOrDefault(CRL_RELATIVE_PATH, null);
    }

    public X509AuthenticatorConfigModel setCRLRelativePath(String path) {
        if (path != null) {
            getConfig().put(CRL_RELATIVE_PATH, path);
        } else {
            getConfig().remove(CRL_RELATIVE_PATH);
        }
        return this;
    }

    public String getOCSPResponder() {
        return getConfig().getOrDefault(OCSPRESPONDER_URI, null);
    }

    public X509AuthenticatorConfigModel setOCSPResponder(String responderUri) {
        if (responderUri != null) {
            getConfig().put(OCSPRESPONDER_URI, responderUri);
        } else {
            getConfig().remove(OCSPRESPONDER_URI);
        }
        return this;
    }

    public MappingSourceType getMappingSourceType() {
        return MappingSourceType.parse(getConfig().getOrDefault(MAPPING_SOURCE_SELECTION, MAPPING_SOURCE_CERT_SUBJECTDN));
    }

    public X509AuthenticatorConfigModel setMappingSourceType(MappingSourceType value) {
        getConfig().put(MAPPING_SOURCE_SELECTION, value.getName());
        return this;
    }

    public IdentityMapperType getUserIdentityMapperType() {
        return IdentityMapperType.parse(getConfig().getOrDefault(USER_MAPPER_SELECTION, USERNAME_EMAIL_MAPPER));
    }

    public X509AuthenticatorConfigModel setUserIdentityMapperType(IdentityMapperType value) {
        getConfig().put(USER_MAPPER_SELECTION, value.getName());
        return this;
    }

    public String getRegularExpression() {
        return getConfig().getOrDefault(REGULAR_EXPRESSION,DEFAULT_MATCH_ALL_EXPRESSION);
    }

    public X509AuthenticatorConfigModel setRegularExpression(String value) {
        if (value != null) {
            getConfig().put(REGULAR_EXPRESSION, value);
        } else {
            getConfig().remove(REGULAR_EXPRESSION);
        }
        return this;
    }

    public String getCustomAttributeName() {
        return getConfig().getOrDefault(CUSTOM_ATTRIBUTE_NAME, DEFAULT_ATTRIBUTE_NAME);
    }

    public X509AuthenticatorConfigModel setCustomAttributeName(String value) {
        if (value != null) {
            getConfig().put(CUSTOM_ATTRIBUTE_NAME, value);
        } else {
            getConfig().remove(CUSTOM_ATTRIBUTE_NAME);
        }
        return this;
    }

    public String getKeyUsage() {
        return getConfig().getOrDefault(CERTIFICATE_KEY_USAGE, null);
    }

    public X509AuthenticatorConfigModel setKeyUsage(String value) {
        if (value != null) {
            getConfig().put(CERTIFICATE_KEY_USAGE, value);
        } else {
            getConfig().remove(CERTIFICATE_KEY_USAGE);
        }
        return this;
    }

    public String getExtendedKeyUsage() {
        return getConfig().getOrDefault(CERTIFICATE_EXTENDED_KEY_USAGE, null);
    }

    public X509AuthenticatorConfigModel setExtendedKeyUsage(String value) {
        if (value != null) {
            getConfig().put(CERTIFICATE_EXTENDED_KEY_USAGE, value);
        } else {
            getConfig().remove(CERTIFICATE_EXTENDED_KEY_USAGE);
        }
        return this;
    }

    public boolean getConfirmationPageDisallowed() {
        return Boolean.parseBoolean(getConfig().get(CONFIRMATION_PAGE_DISALLOWED));
    }

    public boolean getConfirmationPageAllowed() {
        return !Boolean.parseBoolean(getConfig().get(CONFIRMATION_PAGE_DISALLOWED));
    }

    public X509AuthenticatorConfigModel setConfirmationPageDisallowed(boolean value) {
        getConfig().put(CONFIRMATION_PAGE_DISALLOWED, Boolean.toString(value));
        return this;
    }

    public X509AuthenticatorConfigModel setConfirmationPageAllowed(boolean value) {
        getConfig().put(CONFIRMATION_PAGE_DISALLOWED, Boolean.toString(!value));
        return this;
    }

}
