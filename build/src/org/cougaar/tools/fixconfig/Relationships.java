package org.cougaar.tools.fixconfig;

import java.io.*;
import java.util.*;

/**
 * This class does a simple parse of prototype-ini.dat files and
 * builds a data structure that can be used to, for example, form the
 * closure of organizations needed to support another. This code make
 * several simplifying assumptions about the format of the
 * prototype-ini files. In particular, it expects files written by a
 * particular version of the configgen tool. Changes to that tool may
 * invalidate the assumptions here.
 *
 * Assumptions are noted in comments
 **/
public class Relationships {
    private static class Role {
        String name;
        Role converse;
        public Role(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
    }

    public static class Org implements Comparable {
        private Map relationships = new HashMap(13);
        private String name;
        public Org(String name) {
            this.name = name;
        }

        public void addRelationship(Role role, Org otherOrg) {
            findOrMakeOrgSet(role).add(otherOrg);
//              System.err.println("Adding "
//                                 + otherOrg.name
//                                 + " to "
//                                 + this.name
//                                 + " as "
//                                 + role);
        }

        public Set getOrgs(Role role) {
            return findOrMakeOrgSet(role);
        }

        private Set findOrMakeOrgSet(Role role) {
            Set set = (Set) relationships.get(role.name);
            if (set == null) {
                set = new HashSet(13);
                relationships.put(role.name, set);
            }
            return set;
        }

        public int hashCode() {
            return name.hashCode();
        }

        public int compareTo(Object o) {
            return name.compareTo(((Org) o).name);
        }

        public boolean equals(Object o) {
            if (o instanceof Org) return name.equals(((Org) o).name);
            return false;
        }

        public String toString() {
            return name;
        }
    }

    public static final String SUPERIOR = "Superior";
    public static final String SUBORDINATE = "Subordinate";

    private static final String SUFFIX = "-prototype-ini.dat";
    // Assumption -- Relationship section starts exactly like this.
    private static final String RELATIONSHIP = "[Relationship]";
    private static final String SUPPORTING = "Supporting";
    private static final String PROVIDER = "Provider";
    private static final String CUSTOMER = "Customer";

    private static Map roleMap = new HashMap();

    static Role findRole(String roleName) {
        return (Role) roleMap.get(roleName);
    }

    static Role createRole(String roleName, String converseRoleName) {
        Role role = findRole(roleName);
        Role converseRole = findRole(converseRoleName);
        if (role == null) {
            if (converseRole != null) {
                throw new IllegalArgumentException("Bogus role pair "
                                                   + roleName + ":" + converseRoleName);
            }
            role = new Role(roleName);
            converseRole = new Role(converseRoleName);
            role.converse = converseRole;
            converseRole.converse = role;
//              System.err.println("New Role pair: " + roleName + ":" + converseRoleName);
            roleMap.put(roleName, role);
            roleMap.put(converseRoleName, converseRole);
        } else {
            if (converseRole == null) {
                throw new IllegalArgumentException("Bogus role pair "
                                                   + roleName + ":" + converseRoleName);
            }
        }
        return role;
    }

    static Role superior = createRole(SUPERIOR, SUBORDINATE);

    private static FileFilter pidFilter = new FileFilter() {
        public boolean accept(File f) {
            return f.getName().endsWith(SUFFIX);
        }
    };

    private Map orgs = new HashMap();

    public Relationships(File dir) throws IOException {
        File[] pidFiles = dir.listFiles(pidFilter);
        for (int i = 0; i < pidFiles.length; i++) {
            File pidFile = pidFiles[i];
            String pidName = pidFile.getName();
            // Assumption -- file name, org name, cluster name are same
            String orgName = pidName.substring(0, pidName.length() - SUFFIX.length());
            Org org = findOrMakeOrg(orgName);
            parse(org, pidFile);
        }
    }

    public Set getClosure(Org org, String roleName) {
        Role role = findRole(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Unknown role: " + roleName);
        }
        Set result = new HashSet();
        getClosure(result, org, role);
        return result;
    }

    private void getClosure(Set result, Org org, Role role) {
        Set orgs = org.getOrgs(role);
//          System.err.println(org + " has " + orgs.size() + " " + role);
        orgs.removeAll(result);
        result.addAll(orgs);
        for (Iterator i = orgs.iterator(); i.hasNext(); ) {
            getClosure(result, (Org) i.next(), role);
        }
    }

    public Org findOrMakeOrg(String orgName) {
        Org org = (Org) orgs.get(orgName);
        if (org == null) {
            org = new Org(orgName);
            orgs.put(orgName, org);
        }
        return org;
    }

    private void parse(Org org, File pidFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(pidFile));
        try {
            String line;
            while (true) {
                line = reader.readLine().trim(); // Allow leading and trailing spaces
                if (line == null) throw new IOException(RELATIONSHIP + " not found");
                if (line.startsWith(RELATIONSHIP)) break;
            }
            while (true) {
                line = reader.readLine();
                if (line == null) throw new IOException("Premature EOF");
                if (line.startsWith(SUPPORTING)) {
                    parseRelationship(org, line.substring(SUPPORTING.length()).trim(), true);
                    continue;
                }
                if (line.startsWith(SUPERIOR)) {
                    parseRelationship(org, line.substring(SUPERIOR.length()).trim(), false);
                    continue;
                }
                break;          // Assumption: no blank lines or other stuff
            }
        } finally {
            reader.close();
        }
    }

    private void parseRelationship(Org org, String tail, boolean isSupporting) throws IOException {
        // Assumption -- format is "orgname" "relationship"
        // With quotes and one space between
        int pos = tail.indexOf("\" \"");
        if (pos < 0) throw new IOException("Bad format: " + tail);
        String orgName = tail.substring(1, pos);
        if (orgName.equals("")) return; // No real relationship here
        String roleName = tail.substring(pos + 3, tail.length() - 1);
        Org otherOrg = findOrMakeOrg(orgName);
        Role role;
        if (isSupporting) {
            if (!roleName.endsWith(PROVIDER)) {
                return;         // Ignore these special roles
            }
            String prefix = roleName.substring(0, roleName.length() - PROVIDER.length());
            role = createRole(roleName, prefix + CUSTOMER);
        } else {
            role = superior.converse; // Our role is "Subordinate"
        }
        org.addRelationship(role.converse, otherOrg);
        otherOrg.addRelationship(role, org);
    }
}
