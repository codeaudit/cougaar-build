Readme for the build module.

The build module is for developer use, and not used at Cougaar
runtime.

It consists of 3 major pieces:
1) Code generators, use to read .def files and generate Java for
Assets and Property Groups. This Java code is in src/ and ends up in
CIP/cslib/build.jar
2) Javadoc extensions: These include support for @property, @todo,
@note, and @generated. This Java code is in src/ and ends up in
CIP/cslib/build.jar
3) Build scripts. These Perl and Ant XML files are in build/bin and
build/data. These are the scripts used to build CougaarSE releases,
and by developers to recompile Cougaar modules.

Some highlights:
build/bin includes the Perl "build" script which can parse a data file
in build/data/ -- like default.build. It can then do CVS branches and
Tags, Exports, compilation, post the builds, etc.
It also uses the template Ant XML files in build/data to generate
CIP/build.xml (from productTemplate.xml) and CIP/<module>/build.xml
(from moduleTemplate.xml)

build/bin includes
- build -- the mammoth Perl build script described above
- clines: to count Code lines
- generate_keys.pl -- to generate the keys used to sign the Jars
- unify_sources -- to put all Cougaar source in one tree
- updatecr - to update the copyright notice on source files
- package_map.sh -- to lay out the Cougaar packages
- checkout_modules -- to check out a bunch of source, as specified by
cougaar.mod data file
- autobuild -- used to run the build script from a cron job

build/data includes:
- ant-1.6.dtd -- used with some XML editors to show per-module Ant
build scripts
- default.build -- default data file to drive the "build" script above
to build Cougaar.
- moduleTemplate.xml -- template for Ant per-module build script, used
by the "build" script
- productTemplate.xml -- template for Ant CIP/build.xml script, used
by the "build" script
