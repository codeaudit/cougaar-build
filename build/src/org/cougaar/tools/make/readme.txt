Using Make

The Make tool is a make replacement that generates various target
files from source files. Make assumes a particular development
directory layout consisting of a project directory under which are a
number of module directories. Each module directory is assumed to have
a src subdirectory under which java source files exist according to
their package names.

All compilations store their class files in the tmpdir/classes
subdirectory of the module directory. All generated code files are
stored in the tmpdir/gencode subdirectory. All jar files are stored or
found in the lib subdirectory of the project directory. All third
party jars are found, by default, in the sys subdirectory of the
project directory.

Targets

All targets are of the form: <module>.<target> The special module
"all" denotes a target for all modules. For example if the modules are
named core, glm, and toolkit, then the all.compile target is
equivalent to core.compile glm.compile toolkit.compile. The list of
modules is constructed automatically by finding all subdirectories of
the project directory that have a src subdirectory.

The current targets are:

projectLib*    -- creates the lib directory of the project
moduleClasses* -- creates the tmpdir/classes directory
moduleGenCode* -- creates the tmpdir/gencode directory
compileDir     -- compiles all the sources in the current directory
compileAll     -- compiles all the sources in and under the current
                  directory
compile        -- compiles all the sources in the module
recompile      -- composite of cleanGenCode, cleanClassFiles, and
                  compile
compileGenCode -- compiles all generated code in the module
clean          -- removes all files in tmpdir
cleanDir       -- removes all the classFiles for the current directory
cleanAll       -- removes all the classFiles in and under the cwd
cleanGenCode   -- deletes all the generated files
cleanClassFiles-- deletes all the class files
generateCode   -- generates code from all the .def files of the module
jar            -- makes the rmic target and then jars all the class
                  files into a jar file in the project lib directory
jax            -- creates executable jars for corresponding to every
                  .ejm (executable jar manifest) file. The executable
                  jar contains all the class files and files having
                  extensions listed by "extensionsToJar" in and below
                  the directory containing the .ejm file. It also
                  includes all the jarable files from all prerequisite
                  modules.
tags           -- makes a TAGS file using etags of all the source and
                  generated code for the module. The tags files (named
                  TAGS) are stored in the tmpdir directory of the
                  module.
projectTags    -- makes all.tags and the project tags file
rmic           -- makes the compile target and then runs rmic to
                  create stub classes for rmi server implementations.
                  The server classes are identified by listing their
                  the names in a file having a .rmic extension. When
                  the rmic target is made, the module source
                  directories aresearched for any files with the .rmic
                  extension. The contents are parsed for the names of
                  the classes. The class names should be listed one
                  per line. Lines beginning with # or // are ignored
                  (comments). Also, any leading or trailing whitespace
                  is removed.
rmicDir        -- makes the compileDir target and the runs rmic only
                  one .rmic files found in the current directory

Targets marked with * are used internally and there is no reason for
them to be used by a user.

The targets have built-in dependencies on prerequisite targets. For
example, the compileGenCode has a built-in dependency on the
generateCode target; the compile targets have build-in dependencies on
the generateCode target.

Prerequisite Modules

Modules can have prerequisite modules (defined by properties below).
By default, all prerequisite modules are compiled (compile target)
before any of the compile targets are made. Furthermore, the classpath
during compilation will include the <module>/src,
<module>/tmpdir/classes, and lib/<module>.jar of the prerequisite
modules. It will _not_ include any of these elements for modules that
are not listed as prerequisites. There is a command line option
(-noprerequisites) that skips the compilation of prerequisite modules.
However, this option does not change the compilation classpath.

Properties

Make has few options. Those that it has are controlled by properties.
Properties may be specified on the command line (using -D...), in a
property file or by command line options (e.g. -debug). Make looks for
property files in the following order:
~/.make.properties
.../make.properties (the first such file found by searching the parent
chain from the current directory)
files specified with the -properties option

The following options may be specified:

org.cougaar.tools.make.basedir
       the project directory
org.cougaar.tools.make.jikes.class.path
       the additional classpath need to run jikes
org.cougaar.tools.make.jikes
       true to use jikes instead of javac (typically
       $(JDK)/jre/lib/rt.jar)
org.cougaar.tools.make.jdk.tools
       The location of the jdk tools jar (typically
       $(JDK)/lib/tools.jar)
org.cougaar.tools.make.debug
       true to turn on debugging printout
org.cougaar.tools.make.3rd.party.jars
       the location of the third party jar files. Defaults to
       <project>/sys
org.cougaar.tools.make.omit.module.<module>
       set to true to leave <module> out of the list of all modules
org.cougaar.tools.make.default.target
       the default target when no targets are given on the command
       line. The default default target is compileDir -- compile all
       the files in the current directory.

Prerequisites

Compilation and code generation in any module may require some other
module to be compiled first. Default prerequisites are created by
Make, but may be overridden with properties of the form:
org.cougaar.tools.make.<module>.prerequisites=<module1> <module2> <module3> ...
