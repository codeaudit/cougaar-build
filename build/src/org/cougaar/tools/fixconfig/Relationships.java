/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.tools.fixconfig;

import java.io.*;
import java.util.*;
import java.text.*;

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
    private static DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

    public static class Role {
        String name;
        Role converse;
        public Role(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
    }

    private static class RelationshipEntry {
        private Set orgs = new HashSet(13); // All orgs with this relationship
        private List rels = new ArrayList(); // The individual relationships
        public void add(Relationship rel) {
            rels.add(rel);
            orgs.add(rel.getOtherOrg());
        }
        public void remove(Relationship rel) {
            rels.remove(rel);
            Org otherOrg = rel.getOtherOrg();
            for (Iterator i = rels.iterator(); i.hasNext(); ) {
                Org anOrg = ((Relationship) i.next()).getOtherOrg();
                if (otherOrg.equals(anOrg)) return;
            }
            orgs.remove(otherOrg);
        }
        public Set getOrgs() {
            return orgs;
        }
        public List getRelationships() {
            return rels;
        }
    }

    /**
     * Detail of an individual relationship with another org
     **/
    private abstract static class Relationship {
        private Org otherOrg;
        private Role role;
        private Date from;
        private Date to;

        public Relationship(Org otherOrg, Role role, Date from, Date to) {
            this.otherOrg = otherOrg;
            this.role = role;
            this.from = from;
            this.to = to;
        }

        public Role getRole() {
            return role;
        }

        public Org getOtherOrg() {
            return otherOrg;
        }

        public Date getFrom() {
            return from;
        }

        public Date getTo() {
            return to;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Relationship)) return false;
            Relationship that = (Relationship) o;
            if (this.otherOrg.equals(that.otherOrg)) {
                if (this.role.equals(that.role)) {
                    if (this.from == that.from) return true;
                    if (this.from != null) {
                        return this.from.equals(that.from);
                    }
                }
            }
            return false;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            if (role == superior) {
                buf.append(SUPERIOR);
            } else {
                buf.append(SUPPORTING);
            }
            buf.append(' ');
            buf.append('"').append(otherOrg).append('"');
            if (role == superior) {
                buf.append(" \"\"");
            } else {
                buf.append(" \"").append(role).append('"');
            }
            if (isFromRelationships()) {
                if (from != null) {
                    buf.append(" \"").append(dateFormat.format(from)).append('"');
                    buf.append(" \"").append(dateFormat.format(to)).append('"');
                } else {
                    buf.append(" \"\" \"\"");
                }
            }
            return buf.toString();
        }

        public abstract boolean isImplied();
        public abstract boolean isFromPrototypeIni();
        public abstract boolean isFromRelationships();
    }

    /** An implied Relationship (no file) **/
    private static class ImpliedRelationship extends Relationship {
        public ImpliedRelationship(Org otherOrg, Role role, Date from, Date to) {
            super(otherOrg, role, from, to);
        }
        public boolean isImplied() { return true; }
        public boolean isFromPrototypeIni() { return false; }
        public boolean isFromRelationships() { return false; }
    }

    /** A Relationship from a prototype-ini.dat file **/
    private static class ProtoRelationship extends Relationship {
        public ProtoRelationship(Org otherOrg, Role role, Date from, Date to) {
            super(otherOrg, role, from, to);
        }
        public boolean isImplied() { return false; }
        public boolean isFromPrototypeIni() { return true; }
        public boolean isFromRelationships() { return false; }

    }

    /** A Relationship from a relationship.dat file **/
    private static class RelRelationship extends Relationship {
        public RelRelationship(Org otherOrg, Role role, Date from, Date to) {
            super(otherOrg, role, from, to);
        }
        public boolean isImplied() { return false; }
        public boolean isFromPrototypeIni() { return false; }
        public boolean isFromRelationships() { return true; }
    }

    /**
     * Inner class describing the relationships of an organization.
     **/
    public static class Org implements Comparable {
        private Map relationships = new HashMap(13);
        private List pidInitialLines = new ArrayList();
        private List pidFinalLines = new ArrayList();
        private List relInitialLines = new ArrayList();
        private List relFinalLines = new ArrayList();
        private String name;
        public Org(String name) {
            this.name = name;
        }

        public void addInitialLine(String line, boolean pid) {
            (pid ? pidInitialLines : relInitialLines).add(line);
        }

        public List getInitialLines(boolean pid) {
            return pid ? pidInitialLines : relInitialLines;
        }

        public void addFinalLine(String line, boolean pid) {
            (pid ? pidFinalLines : relFinalLines).add(line);
        }

        public List getFinalLines(boolean pid) {
            return pid ? pidFinalLines : relFinalLines;
        }

        public void addRelationship(Relationship rel) {
            findOrMakeRelationshipEntry(rel.getRole()).add(rel);
//              Fix.logFile.println("Adding "
//                                 + otherOrg.name
//                                 + " to "
//                                 + this.name
//                                 + " as "
//                                 + role);
        }

        public List getRelationships(Role role) {
            return findOrMakeRelationshipEntry(role).getRelationships();
        }

        public List removeRelationships(Role role) {
            RelationshipEntry entry = (RelationshipEntry) relationships.remove(role.name);
            if (entry != null) {
                return entry.getRelationships();
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        public List getSelectedRelationships(Set orgs, Set roles, boolean pid) {
            List result = new ArrayList();
            Class cls = pid ? ProtoRelationship.class : RelRelationship.class;
            for (Iterator i = roles.iterator(); i.hasNext(); ) {
                Role role = (Role) i.next();
                List rels = getRelationships(role);
                for (Iterator j = rels.iterator(); j.hasNext(); ) {
                    Relationship r = (Relationship) j.next();
                    if (r.getClass() == cls && orgs.contains(r.getOtherOrg())) {
                        result.add(r);
                    }
                }
            }
            return result;
        }

        public void removeRelationship(Relationship rel) {
            RelationshipEntry entry = (RelationshipEntry) relationships.get(rel.getRole().name);
            if (entry != null) {
                entry.remove(rel);
            }
        }

        public Set getOrgs(Role role) {
            RelationshipEntry entry = findOrMakeRelationshipEntry(role);
            return entry.getOrgs();
        }

        private RelationshipEntry findOrMakeRelationshipEntry(Role role) {
            RelationshipEntry entry = (RelationshipEntry) relationships.get(role.name);
            if (entry == null) {
                entry = new RelationshipEntry();
                relationships.put(role.name, entry);
            }
            return entry;
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

        public String getName() {
            return name;
        }

        public String toString() {
            return name;
        }
    }

    public static final String SUPERIOR = "Superior";
    public static final String SUBORDINATE = "Subordinate";

    private static final String PID_SUFFIX = "-prototype-ini.dat";
    private static final String REL_SUFFIX = "-relationships.ini";
    // Assumption -- Relationship section starts exactly like this.
    private static final String RELATIONSHIP = "[Relationship]";
    private static final String SUPPORTING = "Supporting";
    private static final String PROVIDER = "Provider";
    private static final String CUSTOMER = "Customer";
    private static final String AUTHORITY = "Authority";
    private static final String SUBJECT = "Subject";
    private static final String SUPPLIER = "Supplier";
    private static final String SUPPLYEE = "Supplyee";
    private static final String POSTER = "Poster";
    private static final String POSTEE = "Postee";

    private static class NamePair {
        public String name1, name2;
        public NamePair(String n1, String n2) {
            name1 = n1;
            name2 = n2;
        }
    }

    private static NamePair[] roleSuffixPairs = {
        new NamePair(PROVIDER, CUSTOMER),
        new NamePair(AUTHORITY, SUBJECT),
        new NamePair(SUPPLIER, SUPPLYEE),
        new NamePair(POSTER, POSTEE)
    };

    private static Map roleMap = new HashMap();

    static Role findRole(String roleName) {
        return (Role) roleMap.get(roleName);
    }

    public static Role createRole(String roleName) {
        String originalRoleName = roleName;
        String roleSuffix = null;
        String converseRoleSuffix = null;
        String hyphenSuffix = "";
        int hyphenPos = roleName.lastIndexOf('-');
        if (hyphenPos >= 0) {
            hyphenSuffix = roleName.substring(hyphenPos);
            roleName = roleName.substring(0, hyphenPos);
        }
        for (int i = 0; i < roleSuffixPairs.length; i++) {
            NamePair pair = roleSuffixPairs[i];
            if (roleName.endsWith(pair.name1)) {
                roleSuffix = pair.name1;
                converseRoleSuffix = pair.name2;
                break;
            }
            if (roleName.endsWith(pair.name2)) {
                roleSuffix = pair.name2;
                converseRoleSuffix = pair.name1;
                break;
            }
        }
        if (roleSuffix == null)
            throw new IllegalArgumentException("Not a provider role: " + originalRoleName);
        String prefix = roleName.substring(0, roleName.length() - roleSuffix.length());
        return createRole(roleName + hyphenSuffix, prefix + converseRoleSuffix + hyphenSuffix);
    }

    public static Role createRole(String roleName, String converseRoleName) {
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
//              Fix.logFile.println("New Role pair: " + roleName + ":" + converseRoleName);
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

    public static Set getAllRoles() {
        return new HashSet(roleMap.values());
    }

    static Role superior = createRole(SUPERIOR, SUBORDINATE);

    private Map orgs = new HashMap();

    public Relationships(File dir) throws IOException {
        readFiles(dir, true);
        readFiles(dir, false);
    }

    private void readFiles(File dir, boolean pid) throws IOException {
        final String suffix = pid ? PID_SUFFIX : REL_SUFFIX;
        FileFilter filter = new FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(suffix);
            }
        };
        File[] files = dir.listFiles(filter);
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String name = file.getName();
            // Assumption -- file name, org name, cluster name are same
            String orgName = name.substring(0, name.length() - suffix.length());
            Org org = findOrMakeOrg(orgName);
            parse(org, file, pid);
        }
    }

    public void writeFiles(File dir, Set closure, Set roles) throws IOException {
        if (roles == null) roles = getAllRoles();
        writeFile(dir, closure, roles, true);
        writeFile(dir, closure, roles, false);
    }

    private void writeFile(File dir, Set closure, Set roles, boolean pid)
        throws IOException
    {
        final String suffix = (pid ? PID_SUFFIX : REL_SUFFIX);
        for (Iterator i = closure.iterator(); i.hasNext(); ) {
            Org org = (Org) i.next();
            File file = new File(dir, org.getName() + suffix);
            write(org, file, pid, closure, roles);
        }
    }

    public Set getClosure(Org org, Role role) {
        Set result = new HashSet();
        getClosure(result, org, role);
        return result;
    }

    private void getClosure(Set result, Org org, Role role) {
        Set orgs = org.getOrgs(role.converse);
//          Fix.logFile.println(org + " has " + orgs.size() + " " + role);
        orgs.removeAll(result);
        result.addAll(orgs);
        for (Iterator i = orgs.iterator(); i.hasNext(); ) {
            getClosure(result, (Org) i.next(), role);
        }
    }

    public void remove(String orgName, Role roleToRemove) {
        Org org = findOrMakeOrg(orgName);
        List rels = org.removeRelationships(roleToRemove);
        for (Iterator i = rels.iterator(); i.hasNext(); ) {
            Relationship rel = (Relationship) i.next();
            Org otherOrg = rel.getOtherOrg();
            Relationship oprel =
                new ImpliedRelationship(org, roleToRemove.converse,
                                        rel.getFrom(), rel.getTo());
            otherOrg.removeRelationship(oprel);
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

    private void parse(Org org, File file, boolean pid) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while (pid && true) {
                line = reader.readLine();
                if (line == null) throw new IOException(RELATIONSHIP + " not found in " + file);
                org.addInitialLine(line, pid);
                if (line.startsWith(RELATIONSHIP)) break;
            }
            while (true) {
                line = reader.readLine();
                if (line == null) break;
                if (line.startsWith(SUPPORTING)) {
                    if (!parseRelationship(org, line.substring(SUPPORTING.length()).trim(), true, pid)) {
//                      org.addFinalLine(line, pid);
                        Fix.logFile.println("Ignoring: " + line);
                    }
                    continue;
                }
                if (line.startsWith(SUPERIOR)) {
                    if (!parseRelationship(org, line.substring(SUPERIOR.length()).trim(), false, pid))
                        org.addFinalLine(line, pid);
                    continue;
                }
                break;          // Assumption: no blank lines or other stuff
            }
            while (line != null) {
                org.addFinalLine(line, pid);
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }
    }

    private Date parseDate(String s) {
        if (s == null) return null;
        if (s.equals("")) return null;
        try {
            return dateFormat.parse(s);
        } catch (ParseException pe) {
            System.err.println(pe + ": " + s);
            return null;
        }
    }

    private boolean parseRelationship(Org org, String tail,
                                      boolean isSupporting, boolean pid)
        throws IOException
    {
        // Assumption -- format is "orgname" "relationship" ["from" "to"]
        // With quotes and one space between
        String[] strings = new String[4];
        String s = tail;
        int nStrings = 0;
    loop:
        while (true) {
            int pos = s.indexOf("\" \"");
            String arg = pos < 0 ? s.substring(1, s.length() - 1) : s.substring(1, pos);
            strings[nStrings++] = arg;
            if (pos < 0) break;
            if (nStrings == 4) throw new IOException("Too many args: " + tail);
            s = s.substring(pos + 2);
        }
        switch (nStrings) {
        default:
            throw new IOException(nStrings + " args: " + tail);
        case 2:
        case 4:
            break;
        }
        String orgName = strings[0];
        if (orgName.equals("")) return false; // No real relationship here
        String roleName = strings[1];
        Org otherOrg = findOrMakeOrg(orgName);
        Date from = parseDate(strings[2]);
        Date to = parseDate(strings[3]);
        Role role;
        if (!isSupporting) {
            role = superior;
        } else {
            try {
                role = createRole(roleName);
            } catch (IllegalArgumentException iae) {
                return false;   // Not a provider role
            }
        }
        otherOrg.addRelationship(new ImpliedRelationship(org, role.converse, from, to));
        if (pid) {
            org.addRelationship(new ProtoRelationship(otherOrg, role, from, to));
        } else {
            org.addRelationship(new RelRelationship(otherOrg, role, from, to));
        }
        return true;
    }

    private void write(Org org, File file, boolean pid, Set closure, Set roles)
        throws IOException
    {
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        writeLines(writer, org.getInitialLines(pid));
        writeLines(writer, org.getSelectedRelationships(closure, Collections.singleton(superior), pid));
        writeLines(writer, org.getSelectedRelationships(closure, roles, pid));
        writeLines(writer, org.getFinalLines(pid));
        writer.close();
    }

    private void writeLines(PrintWriter writer, Collection lines) {
        for (Iterator i = lines.iterator(); i.hasNext(); ) {
            writer.println(i.next());
        }
    }
}
