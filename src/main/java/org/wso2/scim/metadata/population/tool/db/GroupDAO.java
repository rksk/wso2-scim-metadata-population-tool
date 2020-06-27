package org.wso2.scim.metadata.population.tool.db;

import org.wso2.scim.metadata.population.tool.Constants;
import org.wso2.scim.metadata.population.tool.IdentityException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class GroupDAO {


    public static void addSCIMGroupAttributes(int tenantId, String roleName, Map<String, String> attributes)
            throws IdentityException {

        Connection connection = DBUtils.getDBConnection();
        PreparedStatement prepStmt = null;

        try {
            prepStmt = connection.prepareStatement(SQLQueries.ADD_ATTRIBUTES_SQL);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, roleName);

            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (!isExistingAttribute(entry.getKey(), roleName, tenantId)) {
                    prepStmt.setString(3, entry.getKey());
                    prepStmt.setString(4, entry.getValue());
                    prepStmt.addBatch();

                } else {
                    throw new IdentityException("Error when adding SCIM Attribute: "
                            + entry.getKey()
                            + " An attribute with the same name already exists.");
                }
            }
            prepStmt.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            throw new IdentityException("Error when adding SCIM Attributes", e);
        } finally {
            DBUtils.closeAllConnections(connection, null, prepStmt);
        }
    }

    public static boolean isExistingGroup(String groupName, int tenantId) throws IdentityException {

        Connection connection = DBUtils.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;

        boolean isExistingGroup = false;

        try {
            prepStmt = connection.prepareStatement(SQLQueries.CHECK_EXISTING_ATTRIBUTE_SQL);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, groupName);

            // Specifically checking SCIM 2.0 ID attribute to avoid conflict with SCIM 1.1
            prepStmt.setString(3, Constants.ID_URI);

            rSet = prepStmt.executeQuery();
            if (rSet.next()) {
                isExistingGroup = true;
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IdentityException("Error when checking SCIM group existance", e);
        } finally {
            DBUtils.closeAllConnections(connection, rSet, prepStmt);
        }
        return isExistingGroup;
    }

    private static boolean isExistingAttribute(String attributeName, String groupName, int tenantId)
            throws IdentityException {
        Connection connection = DBUtils.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;
        boolean isExistingAttribute = false;

        try {
            prepStmt = connection.prepareStatement(SQLQueries.CHECK_EXISTING_ATTRIBUTE_SQL);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, groupName);
            prepStmt.setString(3, attributeName);

            rSet = prepStmt.executeQuery();
            if (rSet.next()) {
                isExistingAttribute = true;
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IdentityException("Error when checking for SCIM Attributes", e);
        } finally {
            DBUtils.closeAllConnections(connection, rSet, prepStmt);
        }
        return isExistingAttribute;
    }

    public static String getGroupNameById(int tenantId, String id) throws IdentityException {

        Connection connection = DBUtils.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;
        String roleName = null;

        try {
            prepStmt = connection.prepareStatement(SQLQueries.GET_GROUP_NAME_BY_ID_SQL);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, id);
            prepStmt.setString(3, Constants.ID_URI);
            rSet = prepStmt.executeQuery();
            while (rSet.next()) {
                //we assume only one result since group id and tenant id is unique.
                roleName = rSet.getString(1);
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IdentityException("Error when reading the SCIM Group information from the persistence store.", e);
        } finally {
            DBUtils.closeAllConnections(connection, rSet, prepStmt);
        }
        if (DBUtils.isNotEmpty(roleName)) {
            return roleName;
        }
        return null;
    }

    public static void updateRoleName(int tenantId, String oldRoleName, String newRoleName)
            throws IdentityException {

        Connection connection = DBUtils.getDBConnection();
        PreparedStatement prepStmt = null;

        if (isExistingGroup(oldRoleName, tenantId)) {
            try {
                prepStmt = connection.prepareStatement(SQLQueries.UPDATE_GROUP_NAME_SQL);

                prepStmt.setString(1, newRoleName);
                prepStmt.setInt(2, tenantId);
                prepStmt.setString(3, oldRoleName);

                prepStmt.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                throw new IdentityException("Error updating the SCIM Group Attributes", e);
            } finally {
                DBUtils.closeAllConnections(connection, null, prepStmt);
            }
        } else {
            throw new IdentityException("Error when updating role name of the role: " + oldRoleName);
        }
    }

    public static void updateSCIMGroupAttributes(int tenantId, String roleName,
                                                 Map<String, String> attributes) throws IdentityException {

        Connection connection = DBUtils.getDBConnection();
        PreparedStatement prepStmt = null;

        if (isExistingGroup(roleName, tenantId)) {
            try {
                prepStmt = connection.prepareStatement(SQLQueries.UPDATE_ATTRIBUTES_SQL);

                prepStmt.setInt(2, tenantId);
                prepStmt.setString(3, roleName);

                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    if (isExistingAttribute(entry.getKey(), roleName, tenantId)) {
                        prepStmt.setString(4, entry.getKey());
                        prepStmt.setString(1, entry.getValue());
                        prepStmt.addBatch();

                    } else {
                        throw new IdentityException("Error when adding SCIM Attribute: "
                                + entry.getKey()
                                + " An attribute with the same name doesn't exists.");
                    }
                }
                prepStmt.executeBatch();
                connection.commit();

            } catch (SQLException e) {
                throw new IdentityException("Error updating the SCIM Group Attributes of the group: " + roleName, e);
            } finally {
                DBUtils.closeAllConnections(connection, null, prepStmt);
            }
        } else {
            throw new IdentityException("Error when updating SCIM Attributes for the group: "
                    + roleName + " A Group with the same name doesn't exists.");
        }
    }


    public static Set<String> listGroupsOnUserstore(int tenantId, String userstoreDomain) throws IdentityException {
            Connection connection = DBUtils.getDBConnection();
            PreparedStatement prepStmt = null;
            ResultSet resultSet = null;
            Set<String> groups = new HashSet<>();

        try {
            prepStmt = connection.prepareStatement(SQLQueries.LIST_SCIM_GROUPS_FOR_USERSTORE_SQL);
            prepStmt.setString(1, Constants.ID_URI);
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, userstoreDomain.toUpperCase() + "%");
            resultSet = prepStmt.executeQuery();
            while (resultSet.next()) {
                String group = resultSet.getString(1);
                if (DBUtils.isNotEmpty(group)) {
                    groups.add(group);
                }
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IdentityException("Error when reading the SCIM Group information from the " +
                    "persistence store.", e);
        } finally {
            DBUtils.closeAllConnections(connection, resultSet, prepStmt);
        }
        return groups;
    }

    public static void removeSCIMGroups(int tenantId, Set<String> groups) throws IdentityException {
        Connection connection = DBUtils.getDBConnection();
        PreparedStatement prepStmt = null;

        try {
            prepStmt = connection.prepareStatement(SQLQueries.DELETE_GROUP_SQL);
            prepStmt.setInt(1, tenantId);

            for (String group : groups) {
                prepStmt.setString(2, group);
                prepStmt.addBatch();
            }
            prepStmt.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            throw new IdentityException("Error deleting the SCIM Groups.", e);
        } finally {
            DBUtils.closeAllConnections(connection, null, prepStmt);
        }
    }

    private GroupDAO() {};
}
