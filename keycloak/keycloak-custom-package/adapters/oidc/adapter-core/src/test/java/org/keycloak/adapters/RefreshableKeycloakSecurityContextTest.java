package org.keycloak.adapters;

import org.junit.Test;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;

import static org.junit.Assert.assertFalse;
/**
 * @author github.com/tubbynl
 *
 */
public class RefreshableKeycloakSecurityContextTest {

	@Test
	public void isActive() {
		TokenMetadataRepresentation token = new TokenMetadataRepresentation();
		token.setActive(true);
		token.issuedNow();
		RefreshableKeycloakSecurityContext sut = new RefreshableKeycloakSecurityContext(null,null,null,token,null, null, null);
		
		// verify false if null deployment (KEYCLOAK-3050; yielded a npe)
		assertFalse(sut.isActive());
	}
}
