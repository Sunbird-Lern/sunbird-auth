# Sunbird Keycloak Setup


## Configuration Values
Here are the configuration values pulled from `keycloak.conf`:
- **Database Type**: `db=postgres`
- **Database Username**: `db-username=postgres`
- **Database Password**: `db-password=postgres`
- **Database URL**: `db-url=jdbc:postgresql://localhost:5432/keycloak?sslmode=require`
- **HTTP Relative Path**: `http-relative-path=/auth`

## Configuration Values with Placeholders

Any placeholders in the pattern `{{ .Values.<key> }}` in `imports/sunbird-realm.json` need to be filled with appropriate values during local setup.

## Docker Build Command
To build the Docker image, use the following command:
```bash
docker build -t my-keycloak-image .