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

var module = angular.module('product', []);

function getAuthServerUrl() {
    var url = 'http://localhost-auth:8180';
    if (window.location.href.indexOf("8643") > -1) {
        url = url.replace("8180","8543");
        url = url.replace("http","https");
    }

    return url;
}

function getAppServerUrl(domain) {
    var url = "http://" + domain + ":8280";
    if (window.location.href.indexOf("8643") > -1) {
        url = url.replace("8280","8643");
        url = url.replace("http","https");
    }

    return url;
}

var auth = {};
var logout = function(){
    console.log('*** LOGOUT');
    auth.loggedIn = false;
    auth.authz = null;
    window.location = auth.logoutUrl;
};


angular.element(document).ready(function ($http) {
    console.log("*** here");
    var keycloakAuth = new Keycloak('keycloak.json');
    auth.loggedIn = false;

    keycloakAuth.init({ onLoad: 'login-required' }).success(function () {
        console.log('here login');
        auth.loggedIn = true;
        auth.authz = keycloakAuth;
        auth.logoutUrl = keycloakAuth.authServerUrl + "/realms/" + keycloakAuth.realm + "/protocol/openid-connect/logout?redirect_uri=" + getAppServerUrl("localhost") + "/angular-cors-product/index.html"
        module.factory('Auth', function() {
            return auth;
        });
        angular.bootstrap(document, ["product"]);
    }).error(function () {
            alert("failed to login");
        });

});

module.controller('GlobalCtrl', function($scope, $http) {
    $scope.products = [];
    $scope.roles = [];
    $scope.serverInfo = [];
    $scope.realm = [];
    $scope.version = [];
    $scope.reloadData = function() {
        $http.get(getAppServerUrl("localhost-db") + "/cors-database/products").success(function(data) {
            $scope.products = angular.fromJson(data);

        });

    };
    $scope.loadRoles = function() {
        $http.get(getAuthServerUrl() + "/auth/admin/realms/" + auth.authz.realm + "/roles").success(function(data) {
            $scope.roles = angular.fromJson(data);

        });

    };
    $scope.addRole = function() {
        $http.post(getAuthServerUrl() + "/auth/admin/realms/" + auth.authz.realm + "/roles", {name: 'stuff'}).success(function() {
            $scope.loadRoles();
        });

    };
    $scope.deleteRole = function() {
        $http.delete(getAuthServerUrl() + "/auth/admin/realms/" + auth.authz.realm + "/roles/stuff").success(function() {
            $scope.loadRoles();
        });

    };

    $scope.loadServerInfo = function() {
        $http.get(getAuthServerUrl() + "/auth/admin/serverinfo").success(function(data) {
            $scope.serverInfo = angular.fromJson(data);
        });

    };

    $scope.loadPublicRealmInfo = function() {
        $http.get(getAuthServerUrl() + "/auth/realms/cors").success(function(data) {
            $scope.realm = angular.fromJson(data);
        });
    };

    $scope.loadVersion = function() {
        $http.get(getAuthServerUrl() + "/auth/version").success(function(data) {
            $scope.version = angular.fromJson(data);
        });
    };

    $scope.logout = logout;
});


module.factory('authInterceptor', function($q, Auth) {
    return {
        request: function (config) {
            var deferred = $q.defer();
            if (Auth.authz.token) {
                Auth.authz.updateToken(5).success(function() {
                    config.headers = config.headers || {};
                    config.headers.Authorization = 'Bearer ' + Auth.authz.token;

                    deferred.resolve(config);
                }).error(function() {
                        deferred.reject('Failed to refresh token');
                    });
            }
            return deferred.promise;
        }
    };
});




module.config(function($httpProvider) {
    $httpProvider.responseInterceptors.push('errorInterceptor');
    $httpProvider.interceptors.push('authInterceptor');

});

module.factory('errorInterceptor', function($q) {
    return function(promise) {
        return promise.then(function(response) {
            return response;
        }, function(response) {
            if (response.status == 401) {
                console.log('session timeout?');
                logout();
            } else if (response.status == 403) {
                alert("Forbidden");
            } else if (response.status == 404) {
                alert("Not found");
            } else if (response.status) {
                if (response.data && response.data.errorMessage) {
                    alert(response.data.errorMessage);
                } else {
                    alert("An unexpected server error has occurred");
                }
            }
            return $q.reject(response);
        });
    };
});
