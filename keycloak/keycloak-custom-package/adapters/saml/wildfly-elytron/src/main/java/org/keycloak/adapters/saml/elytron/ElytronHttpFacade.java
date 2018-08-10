/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.saml.elytron;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.cert.X509Certificate;

import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.wildfly.security.auth.callback.AnonymousAuthorizationCallback;
import org.wildfly.security.auth.callback.AuthenticationCompleteCallback;
import org.wildfly.security.auth.callback.SecurityIdentityCallback;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.http.HttpAuthenticationException;
import org.wildfly.security.http.HttpScope;
import org.wildfly.security.http.HttpServerCookie;
import org.wildfly.security.http.HttpServerMechanismsResponder;
import org.wildfly.security.http.HttpServerRequest;
import org.wildfly.security.http.HttpServerResponse;
import org.wildfly.security.http.Scope;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class ElytronHttpFacade implements HttpFacade {

    private final HttpServerRequest request;
    private final CallbackHandler callbackHandler;
    private final SamlDeploymentContext deploymentContext;
    private final SamlSessionStore sessionStore;
    private Consumer<HttpServerResponse> responseConsumer;
    private SecurityIdentity securityIdentity;
    private boolean restored;
    private SamlSession samlSession;

    public ElytronHttpFacade(HttpServerRequest request, SessionIdMapper idMapper, SamlDeploymentContext deploymentContext, CallbackHandler handler) {
        this.request = request;
        this.deploymentContext = deploymentContext;
        this.callbackHandler = handler;
        this.responseConsumer = response -> {};
        this.sessionStore = createTokenStore(idMapper);
    }

    private SamlSessionStore createTokenStore(SessionIdMapper idMapper) {
        return new ElytronSamlSessionStore(this, idMapper, getDeployment());
    }

    void authenticationComplete(SamlSession samlSession) {
        this.samlSession = samlSession;
    }

    void authenticationComplete() {
        this.securityIdentity = SecurityIdentityUtil.authorize(this.callbackHandler, samlSession.getPrincipal());
        this.request.authenticationComplete(response -> {
            if (!restored) {
                responseConsumer.accept(response);
            }
        }, () -> ((ElytronTokeStore) sessionStore).logout(true));
    }

    void authenticationCompleteAnonymous() {
        try {
            AnonymousAuthorizationCallback anonymousAuthorizationCallback = new AnonymousAuthorizationCallback(null);

            callbackHandler.handle(new Callback[]{anonymousAuthorizationCallback});

            if (anonymousAuthorizationCallback.isAuthorized()) {
                callbackHandler.handle(new Callback[]{AuthenticationCompleteCallback.SUCCEEDED, new SecurityIdentityCallback()});
            }

            request.authenticationComplete(response -> response.forward(getRequest().getRelativePath()));
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error processing callbacks during logout.", e);
        }
    }

    void authenticationFailed() {
        this.request.authenticationFailed("Authentication Failed", response -> responseConsumer.accept(response));
    }

    void noAuthenticationInProgress(AuthChallenge challenge) {
        if (challenge != null) {
            challenge.challenge(this);
        }
        this.request.noAuthenticationInProgress(response -> responseConsumer.accept(response));
    }

    void authenticationInProgress() {
        this.request.authenticationInProgress(response -> responseConsumer.accept(response));
    }

    HttpScope getScope(Scope scope) {
        return request.getScope(scope);
    }

    HttpScope getScope(Scope scope, String id) {
        return request.getScope(scope, id);
    }

    Collection<String> getScopeIds(Scope scope) {
        return request.getScopeIds(scope);
    }

    SamlDeployment getDeployment() {
        return deploymentContext.resolveDeployment(this);
    }

    @Override
    public Request getRequest() {
        return new Request() {
            @Override
            public String getMethod() {
                return request.getRequestMethod();
            }

            @Override
            public String getURI() {
                try {
                    return URLDecoder.decode(request.getRequestURI().toString(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Failed to decode request URI", e);
                }
            }

            @Override
            public String getRelativePath() {
                return request.getRequestPath();
            }

            @Override
            public boolean isSecure() {
                return request.getRequestURI().getScheme().equals("https");
            }

            @Override
            public String getFirstParam(String param) {
                return request.getFirstParameterValue(param);
            }

            @Override
            public String getQueryParamValue(String param) {
                return request.getFirstParameterValue(param);
            }

            @Override
            public Cookie getCookie(final String cookieName) {
                List<HttpServerCookie> cookies = request.getCookies();

                if (cookies != null) {
                    for (HttpServerCookie cookie : cookies) {
                        if (cookie.getName().equals(cookieName)) {
                            return new Cookie(cookie.getName(), cookie.getValue(), cookie.getVersion(), cookie.getDomain(), cookie.getPath());
                        }
                    }
                }

                return null;
            }

            @Override
            public String getHeader(String name) {
                return request.getFirstRequestHeaderValue(name);
            }

            @Override
            public List<String> getHeaders(String name) {
                return request.getRequestHeaderValues(name);
            }

            @Override
            public InputStream getInputStream() {
                return request.getInputStream();
            }

            @Override
            public String getRemoteAddr() {
                InetSocketAddress sourceAddress = request.getSourceAddress();
                if (sourceAddress == null) {
                    return "";
                }
                InetAddress address = sourceAddress.getAddress();
                if (address == null) {
                    // this is unresolved, so we just return the host name not exactly spec, but if the name should be
                    // resolved then a PeerNameResolvingHandler should be used and this is probably better than just
                    // returning null
                    return sourceAddress.getHostString();
                }
                return address.getHostAddress();
            }

            @Override
            public void setError(AuthenticationError error) {
                request.getScope(Scope.EXCHANGE).setAttachment(AuthenticationError.class.getName(), error);
            }

            @Override
            public void setError(LogoutError error) {
                request.getScope(Scope.EXCHANGE).setAttachment(LogoutError.class.getName(), error);
            }
        };
    }

    @Override
    public Response getResponse() {
        return new Response() {
            @Override
            public void setStatus(final int status) {
                responseConsumer = responseConsumer.andThen(response -> response.setStatusCode(status));
            }

            @Override
            public void addHeader(final String name, final String value) {
                responseConsumer = responseConsumer.andThen(response -> response.addResponseHeader(name, value));
            }

            @Override
            public void setHeader(String name, String value) {
                addHeader(name, value);
            }

            @Override
            public void resetCookie(final String name, final String path) {
                responseConsumer = responseConsumer.andThen(response -> setCookie(name, "", path, null, 0, false, false, response));
            }

            @Override
            public void setCookie(final String name, final String value, final String path, final String domain, final int maxAge, final boolean secure, final boolean httpOnly) {
                responseConsumer = responseConsumer.andThen(response -> setCookie(name, value, path, domain, maxAge, secure, httpOnly, response));
            }

            private void setCookie(final String name, final String value, final String path, final String domain, final int maxAge, final boolean secure, final boolean httpOnly, HttpServerResponse response) {
                response.setResponseCookie(new HttpServerCookie() {
                    @Override
                    public String getName() {
                        return name;
                    }

                    @Override
                    public String getValue() {
                        return value;
                    }

                    @Override
                    public String getDomain() {
                        return domain;
                    }

                    @Override
                    public int getMaxAge() {
                        return maxAge;
                    }

                    @Override
                    public String getPath() {
                        return path;
                    }

                    @Override
                    public boolean isSecure() {
                        return secure;
                    }

                    @Override
                    public int getVersion() {
                        return 0;
                    }

                    @Override
                    public boolean isHttpOnly() {
                        return httpOnly;
                    }
                });
            }

            @Override
            public OutputStream getOutputStream() {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                responseConsumer = responseConsumer.andThen(new Consumer<HttpServerResponse>() {
                    @Override
                    public void accept(HttpServerResponse httpServerResponse) {
                        try {
                            httpServerResponse.getOutputStream().write(stream.toByteArray());
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to write to response output stream", e);
                        }
                    }
                });
                return stream;
            }

            @Override
            public void sendError(int code) {
                setStatus(code);
            }

            @Override
            public void sendError(final int code, final String message) {
                responseConsumer = responseConsumer.andThen(response -> {
                    response.setStatusCode(code);
                    response.addResponseHeader("Content-Type", "text/html");
                    try {
                        response.getOutputStream().write(message.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            @Override
            public void end() {

            }
        };
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        return new X509Certificate[0];
    }

    public boolean restoreRequest() {
        restored = this.request.resumeRequest();
        return restored;
    }

    public void suspendRequest() {
        responseConsumer = responseConsumer.andThen(httpServerResponse -> request.suspendRequest());
    }

    public boolean isAuthorized() {
        return this.securityIdentity != null;
    }

    public URI getURI() {
        return request.getRequestURI();
    }

    public SamlSessionStore getSessionStore() {
        return sessionStore;
    }
}
