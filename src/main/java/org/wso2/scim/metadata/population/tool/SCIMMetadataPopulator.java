package org.wso2.scim.metadata.population.tool;

import org.wso2.scim.metadata.population.tool.db.GroupDAO;
import org.wso2.scim.metadata.population.tool.ldap.LDAPUtils;
import org.wso2.scim.metadata.population.tool.db.DBUtils;
import org.wso2.scim.metadata.population.tool.model.SCIMGroup;
import org.wso2.scim.metadata.population.tool.util.ConfigParser;

import java.util.*;

class SCIMMetadataPopulator {

    private static int tenantId;
    private static String userstoreDomain;
    private static String baseLocation;
    private static boolean debug;
    private static int added, renamed, updated, deleted;
    private static ConfigParser configParser;

    public static void main(String[] args) {

        try {
            configParser = new ConfigParser();
            tenantId = Integer.parseInt(configParser.getProperty("TENANT_ID"));
            userstoreDomain = configParser.getProperty("USERSTORE_DOMAIN").toUpperCase();
            baseLocation = configParser.getProperty("SCIM_GROUP_RESOURCE_LOCATION");
            debug = Boolean.valueOf(configParser.getProperty("DEBUG_MODE"));

            ArrayList<SCIMGroup> allLDAPgroups = LDAPUtils.getAllGroupsFromLDAP();

            System.out.println("=== " + allLDAPgroups.size() + " group(s) found in the LDAP/AD userstore. ===\n");

            // provisioning the LDAP groups into Identity DB
            provisionLDAPGroups(allLDAPgroups);

            // remove any stale data on Identity DB
            removeStaleGroupsFromDB(allLDAPgroups);

            // close DB connection at the end
            DBUtils.closeConnection(DBUtils.getDBConnection());
        } catch (IdentityException e) {
            System.out.println("=== An error occurred! ===");
            e.printStackTrace();
            System.out.println("=======================");
        }

        System.out.println(String.format(
                "\n=== Results ===" +
                "\nAdded groups: %s" +
                "\nRenamed groups: %s" +
                "\nUpdated groups: %s" +
                "\nDeleted groups: %s", added, renamed, updated, deleted));

    }

    private static void provisionLDAPGroups(ArrayList<SCIMGroup> allLDAPgroups) throws IdentityException {

        System.out.println("Provisioning LDAP/AD groups into Identity DB...");
        for (SCIMGroup group : allLDAPgroups) {
            if (debug) {
                System.out.println(group);
            } else {
                System.out.println(group.getName());
            }
            provisionGroup(group);
            System.out.println();
        }
    }
    private static void provisionGroup(SCIMGroup group) throws IdentityException {
        Map<String, String> attributes =  new HashMap<String, String>();
        attributes.put(Constants.ID_URI, group.getId());
        attributes.put(Constants.CREATED_URI, group.getCreatedAt());
        attributes.put(Constants.LAST_MODIFIED_URI, group.getUpdatedAt());
        attributes.put(Constants.LOCATION_URI, baseLocation + "/" + group.getId());

        String roleName = userstoreDomain + "/" + group.getName();

        if (!GroupDAO.isExistingGroup(roleName, tenantId)) {
            String existingGroupNameForID = GroupDAO.getGroupNameById(tenantId, group.getId());
            if (existingGroupNameForID == null) {
                if (debug) {
                    System.out.printf("Adding group: " + roleName + " with attributes: " + attributes);
                }
                GroupDAO.addSCIMGroupAttributes(tenantId, roleName, attributes);
                added++;
            } else {
                if (debug) {
                    System.out.printf("An exiting group found for ID: " + group.getId() +
                            ". Hence, renaming the existing group: " + existingGroupNameForID + " to: " + roleName);
                }
                GroupDAO.updateRoleName(tenantId, existingGroupNameForID, roleName);
                attributes.remove(Constants.ID_URI);
                GroupDAO.updateSCIMGroupAttributes(tenantId, roleName, attributes);
                renamed++;
            }
        } else {
            if (debug) {
                System.out.printf("An exiting group found with the same name. Hence, just updating attributes of: " + roleName);
            }
            GroupDAO.updateSCIMGroupAttributes(tenantId, roleName, attributes);
            updated++;
        }
    }

    private static void removeStaleGroupsFromDB(ArrayList<SCIMGroup> allLDAPgroups) throws IdentityException {

        Set<String> allSCIMGroups = GroupDAO.listGroupsOnUserstore(tenantId, userstoreDomain);
        for (SCIMGroup group: allLDAPgroups) {
            String roleName = userstoreDomain + "/" + group.getName();
            allSCIMGroups.remove(roleName);
        }
        System.out.printf("\n=== " + allSCIMGroups.size() + " group(s) found in the Identity DB which are not in the " +
                "LDAP/AD userstore. ===\n");
        if (allSCIMGroups.size() > 0) {
            if (debug) {
                System.out.println("Removing all stale group metadata: " + allSCIMGroups.toString());
            } else {
                System.out.println("Removing all stale group metadata...");
            }
            GroupDAO.removeSCIMGroups(tenantId, allSCIMGroups);
            deleted = allSCIMGroups.size();
        }
    }
}
