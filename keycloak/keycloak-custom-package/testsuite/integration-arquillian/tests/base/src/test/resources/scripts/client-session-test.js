AuthenticationFlowError = Java.type("org.keycloak.authentication.AuthenticationFlowError");

function authenticate(context) {

    if (authenticationSession.getRealm().getName() != "${realm}") {
        context.failure(AuthenticationFlowError.INVALID_CLIENT_SESSION);
        return;
    }

    if (authenticationSession.getClient().getClientId() != "${clientId}") {
        context.failure(AuthenticationFlowError.UNKNOWN_CLIENT);
        return;
    }

    if (authenticationSession.getProtocol() != "${authMethod}") {
        context.failure(AuthenticationFlowError.INVALID_CLIENT_SESSION);
        return;
    }

    context.success();
}