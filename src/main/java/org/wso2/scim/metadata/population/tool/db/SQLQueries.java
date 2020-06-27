package org.wso2.scim.metadata.population.tool.db;

public class SQLQueries {

    public static final String LIST_SCIM_GROUPS_FOR_USERSTORE_SQL =
            "SELECT ROLE_NAME FROM IDN_SCIM_GROUP WHERE IDN_SCIM_GROUP.ATTR_NAME = ? AND " +
                    "IDN_SCIM_GROUP.TENANT_ID = ? AND ROLE_NAME LIKE ?";
    public static final String GET_GROUP_NAME_BY_ID_SQL =
            "SELECT ROLE_NAME FROM IDN_SCIM_GROUP WHERE IDN_SCIM_GROUP.TENANT_ID=? AND " +
                    "IDN_SCIM_GROUP.ATTR_VALUE=? AND IDN_SCIM_GROUP.ATTR_NAME=?";
    public static final String ADD_ATTRIBUTES_SQL =
            "INSERT INTO IDN_SCIM_GROUP (TENANT_ID, ROLE_NAME, ATTR_NAME, ATTR_VALUE) VALUES (?, ?, ?, ?)";
    public static final String UPDATE_ATTRIBUTES_SQL =
            "UPDATE IDN_SCIM_GROUP SET ATTR_VALUE=? WHERE TENANT_ID=? AND ROLE_NAME=? AND ATTR_NAME=?";
    public static final String UPDATE_GROUP_NAME_SQL =
            "UPDATE IDN_SCIM_GROUP SET ROLE_NAME=? WHERE TENANT_ID=? AND ROLE_NAME=?";
    public static final String DELETE_GROUP_SQL =
            "DELETE FROM IDN_SCIM_GROUP WHERE TENANT_ID=? AND ROLE_NAME=?";
    public static final String CHECK_EXISTING_ATTRIBUTE_SQL =
            "SELECT TENANT_ID, ROLE_NAME, ATTR_NAME FROM IDN_SCIM_GROUP WHERE IDN_SCIM_GROUP.TENANT_ID=? AND " +
                    "IDN_SCIM_GROUP.ROLE_NAME=? AND IDN_SCIM_GROUP.ATTR_NAME=?";

    private SQLQueries(){}
}
