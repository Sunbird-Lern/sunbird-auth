package org.sunbird.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

/**
 * Created by nickpack on 09/08/2017.
 */
public class KeycloakSmsAuthenticatorCredentialProviderFactory implements CredentialProviderFactory<KeycloakSmsAuthenticatorCredentialProvider> {
    private static Logger logger = Logger.getLogger(KeycloakSmsAuthenticator.class);

    @Override
    public String getId() {
        return "smsCode";
    }

    @Override
    public CredentialProvider create(KeycloakSession session) {
        logger.debug("KeycloakSmsAuthenticatorCredentialProviderFactory -  create");
        return new KeycloakSmsAuthenticatorCredentialProvider(session);
    }
}
