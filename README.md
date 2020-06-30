# SCIM Group metadata population tool

This tool is written to populate SCIM Group metadata in to WSO2 Identity Server's DB when pluggin in an existing external userstore (AD/LDAP).

This tool can be run once if we are to manage all the groups on external userstore via WSO2 IS after that. But if we intend to manage groups directly from the userstore direclty, we might have to run it after making any of following operations.
* Add new groups
* Remove existing groups
* Rename existing groups

## Configuration files
### Common configurations
The file `config.properties` defines DB connection parameters and other common configs.

* DB config parameters (This is the datasource configured in the identity.xml)
```
CONNECTION_URL = <DB connection URL>
CONNECTION_USERNAME = <DB username>
CONNECTION_PASSWORD = <DB password>
CONNECTION_DRIVERCLASS = <JDBC driver class>
CONNECTION_JDBCDRIVER = <path of the JDBC drive>
```

* Other common configs
```
SCIM_GROUP_RESOURCE_LOCATION = <SCIM GROUPs resource endpoint>
DEBUG_MODE = <boolean; whether to show debug information>
DEPROVISION_GROUPS = <boolean; whether to delete role names in DB which are not in the userstore>
```
Sample configs can be found in the `config.properties` file in this repository.

### Userstore configurations
This file contains userstore specific configurations, and it is possible to have multiple files if needed.

```
TENANT_ID = <tenanr domain>
USERSTORE_DOMAIN = <userstore domain>

# Userstore connection properties
LDAP_URL = <connection URL>
LDAP_USER = <admin user>
LDAP_PASSWORD = <admin password>
GROUP_SEARCH_BASE = <group search base>>
GROUP_SEARCH_FILTER = <group search filter>
KEYSTORE = <path to client-truststore.jks>
KEYSTORE_PASSWORD = <client-truststore password>

USERSTORE_TYPE = <userstore type; LDAP/AD>
```

Valid client-truststore configuration are mandatory if we are using `ldaps` in the connection URL.

Sample configs can be found in the `ad.properties` and `ldap.properties` files in this repository.

## Steps to execute the tool

* First compile the code with following command.
```
mvn clean install
```
* Execute the binary file with the userstore configuration file as the first argument.
```
java -jar target/scim.metadata.population.tool-1.0.jar <userstore properties file>
```
* If you have multiple userstores, you can have multiple userstore configuration files and run the client with each of them.