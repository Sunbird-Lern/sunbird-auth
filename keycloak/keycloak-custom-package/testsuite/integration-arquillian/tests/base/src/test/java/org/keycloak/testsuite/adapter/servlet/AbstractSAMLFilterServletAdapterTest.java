package org.keycloak.testsuite.adapter.servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.arquillian.annotation.UseServletFilter;

/**
 * @author mhajas
 */

@UseServletFilter(filterName = "saml-filter", filterClass = "org.keycloak.adapters.saml.servlet.SamlFilter",
        filterDependency = "org.keycloak:keycloak-saml-servlet-filter-adapter")
public abstract class AbstractSAMLFilterServletAdapterTest extends AbstractSAMLServletsAdapterTest {

    @Before
    public void checkRoles() {
        badClientSalesPostSigServletPage.checkRoles(true);
        badRealmSalesPostSigServletPage.checkRoles(true);
        employeeAcsServletPage.checkRoles(true);
        employeeSigServletPage.checkRoles(true);
        employeeSigFrontServletPage.checkRoles(true);
        salesMetadataServletPage.checkRoles(true);
        salesPostServletPage.checkRoles(true);
        salesPostEncServletPage.checkRoles(true);
        salesPostEncSignAssertionsOnlyServletPage.checkRoles(true);
        salesPostSigServletPage.checkRoles(true);
        salesPostPassiveServletPage.checkRoles(true);
        salesPostSigPersistentServletPage.checkRoles(true);
        salesPostSigTransientServletPage.checkRoles(true);
        salesPostAssertionAndResponseSigPage.checkRoles(true);
        employeeSigPostNoIdpKeyServletPage.checkRoles(true);
        employeeSigRedirNoIdpKeyServletPage.checkRoles(true);
        employeeSigRedirOptNoIdpKeyServletPage.checkRoles(true);

        //using endpoint instead of query param because we are not able to put query param to IDP initiated login
        employee2ServletPage.navigateTo();
        testRealmLoginPage.form().login(bburkeUser);
        employee2ServletPage.checkRolesEndPoint(true);
        employee2ServletPage.logout();

        salesPostSigEmailServletPage.navigateTo();
        testRealmLoginPage.form().login(bburkeUser);
        salesPostSigEmailServletPage.checkRolesEndPoint(true);
        salesPostSigEmailServletPage.logout();
    }

    @After
    public void uncheckRoles() {
        badClientSalesPostSigServletPage.checkRoles(false);
        badRealmSalesPostSigServletPage.checkRoles(false);
        employeeAcsServletPage.checkRoles(false);
        employee2ServletPage.checkRoles(false);
        employeeSigServletPage.checkRoles(false);
        employeeSigFrontServletPage.checkRoles(false);
        salesMetadataServletPage.checkRoles(false);
        salesPostServletPage.checkRoles(false);
        salesPostEncServletPage.checkRoles(false);
        salesPostEncSignAssertionsOnlyServletPage.checkRoles(false);
        salesPostSigServletPage.checkRoles(false);
        salesPostPassiveServletPage.checkRoles(false);
        salesPostSigEmailServletPage.checkRoles(false);
        salesPostSigPersistentServletPage.checkRoles(false);
        salesPostSigTransientServletPage.checkRoles(false);
        employeeSigPostNoIdpKeyServletPage.checkRoles(false);
        employeeSigRedirNoIdpKeyServletPage.checkRoles(false);
        employeeSigRedirOptNoIdpKeyServletPage.checkRoles(false);
    }

    @Test
    @Override
    @Ignore
    public void testSavedPostRequest() {

    }

    @Test
    @Override
    @Ignore
    public void testErrorHandlingUnsigned() {

    }
}
