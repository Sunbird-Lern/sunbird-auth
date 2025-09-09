/**
 * Function to handle Keycloak logout
 * @param {string} redirectUri - Optional URL to redirect after logout
 * @returns {Promise} - Returns a promise that resolves when logout is complete
 */
function handleKeycloakLogout(redirectUri) {
    if (typeof keycloak === 'undefined') {
        console.error('Keycloak is not initialized');
        return Promise.reject('Keycloak is not initialized');
    }

    const options = {};
    console.log('options', options);
    if (redirectUri) {
        options.redirectUri = redirectUri;
    }

    return keycloak.logout(options)
        .catch(error => {
            console.error('Logout failed:', error);
            throw error;
        });
}
