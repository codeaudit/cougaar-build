package org.cougaar.tools.fixconfig;

import java.io.*;
import java.util.StringTokenizer;

public abstract class Fix {
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
}
