package org.keycloak.testsuite.console.page.roles;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.AdminConsoleCreate;

/**
 *
 * @author tkyjovsk
 */
public class CreateRole extends AdminConsoleCreate {

    public CreateRole() {
        setEntity("role");
    }

    @Page
    private RoleForm form;
    
    public RoleForm form() {
        return form;
    }

}
