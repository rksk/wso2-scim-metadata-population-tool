package org.wso2.scim.metadata.population.tool.ldap;

import org.wso2.scim.metadata.population.tool.IdentityException;
import org.wso2.scim.metadata.population.tool.model.SCIMGroup;
import org.wso2.scim.metadata.population.tool.util.ConfigParser;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class LDAPUtils {

    private static ConfigParser configParser;
    private static String LDAP_URL;
    private static String LDAP_USER;
    private static String LDAP_PASSWORD;
    private static String GROUP_SEARCH_BASE;
    private static String GROUP_SEARCH_FILTER;
    private static String LDAP_REFERRAL;
    private static String KEYSTORE;
    private static String KEYSTORE_PASSWORD;

    private static String NAME_ATTRIBUTE;
    private static String UUID_ATTRIBUTE;
    private static String CREATED_AT_ATTRIBUTE;
    private static String UPDATED_AT_ATTRIBUTE;

    private static boolean debug;

    private static Calendar calendarForTimestampConversion;

    public static ArrayList<SCIMGroup> getAllGroupsFromLDAP() throws IdentityException {

        configParser = new ConfigParser();
        debug = Boolean.valueOf(configParser.getProperty("DEBUG_MODE"));
        LDAP_URL = configParser.getProperty("LDAP_URL");
        LDAP_USER = configParser.getProperty("LDAP_USER");
        LDAP_PASSWORD = configParser.getProperty("LDAP_PASSWORD");
        GROUP_SEARCH_BASE = configParser.getProperty("GROUP_SEARCH_BASE");
        GROUP_SEARCH_FILTER = configParser.getProperty("GROUP_SEARCH_FILTER");
        LDAP_REFERRAL = "ignore";
        KEYSTORE = configParser.getProperty("KEYSTORE");
        KEYSTORE_PASSWORD = configParser.getProperty("KEYSTORE_PASSWORD");
        NAME_ATTRIBUTE = configParser.getProperty("NAME_ATTRIBUTE");
        UUID_ATTRIBUTE = configParser.getProperty("UUID_ATTRIBUTE");
        CREATED_AT_ATTRIBUTE = configParser.getProperty("CREATED_AT_ATTRIBUTE");
        UPDATED_AT_ATTRIBUTE = configParser.getProperty("UPDATED_AT_ATTRIBUTE");

        ArrayList<SCIMGroup> allGroups = new ArrayList<>();

        if (debug) {
            System.out.println("LDAP URL: " + LDAP_URL);
            System.out.println("LDAP User: " + LDAP_USER);
            System.out.println("LDAP Search Base: " + GROUP_SEARCH_BASE);
            System.out.println("LDAP Search Filter: " + GROUP_SEARCH_FILTER);
            System.out.println("LDAP Referral: " + LDAP_REFERRAL);
            System.out.println("LDAP Attribute: " + NAME_ATTRIBUTE);
            System.out.println("Trust store location: " + KEYSTORE);
        }

        System.setProperty("javax.net.ssl.trustStore", KEYSTORE);
        System.setProperty("javax.net.ssl.trustStorePassword", KEYSTORE_PASSWORD);

        Hashtable<String, String> environment = new Hashtable<>();
        environment.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put("java.naming.security.authentication", "simple");
        environment.put("java.naming.referral", LDAP_REFERRAL);
        environment.put("java.naming.provider.url", LDAP_URL);
        environment.put("java.naming.security.principal", LDAP_USER);
        environment.put("java.naming.security.credentials", LDAP_PASSWORD);
        environment.put("java.naming.ldap.attributes.binary", "objectSid objectGUID");

        InitialDirContext dirContext = null;
        NamingEnumeration<SearchResult> results = null;
        SearchResult searchResult = null;

        try {
            if (debug) System.out.println("Initializing DirContext");
            dirContext = new InitialDirContext(environment);

            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(2);

            String[] returningAttributes = new String[]{NAME_ATTRIBUTE, UUID_ATTRIBUTE, CREATED_AT_ATTRIBUTE, UPDATED_AT_ATTRIBUTE};
            searchControls.setReturningAttributes(returningAttributes);

            results = dirContext.search(GROUP_SEARCH_BASE, GROUP_SEARCH_FILTER, searchControls);

            while(results.hasMore()) {
                try {
                    searchResult = results.next();
                    Attributes attributes = searchResult.getAttributes();

                    if (attributes.get(NAME_ATTRIBUTE) == null) {
                        throw new IdentityException("No value found for the NAME_ATTRIBUTE: " + NAME_ATTRIBUTE);
                    }
                    String name = String.valueOf(attributes.get(NAME_ATTRIBUTE).get(0));

                    if (attributes.get(UUID_ATTRIBUTE) == null) {
                        throw new IdentityException("No value found for the UUID_ATTRIBUTE: " + UUID_ATTRIBUTE);
                    }
                    Object ido = attributes.get(UUID_ATTRIBUTE).get(0);
                    String id = null;
                    if (ido instanceof byte[]) {
                        if ("objectGUID".equalsIgnoreCase(UUID_ATTRIBUTE) || "objectSid".equalsIgnoreCase(UUID_ATTRIBUTE)) {
                            byte[] bytes = (byte[]) ido;
                            final ByteBuffer bb = ByteBuffer.wrap(swapBytes(bytes));
                            id = new java.util.UUID(bb.getLong(), bb.getLong()).toString();
                        } else {
                            id = String.valueOf(ido);
                        }
                    } else {
                        id = String.valueOf(ido);
                    }

                    if (debug) System.out.println("\nFound group: " + name + " with ID: " + id);
                    SCIMGroup group = new SCIMGroup(name, id);

                    String createdAt;
                    if (attributes.get(CREATED_AT_ATTRIBUTE) == null) {
                        if (debug) System.out.println("createdAt time was null, using current timestamp instead.");
                        createdAt = getCurrentTime();
                    } else {
                        createdAt = String.valueOf(attributes.get(CREATED_AT_ATTRIBUTE).get(0));
                        createdAt = convertDateFormatFromAD(createdAt);
                    }

                    String updatedAt;
                    if (attributes.get(UPDATED_AT_ATTRIBUTE) == null) {
                        if (debug) System.out.println("updatedAt time was null, using createdAt timestamp instead.");
                        updatedAt = createdAt;
                    } else {
                        updatedAt = String.valueOf(attributes.get(UPDATED_AT_ATTRIBUTE).get(0));
                        updatedAt = convertDateFormatFromAD(updatedAt);
                    }

                    group.setCreatedAt(createdAt);
                    group.setUpdatedAt(updatedAt);
                    allGroups.add(group);
                } catch (Exception e) {
                    throw new IdentityException("An error occurred while reading LDAP data", e);
                }
            }
        } catch (NamingException e) {
            throw new IdentityException("An error occurred performing the LDAP search", e);
        } finally {
            if (results != null) {
                try {
                    results.close();
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }

            if (dirContext != null) {
                try {
                    dirContext.close();
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }

        }

        return allGroups;
    }

    private static byte[] swapBytes(byte[] bytes) {
        // bytes[0] <-> bytes[3]
        byte swap = bytes[3];
        bytes[3] = bytes[0];
        bytes[0] = swap;
        // bytes[1] <-> bytes[2]
        swap = bytes[2];
        bytes[2] = bytes[1];
        bytes[1] = swap;
        // bytes[4] <-> bytes[5]
        swap = bytes[5];
        bytes[5] = bytes[4];
        bytes[4] = swap;
        // bytes[6] <-> bytes[7]
        swap = bytes[7];
        bytes[7] = bytes[6];
        bytes[6] = swap;
        return bytes;
    }

    private static String convertDateFormatFromAD(String fromDate) throws ParseException {

        final String WSO2_CLAIM_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
        if (fromDate == null) {
            throw new ParseException("Value provided for date conversion is null.", 0);
        }

        SimpleDateFormat scimDateFormat = new SimpleDateFormat(WSO2_CLAIM_DATE_TIME_FORMAT);

        return scimDateFormat.format(parseGeneralizedTime(fromDate));
    }

    private static String getCurrentTime() throws ParseException {

        final String WSO2_CLAIM_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
        SimpleDateFormat scimDateFormat = new SimpleDateFormat(WSO2_CLAIM_DATE_TIME_FORMAT);
        Date date = new Date();
        return scimDateFormat.format(date);
    }

    /*
     * Below code snippets were borrowed from Apache LDAP Directory API v2.0.0.
     * As the required Date Time APIs for Active Directory Date format conversion are not available in Java 7.
     * For code comments and further reference,
     * {@See https://github.com/apache/directory-ldap-api/blob/2.0.0/util/src/main/java/
     * org/apache/directory/api/util/GeneralizedTime.java}
     *
     * <code> - Begining snippet of the code borrowed from Apache LDAP API
     */
    private static Date parseGeneralizedTime(String generalizedTime) throws ParseException {

        if (calendarForTimestampConversion == null) {
            calendarForTimestampConversion = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.ROOT);
        }

        calendarForTimestampConversion.setTimeInMillis(0);
        calendarForTimestampConversion.setLenient(false);

        parseYear(generalizedTime);
        parseMonth(generalizedTime);
        parseDay(generalizedTime);
        parseHour(generalizedTime);

        if (generalizedTime.length() < 11) {
            throw new ParseException("Bad Generalized Time.", 10);
        }

        int positionOfElement = 10;
        char charAtPositionOfElement = generalizedTime.charAt(positionOfElement);

        if (('0' <= charAtPositionOfElement) && (charAtPositionOfElement <= '9')) {
            parseMinute(generalizedTime);

            if (generalizedTime.length() < 13) {
                throw new ParseException("Bad Generalized Time.", 12);
            }

            positionOfElement = 12;
            charAtPositionOfElement = generalizedTime.charAt(positionOfElement);

            if (('0' <= charAtPositionOfElement) && (charAtPositionOfElement <= '9')) {
                parseSecond(generalizedTime);

                if (generalizedTime.length() < 15) {
                    throw new ParseException("Bad Generalized Time.", 14);
                }

                positionOfElement = 14;
                charAtPositionOfElement = generalizedTime.charAt(positionOfElement);

                if ((charAtPositionOfElement == '.') || (charAtPositionOfElement == ',')) {
                    parseFractionOfSecond(generalizedTime);
                    positionOfElement += 1;
                    parseTimezone(generalizedTime, positionOfElement);
                } else if ((charAtPositionOfElement == 'Z') || (charAtPositionOfElement == '+')
                        || (charAtPositionOfElement == '-')) {
                    parseTimezone(generalizedTime, positionOfElement);
                } else {
                    throw new ParseException("Time is too short.", 14);
                }
            } else if ((charAtPositionOfElement == '.') || (charAtPositionOfElement == ',')) {
                parseFractionOfMinute(generalizedTime);
                positionOfElement += 1;

                parseTimezone(generalizedTime, positionOfElement);
            } else if ((charAtPositionOfElement == 'Z') || (charAtPositionOfElement == '+')
                    || (charAtPositionOfElement == '-')) {
                parseTimezone(generalizedTime, positionOfElement);
            } else {
                throw new ParseException("Time is too short.", 12);
            }
        } else if ((charAtPositionOfElement == '.') || (charAtPositionOfElement == ',')) {
            parseFractionOfHour(generalizedTime);
            positionOfElement += 1;

            parseTimezone(generalizedTime, positionOfElement);
        } else if ((charAtPositionOfElement == 'Z') || (charAtPositionOfElement == '+')
                || (charAtPositionOfElement == '-')) {
            parseTimezone(generalizedTime, positionOfElement);
        } else {
            throw new ParseException("Invalid Generalized Time.", 10);
        }

        try {
            calendarForTimestampConversion.getTimeInMillis();
        } catch (IllegalArgumentException iae) {
            throw new ParseException("Invalid date time.", 0);
        }

        calendarForTimestampConversion.setLenient(true);
        return calendarForTimestampConversion.getTime();
    }

    private static void parseTimezone(String generalizedTime, int positionOfElement) throws ParseException {

        if (generalizedTime.length() < positionOfElement + 1) {
            throw new ParseException("Time is too short, no 'timezone' element found.", positionOfElement);
        }

        char charAtPositionOfElement = generalizedTime.charAt(positionOfElement);

        if (charAtPositionOfElement == 'Z') {
            calendarForTimestampConversion.setTimeZone(TimeZone.getTimeZone("GMT"));

            if (generalizedTime.length() > positionOfElement + 1) {
                throw new ParseException("Time is too short, no 'timezone' element found.", positionOfElement + 1);
            }
        } else if ((charAtPositionOfElement == '+') || (charAtPositionOfElement == '-')) {
            StringBuilder stringBuilder = new StringBuilder("GMT");
            stringBuilder.append(charAtPositionOfElement);

            String digits = getAllDigits(generalizedTime, positionOfElement + 1);
            stringBuilder.append(digits);

            if (digits.length() == 2 && digits.matches("^([01]\\d|2[0-3])$")) {
                TimeZone timeZone = TimeZone.getTimeZone(stringBuilder.toString());
                calendarForTimestampConversion.setTimeZone(timeZone);
            } else if (digits.length() == 4 && digits.matches("^([01]\\d|2[0-3])([0-5]\\d)$")) {
                TimeZone timeZone = TimeZone.getTimeZone(stringBuilder.toString());
                calendarForTimestampConversion.setTimeZone(timeZone);
            } else {
                throw new ParseException("Value of 'timezone' must be 2 digits or 4 digits.", positionOfElement);
            }

            if (generalizedTime.length() > positionOfElement + 1 + digits.length()) {
                throw new ParseException("Time is too short, no 'timezone' element found.",
                        positionOfElement + 1 + digits.length());
            }
        }
    }

    private static void parseFractionOfSecond(String fromDate) throws ParseException {

        String fraction = getFraction(fromDate, 14 + 1);
        double fractionDouble = Double.parseDouble("0." + fraction);
        int millisecond = (int) Math.floor(fractionDouble * 1000);

        calendarForTimestampConversion.set(GregorianCalendar.MILLISECOND, millisecond);
    }

    private static void parseFractionOfMinute(String generalizedTime) throws ParseException {

        String fraction = getFraction(generalizedTime, 12 + 1);
        double fractionDouble = Double.parseDouble("0." + fraction);
        int milliseconds = (int) Math.round(fractionDouble * 1000 * 60);
        int second = milliseconds / 1000;
        int millisecond = milliseconds - (second * 1000);

        calendarForTimestampConversion.set(Calendar.SECOND, second);
        calendarForTimestampConversion.set(Calendar.MILLISECOND, millisecond);
    }

    private static void parseFractionOfHour(String generalizedTime) throws ParseException {

        String fraction = getFraction(generalizedTime, 10 + 1);
        double fractionDouble = Double.parseDouble("0." + fraction);
        int milliseconds = (int) Math.round(fractionDouble * 1000 * 60 * 60);
        int minute = milliseconds / (1000 * 60);
        int second = (milliseconds - (minute * 60 * 1000)) / 1000;
        int millisecond = milliseconds - (minute * 60 * 1000) - (second * 1000);

        calendarForTimestampConversion.set(Calendar.MINUTE, minute);
        calendarForTimestampConversion.set(Calendar.SECOND, second);
        calendarForTimestampConversion.set(Calendar.MILLISECOND, millisecond);
    }

    private static String getAllDigits(String generalizedTime, int startIndex) {

        StringBuilder stringBuilder = new StringBuilder();
        while (generalizedTime.length() > startIndex) {
            char charAtStartIndex = generalizedTime.charAt(startIndex);
            if ('0' <= charAtStartIndex && charAtStartIndex <= '9') {
                stringBuilder.append(charAtStartIndex);
                startIndex++;
            } else {
                break;
            }
        }
        return stringBuilder.toString();
    }

    private static void parseSecond(String generalizedTime) throws ParseException {

        if (generalizedTime.length() < 14) {
            throw new ParseException("Time is too short, no 'second' element found.", 12);
        }
        try {
            int second = Integer.parseInt(generalizedTime.substring(12, 14));
            calendarForTimestampConversion.set(Calendar.SECOND, second);
        } catch (NumberFormatException e) {
            throw new ParseException("Value of 'second' is not a number.", 12);
        }
    }

    private static void parseMinute(String generalizedTime) throws ParseException {

        if (generalizedTime.length() < 12) {
            throw new ParseException("Time is too short, no 'minute' element found.", 10);
        }
        try {
            int minute = Integer.parseInt(generalizedTime.substring(10, 12));
            calendarForTimestampConversion.set(Calendar.MINUTE, minute);
        } catch (NumberFormatException e) {
            throw new ParseException("Value of 'minute' is not a number.", 10);
        }
    }

    private static void parseHour(String generalizedTime) throws ParseException {

        if (generalizedTime.length() < 10) {
            throw new ParseException("Time is too short, no 'hour' element found.", 8);
        }
        try {
            int hour = Integer.parseInt(generalizedTime.substring(8, 10));
            calendarForTimestampConversion.set(Calendar.HOUR_OF_DAY, hour);
        } catch (NumberFormatException e) {
            throw new ParseException("Value of 'hour' is not a number.", 8);
        }
    }

    private static void parseDay(String generalizedTime) throws ParseException {

        if (generalizedTime.length() < 8) {
            throw new ParseException("Time is too short, no 'day' element found.", 6);
        }
        try {
            int day = Integer.parseInt(generalizedTime.substring(6, 8));
            calendarForTimestampConversion.set(Calendar.DAY_OF_MONTH, day);
        } catch (NumberFormatException e) {
            throw new ParseException("Value of 'day' is not a number.", 6);
        }
    }

    private static void parseMonth(String generalizedTime) throws ParseException {

        if (generalizedTime.length() < 6) {
            throw new ParseException("Time is too short, no 'month' element found.", 4);
        }
        try {
            int month = Integer.parseInt(generalizedTime.substring(4, 6));
            calendarForTimestampConversion.set(Calendar.MONTH, month - 1);
        } catch (NumberFormatException e) {
            throw new ParseException("Value of 'month' is not a number.", 4);
        }
    }

    private static void parseYear(String generalizedTime) throws ParseException {

        if (generalizedTime.length() < 4) {
            throw new ParseException("Time is too short, no 'year' element found.", 0);
        }
        try {
            int year = Integer.parseInt(generalizedTime.substring(0, 4));
            calendarForTimestampConversion.set(Calendar.YEAR, year);
        } catch (NumberFormatException e) {
            throw new ParseException("Value of 'year' is not a number.", 0);
        }
    }

    private static String getFraction(String generalizedTime, int startIndex) throws ParseException {

        String fraction = getAllDigits(generalizedTime, startIndex);

        if (fraction.length() == 0) {
            throw new ParseException("Time is too short, no 'fraction' element found.", startIndex);
        }

        return fraction;
    }
    /* </code> - Ending snippet of the code borrowed from Apache LDAP API */
}
