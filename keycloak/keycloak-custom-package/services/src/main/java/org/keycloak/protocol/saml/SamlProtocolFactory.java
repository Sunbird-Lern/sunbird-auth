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

package org.keycloak.protocol.saml;

import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.RoleListMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlProtocolFactory extends AbstractLoginProtocolFactory {

    private static final Pattern PROTOCOL_MAP_PATTERN = Pattern.compile("\\s*([a-zA-Z][a-zA-Z\\d+-.]*)\\s*=\\s*(\\d+)\\s*");
    private static final String[] DEFAULT_PROTOCOL_TO_PORT_MAP = new String[] { "http=80", "https=443" };

    private final Map<Integer, String> knownPorts = new HashMap<>();
    private final Map<String, Integer> knownProtocols = new HashMap<>();

    private void addToProtocolPortMaps(String protocolMapping) {
        Matcher m = PROTOCOL_MAP_PATTERN.matcher(protocolMapping);
        if (m.matches()) {
            Integer port = Integer.valueOf(m.group(2));
            String proto = m.group(1);

            knownPorts.put(port, proto);
            knownProtocols.put(proto, port);
        }
    }

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event) {
        return new SamlService(realm, event, knownProtocols, knownPorts);
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new SamlProtocol().setSession(session);
    }

    @Override
    public void init(Config.Scope config) {
        //PicketLinkCoreSTS sts = PicketLinkCoreSTS.instance();
        //sts.installDefaultConfiguration();

        String[] protocolMappings = config.getArray("knownProtocols");
        if (protocolMappings == null) {
            protocolMappings = DEFAULT_PROTOCOL_TO_PORT_MAP;
        }

        for (String protocolMapping : protocolMappings) {
            addToProtocolPortMaps(protocolMapping);
        }
    }

    @Override
    public String getId() {
        return SamlProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public List<ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    @Override
    public List<ProtocolMapperModel> getDefaultBuiltinMappers() {
        return defaultBuiltins;
    }

    static List<ProtocolMapperModel> builtins = new ArrayList<>();
    static List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    static {
        ProtocolMapperModel model;
        model = UserPropertyAttributeStatementMapper.createAttributeMapper("X500 email",
                "email",
                X500SAMLProfileConstants.EMAIL.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.EMAIL.getFriendlyName(),
                true, "${email}");
        builtins.add(model);
        model = UserPropertyAttributeStatementMapper.createAttributeMapper("X500 givenName",
                "firstName",
                X500SAMLProfileConstants.GIVEN_NAME.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.GIVEN_NAME.getFriendlyName(),
                true, "${givenName}");
        builtins.add(model);
        model = UserPropertyAttributeStatementMapper.createAttributeMapper("X500 surname",
                "lastName",
                X500SAMLProfileConstants.SURNAME.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.SURNAME.getFriendlyName(),
                true, "${familyName}");
        builtins.add(model);
        model = RoleListMapper.create("role list", "Role", AttributeStatementHelper.BASIC, null, false);
        builtins.add(model);
        defaultBuiltins.add(model);

    }


    @Override
    protected void addDefaults(ClientModel client) {
        for (ProtocolMapperModel model : defaultBuiltins) {
            model.setProtocol(getId());
            client.addProtocolMapper(model);
        }
    }

    @Override
    public void setupClientDefaults(ClientRepresentation clientRep, ClientModel newClient) {
        SamlRepresentationAttributes rep = new SamlRepresentationAttributes(clientRep.getAttributes());
        SamlClient client = new SamlClient(newClient);
        if (clientRep.isStandardFlowEnabled() == null) newClient.setStandardFlowEnabled(true);
        if (rep.getCanonicalizationMethod() == null) {
            client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        }
        if (rep.getSignatureAlgorithm() == null) {
            client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        }

        if (rep.getNameIDFormat() == null) {
            client.setNameIDFormat("username");
        }

        if (rep.getIncludeAuthnStatement() == null) {
            client.setIncludeAuthnStatement(true);
        }

        if (rep.getForceNameIDFormat() == null) {
            client.setForceNameIDFormat(false);
        }

        if (rep.getSamlServerSignature() == null) {
            client.setRequiresRealmSignature(true);
        }
        if (rep.getForcePostBinding() == null) {
            client.setForcePostBinding(true);
        }

        if (rep.getClientSignature() == null) {
            client.setRequiresClientSignature(true);
        }

        if (client.requiresClientSignature() && client.getClientSigningCertificate() == null) {
            CertificateRepresentation info = KeycloakModelUtils.generateKeyPairCertificate(newClient.getClientId());
            client.setClientSigningCertificate(info.getCertificate());
            client.setClientSigningPrivateKey(info.getPrivateKey());

        }

        if (clientRep.isFrontchannelLogout() == null) {
            newClient.setFrontchannelLogout(true);
        }
    }

    @Override
    public void setupTemplateDefaults(ClientTemplateRepresentation clientRep, ClientTemplateModel newClient) {
        SamlRepresentationAttributes rep = new SamlRepresentationAttributes(clientRep.getAttributes());
        SamlClientTemplate client = new SamlClientTemplate(newClient);
        if (clientRep.isStandardFlowEnabled() == null) newClient.setStandardFlowEnabled(true);
        if (rep.getCanonicalizationMethod() == null) {
            client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        }
        if (rep.getSignatureAlgorithm() == null) {
            client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        }

        if (rep.getNameIDFormat() == null) {
            client.setNameIDFormat("username");
        }

        if (rep.getIncludeAuthnStatement() == null) {
            client.setIncludeAuthnStatement(true);
        }

        if (rep.getForceNameIDFormat() == null) {
            client.setForceNameIDFormat(false);
        }

        if (rep.getSamlServerSignature() == null) {
            client.setRequiresRealmSignature(true);
        }
        if (rep.getForcePostBinding() == null) {
            client.setForcePostBinding(true);
        }

        if (rep.getClientSignature() == null) {
            client.setRequiresClientSignature(true);
        }

        if (clientRep.isFrontchannelLogout() == null) {
            newClient.setFrontchannelLogout(true);
        }
    }
}
