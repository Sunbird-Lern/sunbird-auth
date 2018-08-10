package org.keycloak.testsuite.cli.admin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.client.admin.cli.config.FileConfigHandler;
import org.keycloak.testsuite.cli.KcAdmExec;
import org.keycloak.testsuite.util.TempFileResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.cli.KcAdmExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcAdmSessionTest extends AbstractAdmCliTest {

    static Class<? extends List<ObjectNode>> LIST_OF_JSON = new ArrayList<ObjectNode>() {}.getClass();

    @Test
    public void test() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            // login as admin
            loginAsUser(configFile.getFile(), serverUrl, "master", "admin", "admin");

            // create realm
            KcAdmExec exe = execute("create realms --config '" + configFile.getName() + "' -s realm=demorealm -s enabled=true");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);
            Assert.assertTrue(exe.stderrLines().get(0).startsWith("Created "));

            // create user
            exe = execute("create users --config '" + configFile.getName() + "' -r demorealm -s username=testuser -s enabled=true -i");

            assertExitCodeAndStreamSizes(exe, 0, 1, 0);
            String userId = exe.stdoutLines().get(0);

            // add realm admin capabilities to user
            exe = execute("add-roles --config '" + configFile.getName() + "' -r demorealm --uusername testuser --cclientid realm-management --rolename realm-admin");

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // set password for the user
            exe = execute("set-password --config '" + configFile.getName() + "' -r demorealm --username testuser -p password");

            assertExitCodeAndStdErrSize(exe, 0, 0);


            // login as testuser
            loginAsUser(configFile.getFile(), serverUrl, "demorealm", "testuser", "password");


            // get realm roles
            exe = execute("get-roles --config '" + configFile.getName() + "'");

            assertExitCodeAndStdErrSize(exe, 0, 0);
            List<ObjectNode> roles = loadJson(exe.stdout(), LIST_OF_JSON);
            Assert.assertTrue("expect two realm roles available", roles.size() == 2);

            // create realm role
            exe = execute("create roles --config '" + configFile.getName() + "' -s name=testrole -s 'description=Test role' -o");

            assertExitCodeAndStdErrSize(exe, 0, 0);
            ObjectNode role = loadJson(exe.stdout(), ObjectNode.class);
            Assert.assertEquals("testrole", role.get("name").asText());
            String roleId = role.get("id").asText();

            // get realm roles again
            exe = execute("get-roles --config '" + configFile.getName() + "'");

            assertExitCodeAndStdErrSize(exe, 0, 0);
            roles = loadJson(exe.stdout(), LIST_OF_JSON);
            Assert.assertTrue("expect three realm roles available", roles.size() == 3);

            // create client
            exe = execute("create clients --config '" + configFile.getName() + "' -s clientId=testclient -i");

            assertExitCodeAndStreamSizes(exe, 0, 1, 0);
            String idOfClient = exe.stdoutLines().get(0);


            // create client role
            exe = execute("create clients/" + idOfClient + "/roles --config '" + configFile.getName() + "' -s name=clientrole  -s 'description=Test client role'");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);
            Assert.assertTrue(exe.stderrLines().get(0).startsWith("Created "));

            // make sure client role has been created
            exe = execute("get-roles --config '" + configFile.getName() + "' --cclientid testclient");

            assertExitCodeAndStdErrSize(exe, 0, 0);
            roles = loadJson(exe.stdout(), LIST_OF_JSON);
            Assert.assertTrue("expect one role", roles.size() == 1);
            Assert.assertEquals("clientrole", roles.get(0).get("name").asText());

            // add created role to user - we are realm admin so we can add role to ourself
            exe = execute("add-roles --config '" + configFile.getName() + "' --uusername testuser --cclientid testclient --rolename clientrole");

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);


            // make sure the roles have been added
            exe = execute("get-roles --config '" + configFile.getName() + "' --uusername testuser --all");

            assertExitCodeAndStdErrSize(exe, 0, 0);
            ObjectNode node = loadJson(exe.stdout(), ObjectNode.class);
            Assert.assertNotNull(node.get("realmMappings"));

            List<String> realmMappings = StreamSupport.stream(node.get("realmMappings").spliterator(), false)
                    .map(o -> o.get("name").asText()).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("offline_access", "uma_authorization"), realmMappings);

            ObjectNode clientRoles = (ObjectNode) node.get("clientMappings");
            //List<String> fields = asSortedList(clientRoles.fieldNames());
            List<String> fields = StreamSupport.stream(clientRoles.spliterator(), false)
                    .map(o -> o.get("client").asText()).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("account", "realm-management", "testclient"), fields);

            realmMappings = StreamSupport.stream(clientRoles.get("account").get("mappings").spliterator(), false)
                    .map(o -> o.get("name").asText()).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("manage-account", "view-profile"), realmMappings);

            realmMappings = StreamSupport.stream(clientRoles.get("realm-management").get("mappings").spliterator(), false)
                    .map(o -> o.get("name").asText()).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("realm-admin"), realmMappings);

            realmMappings = StreamSupport.stream(clientRoles.get("testclient").get("mappings").spliterator(), false)
                    .map(o -> o.get("name").asText()).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("clientrole"), realmMappings);



            // add a realm role to the user
            exe = execute("add-roles --config '" + configFile.getName() + "' --uusername testuser --rolename testrole");

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);


            // get all roles for the user again
            exe = execute("get-roles --config '" + configFile.getName() + "' --uusername testuser --all");
            assertExitCodeAndStdErrSize(exe, 0, 0);

            node = loadJson(exe.stdout(), ObjectNode.class);
            Assert.assertNotNull(node.get("realmMappings"));

            realmMappings = StreamSupport.stream(node.get("realmMappings").spliterator(), false)
                    .map(o -> o.get("name").asText()).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("offline_access", "testrole", "uma_authorization"), realmMappings);

            // create a group
            exe = execute("create groups --config '" + configFile.getName() + "' -s name=TestUsers -i");
            assertExitCodeAndStdErrSize(exe, 0, 0);
            String groupId = exe.stdoutLines().get(0);

            // create a sub-group
            exe = execute("create groups/" + groupId + "/children --config '" + configFile.getName() + "' -s name=TestPowerUsers -i");
            assertExitCodeAndStdErrSize(exe, 0, 0);
            String subGroupId = exe.stdoutLines().get(0);

            // add testuser to TestPowerUsers
            exe = execute("update users/" + userId + "/groups/" + subGroupId + " --config '" + configFile.getName()
                    + "' -s realm=demorealm -s userId=" + userId +  " -s groupId=" + subGroupId + " -n");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // delete everything
            exe = execute("delete groups/" + subGroupId + " --config '" + configFile.getName() + "'");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            exe = execute("delete groups/" + groupId + " --config '" + configFile.getName() + "'");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            exe = execute("delete clients/" + idOfClient + " --config '" + configFile.getName() + "'");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            exe = execute("delete roles/testrole --config '" + configFile.getName() + "'");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            exe = execute("delete users/" + userId + " --config '" + configFile.getName() + "'");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // delete realm as well - using initial master realm session still saved in config file
            exe = execute("delete realms/demorealm --config '" + configFile.getName() + "' --realm master");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);
        }
    }
}
