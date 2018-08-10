# Keycloak Arquillian Integration Testsuite

## Overview

For overview see the **Modules Overview** section at the bottom of this README.

## How to run tests

See the file [HOW-TO-RUN.md](HOW-TO-RUN.md) .

## Container Lifecycles

### Auth Server

Keycloak server is automatically started by the testsuite on the `BeforeSuite` event and stopped on `AfterSuite` event.

By default the server runs in embedded Undertow.

#### Wildfly/EAP

Testsuite supports running server on Wildfly/EAP. For this it's necessary to:
- build the project including the `distribution` module
 (artifact `keycloak-server-dist`/`-overlay` needs to be available before running the testsuite),
- activate profile `auth-server-wildfly` or `auth-server-eap7`.

[More details...](servers/auth-server/README.md)

#### Cluster Setup

The cluster setup for server can be enabled by activating profile `auth-server-cluster`.

The cluster setup is not supported for server on Undertow. Profile `auth-server-wildfly` or `auth-server-eap` needs to be activated.

The setup includes:
- a `mod_cluster` load balancer on Wildfly
- two clustered nodes of Keycloak server on Wildfly/EAP

Clustering tests require MULTICAST to be enabled on machine's `loopback` network interface.
This can be done by running the following commands under root privileges:
```
route add -net 224.0.0.0 netmask 240.0.0.0 dev lo
ifconfig lo multicast
```

### App Servers / Adapter Tests

Lifecycle of application server is always tied to a particular TestClass.

Each *adapter* test class is annotated by `@AppServerContainer("app-server-*")` annotation 
that links it to a particular Arquillian container in `arquillian.xml`.
The `AppServerTestEnricher` then ensures the server is started during `BeforeClass` event and stopped during `AfterClass` event for that particular test class. 
In case the `@AppServerContainer` annotation has no value it's assumed that the application container 
is the same as the auth server container - a "relative" adapter test scenario.

The app-servers with installed Keycloak adapter are prepared in `servers/app-server` submodules, activated by `-Papp-server-MODULE`.
[More details.](servers/app-server/README.md)

The corresponding adapter test modules are in `tests/other/adapters` submodules, and are activated by the same profiles.

## SuiteContext and TestContext

These objects are injected into `AbstractKeycloakTest` class so they can be used everywhere.
They can be used to get information about the tested containers, and to store information that won't survive across test classes or test methods.
(Arquillian creates a new instance of test class for each test method run, so all data in the fields is always lost.)

## REST Testing

The `AbstractKeycloakTest` has an initialized instance of AdminClient. Subclasses can use it to access any REST subresources.

## UI Testing

### Page Objects

Page Objects are used by tests to access and operate on UI. 
They can be injected using annotation `@Page` provided by the *Arquillian Graphene* extension.

The base class for all page objects used throughout this Arquillian testsuite is `AbstractPage`, and it's subclass `AbstractPageWithInjectedUrl`.

For the page objects for the *adapter test apps* the URLs are injected automatically by Arquillian depending on actual URL of the deployed app/example.

For the pages under the `/auth` context the URL is only injected to the `AuthServerContextRoot` page object, 
and the URL hierarchy is modeled by the class inheritance hierarchy (subclasses/extending of `AuthServerContextRoot`).


### Browsers

The default browser for UI testing is `htmlunit` which is used for fast "headless" testing.
Other browsers can be selected with the `-Dbrowser` property, for example `firefox`.
See [HOW-TO-RUN.md](HOW-TO-RUN.md) and Arquillian Graphene documentation for more details.

### Utils classes
UI testing is sometimes very tricky due to different demands and behaviours of different browsers and their drivers. So there are some very useful Utils classes which are already dealing with some common stability issues while testing. See `UIUtils`, `URLUtils` and `WaitUtils` classes in the Base Testsuite.


## Test Modules

### Base Testsuite

The base testsuite contains custom Arquillian extensions and most functional tests.
The other test modules depend on this module.

### Admin Console UI Tests

Tests for Keycloak Admin Console are located in a separate module `tests/other/console` 
and are **disabled** by default. Can be enabled by `-Pconsole-ui-tests`.

### Adapter Tests

Adapter tests are located in submodules of the `tests/other/adapters` module.

They are **disabled** by default; they can be enabled by corresponding profiles.
Multiple profiles can be enabled for a single test execution.

#### Types of adapter tests

1. Using *custom test servlets*
2. Using *example demo apps* from `keycloak/examples` modules.

#### Relative vs Non-relative scenario

The test suite can handle both types.
It automatically modifies imported test realms and deployments' adapter configs based on scenario type.

| Scenario | Description | Realm config (server side) | Adapter config (client side) |
| --- | --- | --- | --- |
| **Relative** | auth server == app server | client `baseUrl`, `adminUrl` and `redirect-uris` can be relative | `auth-server-url` can be relative |
| **Non-relative** | auth server != app server  | client `baseUrl`, `adminUrl` and `redirect-uris` need to include FQDN of the app server | `auth-server-url` needs to include FQDN of the auth server|

#### Adapter Config Mode

1. ~~**Provided** - In `standalone.xml` using `secure-deployment`. *Wildfly only.*~~ WIP
2. **Bundled** - In the deployed war in `/WEB-INF/keycloak.json`. **Default.**


## Custom Arquillian Extensions

Custom extensions are registered in `META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension`.

### MultipleContainersExtension
 * Replaces Arquillian's default container handling.
 * Allows to manage multiple container instances of different types within a single test run.
 * Allows to skip loading disabled containers based on `enabled` config property in `arquillian.xml`.

### KeycloakArquillianExtension
 * `AuthServerTestEnricher` - Handles lifecycle of auth server and migrated auth server.
 * `AppServerTestEnricher` - Handles lifecycles of app servers.
 * `CustomUndertowContainer` - Custom container controller for JAX-RS-enabled Undertow with Keycloak Server.
 * `DeploymentArchiveProcessor` - Modifies adapter configs before test apps are deployed.
 * `DeploymentTargetModifier` - Ensures all test app deployments are targeted at app server containers.
 * `URLProvider` - Fixes URLs injected by Arquillian Graphene which contain value `127.0.0.1` instead of required `localhost`.
 
## Modules Overview

```
integration-arquillian
│
├──servers   (preconfigured test servers)
│  │
│  ├──auth-server
│  │  ├──jboss (wildfly/eap)
│  │  └──undertow (arq. extension)
│  │
│  ├──app-server
│  │  ├──jboss (wildfly/eap/as)
│  │  ├──tomcat
│  │  └──karaf
│  │
│  └──wildfly-balancer
│
└──tests   (common settings for all test modules)
   │
   ├──base    (custom ARQ extensions + base functional tests)
   │
   └──other   (common settings for all test modules dependent on base)
      │
      ├──adapters         (common settings for all adapter test modules)
      │  ├──jboss
      │  ├──tomcat
      │  └──karaf
      │
      ├──console          
      ├──console_no_users 
      └──...
```

