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

public class Fix {
    private static final String CLUSTERS = "[ Clusters ]";
    private static final String CLUSTER_EQUALS = "cluster = ";
    private static final String DOT_INI = ".ini";
    private static final String RELATIONSHIPS_INI = "-relationships.ini";

    private static boolean checkOnly = false;
    public static PrintStream logFile = System.err;
    public static PrintStream outFile = System.out;

    public static class ClusterOptionSet {
        String option;
        private String[] clusters;
        public ClusterOptionSet(String option, String[] clusters) {
            this.option = option;
            this.clusters = clusters;
        }
    }
    public interface Fixer {
        String fix(String line);
    }

    public interface Checker extends Matcher {
        Relationships.Role getRole();
    }

    public interface Matcher {
        boolean matches(String line);
    }

    public static class MatchPlugIn implements Matcher {
        private String pattern;
        public MatchPlugIn(String pattern) {
            this.pattern = pattern;
        }

        public boolean matches(String line) {
            return line.startsWith(pattern);
        }
    }

    public static class PlugIn implements Fixer {
        String pattern;
        public PlugIn(String pattern) {
            this.pattern = pattern;
        }
        public String fix(String line) {
            if (line.startsWith(pattern)) {
                return null;
            }
            return line;
        }
    }

    public static class CheckParameter extends MatchParameter implements Checker {
        private Relationships.Role role;

        public CheckParameter(Relationships.Role role, String pluginPattern, String parameterPattern) {
            super(pluginPattern, parameterPattern);
            this.role = role;
        }

        public Relationships.Role getRole() {
            return role;
        }
    }

    public static class CheckAny implements Checker {
        private Relationships.Role role;
        private Matcher[] matchers;

        public CheckAny(Relationships.Role role, Matcher[] matchers) {
            this.matchers = matchers;
            this.role = role;
        }

        public Relationships.Role getRole() {
            return role;
        }
        
        public boolean matches(String line) {
            for (int i = 0; i < matchers.length; i++) {
                if (matchers[i].matches(line)) return true;
            }
            return false;
        }
    }

    public static class MatchParameter extends DeleteParameter implements Matcher {
        public MatchParameter(String pluginPattern, String parameterPattern) {
            super(pluginPattern, parameterPattern, true);
        }

        public boolean matches(String line) {
            if (line.equals(super.fix(line))) {
                return false;
            } else {
                return true;
            }
        }
    }

    public static class DeleteParameter implements Fixer {
        String pluginPattern;
        String parameterPattern;
        boolean keepPlugInIfNone;
        public DeleteParameter(String pluginPattern, String parameterPattern) {
            this(pluginPattern, parameterPattern, false);
        }

        public DeleteParameter(String pluginPattern, String parameterPattern, boolean keepPlugInIfNone) {
            this.pluginPattern = pluginPattern;
            this.parameterPattern = parameterPattern;
            this.keepPlugInIfNone = keepPlugInIfNone;
        }

        public String fix(String line) {
            if (line.startsWith(pluginPattern)) {
                int lparen = line.indexOf('(');
                if (lparen >= 0) {
                    int rparen = line.indexOf(')', lparen);
                    if (rparen >= 0) {
                        String head = line.substring(0, lparen);
                        String tail = line.substring(rparen + 1);
                        StringTokenizer tokens =
                            new StringTokenizer(line.substring(lparen + 1, rparen), ", ");
                        StringBuffer buf = new StringBuffer();
                        buf.append('(');
                        boolean first = true;
                        while (tokens.hasMoreTokens()) {
                            String parameter = tokens.nextToken();
                            if (!parameter.equals(parameterPattern)) {
                                if (first) {
                                    first = false;
                                } else {
                                    buf.append(", ");
                                }
                                buf.append(parameter);
                            }
                        }
                        buf.append(')');
                        if (first) {
                            if (keepPlugInIfNone) {
                                return head + tail;
                            } else {
                                return null;
                            }
                        } else {
                            return head + buf.toString() + tail;
                        }
                    }
                }
            }
            return line;
        }
    }

    private String[] clusters;

    protected Fixer[] concatenateFixers(Fixer[][] f) {
        int n = 0;
        for (int i = 0; i < f.length; i++) n += f[i].length;
        Fixer[] result = new Fixer[n];
        n = 0;
        for (int i = 0; i < f.length; i++) {
            System.arraycopy(f[i], 0, result, n, f[i].length);
            n += f[i].length;
        }
        return result;
    }

    private Fixer[] addedFixers = new Fixer[0];

    protected void addFixers(Fixer[] fixers) {
        Fixer[] newFixers = new Fixer[addedFixers.length + fixers.length];
        System.arraycopy(addedFixers, 0, newFixers, 0, addedFixers.length);
        System.arraycopy(fixers, 0, newFixers, addedFixers.length, fixers.length);
        addedFixers = newFixers;
    }

    protected Fixer[] getFixes() {
        return addedFixers;
    }

    public void setClusters(String[] clusters) {
        this.clusters = clusters;
        outFile.println(CLUSTERS);
        for (int i = 0; i < clusters.length; i++) {
            outFile.println("cluster = " + clusters[i]);
        }
    }

    public void fix(Relationships ships) {
        Fixer[] fixers = getFixes();
        Checker[] checkers = CheckDemand.getDemandCheckers();
        boolean[] hasDemand = new boolean[checkers.length];
        boolean doWrite = fixers.length > 0 && !checkOnly;
        logFile.println(hasDemand.length + " checkers");
        logFile.println(fixers.length + " fixers");
        for (int i = 0; i < clusters.length; i++) {
            String cluster = clusters[i];
            File file = new File(cluster + ".ini");
            if (!file.exists()) {
                logFile.println("No ini file for cluster: " + cluster);
                continue;
            }
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                boolean changed = false;
                File out = null;
                File bak = null;
                if (doWrite) {
                    out = new File(file.getParentFile(), "#" + file.getName() + "#");
                    bak = new File(file.getParentFile(), file.getName() + "~");
                }
                Arrays.fill(hasDemand, false);
                try {
                    PrintWriter writer = null;
                    if (doWrite) {
                        writer = new PrintWriter(new FileWriter(out));
                    }
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (fixers.length > 0) {
                                String fixed = null;
                                for (int j = 0; line != null && j < fixers.length; j++) {
                                    fixed = fixers[j].fix(line);
                                    if (!line.equals(fixed)) {
                                        changed = true;
                                        line = fixed;
                                    }
                                }
                                if (doWrite && line != null) writer.println(line);
                            }
                            if (line != null) {
                                for (int j = 0; j < checkers.length; j++) {
                                    if (!hasDemand[j]) {
                                        if (checkers[j].matches(line)) {
                                            hasDemand[j] = true;
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        if (doWrite) writer.close();
                    }
                } finally {
                    reader.close();
                }
                for (int j = 0; j < hasDemand.length; j++) {
                    Relationships.Role role = checkers[j].getRole().converse;
                    Relationships.Org org = ships.findOrMakeOrg(cluster);
                    Collection has = org.getOrgs(role);
                    if (has.isEmpty()) {
                        if (hasDemand[j]) {
                            logFile.println(cluster
                                            + " is missing "
                                            + role.converse);
                        }
                    } else {
                        if (!hasDemand[j]) {
                            for (Iterator iter = has.iterator(); iter.hasNext(); ) {
                                logFile.println(iter.next()
                                                + " is unneeded "
                                                + role.converse
                                                + " for "
                                                + cluster
                                                );
                            }
                            if (!checkOnly) {
                                ships.remove(cluster, role);
                                logFile.println(cluster + " remove " + role);
                            }
                        }
                    }
                }
                if (doWrite) {
                    if (changed) {
                        bak.delete();
                        file.renameTo(bak);
                        out.renameTo(file);
                    } else {
                        out.delete();
                    }
                }
            } catch (IOException ioe) {
                System.err.println(ioe + ": " + file);
            }
        }
    }

    private static void addOrgs(Set clusters, Relationships ships, String[] names) {
        for (int k = 0; k < names.length; k++) {
            clusters.add(ships.findOrMakeOrg(names[k]));
        }
    }

    private static List readNodeIniFile(String filename)
        throws IOException
    {
        List result = null;
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (result == null) {
                    if (line.equals(CLUSTERS)) {
                        result = new ArrayList();
                    } else {
                        return null; // Not a node .ini file
                    }
                }
                if (line.startsWith(CLUSTER_EQUALS)) {
                    String orgName = line.substring(CLUSTER_EQUALS.length());
                    result.add(orgName);
                }
            }
        } finally {
            reader.close();
        }
        return result;
    }

    private static Collection getClustersFromStrings(Collection c) {
        List result = new ArrayList();
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            String line = (String) i.next();
            if (line.startsWith(CLUSTER_EQUALS)) {
                result.add(line.substring(CLUSTER_EQUALS.length()));
            }
        }
        return result;
    }

    protected static void fix(ClusterOptionSet[] options, String[] args, String[] roles, Fix fix) {
        fix1(options, args, roles, fix);
        if (logFile != System.err) logFile.close();
        if (outFile != System.err) outFile.close();
    }

    protected static void fix1(ClusterOptionSet[] options, String[] args, String[] roles, Fix fix) {
        Relationships ships;
        Set roleSet = new HashSet();
        boolean explicitRolesSpecified = false;
        File dir = new File(".");
        for (int i = 0; i < roles.length; i++) {
            roleSet.add(Relationships.createRole(roles[i]));
            explicitRolesSpecified = true;
        }
        try {
            ships = new Relationships(dir);
        } catch (IOException ioe) {
            System.err.println("Failed to read prototype-ini files");
            ioe.printStackTrace();
            return;
        }
        Set clusters = new TreeSet();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (arg.startsWith("-log")) {
                    try {
                        logFile = new PrintStream(new FileOutputStream(args[++i]));
                    } catch (IOException ioe) {
                        System.err.println(ioe + ": " + args[i]);
                    }
                    continue;
                }
                if (arg.startsWith("-out")) {
                    try {
                        outFile = new PrintStream(new FileOutputStream(args[++i]));
                    } catch (IOException ioe) {
                        System.err.println(ioe + ": " + args[i]);
                    }
                    continue;
                }
                if (arg.equals("-role")) {
                    roleSet.add(Relationships.createRole(args[++i]));
                    explicitRolesSpecified = true;
                    continue;
                }
                if (arg.startsWith("-check")) {
                    checkOnly = true;
                    continue;
                }
                Fixer[] newFixers = Remove.getFixers(arg.substring(1));
                if (newFixers != null) {
                    fix.addFixers(newFixers);
                    continue;
                }
                String[] names = null;
                for (int j = 0; j < options.length; j++) {
                    if (options[j].option.startsWith(arg)) {
                        names = options[j].clusters;
                        break;
                    }
                }
                if (names == null) {
                    System.err.println("Unrecognized option: " + arg);
                    System.exit(1);
                }
                for (int j = 0; j < names.length; j++) {
                    clusters.add(ships.findOrMakeOrg(names[j]));
                }
            } else if (arg.endsWith(RELATIONSHIPS_INI)) {
                // Just ignore these files. They are processed implicitly
            } else if (arg.endsWith(DOT_INI)) {
                try {
                    Collection c = readNodeIniFile(arg);
                    if (c == null) { // Not a node ini file
                        arg = arg.substring(0, arg.length() - DOT_INI.length());
                        clusters.add(ships.findOrMakeOrg(arg));
                    } else {
                        for (Iterator q = c.iterator(); q.hasNext(); ) {
                            String orgName = (String) q.next();
                            clusters.add(ships.findOrMakeOrg(orgName));
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    return;
                }
            } else {
                System.err.println("cluster: " + arg);
                clusters.add(ships.findOrMakeOrg(arg));
            }
        }
        if (checkOnly) logFile.println("checkOnly");

        if (clusters.size() == 0) {
            if (options.length > 0) addOrgs(clusters, ships, options[0].clusters);
        }

        if (!explicitRolesSpecified) {
            roleSet.addAll(ships.getAllRoles());
        }
        boolean done = false;
        while (!done) {
            done = true;
            Collection needed = new HashSet();
            for (Iterator i = clusters.iterator(); i.hasNext(); ) {
                Relationships.Org org = (Relationships.Org) i.next();
                needed.addAll(ships.getClosure(org, Relationships.superior.converse));
                for (Iterator iter = roleSet.iterator(); iter.hasNext(); ) {
                    needed.addAll(ships.getClosure(org, (Relationships.Role) iter.next()));
                }
            }
            needed.removeAll(clusters);
            done = needed.size() == 0;
            if (!done) {
                logFile.println("Additional clusters needed:");
                for (Iterator j = needed.iterator(); j.hasNext(); ) {
                    logFile.println("   " + j.next());
                }
                clusters.addAll(needed);
            }
        }
        String[] clusterNames = new String[clusters.size()];
        int j = 0;
        for (Iterator i = clusters.iterator(); i.hasNext(); ) {
            clusterNames[j++] = i.next().toString();
        }
        fix.setClusters(clusterNames);
        fix.fix(ships);
        if (!checkOnly) {
            try {
                ships.writeFiles(dir, clusters, roleSet);
            } catch (IOException ioe) {
                System.err.println("Failed to write prototype-ini files: " + ioe);
                ioe.printStackTrace();
                return;
            }
        }
    }

    public static void main(String[] args) {
        fix(new ClusterOptionSet[0], args, new String[0], new Fix());
    }
}
