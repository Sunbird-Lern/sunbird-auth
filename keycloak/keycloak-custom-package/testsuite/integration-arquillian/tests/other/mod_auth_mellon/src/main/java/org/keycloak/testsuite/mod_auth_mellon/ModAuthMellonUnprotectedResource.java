package org.keycloak.testsuite.mod_auth_mellon;

import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author mhajas
 */
public class ModAuthMellonUnprotectedResource extends AbstractPageWithInjectedUrl {

    @Override
    public URL getInjectedUrl() {
        try {
            return new URL(System.getProperty("apache.mod_auth_mellon.url", "http://localhost:8380/"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
