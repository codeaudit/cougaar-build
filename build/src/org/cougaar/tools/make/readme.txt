Using Make

The Make tool is a make replacement that generates various target
files from source files. Make assumes a particular development
directory layout consisting of a project directory under which are a
number of module directories. Each module directory is assumed to have
a src subdirectory under which java source files exist according to
their package names.

All compilations store their class files in the tmpdir/classes
subdirectory of the module directory. All generated code files are
stored in the tmpdir/gencode subdirectory. All jar files are stored in
the lib subdirectory of the project directory.

Targets

All targets are of the form: <module>.<target> The special module
"all" denotes a target for all modules. For example if the modules are
named core, glm, and toolkit, the the all.compile target is equivalent
to core.compile glm.compile toolkit.compile. The list of modules is
constructed automatically by finding all subdirectories of the project
directory that have a src subdirectory.

The current targets are:

projectLib*    -- creates the lib directory of the project
moduleClasses* -- creates the tmpdir/classes directory
moduleGenCode* -- creates the tmpdir/gencode directory
compileDir     -- compiles all the sources in the current directory
compileAll     -- compiles all the sources in and under the current
                  directory
compile        -- compiles all the sources in the module
compileGenCode -- compiles all generated code in the module
clean          -- deletes all generated and compiled files
generateCode   -- generates code from all the .def files of the module
jar            -- jars all the class files into a jar file in the
                  project lib directory

Targets marked with * are used internally and there is no reason for
them to be used by a user.

The targets have built-in dependencies on prerequisite targets. For
example, the compileGenCode has a built-in dependency on the
generateCode target; the compile targets have build-in dependencies on
the generateCode target.

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

basedir              -- the project directory
jikes.class.path     -- the additional classpath need to run jikes
jikes                -- true to use jikes instead of javac
                        (typically $(JDK)/jre/lib/rt.jar)
debug                -- true to turn on debugging printout
3rd.party.jars       -- the location of the third party jar files. Defaults
                        to <project>/jars
omit.module.<module> -- set to true to leave <module> out of the list
                        of all modules

Prequisites

Compilation and code generation in any module may require some other
module to be compiled first. Default prerequisites are created by
Make, but may be overridden with properties of the form:
<module>.prerequisites=<module1> <module2> <module3> ...
