package org.keycloak.testsuite.broker;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;

import java.util.List;

/**
 *
 * @author hmlnarik
 */
public interface BrokerConfiguration {

    /**
     * @return Representation of the realm at the identity provider side.
     */
    RealmRepresentation createProviderRealm();

    /**
     * @return Representation of the realm at the broker side.
     */
    RealmRepresentation createConsumerRealm();

    List<ClientRepresentation> createProviderClients(SuiteContext suiteContext);

    List<ClientRepresentation> createConsumerClients(SuiteContext suiteContext);

    /**
     * @return Representation of the identity provider for declaration in the broker
     */
    IdentityProviderRepresentation setUpIdentityProvider(SuiteContext suiteContext);

    /**
     * @return Name of realm containing identity provider. Must be consistent with {@link #createProviderRealm()}
     */
    String providerRealmName();

    /**
     * @return Realm name of the broker. Must be consistent with {@link #createConsumerRealm()}
     */
    String consumerRealmName();

    /**
     * @return Client ID of the identity provider as set in provider realm.
     */
    String getIDPClientIdInProviderRealm(SuiteContext suiteContext);

    /**
     * @return User login name of the brokered user
     */
    String getUserLogin();

    /**
     * @return Password of the brokered user
     */
    String getUserPassword();

    /**
     * @return E-mail of the brokered user
     */
    String getUserEmail();

    /**
     * @return Alias of the identity provider as defined in the broker realm
     */
    String getIDPAlias();
}
