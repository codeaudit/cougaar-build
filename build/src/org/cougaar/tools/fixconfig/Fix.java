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
    public static class ClusterOptionSet {
        String option;
        private String[] clusters;
        public ClusterOptionSet(String option, String[] clusters) {
            this.option = option;
            this.clusters = clusters;
        }
    }
    public static interface Fixer {
        String fix(String line);
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

    private File[] files;

    protected Fixer[] getFixes() {
        return new Fixer[0];
    }

    public void setClusters(String[] clusters) {
        System.out.println("[ Clusters ]");
        files = new File[clusters.length];
        for (int i = 0; i < clusters.length; i++) {
            files[i] = new File(clusters[i] + ".ini");
            System.out.println("cluster = " + clusters[i]);
        }
    }

    protected final File[] getFiles() {
        return files;
    }

    public void fix() {
        Fixer[] fixers = getFixes();
        if (fixers.length > 0) {
            File[] files = getFiles();
            for (int i = 0; i < files.length; i++) {
                try {
                    File file = files[i];
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    File out = new File(file.getParentFile(), "#" + file.getName() + "#");
                    File bak = new File(file.getParentFile(), file.getName() + "~");
                    boolean changed = false;
                    try {
                        PrintWriter writer = new PrintWriter(new FileWriter(out));
                        try {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String fixed = null;
                                for (int j = 0; line != null && j < fixers.length; j++) {
                                    fixed = fixers[j].fix(line);
                                    if (!line.equals(fixed)) {
                                        changed = true;
                                        line = fixed;
                                    }
                                }
                                if (line != null) writer.println(line);
                            }
                        } finally {
                            writer.close();
                        }
                    } finally {
                        reader.close();
                    }
                    if (changed) {
                        bak.delete();
                        file.renameTo(bak);
                        out.renameTo(file);
                    } else {
                        out.delete();
                    }
                } catch (IOException ioe) {
                    System.err.println(ioe + ": " + files[i]);
                }
            }
        }
    }

    private static void addOrgs(Set clusters, Relationships ships, String[] names) {
        for (int k = 0; k < names.length; k++) {
            clusters.add(ships.findOrMakeOrg(names[k]));
        }
    }

    protected static void fix(ClusterOptionSet[] options, String[] args, String[] roles, Fix fix) {
        Relationships ships;
        Set roleSet = new HashSet(Arrays.asList(roles));
        try {
            ships = new Relationships(new File("."));
        } catch (IOException ioe) {
            System.err.println("Failed to read prototype-ini files");
            ioe.printStackTrace();
            return;
        }
        Set clusters = new TreeSet();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (arg.equals("-role")) {
                    roleSet.add(args[++i]);
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
            } else {
                clusters.add(ships.findOrMakeOrg(arg));
            }
        }
        if (clusters.size() == 0) {
            addOrgs(clusters, ships, options[0].clusters);
        }
        boolean done = false;
        while (!done) {
            done = true;
            Collection needed = new HashSet();
            for (Iterator i = clusters.iterator(); i.hasNext(); ) {
                Relationships.Org org = (Relationships.Org) i.next();
                needed.addAll(ships.getClosure(org, Relationships.SUPERIOR));
                for (Iterator iter = roleSet.iterator(); iter.hasNext(); ) {
                    needed.addAll(ships.getClosure(org, (String) iter.next()));
                }
            }
            needed.removeAll(clusters);
            done = needed.size() == 0;
            if (!done) {
                System.err.println("Additional clusters needed:");
                for (Iterator j = needed.iterator(); j.hasNext(); ) {
                    System.err.println("   " + j.next());
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
        fix.fix();
    }

    public static void main(String[] args) {
        fix(new ClusterOptionSet[0], args, new String[0], new Fix());
    }
}
