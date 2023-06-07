# sunbird-auth
Repository for sunbird authentication service.

## Sunbird-auth Setup

To set up and run the sunbird-auth, follow the steps below:

1. Clone the latest branch of the sunbird-auth service using the following command:
```shell
git clone https://github.com/Sunbird-Lern/sunbird-auth.git
```

2. Build the application using the following maven command in the path `<project-base-path>/sunbird-auth`:
```shell
mvn clean install -DskipTests
```
Make sure the build is successful before proceeding to the next step. If the build is not successful,
fix any configuration issues and rebuild the application.

3. Create the folder with name `providers` inside the Keycloak base path
```shell
mkdir providers
```

4. Copy the jar which got created in step:2 and keep the jar `keycloak-email-phone-autthenticator-1.0-SNAPSHOT.jar` inside the providers folder

5. Before running the Keycloak, set up the necessary environment variables by running the following script in the path `<project-base-path>/sunbird-auth`:
```shell
./keycloak/scripts/sunbird_auth-config.sh
```

6. Run the Keycloack server using the following command in the path `<keycloack-base-path>/bin/`:
```shell
For mac/unix:
./standalone.sh

For windows:
standalone.bat
```

7. Verify the spi `cassandra-storage-provider` is available in the Keycloak web page in the `User Fedaration` section.