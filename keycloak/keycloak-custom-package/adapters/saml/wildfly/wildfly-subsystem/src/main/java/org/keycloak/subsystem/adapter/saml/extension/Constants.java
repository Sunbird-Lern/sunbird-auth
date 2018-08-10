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
package org.keycloak.subsystem.adapter.saml.extension;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class Constants {

    static class Model {
        static final String SECURE_DEPLOYMENT = "secure-deployment";
        static final String SERVICE_PROVIDER = "SP";

        static final String SSL_POLICY = "sslPolicy";
        static final String NAME_ID_POLICY_FORMAT = "nameIDPolicyFormat";
        static final String LOGOUT_PAGE = "logoutPage";
        static final String FORCE_AUTHENTICATION = "forceAuthentication";
        static final String IS_PASSIVE = "isPassive";
        static final String TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN = "turnOffChangeSessionIdOnLogin";
        static final String ROLE_ATTRIBUTES = "RoleIdentifiers";
        static final String SIGNING = "signing";
        static final String ENCRYPTION = "encryption";
        static final String KEY = "Key";
        static final String RESOURCE = "resource";
        static final String PASSWORD = "password";

        static final String PRIVATE_KEY_ALIAS = "PrivateKey-alias";
        static final String PRIVATE_KEY_PASSWORD = "PrivateKey-password";
        static final String CERTIFICATE_ALIAS = "Certificate-alias";
        static final String KEY_STORE = "KeyStore";
        static final String SIGN_REQUEST = "signRequest";
        static final String VALIDATE_RESPONSE_SIGNATURE = "validateResponseSignature";
        static final String VALIDATE_ASSERTION_SIGNATURE = "validateAssertionSignature";
        static final String REQUEST_BINDING = "requestBinding";
        static final String BINDING_URL = "bindingUrl";
        static final String VALIDATE_REQUEST_SIGNATURE = "validateRequestSignature";
        static final String SIGN_RESPONSE = "signResponse";
        static final String RESPONSE_BINDING = "responseBinding";
        static final String POST_BINDING_URL = "postBindingUrl";
        static final String REDIRECT_BINDING_URL = "redirectBindingUrl";
        static final String SINGLE_SIGN_ON = "SingleSignOnService";
        static final String SINGLE_LOGOUT = "SingleLogoutService";
        static final String IDENTITY_PROVIDER = "IDP";
        static final String PRINCIPAL_NAME_MAPPING_POLICY = "PrincipalNameMapping-policy";
        static final String PRINCIPAL_NAME_MAPPING_ATTRIBUTE_NAME = "PrincipalNameMapping-attribute-name";
        static final String SIGNATURE_ALGORITHM = "signatureAlgorithm";
        static final String SIGNATURE_CANONICALIZATION_METHOD = "signatureCanonicalizationMethod";
        static final String PRIVATE_KEY_PEM = "PrivateKeyPem";
        static final String PUBLIC_KEY_PEM = "PublicKeyPem";
        static final String CERTIFICATE_PEM = "CertificatePem";
        static final String TYPE = "type";
        static final String ALIAS = "alias";
        static final String FILE = "file";
        static final String SIGNATURES_REQUIRED = "signaturesRequired";
    }


    static class XML {
        static final String SECURE_DEPLOYMENT = "secure-deployment";
        static final String SERVICE_PROVIDER = "SP";

        static final String NAME = "name";
        static final String ENTITY_ID = "entityID";
        static final String SSL_POLICY = "sslPolicy";
        static final String NAME_ID_POLICY_FORMAT = "nameIDPolicyFormat";
        static final String LOGOUT_PAGE = "logoutPage";
        static final String FORCE_AUTHENTICATION = "forceAuthentication";
        static final String ROLE_IDENTIFIERS = "RoleIdentifiers";
        static final String SIGNING = "signing";
        static final String ENCRYPTION = "encryption";
        static final String KEYS = "Keys";
        static final String KEY = "Key";
        static final String RESOURCE = "resource";
        static final String PASSWORD = "password";
        static final String KEY_STORE = "KeyStore";
        static final String PRIVATE_KEY = "PrivateKey";
        static final String CERTIFICATE = "Certificate";
        static final String IS_PASSIVE = "isPassive";
        static final String TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN = "turnOffChangeSessionIdOnLogin";

        static final String PRIVATE_KEY_ALIAS = "alias";
        static final String PRIVATE_KEY_PASSWORD = "password";
        static final String CERTIFICATE_ALIAS = "alias";
        static final String SIGN_REQUEST = "signRequest";
        static final String VALIDATE_RESPONSE_SIGNATURE = "validateResponseSignature";
        static final String VALIDATE_ASSERTION_SIGNATURE = "validateAssertionSignature";
        static final String REQUEST_BINDING = "requestBinding";
        static final String BINDING_URL = "bindingUrl";
        static final String VALIDATE_REQUEST_SIGNATURE = "validateRequestSignature";
        static final String SIGN_RESPONSE = "signResponse";
        static final String RESPONSE_BINDING = "responseBinding";
        static final String POST_BINDING_URL = "postBindingUrl";
        static final String REDIRECT_BINDING_URL = "redirectBindingUrl";
        static final String SINGLE_SIGN_ON = "SingleSignOnService";
        static final String SINGLE_LOGOUT = "SingleLogoutService";
        static final String IDENTITY_PROVIDER = "IDP";
        static final String PRINCIPAL_NAME_MAPPING = "PrincipalNameMapping";
        static final String PRINCIPAL_NAME_MAPPING_POLICY = "policy";
        static final String PRINCIPAL_NAME_MAPPING_ATTRIBUTE_NAME = "attribute";
        static final String ATTRIBUTE = "Attribute";
        static final String SIGNATURE_ALGORITHM = "signatureAlgorithm";
        static final String SIGNATURE_CANONICALIZATION_METHOD = "signatureCanonicalizationMethod";
        static final String PRIVATE_KEY_PEM = "PrivateKeyPem";
        static final String PUBLIC_KEY_PEM = "PublicKeyPem";
        static final String CERTIFICATE_PEM = "CertificatePem";
        static final String TYPE = "type";
        static final String ALIAS = "alias";
        static final String FILE = "file";
        static final String SIGNATURES_REQUIRED = "signaturesRequired";
    }
}
