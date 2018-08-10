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

package org.keycloak.storage.ldap.mappers.membership.group;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.CommonLDAPGroupMapperConfig;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.UserRolesRetrieveStrategy;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GroupLDAPStorageMapperFactory extends AbstractLDAPStorageMapperFactory {

    public static final String PROVIDER_ID = "group-ldap-mapper";

    protected static final List<ProviderConfigProperty> configProperties;
    protected static final Map<String, UserRolesRetrieveStrategy> userGroupsStrategies = new LinkedHashMap<>();
    protected static final List<String> MEMBERSHIP_TYPES = new LinkedList<>();
    protected static final List<String> MODES = new LinkedList<>();
    protected static final List<String> NO_IMPORT_MODES = new LinkedList<>();
    protected static final List<String> ROLE_RETRIEVERS;

    // TODO: Merge with RoleLDAPFederationMapperFactory as there are lot of similar properties
    static {
        userGroupsStrategies.put(GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE, new UserRolesRetrieveStrategy.LoadRolesByMember());
        userGroupsStrategies.put(GroupMapperConfig.GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE, new UserRolesRetrieveStrategy.GetRolesFromUserMemberOfAttribute());
        userGroupsStrategies.put(GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY, new UserRolesRetrieveStrategy.LoadRolesByMemberRecursively());
        for (MembershipType membershipType : MembershipType.values()) {
            MEMBERSHIP_TYPES.add(membershipType.toString());
        }
        for (LDAPGroupMapperMode mode : LDAPGroupMapperMode.values()) {
            MODES.add(mode.toString());
        }
        NO_IMPORT_MODES.add(LDAPGroupMapperMode.LDAP_ONLY.toString());
        NO_IMPORT_MODES.add(LDAPGroupMapperMode.READ_ONLY.toString());
        ROLE_RETRIEVERS = new LinkedList<>(userGroupsStrategies.keySet());

        List<ProviderConfigProperty> config = getProps(null);
        configProperties = config;
    }

    private static List<ProviderConfigProperty> getProps(ComponentModel parent) {
        String roleObjectClasses = LDAPConstants.GROUP_OF_NAMES;
        String mode = LDAPGroupMapperMode.LDAP_ONLY.toString();
        String membershipUserAttribute = LDAPConstants.UID;
        boolean importEnabled = true;
        if (parent != null) {
            LDAPConfig config = new LDAPConfig(parent.getConfig());
            roleObjectClasses = config.isActiveDirectory() ? LDAPConstants.GROUP : LDAPConstants.GROUP_OF_NAMES;
            mode = config.getEditMode() == UserStorageProvider.EditMode.WRITABLE ? LDAPGroupMapperMode.LDAP_ONLY.toString() : LDAPGroupMapperMode.READ_ONLY.toString();
            membershipUserAttribute = config.getUsernameLdapAttribute();
            importEnabled = new UserStorageProviderModel(parent).isImportEnabled();
        }

        ProviderConfigurationBuilder config = ProviderConfigurationBuilder.create()
                .property().name(GroupMapperConfig.GROUPS_DN)
                .label("LDAP Groups DN")
                .helpText("LDAP DN where are groups of this tree saved. For example 'ou=groups,dc=example,dc=org' ")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(GroupMapperConfig.GROUP_NAME_LDAP_ATTRIBUTE)
                .label("Group Name LDAP Attribute")
                .helpText("Name of LDAP attribute, which is used in group objects for name and RDN of group. Usually it will be 'cn' . In this case typical group/role object may have DN like 'cn=Group1,ou=groups,dc=example,dc=org' ")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(LDAPConstants.CN)
                .add()
                .property().name(GroupMapperConfig.GROUP_OBJECT_CLASSES)
                .label("Group Object Classes")
                .helpText("Object class (or classes) of the group object. It's divided by comma if more classes needed. In typical LDAP deployment it could be 'groupOfNames' . In Active Directory it's usually 'group' ")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(roleObjectClasses)
                .add()
                .property().name(GroupMapperConfig.PRESERVE_GROUP_INHERITANCE)
                .label("Preserve Group Inheritance")
                .helpText("Flag whether group inheritance from LDAP should be propagated to Keycloak. If false, then all LDAP groups will be mapped as flat top-level groups in Keycloak. Otherwise group inheritance is " +
                        "preserved into Keycloak, but the group sync might fail if LDAP structure contains recursions or multiple parent groups per child groups")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("true")
                .add()
                .property().name(GroupMapperConfig.MEMBERSHIP_LDAP_ATTRIBUTE)
                .label("Membership LDAP Attribute")
                .helpText("Name of LDAP attribute on group, which is used for membership mappings. Usually it will be 'member' ." +
                        "However when 'Membership Attribute Type' is 'UID' then 'Membership LDAP Attribute' could be typically 'memberUid' .")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(LDAPConstants.MEMBER)
                .add()
                .property().name(GroupMapperConfig.MEMBERSHIP_ATTRIBUTE_TYPE)
                .label("Membership Attribute Type")
                .helpText("DN means that LDAP group has it's members declared in form of their full DN. For example 'member: uid=john,ou=users,dc=example,dc=com' . " +
                        "UID means that LDAP group has it's members declared in form of pure user uids. For example 'memberUid: john' .")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(MEMBERSHIP_TYPES)
                .defaultValue(MembershipType.DN.toString())
                .add()
                .property().name(RoleMapperConfig.MEMBERSHIP_USER_LDAP_ATTRIBUTE)
                .label("Membership User LDAP Attribute")
                .helpText("Used just if Membership Attribute Type is UID. It is name of LDAP attribute on user, which is used for membership mappings. Usually it will be 'uid' . For example if value of " +
                        "'Membership User LDAP Attribute' is 'uid' and " +
                        " LDAP group has  'memberUid: john', then it is expected that particular LDAP user will have attribute 'uid: john' .")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(membershipUserAttribute)
                .add()
                .property().name(GroupMapperConfig.GROUPS_LDAP_FILTER)
                .label("LDAP Filter")
                .helpText("LDAP Filter adds additional custom filter to the whole query for retrieve LDAP groups. Leave this empty if no additional filtering is needed and you want to retrieve all groups from LDAP. Otherwise make sure that filter starts with '(' and ends with ')'")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add();
        if (importEnabled) {
            config.property().name(GroupMapperConfig.MODE)
                    .label("Mode")
                    .helpText("LDAP_ONLY means that all group mappings of users are retrieved from LDAP and saved into LDAP. READ_ONLY is Read-only LDAP mode where group mappings are " +
                            "retrieved from both LDAP and DB and merged together. New group joins are not saved to LDAP but to DB. IMPORT is Read-only LDAP mode where group mappings are " +
                            "retrieved from LDAP just at the time when user is imported from LDAP and then " +
                            "they are saved to local keycloak DB.")
                    .type(ProviderConfigProperty.LIST_TYPE)
                    .options(MODES)
                    .defaultValue(mode)
                    .add();
        } else {
            config.property().name(GroupMapperConfig.MODE)
                    .label("Mode")
                    .helpText("LDAP_ONLY means that specified group mappings are writable to LDAP. "
                              + "READ_ONLY means that group mappings are not writable to LDAP.")
                    .type(ProviderConfigProperty.LIST_TYPE)
                    .options(NO_IMPORT_MODES)
                    .defaultValue(mode)
                    .add();

        }
        config.property().name(GroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY)
                .label("User Groups Retrieve Strategy")
                .helpText("Specify how to retrieve groups of user. LOAD_GROUPS_BY_MEMBER_ATTRIBUTE means that roles of user will be retrieved by sending LDAP query to retrieve all groups where 'member' is our user. " +
                        "GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE means that groups of user will be retrieved from 'memberOf' attribute of our user. " +
                        "LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY is applicable just in Active Directory and it means that groups of user will be retrieved recursively with usage of LDAP_MATCHING_RULE_IN_CHAIN Ldap extension.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(ROLE_RETRIEVERS)
                .defaultValue(GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE)
                .add()
                .property().name(GroupMapperConfig.MAPPED_GROUP_ATTRIBUTES)
                .label("Mapped Group Attributes")
                .helpText("List of names of attributes divided by comma. This points to the list of attributes on LDAP group, which will be mapped as attributes of Group in Keycloak. " +
                        "Leave this empty if no additional group attributes are required to be mapped in Keycloak. ")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(GroupMapperConfig.DROP_NON_EXISTING_GROUPS_DURING_SYNC)
                .label("Drop non-existing groups during sync")
                .helpText("If this flag is true, then during sync of groups from LDAP to Keycloak, we will keep just those Keycloak groups, which still exists in LDAP. Rest will be deleted")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("false")
                .add();
        return config.build();
    }

    @Override
    public String getHelpText() {
        return "Used to map group mappings of groups from some LDAP DN to Keycloak group mappings";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Map<String, Object> getTypeMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fedToKeycloakSyncSupported", true);
        metadata.put("fedToKeycloakSyncMessage", "sync-ldap-groups-to-keycloak");
        metadata.put("keycloakToFedSyncSupported", true);
        metadata.put("keycloakToFedSyncMessage", "sync-keycloak-groups-to-ldap");

        return metadata;
    }


    @Override
    public void onParentUpdate(RealmModel realm, UserStorageProviderModel oldParent, UserStorageProviderModel newParent, ComponentModel mapperModel) {
        if (!newParent.isImportEnabled()) {
            if (new RoleMapperConfig(mapperModel).getMode() == LDAPGroupMapperMode.IMPORT) {
                mapperModel.getConfig().putSingle(GroupMapperConfig.MODE, LDAPGroupMapperMode.READ_ONLY.toString());
                realm.updateComponent(mapperModel);

            }
        }
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        ComponentModel parentModel = realm.getComponent(model.getParentId());
        UserStorageProviderModel parent = new UserStorageProviderModel(parentModel);
        onParentUpdate(realm, parent, parent, model);

    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        ComponentModel parentModel = realm.getComponent(newModel.getParentId());
        UserStorageProviderModel parent = new UserStorageProviderModel(parentModel);
        onParentUpdate(realm, parent, parent, newModel);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(RealmModel realm, ComponentModel parent) {
        return getProps(parent);
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        checkMandatoryConfigAttribute(GroupMapperConfig.GROUPS_DN, "LDAP Groups DN", config);
        checkMandatoryConfigAttribute(GroupMapperConfig.MODE, "Mode", config);

        String mt = config.getConfig().getFirst(CommonLDAPGroupMapperConfig.MEMBERSHIP_ATTRIBUTE_TYPE);
        MembershipType membershipType = mt==null ? MembershipType.DN : Enum.valueOf(MembershipType.class, mt);
        boolean preserveGroupInheritance = Boolean.parseBoolean(config.getConfig().getFirst(GroupMapperConfig.PRESERVE_GROUP_INHERITANCE));
        if (preserveGroupInheritance && membershipType != MembershipType.DN) {
            throw new ComponentValidationException("ldapErrorCantPreserveGroupInheritanceWithUIDMembershipType");
        }

        LDAPUtils.validateCustomLdapFilter(config.getConfig().getFirst(GroupMapperConfig.GROUPS_LDAP_FILTER));
    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        return new GroupLDAPStorageMapper(mapperModel, federationProvider, this);
    }

    protected UserRolesRetrieveStrategy getUserGroupsRetrieveStrategy(String strategyKey) {
        return userGroupsStrategies.get(strategyKey);
    }
}
