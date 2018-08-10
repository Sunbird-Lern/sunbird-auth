/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.util.matchers;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import java.net.URI;
import org.hamcrest.*;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author hmlnarik
 */
public class SamlLogoutRequestTypeMatcher extends BaseMatcher<SAML2Object> {

    private final Matcher<URI> destinationMatcher;

    public SamlLogoutRequestTypeMatcher(URI destination) {
        this.destinationMatcher = is(destination);
    }

    public SamlLogoutRequestTypeMatcher(Matcher<URI> destinationMatcher) {
        this.destinationMatcher = destinationMatcher;
    }

    @Override
    public boolean matches(Object item) {
        return destinationMatcher.matches(((LogoutRequestType) item).getDestination());
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        description.appendText("was ").appendValue(((LogoutRequestType) item).getDestination());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("SAML logout request destination matches ").appendDescriptionOf(this.destinationMatcher);
    }
}
