# sunbird-auth
Repository for sunbird authentication service.

## Steps to generate Sunbird authentication keycloak SPI jar
1. Clone the latest branch of the sunbird-auth repository using the following command:
```shell
git clone https://github.com/Sunbird-Lern/sunbird-auth.git
```
2. Build the application using the following maven command in the path `<project-base-path>/keycloak/sms-provider`:
```shell
mvn clean install -DskipTests
```
Make sure the build is successful before proceeding to the next step. 

3. Sunbird authentication keycloak SPI jar 'keycloak-email-phone-autthenticator-1.0-SNAPSHOT.jar' can be copied from the path `<project-base-path>/keycloak/sms-provider/target/` 