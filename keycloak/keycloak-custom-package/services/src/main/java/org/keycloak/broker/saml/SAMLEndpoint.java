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

package org.keycloak.broker.saml;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.w3c.dom.Element;

import java.util.*;
import javax.xml.crypto.dsig.XMLSignature;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAMLEndpoint {
    protected static final Logger logger = Logger.getLogger(SAMLEndpoint.class);
    public static final String SAML_FEDERATED_SESSION_INDEX = "SAML_FEDERATED_SESSION_INDEX";
    public static final String SAML_FEDERATED_SUBJECT = "SAML_FEDERATED_SUBJECT";
    public static final String SAML_FEDERATED_SUBJECT_NAMEFORMAT = "SAML_FEDERATED_SUBJECT_NAMEFORMAT";
    public static final String SAML_LOGIN_RESPONSE = "SAML_LOGIN_RESPONSE";
    public static final String SAML_ASSERTION = "SAML_ASSERTION";
    public static final String SAML_IDP_INITIATED_CLIENT_ID = "SAML_IDP_INITIATED_CLIENT_ID";
    public static final String SAML_AUTHN_STATEMENT = "SAML_AUTHN_STATEMENT";
    protected RealmModel realm;
    protected EventBuilder event;
    protected SAMLIdentityProviderConfig config;
    protected IdentityProvider.AuthenticationCallback callback;
    protected SAMLIdentityProvider provider;

    @Context
    private UriInfo uriInfo;

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    @Context
    private HttpHeaders headers;


    public SAMLEndpoint(RealmModel realm, SAMLIdentityProvider provider, SAMLIdentityProviderConfig config, IdentityProvider.AuthenticationCallback callback) {
        this.realm = realm;
        this.config = config;
        this.callback = callback;
        this.provider = provider;
    }

    @GET
    @NoCache
    @Path("descriptor")
    public Response getSPDescriptor() {
        return provider.export(uriInfo, realm, null);
    }

    @GET
    public Response redirectBinding(@QueryParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                    @QueryParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                    @QueryParam(GeneralConstants.RELAY_STATE) String relayState)  {
        return new RedirectBinding().execute(samlRequest, samlResponse, relayState, null);
    }


    /**
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postBinding(@FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                @FormParam(GeneralConstants.RELAY_STATE) String relayState) {
        return new PostBinding().execute(samlRequest, samlResponse, relayState, null);
    }

    @Path("clients/{client_id}")
    @GET
    public Response redirectBinding(@QueryParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                    @QueryParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                    @QueryParam(GeneralConstants.RELAY_STATE) String relayState,
                                    @PathParam("client_id") String clientId)  {
        return new RedirectBinding().execute(samlRequest, samlResponse, relayState, clientId);
    }


    /**
     */
    @Path("clients/{client_id}")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postBinding(@FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                @FormParam(GeneralConstants.RELAY_STATE) String relayState,
                                @PathParam("client_id") String clientId) {
        return new PostBinding().execute(samlRequest, samlResponse, relayState, clientId);
    }

    protected abstract class Binding {
        private boolean checkSsl() {
            if (uriInfo.getBaseUri().getScheme().equals("https")) {
                return true;
            } else {
                return !realm.getSslRequired().isRequired(clientConnection);
            }
        }

        protected Response basicChecks(String samlRequest, String samlResponse) {
            if (!checkSsl()) {
                event.event(EventType.LOGIN);
                event.error(Errors.SSL_REQUIRED);
                return ErrorPage.error(session, Messages.HTTPS_REQUIRED);
            }
            if (!realm.isEnabled()) {
                event.event(EventType.LOGIN_ERROR);
                event.error(Errors.REALM_DISABLED);
                return ErrorPage.error(session, Messages.REALM_NOT_ENABLED);
            }

            if (samlRequest == null && samlResponse == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_REQUEST);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);

            }
            return null;
        }

        protected abstract String getBindingType();
        protected abstract void verifySignature(String key, SAMLDocumentHolder documentHolder) throws VerificationException;
        protected abstract SAMLDocumentHolder extractRequestDocument(String samlRequest);
        protected abstract SAMLDocumentHolder extractResponseDocument(String response);
        
        protected KeyLocator getIDPKeyLocator() {
            List<Key> keys = new LinkedList<>();

            for (String signingCertificate : config.getSigningCertificates()) {
                try {
                    X509Certificate cert = XMLSignatureUtil.getX509CertificateFromKeyInfoString(signingCertificate.replaceAll("\\s", ""));
                    keys.add(cert.getPublicKey());
                } catch (ProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            return new HardcodedKeyLocator(keys);
        }

        public Response execute(String samlRequest, String samlResponse, String relayState, String clientId) {
            event = new EventBuilder(realm, session, clientConnection);
            Response response = basicChecks(samlRequest, samlResponse);
            if (response != null) return response;
            if (samlRequest != null) return handleSamlRequest(samlRequest, relayState);
            else return handleSamlResponse(samlResponse, relayState, clientId);
        }

        protected Response handleSamlRequest(String samlRequest, String relayState) {
            SAMLDocumentHolder holder = extractRequestDocument(samlRequest);
            RequestAbstractType requestAbstractType = (RequestAbstractType) holder.getSamlObject();
            // validate destination
            if (requestAbstractType.getDestination() != null && !uriInfo.getAbsolutePath().equals(requestAbstractType.getDestination())) {
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.detail(Details.REASON, "invalid_destination");
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);
            }
            if (config.isValidateSignature()) {
                try {
                    verifySignature(GeneralConstants.SAML_REQUEST_KEY, holder);
                } catch (VerificationException e) {
                    logger.error("validation failed", e);
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SIGNATURE);
                    return ErrorPage.error(session, Messages.INVALID_REQUESTER);
                }
            }

            if (requestAbstractType instanceof LogoutRequestType) {
                logger.debug("** logout request");
                event.event(EventType.LOGOUT);
                LogoutRequestType logout = (LogoutRequestType) requestAbstractType;
                return logoutRequest(logout, relayState);

            } else {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);
            }
        }

        protected Response logoutRequest(LogoutRequestType request, String relayState) {
            String brokerUserId = config.getAlias() + "." + request.getNameID().getValue();
            if (request.getSessionIndex() == null || request.getSessionIndex().isEmpty()) {
                List<UserSessionModel> userSessions = session.sessions().getUserSessionByBrokerUserId(realm, brokerUserId);
                for (UserSessionModel userSession : userSessions) {
                    if (userSession.getState() == UserSessionModel.State.LOGGING_OUT || userSession.getState() == UserSessionModel.State.LOGGED_OUT) {
                        continue;
                    }
                    try {
                        AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers, false);
                    } catch (Exception e) {
                        logger.warn("failed to do backchannel logout for userSession", e);
                    }
                }

            }  else {
                for (String sessionIndex : request.getSessionIndex()) {
                    String brokerSessionId = brokerUserId + "." + sessionIndex;
                    UserSessionModel userSession = session.sessions().getUserSessionByBrokerSessionId(realm, brokerSessionId);
                    if (userSession != null) {
                        if (userSession.getState() == UserSessionModel.State.LOGGING_OUT || userSession.getState() == UserSessionModel.State.LOGGED_OUT) {
                            continue;
                        }
                        try {
                            AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers, false);
                        } catch (Exception e) {
                            logger.warn("failed to do backchannel logout for userSession", e);
                        }
                    }
                }
            }

            String issuerURL = getEntityId(uriInfo, realm);
            SAML2LogoutResponseBuilder builder = new SAML2LogoutResponseBuilder();
            builder.logoutRequestID(request.getID());
            builder.destination(config.getSingleLogoutServiceUrl());
            builder.issuer(issuerURL);
            JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder()
                        .relayState(relayState);
            boolean postBinding = config.isPostBindingLogout();
            if (config.isWantAuthnRequestsSigned()) {
                KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);
                String keyName = config.getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
                binding.signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate())
                        .signatureAlgorithm(provider.getSignatureAlgorithm())
                        .signDocument();
                if (! postBinding && config.isAddExtensionsElementWithKeyInfo()) {    // Only include extension if REDIRECT binding and signing whole SAML protocol message
                    builder.addExtension(new KeycloakKeySamlExtensionGenerator(keyName));
                }
            }
            try {
                if (postBinding) {
                    return binding.postBinding(builder.buildDocument()).response(config.getSingleLogoutServiceUrl());
                } else {
                    return binding.redirectBinding(builder.buildDocument()).response(config.getSingleLogoutServiceUrl());
                }
            } catch (ConfigurationException e) {
                throw new RuntimeException(e);
            } catch (ProcessingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        private String getEntityId(UriInfo uriInfo, RealmModel realm) {
            return UriBuilder.fromUri(uriInfo.getBaseUri()).path("realms").path(realm.getName()).build().toString();
        }
        protected Response handleLoginResponse(String samlResponse, SAMLDocumentHolder holder, ResponseType responseType, String relayState, String clientId) {

            try {
                KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);
                if (! isSuccessfulSamlResponse(responseType)) {
                    String statusMessage = responseType.getStatus() == null ? Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR : responseType.getStatus().getStatusMessage();
                    return callback.error(relayState, statusMessage);
                }
                if (responseType.getAssertions() == null || responseType.getAssertions().isEmpty()) {
                    return callback.error(relayState, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                }

                boolean assertionIsEncrypted = AssertionUtil.isAssertionEncrypted(responseType);

                if (config.isWantAssertionsEncrypted() && !assertionIsEncrypted) {
                    logger.error("The assertion is not encrypted, which is required.");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, Messages.INVALID_REQUESTER);
                }

                Element assertionElement;

                if (assertionIsEncrypted) {
                    // This methods writes the parsed and decrypted assertion back on the responseType parameter:
                    assertionElement = AssertionUtil.decryptAssertion(responseType, keys.getPrivateKey());
                } else {
                    /* We verify the assertion using original document to handle cases where the IdP
                    includes whitespace and/or newlines inside tags. */
                    assertionElement = DocumentUtil.getElement(holder.getSamlDocument(), new QName(JBossSAMLConstants.ASSERTION.get()));
                }

                boolean signed = AssertionUtil.isSignedElement(assertionElement);
                if ((config.isWantAssertionsSigned() && !signed)
                        || (signed && config.isValidateSignature() && !AssertionUtil.isSignatureValid(assertionElement, getIDPKeyLocator()))) {
                    logger.error("validation failed");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SIGNATURE);
                    return ErrorPage.error(session, Messages.INVALID_REQUESTER);
                }

                AssertionType assertion = responseType.getAssertions().get(0).getAssertion();

                SubjectType subject = assertion.getSubject();
                SubjectType.STSubType subType = subject.getSubType();
                NameIDType subjectNameID = (NameIDType) subType.getBaseID();
                //Map<String, String> notes = new HashMap<>();
                BrokeredIdentityContext identity = new BrokeredIdentityContext(subjectNameID.getValue());
                identity.getContextData().put(SAML_LOGIN_RESPONSE, responseType);
                identity.getContextData().put(SAML_ASSERTION, assertion);
                if (clientId != null && ! clientId.trim().isEmpty()) {
                    identity.getContextData().put(SAML_IDP_INITIATED_CLIENT_ID, clientId);
                }

                identity.setUsername(subjectNameID.getValue());

                //SAML Spec 2.2.2 Format is optional
                if (subjectNameID.getFormat() != null && subjectNameID.getFormat().toString().equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())) {
                    identity.setEmail(subjectNameID.getValue());
                }

                if (config.isStoreToken()) {
                    identity.setToken(samlResponse);
                }

                AuthnStatementType authn = null;
                for (Object statement : assertion.getStatements()) {
                    if (statement instanceof AuthnStatementType) {
                        authn = (AuthnStatementType)statement;
                        identity.getContextData().put(SAML_AUTHN_STATEMENT, authn);
                        break;
                    }
                }
                if (assertion.getAttributeStatements() != null ) {
                    for (AttributeStatementType attrStatement : assertion.getAttributeStatements()) {
                        for (AttributeStatementType.ASTChoiceType choice : attrStatement.getAttributes()) {
                            AttributeType attribute = choice.getAttribute();
                            if (X500SAMLProfileConstants.EMAIL.getFriendlyName().equals(attribute.getFriendlyName())
                                    || X500SAMLProfileConstants.EMAIL.get().equals(attribute.getName())) {
                                if (!attribute.getAttributeValue().isEmpty()) identity.setEmail(attribute.getAttributeValue().get(0).toString());
                            }
                        }

                    }

                }
                String brokerUserId = config.getAlias() + "." + subjectNameID.getValue();
                identity.setBrokerUserId(brokerUserId);
                identity.setIdpConfig(config);
                identity.setIdp(provider);
                if (authn != null && authn.getSessionIndex() != null) {
                    identity.setBrokerSessionId(identity.getBrokerUserId() + "." + authn.getSessionIndex());
                 }
                identity.setCode(relayState);


                return callback.authenticated(identity);
            } catch (WebApplicationException e) {
                return e.getResponse();
            } catch (Exception e) {
                throw new IdentityBrokerException("Could not process response from SAML identity provider.", e);
            }
        }


        private boolean isSuccessfulSamlResponse(ResponseType responseType) {
            return responseType != null
              && responseType.getStatus() != null
              && responseType.getStatus().getStatusCode() != null
              && responseType.getStatus().getStatusCode().getValue() != null
              && Objects.equals(responseType.getStatus().getStatusCode().getValue().toString(), JBossSAMLURIConstants.STATUS_SUCCESS.get());
        }


        public Response handleSamlResponse(String samlResponse, String relayState, String clientId) {
            SAMLDocumentHolder holder = extractResponseDocument(samlResponse);
            StatusResponseType statusResponse = (StatusResponseType)holder.getSamlObject();
            // validate destination
            if (statusResponse.getDestination() != null && !uriInfo.getAbsolutePath().toString().equals(statusResponse.getDestination())) {
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.detail(Details.REASON, "invalid_destination");
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, Messages.INVALID_FEDERATED_IDENTITY_ACTION);
            }
            if (config.isValidateSignature()) {
                try {
                    verifySignature(GeneralConstants.SAML_RESPONSE_KEY, holder);
                } catch (VerificationException e) {
                    logger.error("validation failed", e);
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SIGNATURE);
                    return ErrorPage.error(session, Messages.INVALID_FEDERATED_IDENTITY_ACTION);
                }
            }
            if (statusResponse instanceof ResponseType) {
                return handleLoginResponse(samlResponse, holder, (ResponseType)statusResponse, relayState, clientId);

            } else {
                // todo need to check that it is actually a LogoutResponse
                return handleLogoutResponse(holder, statusResponse, relayState);
            }
            //throw new RuntimeException("Unknown response type");

        }

        protected Response handleLogoutResponse(SAMLDocumentHolder holder, StatusResponseType responseType, String relayState) {
            if (relayState == null) {
                logger.error("no valid user session");
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(session, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
            UserSessionModel userSession = session.sessions().getUserSession(realm, relayState);
            if (userSession == null) {
                logger.error("no valid user session");
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(session, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
            if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
                logger.error("usersession in different state");
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(session, Messages.SESSION_NOT_ACTIVE);
            }
            return AuthenticationManager.finishBrowserLogout(session, realm, userSession, uriInfo, clientConnection, headers);
        }





    }

    protected class PostBinding extends Binding {
        @Override
        protected void verifySignature(String key, SAMLDocumentHolder documentHolder) throws VerificationException {
            NodeList nl = documentHolder.getSamlDocument().getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            boolean anyElementSigned = (nl != null && nl.getLength() > 0);
            if ((! anyElementSigned) && (documentHolder.getSamlObject() instanceof ResponseType)) {
                ResponseType responseType = (ResponseType) documentHolder.getSamlObject();
                List<ResponseType.RTChoiceType> assertions = responseType.getAssertions();
                if (! assertions.isEmpty() ) {
                    // Only relax verification if the response is an authnresponse and contains (encrypted/plaintext) assertion.
                    // In that case, signature is validated on assertion element
                    return;
                }
            }
            SamlProtocolUtils.verifyDocumentSignature(documentHolder.getSamlDocument(), getIDPKeyLocator());
        }

        @Override
        protected SAMLDocumentHolder extractRequestDocument(String samlRequest) {
            return SAMLRequestParser.parseRequestPostBinding(samlRequest);
        }
        @Override
        protected SAMLDocumentHolder extractResponseDocument(String response) {
            byte[] samlBytes = PostBindingUtil.base64Decode(response);
            return SAMLRequestParser.parseResponseDocument(samlBytes);
        }

        @Override
        protected String getBindingType() {
            return SamlProtocol.SAML_POST_BINDING;
        }
    }

    protected class RedirectBinding extends Binding {
        @Override
        protected void verifySignature(String key, SAMLDocumentHolder documentHolder) throws VerificationException {
            KeyLocator locator = getIDPKeyLocator();
            SamlProtocolUtils.verifyRedirectSignature(documentHolder, locator, uriInfo, key);
        }



        @Override
        protected SAMLDocumentHolder extractRequestDocument(String samlRequest) {
            return SAMLRequestParser.parseRequestRedirectBinding(samlRequest);
        }

        @Override
        protected SAMLDocumentHolder extractResponseDocument(String response) {
            return SAMLRequestParser.parseResponseRedirectBinding(response);
        }

        @Override
        protected String getBindingType() {
            return SamlProtocol.SAML_REDIRECT_BINDING;
        }

    }

}
