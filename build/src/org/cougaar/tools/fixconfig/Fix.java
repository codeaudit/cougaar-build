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

public abstract class Fix {
    public static class ClusterOptionSet {
        String option;
        String[] clusters;
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
    protected abstract File[] getFiles();

    protected abstract Fixer[] getFixes();

    public void fix() {
        File[] files = getFiles();
        Fixer[] fixers = getFixes();
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

    private static void addOrgs(Set clusters, Relationships ships, String[] names) {
        for (int k = 0; k < names.length; k++) {
            clusters.add(ships.findOrMakeOrg(names[k]));
        }
    }

    protected static void fix(ClusterOptionSet[] options, String[] args, String[] roles) {
        Relationships ships;
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
                boolean matched = false;
                for (int j = 0; j < options.length; j++) {
                    if (options[j].option.startsWith(arg)) {
                        String[] names = options[j].clusters;
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    System.err.println("Unrecognized option: " + arg);
                    System.exit(1);
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
                for (int j = 0; j < roles.length; j++) {
                    needed.addAll(ships.getClosure(org, roles[j]));
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
        new FixFood(clusterNames).fix();
    }
}
