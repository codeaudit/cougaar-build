#!/usr/local/bin/perl -w

#-------- Notes -------#
#
#   author: Mark Damish
#     date: Initial construction
#     file: build-alp.pl
#  purpose: alp R7 build script (perl)
#  expects: 
#    usage: ./build-alp <args>
#    notes: build-alp.txt & build-alp.pod
# --------
# revision: Dec 2000. Initial transformation from 6.x builds. 
#


#------- Includes -------#
use strict;
use Getopt::Std;


#------- Globals -------#

#
# Defaults
#
my $BUILD_DATE           = `date +%Y%m%d`; chomp $BUILD_DATE;      # yyyymmdd
my $BUILD_TIME           = `date +%H%M`;   chomp $BUILD_TIME;      # hhmm
my $BUILD_DAYOFWEEK      = `date +%a`;     chomp $BUILD_DAYOFWEEK; # Sun..Sat 

my $BUILD_CVSROOT        = "/cvs/alp/cougaar";   # (-r <string>) CVS repository location.
my $BUILD_CVS_TAG        = "HEAD";               # (-t <string>) CVS tag of sources.
my $BUILD_BUILD_CVS_TAG  = "HEAD";               # (-T <string>) CVS tag of build scripts.

my $BUILD_BOOTCLASSPATH  = "";                   # (-B <path1:path2:path3>) Boot class path. for overridden stuff and rt.jar

my $BUILD_PREFIX         = "core";               # (-p <string>) Prefix in build name. 
my $BUILD_VERSION        = "HEAD";               # (-v <string>) Version of build.


my $BUILD_LOCATION       = "/build/dist/builds"; # (-b <path>) Where to create build directory. 
my $BUILD_DIST_LOCATION  = "/build/dist/zips";   # (-d <path>) Where to place finished build. zip file.

my $BUILD_WITH           = "";                   # (-w <string>) Used to point a plugin
                                                 # to a specific build to buld against.
my $BUILD_WITH_PATH      = "";                   # (-W <path:path>) physical path to jar file directories.



# Constructed here from defaults, later with command line options.
#
# The name of the build is constructed from 3 parts:
#  - A prefix:  ie: 'alp', 'fgi'...      (Default is 'alp')
#  - A version: ie: 'nb', 'Release_3'... (Default is the cvs tag name)  
#  - A date in yyyymmdd format.  This currently cannot be overridded.
#
# The build name is used to name the build directory, the zip file,
# and the log file.
#
# Set when options are parsed.
#
my $BUILD_NAME  = "$BUILD_PREFIX-$BUILD_VERSION-$BUILD_DATE";
my $BUILD_DIR   = "$BUILD_LOCATION/$BUILD_NAME";
my $BUILD_LOG   = "$BUILD_LOCATION/$BUILD_NAME.log";

#
# Behaviour set by command line args.
#
my $BUILD_INTERACTIVE = "FALSE";  # (-i)
my $BUILD_SKIP_CVS    = "FALSE";  # (-s)
my $BUILD_SKIP_BUILD  = "FALSE";  # (-S)
my $BUILD_VERBOSE     = "FALSE";  # (-V)

# END common globals defaults.
#----


#--
# Global locations for this build.
my $DIST;
my $SRC;
my $CLASS;

# Distribution subdirs
my $LIB;
my $PLUGINS;
my $DOC;
my $JAVADOC;

$ENV{'TERM'} = "vt100";



#------- Main -------#

# Parse command line, modify defaults.
parse_options();

# Send STDOUT/STDERR to the logfile..
log_output($BUILD_LOG)         if ($BUILD_INTERACTIVE eq "FALSE");

# Print what we are using for the build.
print_build_parameters();

# Set the global locations.
set_globals();

# Set path used to search for binaries.
set_bin_path();

# Create directory skeleton used in build.
create_directory_skeleton()    if ($BUILD_SKIP_CVS eq "FALSE");

# Check out the recursive build (AlpMake)  HARD-CODED location for build stuff.
check_out_sources($SRC, "/cvs/alp/cougaar", $BUILD_BUILD_CVS_TAG, "build")        if ($BUILD_SKIP_CVS eq "FALSE");

# Check out the sources for this build, defined by BUILD_PREFIX
check_out_sources($SRC, $BUILD_CVSROOT,    $BUILD_CVS_TAG, $BUILD_PREFIX)  if ($BUILD_SKIP_CVS eq "FALSE");

# Copy 'stuff' to distribution from CVS.
copy_from_cvs_to_distribution();

# Copy 'stuff' from eigers drive to distribution, based on BUILD_PREFIX.
copy_disk_overlays_to_distribution(); 

# Expand -w and -W command line options. Set up the class path.
set_classpath();

# Build the build the 'Writers' (Use the freshly checked out version, rather than build.jar)
##6x##recursive_build("$SRC/build/src",  "$SRC/build/src")                if ($BUILD_SKIP_BUILD eq "FALSE");

##
## Hack to try and build alpio.jar and add it to the classpath.
##
#if ($BUILD_PREFIX eq "core") {
#  recursive_build("$SRC/core/src/java",  "$CLASS/$BUILD_PREFIX"); 
#   `mkdir $CLASS/alpio`; 
#   `cp -r $CLASS/core/java $CLASS/alpio`;
#   create_jar($LIB, "alpio.jar", "$CLASS/alpio");
#   $ENV{'CLASSPATH'} = "$LIB/alpio.jar" . ":" . $ENV{'CLASSPATH'};
#} # if


# Build the module defined by BUILD_PREFIX
recursive_build("$SRC/$BUILD_PREFIX/src",  "$CLASS/$BUILD_PREFIX")  if ($BUILD_SKIP_BUILD eq "FALSE");

# Create the jar for the modules, and sign it.
create_jar($LIB, "$BUILD_PREFIX.jar", "$CLASS/$BUILD_PREFIX")      if ($BUILD_SKIP_BUILD eq "FALSE");
update_jar($LIB, "$BUILD_PREFIX.jar", "$SRC/$BUILD_PREFIX/src")    if ($BUILD_SKIP_BUILD eq "FALSE");
sign_jar("$LIB/$BUILD_PREFIX.jar", "/usr/local/etc/.keystore")     if ($BUILD_SKIP_BUILD eq "FALSE");


###
### a tops specific request 20010221
### Create a jar file, from specific files, specified by Todd Wright or Ben Lubin.
###
if ($BUILD_PREFIX eq "tops") {
   print("\n=> Creating tops specific  hierarchyPSP.jar...  \n") if ($BUILD_VERBOSE eq "TRUE");
   my $src_path = "$CLASS/$BUILD_PREFIX";
   my $dest_path = $LIB;
   my $name = "hierarchyPSP.jar";
   my $hierarchyPSP_files = 
     'org/cougaar/domain/mlm/ui/psp/transit/data/Failure.class \
      org/cougaar/domain/mlm/ui/psp/transit/data/hierarchy/HierarchyData.class \
      org/cougaar/domain/mlm/ui/psp/transit/data/hierarchy/Organization\$OrgRelation.class \
      org/cougaar/domain/mlm/ui/psp/transit/data/hierarchy/Organization.class \
      org/cougaar/domain/mlm/ui/psp/transit/data/xml/DeXMLable.class \
      org/cougaar/domain/mlm/ui/psp/transit/data/xml/UnexpectedXMLException.class \
      org/cougaar/domain/mlm/ui/psp/transit/data/xml/XMLable.class \
      org/cougaar/domain/mlm/ui/psp/transit/data/xml/XMLWriter.class \
      org/cougaar/domain/mlm/ui/psp/transit/PSP_Hierarchy\$1.class \
      org/cougaar/domain/mlm/ui/psp/transit/PSP_Hierarchy\$MyPSPState.class \
      org/cougaar/domain/mlm/ui/psp/transit/PSP_Hierarchy\$XMLtoHTMLOutputStream.class \
      org/cougaar/domain/mlm/ui/psp/transit/PSP_Hierarchy.class';
   my $cmd;
   chdir $src_path;
   $cmd = "jar -cf ${dest_path}/${name} $hierarchyPSP_files ";
   print ("\n=> $cmd \n");
   system($cmd); 
   #
   sign_jar("$LIB/hierarchyPSP.jar", "/usr/local/etc/.keystore");    
} # if




# Jar/Sign the 'Writers' too.
##6x##create_jar($LIB, "build.jar", "$SRC/build/src")            if ($BUILD_PREFIX eq "core");
##6x##sign_jar("$LIB/build.jar", "/usr/local/etc/.keystore")     if ($BUILD_PREFIX eq "core");

# Jar/Sign the java.io tree for Noelle
##6x##if ($BUILD_PREFIX eq "core") {
##6x##   `mkdir $CLASS/alpio`; 
##6x##   `cp -r $CLASS/core/java $CLASS/alpio`;
##6x##   create_jar($LIB, "alpio.jar", "$CLASS/alpio");
##6x##   sign_jar("$LIB/alpio.jar", "/usr/local/etc/.keystore");
##6x##} # if

# copy keys to distribution.

create_javadoc()     if ($BUILD_SKIP_BUILD eq "FALSE");
zip_sources()        if ( (building_plugin() eq 'FALSE') || ($BUILD_PREFIX eq "vishnu") );
zip_distribution();
check_for_errors()   if ($BUILD_INTERACTIVE eq "FALSE"); 
move_log_files()     if ($BUILD_INTERACTIVE eq "FALSE");
print("\n=> NNNN \n");
exit 0;




#------- Subroutines -------#



##
## Parse the command line switches and set the 'BUILD_'
## global variables.  The 'BUILD_' variables are those
## that have defaults, and may be modified by
## various command line flags, or are used to construct
## other 'BUILD_' variables.
##
## Args:    none
## Returns: nothing
##
sub parse_options() {

    print("\n=> parse_options() ... \n") if ($BUILD_VERBOSE eq "TRUE");

    my %option = ();
    
    getopts("r:t:T:p:b:d:v:w:W:B:hisSV", \%option);

    print_help()                          if $option{'h'};
    exit 0                                if $option{'h'};

    $BUILD_INTERACTIVE    = "TRUE"        if $option{'i'};
    $BUILD_SKIP_CVS       = "TRUE"        if $option{'s'};
    $BUILD_SKIP_BUILD     = "TRUE"        if $option{'S'};
    $BUILD_VERBOSE        = "TRUE"        if $option{'V'};
    
    $BUILD_CVSROOT        = $option{'r'}  if $option{'r'};
    $BUILD_CVS_TAG        = $option{'t'}  if $option{'t'};
    $BUILD_BUILD_CVS_TAG  = $option{'t'}  if $option{'t'};  # pick up build scripts from the source tag.
    $BUILD_BUILD_CVS_TAG  = $option{'T'}  if $option{'T'};  # pick up build scripts elsewhere.
    
    $BUILD_PREFIX         = $option{'p'}  if $option{'p'};
    $BUILD_VERSION        = $option{'v'}  if $option{'v'};
    $BUILD_LOCATION       = $option{'b'}  if $option{'b'};
    $BUILD_BOOTCLASSPATH  = $option{'B'}  if $option{'B'};
    $BUILD_WITH           = $option{'w'}  if $option{'w'};
    $BUILD_WITH_PATH      = $option{'W'}  if $option{'W'};
    
    $BUILD_DIST_LOCATION  = $option{'d'}  if $option{'d'};
    
    $BUILD_NAME = "$BUILD_PREFIX-$BUILD_VERSION-$BUILD_DATE";
    $BUILD_DIR  = "$BUILD_LOCATION/$BUILD_NAME";
    $BUILD_LOG  = "$BUILD_LOCATION/$BUILD_NAME.log";

    # foreach (keys(%option)) {
    #   print "Key: $_  Value: $option{$_} \n" if (exists $option{$_});
    # } # foreach

} # sub parse_options


##
## Display a help message when is a command line arg.
## Will be displayed with -h is a command line arg.
##
## Args:    none.
## Returns: nothing.
##
sub print_help() {

    print("\n=> print_help() ... \n") if ($BUILD_VERBOSE eq "TRUE");

print <<EOT;

Usage: $0 <options> 

   -p  <Build name prefix>
   -v  <Build name version>
   -b  <Build location>
   -d  <Distribution destination directory>
   -r  <CVS root>
   -t  <CVS tag of sources to build.>
   -T  <CVS tag of build scripts.>
   -w  <build name>   Build with jars in this build.
   -W  <path1:path2>  Build with jars in this path.
   -B  <path1:path2>  Boot Class Path. (passed as -bootclasspath to javac)
   -i  Interactive. Don\'t create log or send mail.
   -s  Skip CVS checkout, and dir creation 
   -S  Skip the recursive build process
   -V  Add some verbocity to the messages. 
   -h  Help. (This blurb)

   The build name is constucted to form:
     prefix-version-date
   The default prefix is:                    $BUILD_PREFIX
   The default version is:                   $BUILD_VERSION
   The default date is today (unchangable):  $BUILD_DATE
   The default build name is:                $BUILD_NAME
   The default cvs repository is:            $BUILD_CVSROOT
   The default build location is:            $BUILD_LOCATION
   The default cvs tag for sources is:       $BUILD_CVS_TAG
   The default cvs tag for build scripts is: $BUILD_BUILD_CVS_TAG
   The default distribution location is:     $BUILD_DIST_LOCATION
   The default Boot Class Path is:           $BUILD_BOOTCLASSPATH

EOT

} # print_help();



##
## Display the parameters used for this build after
## setting the defaults, and parsing the command
## line switches.
##
## Args:    none.
## Returns: nothing.
##
sub print_build_parameters()
{

    print("\n=> print_build_parameters() ... \n") if ($BUILD_VERBOSE eq "TRUE");

    my $JAVA_WHICH    =  `which java`;
    #my $JAVA_VERSION  =  `java -version`;
    chomp $JAVA_WHICH;
    #chomp $JAVA_VERSION;

print <<EOT;

=> Using the following parameters for the build: 
=> ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
=> Build Date:                    $BUILD_DATE
=> Build Time:                    $BUILD_TIME
=> Build day-of-week:             $BUILD_DAYOFWEEK
=> Build name prefix:             $BUILD_PREFIX
=> Build version:                 $BUILD_VERSION
=> Build name:                    $BUILD_NAME
=> Build Location:                $BUILD_LOCATION
=> Build Dir:                     $BUILD_DIR
=> Plugin build with:             $BUILD_WITH
=> Build with path:               $BUILD_WITH_PATH
=> Boot Class Path:               $BUILD_BOOTCLASSPATH
=> Build Distribution location:   $BUILD_DIST_LOCATION
=> Build CVS repository:          $BUILD_CVSROOT
=> Build CVS tag (sources):       $BUILD_CVS_TAG
=> Build CVS tag (Build scripts): $BUILD_BUILD_CVS_TAG
=> Skip CVS checkout:             $BUILD_SKIP_CVS
=> Skip Build:                    $BUILD_SKIP_BUILD
=> Skip logs and mail:            $BUILD_INTERACTIVE
=> Verbose:                       $BUILD_VERBOSE
=> Which java:                    $JAVA_WHICH
=> ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

EOT

    ## Check expand the -w flag. See if dir(s) exist.
    #if ($BUILD_WITH ne "") {
    #    print("\n=> BUILD_WITH (-w) set.  \n");
    #    print("\n=> ...$BUILD_LOCATION/core-$BUILD_WITH    DOESN'T EXIST. \n") if !(-d "$BUILD_LOCATION/core-$BUILD_WITH");
    #} # if


} # sub print_build_parameters()


##
## Tee stdout and stderr to log file specified by
## the BUILD_LOG variable.  Code snipped from the
## amp build-world script.
##
## TTD: Validate that there is an argument present,
##      and verify the path is usable.
##
## args:    path/filename string.
## returns: nothing.
##
sub log_output()
{
    print("\n=> log_output() ... \n") if ($BUILD_VERBOSE eq "TRUE");

    my ($logfile) = @_;

    print "Tee STDOUT and STDERR to $logfile \n";
    open(NEWOUT, "|tee $logfile"); select(NEWOUT); $| = 1;
    close(STDOUT);
    close(STDERR);
    open(STDOUT, ">&NEWOUT"); select(STDOUT); $| = 1;
    open(STDERR, ">&NEWOUT"); select(STDERR); $| = 1;       
} # sub log_output()



##
## Set the global locations used with the build.
## PATH and CLASSPATH set elsewhere.
##
## args:    none
## returns: nothing
##
sub set_globals()
{
    print("\n=> set_globals() ... \n") if ($BUILD_VERBOSE eq "TRUE");

    $DIST         = "$BUILD_DIR/$BUILD_PREFIX";       # The distribution tree location, to be zipped, and distributed.
    $SRC          = "$BUILD_DIR/src";                 # Source tree location.
    $CLASS        = "$BUILD_DIR/classes";             # Build classes outside of source tree.
    
    $LIB          = "$DIST/lib";                      # lib
    $PLUGINS      = "$DIST/plugins";                  # plugins
    $DOC          = "$DIST/doc";                      # documentation
    $JAVADOC      = "$DOC/javadoc";                   # javadoc

} # sub init_globals()



##
## Set the binary path... ...Don't want to
## rely on environment, which might change.
## especially when running from cron.
##
## args:     none.
## returns:  nothing
##
sub set_bin_path()
{
    print("\n=> set_bin_path() ...  \n") if ($BUILD_VERBOSE eq "TRUE");

    $ENV{'PATH'} = "";
    $ENV{'PATH'} = $ENV{'PATH'}       . "/opt/jdk1.2/bin"; 
    $ENV{'PATH'} = $ENV{'PATH'} . ':' . "/usr/local/gnu/bin"; 
    $ENV{'PATH'} = $ENV{'PATH'} . ':' . "/usr/local/bin"; 
    $ENV{'PATH'} = $ENV{'PATH'} . ':' . "/bin"; 
    $ENV{'PATH'} = $ENV{'PATH'} . ':' . "/usr/ucb"; 
#    $ENV{'PATH'} = $ENV{'PATH'} . ':' . "/usr/ccs/bin"; 
#    $ENV{'PATH'} = $ENV{'PATH'} . ':' . "/usr/ccs/lib"; 

    print("\n=> PATH set to: $ENV{'PATH'} \n");

} # sub set_bin_path()


##
## Determine if we are building a plugin
##
## Returns 'true' if building a plugin, or 'false' for core related stuff.
##
sub building_plugin()
{
    if ( 
         ($BUILD_PREFIX eq 'build')         || 
         ($BUILD_PREFIX eq 'core')          || 
         ($BUILD_PREFIX eq 'javaiopatch')   || 
         ($BUILD_PREFIX eq 'toolkit')       || 
         ($BUILD_PREFIX eq 'planserver')    || 
         ($BUILD_PREFIX eq 'glm')           || 
         ($BUILD_PREFIX eq 'contract')      || 
         ($BUILD_PREFIX eq 'scalability')   || 
         ($BUILD_PREFIX eq 'server')        || 
         ($BUILD_PREFIX eq 'tutorial')      || 
         ($BUILD_PREFIX eq 'configgen') 
       ) {
	return 'FALSE';
    } # if
    return 'TRUE';

} # sub building_plugin


##
## Create the main build and distribution directories.
##
##  alp-HEAD-20000227  $BUILD_DIR
##   + src             $SRC
##   + classes         $CLASS
##   + <name>          $DIST
##      + lib          $LIB
##      + plugins      $PLUGINS
##      + doc          $DOC
##      |  + javadoc   $JAVADOC
##      + src
##
## args:     none.
## returns:  nothing
##
sub create_directory_skeleton()
{
    print("\n=> create_directory_skeleton() ... \n") if ($BUILD_VERBOSE eq "TRUE");

    # Top level dir for build.
    mkdir ($BUILD_DIR,0755)         || warn "ERROR!! Cannot create $BUILD_DIR  $!";

    # Source, Distribution, and Class directories.
    mkdir ($SRC,   0755)            || warn "ERROR!! Cannot create $SRC $!";
    mkdir ($DIST,  0755)            || warn "ERROR!! Cannot create $DIST $!";
    mkdir ($CLASS, 0755)            || warn "ERROR!! Cannot create $CLASS $!";

    # Distribution subdirs .
    mkdir ($DOC,        0755)       || warn "ERROR!! Cannot create $DOC $!";
    mkdir ($JAVADOC,    0755)       || warn "ERROR!! Cannot create $JAVADOC $!";
    mkdir ($LIB,        0755)       || warn "ERROR!! Cannot create $LIB $!";
    mkdir ($PLUGINS,    0755)       || warn "ERROR!! Cannot create $PLUGINS $!";
    mkdir ("$DIST/src", 0755)       || warn "ERROR!! Cannot create $DIST/src $!";
    
    if (building_plugin() eq 'TRUE') {
        mkdir ("$DIST/$BUILD_PREFIX", 0755) || warn "ERROR!! Cannot create $DIST/$BUILD_PREFIX $!";
    } # if

} # sub create_directory_skeleton()



##
## Checkout modules to a specified dir, using
## supplied cvs tag and repository (CVSROOT)
## location.
##
## Args:
##       - path to working dir.
##       - CVSROOT string.
##       - CVS tag string.
##       - list of modules.
##
## Returns: nothing.
##
## TTD: Make sure arguments are present, and reasonable.
##
sub check_out_sources()
{
    print("\n=> check_out_sources() ... \n") if ($BUILD_VERBOSE eq "TRUE");

    my ($working_dir, $cvsroot, $tag, @modules) = @_;

    chdir ($working_dir) || die "ERROR!! Cannot change to $working_dir. $!";
 
    my $cmd = "cvs -d $cvsroot -q export -r $tag @modules";
    print "\n=> $cmd \n"; 
    # `$cmd`;
    system ($cmd);
 
} # sub check_out_sources();




##
## Copy directories from cvs source tree to distribution.
##
## args:     none.
## returns:  nothing.
##
##
## TTD: - change methodology of copy from 'copy dirs existing in list'
##        to 'copy every dir not in list'.  (??)
##
##      - Plugin code gets copied to a differant level than the core stuff
##
sub copy_from_cvs_to_distribution()
{
    print("\n=> copy_from_cvs_to_distribution() ... \n") if ($BUILD_VERBOSE eq "TRUE");
  
    my $cmd; 

    ##
    #  Change to exclude directories.
    ##
    my @files = `ls $SRC/$BUILD_PREFIX`; 
    chomp @files;
    foreach (@files) {
    #foreach ('bin', 'code', 'config', 'configs', 'data', 'database', 'doc', 'lib', 'portsim', 'scripts') {
	#if ( -d "$SRC/$BUILD_PREFIX/$_") {
	if ( (-d "$SRC/$BUILD_PREFIX/$_") && ($_ ne 'src') ) {
	    print("\n=>    ... Found $SRC/$BUILD_PREFIX/$_ in CVS sources. Copying to distribution...\n");
	    if (building_plugin() eq 'TRUE') {
		$cmd = "     cp -r $SRC/$BUILD_PREFIX/$_  $DIST/$BUILD_PREFIX";
	    } else {
		$cmd = "     cp -r $SRC/$BUILD_PREFIX/$_  $DIST";
	    } # else
	    print("\n=> $cmd \n");
	    system ("$cmd");
	} # if
	else {
	    print("\n=>    ... Didn't see $SRC/$_ in CVS sources. Nothing copied.\n");
	} # else

    } # foreach
    
    #    print "\n=> Copying ALL docs directory (from cvs) to the distribution image  \n";
    #    `cp -R ${SRC}/doc/*  ${DOC} || echo "ERROR! copying docs (from cvs)"`;  

} # copy_from_cvs_to_distribution()



##
## Alp or plugin specific.
##
## args:     none.
## returns:  nothing.
##
sub copy_disk_overlays_to_distribution()
{
    print("\n=> copy_disk_overlays_to_distribution() ... \n") if ($BUILD_VERBOSE eq "TRUE");

    my $cmd;
    my $overlay_root = "/build/dist/alp-overlays-MB7/";
    my $overlay = $overlay_root . "$BUILD_PREFIX";


    if (-d $overlay) {
	print ("\n=>    ... $overlay exists on disk.  Copying to distribution...  \n");
        $cmd = "     cp -r $overlay/*  $DIST";
        print("\n=>    $cmd \n");
        system ("$cmd");
    } # if
    else {
	print ("\n=>    ... $overlay doesn't exist on disk. Nothing to copy...  \n");
    } # else

} # sub copy_disk_overlays_to_distribution()




##
## - Add to the BUILD_WITH_PATH variable.  This var contains
##   absolute paths to directories containing jar files.
##   All of the jar files in this path are added to the
##   CLASSPATH environment variable.
##   The source and destination directories are added
##   with the AlpMake script, and are not added here.
##
## TTD the `ls -1 path` is wrong.  Substitute a method that
##     will determine if a directory contains jar files (and exists)
##
## args:     none.
## returns:  nothing.
##
sub set_classpath()
{
    print("\n=> set_classpath() ...  \n") if ($BUILD_VERBOSE eq "TRUE");

    # path for jars that are not distributed.  Should be a global?
    my $JARS_NODISTRIB="/build/dist/jars_nodistribution-12";

 

    print("\n=> Setting BUILD_WITH_PATH...  \n");

    # 3.5) Add path of plugin's lib directory
    if ( building_plugin() eq 'TRUE') {
        print("\n=>    3.5) Adding path to plugins lib dir, if it exitst BUILD_WITH_PATH \n");
	if ( -d "$DIST/$BUILD_PREFIX/lib") {
	    $BUILD_WITH_PATH .= ":" if ( $BUILD_WITH_PATH ne "" );
	    $BUILD_WITH_PATH .= "$DIST/$BUILD_PREFIX/lib";
	} # if
    } # if


#    # 3.7) Hardcoded carp that can't be handled otherwise.
#    if ( $BUILD_PREFIX eq 'tops' ) {
#        print("\n=>    3.7) Hardcoded: Adding $BUILD_LOCATION/vishnu-$BUILD_WITH/vishnu/lib to tops build.\n");
#	$BUILD_WITH_PATH .= ":" if ( $BUILD_WITH_PATH ne "" );
#	$BUILD_WITH_PATH .= "$BUILD_LOCATION/vishnu-$BUILD_WITH/vishnu/lib";
#    } # if


    # 1) -w flag
    # - Expand 'BUILD_WITH' to a 'core' and 'alpine' build locations.
    # - If they exist, add them to BUILD_WITH_PATH and let the next
    #   section add jar files to the classpath.
    # - The BUILD_WITH is specified as <version>-<date>, which will be
    #   expanded to core-version-date and alpine-version-date, and
    #   furthore expanded to a full path.
    # - The full path is added to BUILD_WITH_PATH
    #
    # TTD: allow -w to specify both <version>-<date> and just <version>
    #      where the default date woule be today. $BUILD_DATE
    #      ls -1 somepath      returns "" when dir is empty.
    #      ls -1 somepath/*    returns "ls: No match." when dir is empty.
    #
    if ($BUILD_WITH ne "") {
	print("\n=>    1) (-w) BUILD_WITH set.  Adding $BUILD_LOCATION/$BUILD_WITH_PATH (core, alpine & utility) $BUILD_WITH to BUILD_WITH_PATH \n");
        
	my @modules = (
                        'build', 
                        'core', 
			'javaiopatch',
                        'toolkit', 
                        'planserver', 
                        'glm', 
                        'contract', 
                        'scalability', 
                        'server', 
                        'tutorial', 
                        'configgen' 
                      );
	#
	my $module;
	foreach $module (@modules) {
	    if ( (-d "$BUILD_LOCATION/${module}-$BUILD_WITH/${module}/lib") && (`ls -1 $BUILD_LOCATION/${module}-$BUILD_WITH/${module}/lib` ne "") ) {
		print("\n=>        adding $BUILD_LOCATION/${module}-$BUILD_WITH/${module}/lib to BUILD_WITH_PATH \n");
		$BUILD_WITH_PATH .= ":" if ( $BUILD_WITH_PATH ne "" );
		$BUILD_WITH_PATH .= "$BUILD_LOCATION/${module}-$BUILD_WITH/${module}/lib";
	    } # if 
	    else {
		print("\n=>        $BUILD_LOCATION/${module}-$BUILD_WITH/${module}/lib doesn't exist, or empty... ...Nothing added. \n");
	    } # else
	} # foreach


    } # if
    else {
	print("\n=>    1) BUILD_WITH (-w) not set. No jars to add.  \n");
    } # else



    # 2) Add path of supplied library to BUILD_WITH_PATH
    print("\n=>    2) Adding LIB $LIB to BUILD_WITH_PATH \n");
    if ( `ls -1 $LIB` ne "" ) {
	print("\n=>        adding $LIB to BUILD_WITH_PATH \n");
	$BUILD_WITH_PATH .= ":" if ( $BUILD_WITH_PATH ne "" );
	$BUILD_WITH_PATH .= "$LIB";
    } # if


    # 3) Add path of non distributed jar files (JARS_NODISTRIB) to BUILD_WITH_PATH
    print("\n=>    3) Adding Non distributed jar directory to BUILD_WITH_PATH \n");
    if ( `ls -1 $JARS_NODISTRIB` ne "" ) {
	print("\n=>        adding $JARS_NODISTRIB to BUILD_WITH_PATH \n");
	$BUILD_WITH_PATH .= ":" if ( $BUILD_WITH_PATH ne "" );
	$BUILD_WITH_PATH .= "$JARS_NODISTRIB";
    } # if


    ## Set the class path using jar files found in dirs in BUILD_WITH_PATH
    print("\n=> Setting up CLASSPATH from BUILD_WTIH_PATH...  \n");

    # 4) Make sure CLASSPATH environment var exists, and is null.
    print("\n=>    4) Resetting CLASSPATH to \"\"  \n");
    $ENV{'CLASSPATH'} = "";


    ## 5) Add the classes that will be created by this build to the CLASSPATH
    #print("\n=>    5) Adding self to CLASSPATH  \n");
    #print("\n=>       ...adding: $CLASS/$BUILD_PREFIX  \n");
    #$ENV{'CLASSPATH'} = $ENV{'CLASSPATH'} . ':' . "$CLASS/$BUILD_PREFIX";
    


    # 6) Add all of the jar files found in the BUILD_WITH_PATH
    #    to the classpath.
    if ($BUILD_WITH_PATH ne "") {
        print("\n=>    6) (-W) Adding .jar files to classpath pointed to by $BUILD_WITH_PATH \n");
        my @paths = split(/:/,$BUILD_WITH_PATH);     # Get the individual paths.
        foreach(@paths) {                            # For every path...
            my @lib_files = `ls -1 $_/*jar`;            # Get a list of jar files in the path.
	    print("\n=>       Adding .jar files to classpath from $_ \n");
            chomp(@lib_files);
            foreach(@lib_files) {                    # For each jar file.
		print("\n=>       ...adding: $_ \n");
		##$ENV{'CLASSPATH'} .= ':' if ($ENV{'CLASSPATH'} ne "");
		$ENV{'CLASSPATH'} .= ':' . $_;  # Add it to the CLASSPATH.
	    } # foreach;
	} # foreach
    } # if
    else {
       print("\n=>    6) BUILD_WITH_PATH not set. No jars to add.  \n");
    } # else


    # 7) Add the build dir containing the 'Writers'
    #
    print("=>    7) Adding build dir for Writers...  \n");
    $ENV{'CLASSPATH'} = "$SRC/build/src" . ":" . $ENV{'CLASSPATH'};
    print("\n=>       ...adding: $SRC/build/src \n");
    


    print "\n=> CLASSPATH set to:  $ENV{'CLASSPATH'}  \n";
    print "\n=> CLASSPATH src and dst directories are picked up elsewhere.  \n";

} # set_classpath()






##
## - Create destination dir for classes
##   in $CLASS.
##
## - Change to the build tools dir, and
##   call Alpmake with the appropriate args.
##
## Args:
##    - source path
##    - destination path
##
## Returns: nothing.
##
## Note: Set $ENV{'CLASSPATH'} before calling
##
## TTD: Test to see if args are present, and reasonable.
##
sub recursive_build()
{
    print("\n=> recursive_build() ...  \n") if ($BUILD_VERBOSE eq "TRUE");

    my ($src_path, $class_dest_path) = @_; 
    my $cmd ="";
 
    print "\n=> Source Path     = $src_path \n";
    print "\n=> Class Dest Path = $class_dest_path \n";

    if ( !(-d "$class_dest_path") ) {
	mkdir ("$class_dest_path", 0755) || warn "ERROR!! Cannot create $class_dest_path  $!";
    } # if
    print "\n" . $ENV{PATH} . "\n";

        
    chdir("$SRC/build/AlpMake") || die "ERROR!! Cannot chdir to $SRC/build/AlpMake.  $!";

    $cmd = "./AlpMake-wrapper.sh $src_path $class_dest_path ";
    if ($BUILD_BOOTCLASSPATH ne "") {
       $cmd .= " -bootclasspath $BUILD_BOOTCLASSPATH ";
    } # if
    print "\n\n =>" . `pwd`; 
    print "\n\n =>  $cmd \n\n";
    system ($cmd);

} # recursive_build()




##
## Create a jar file, given a path to toplevel class tree.
## Adds .class and .gif files, using the found with the
## 'find' command.
##
## Args:
##       - dest_path: Path to where newly created jar file
##                    will be stored.
##       - name:      Name of the jar file.
##       - src_path:  Path to directory containg top level class files.
##
## returns: nothing.
##
sub create_jar()
{
    print("\n=> create_jar() ...  \n") if ($BUILD_VERBOSE eq "TRUE");

    my ($dest_path, $name, $src_path) = @_;
    my $cmd;
    my $manifest_file = "$SRC/$BUILD_PREFIX/src/manifest.mf";

    chdir $src_path;

    if ( -e $manifest_file ) {
       print("\n=> Manifest file: $manifest_file DOES exist.");
       $cmd = "jar -cfm ${dest_path}/${name}  $manifest_file  `find ./ -name \"*class\" -print`  `find ./ -name \"*gif\" -print` ";
    } else {
       print("\n=> Manifest file: $manifest_file DOES NOT exist.");
       $cmd = "jar -cf ${dest_path}/${name} `find ./ -name \"*class\" -print`  `find ./ -name \"*gif\" -print` ";
    }
    print ("\n=> $cmd \n");
    system($cmd); 

} # create_jar


# Quick kludge for Noelle Givler 15 May 2000
#
sub update_jar()
{
    print("\n=> update_jar() ...  \n") if ($BUILD_VERBOSE eq "TRUE");

    my ($dest_path, $name, $src_path) = @_;
    my $cmd;
    my @files;

    chdir $src_path;

    @files = `find ./ -name \"*def\" -print -o -name \"*props\" -print`;
    chomp(@files);

    foreach(@files) {
       $cmd = "jar -uf ${dest_path}/${name} $_";
       print ("\n=> $cmd \n");
       system($cmd); 
    } # foreach

} # update_jar




##
## Args:
##       - FQ path/name of jar file.
##       - FQ path/name of keystore.
##
## Returns: nothing.     
##
sub sign_jar()
{
    print("\n=> sign_jar() ... \n") if ($BUILD_VERBOSE eq "TRUE");

    my ($jarfile, $keystore) = @_;
    my $cmd;
    my (@tmp, $path, $name);

    # Break the fully qualified path/name into a
    # a seperate path and name.    
    # This fragment assumes an absolute path!
    @tmp = split(/\//, $jarfile);   # Split the path/name into components.
    $name = pop(@tmp);              # Grab the name.
    $path = join ('/', @tmp);       # Reconstuct the path.
    #$path = $path . "/";            # Add trailing '/' to path
    # Why does join add a _leading_ '/'? (Because the first split contains a null value on the left)
    #.
    #print ("path = $path\n");
    #print ("name = $name\n");
    #print ("tmp  = @tmp \n");

    # sign the jar file.  The signed jar file is prefixed with 'signed-'
    print ("\n=> Signing jar file. \n");
    $cmd = "jarsigner -signedjar ${path}/signed-${name} -keypass alpalp -storepass alpalp -keystore $keystore ${path}/${name} alpine";
    print ("\n=> $cmd \n");
    system ($cmd);
    
    # move the 'signed-jarfile.jar' to 'jarfile.jar'.
    $cmd = "mv ${path}/signed-${name} ${path}/${name}";
    print ("\n=> $cmd \n");
    system ($cmd);

} # sign_jar()



##
## 1) Generate list of packages.
## 2) Create javadoc tree.
## 3) zip and remove tree.
##
## Args:    NONE right now.
## Returns: nothing
##
##
sub create_javadoc()
{
    print("\n=> create_javadoc() ... \n") if ($BUILD_VERBOSE eq "TRUE");

    #my ($srctree, $destpath, $zip_name) = @_;
    my $cmd;
    my @package_list;

    chdir("$SRC/$BUILD_PREFIX/src");         # Change to the source directory.
    mkdir("$JAVADOC/$BUILD_PREFIX", 0755);   # Create a destination dir.


    ## - Generate a list of packages in the cwd.
    @package_list = `find ./ -type d -print`;          # generate a list of directories.
    chomp(@package_list);                              # loose the EOL.
    print("\n=> package_list: @package_list \n\n");   
    foreach(@package_list) {                           # Iterate over the items.
        my $num_java_files = `ls -1 $_/*java | wc -l`; # See if directory contain .java files.
        $_ = "" if ($num_java_files == 0);             # ...Change dir string to null if no java files. Not a package.
	s,\./,,;                                       # Remove "./" from string.
	s,/,.,g;                                       # Convert '/' to '.'.
	# Now for the exceptions... blah.
        $_ = "" if ($_ eq "alp");
    } # foreach
    print("\n=> package_list: @package_list \n\n");

   
    ## - Call javadoc with the list that was created.
    $cmd = " javadoc -J-Xms96m -J-Xmx96m \\
             -d ${JAVADOC}/${BUILD_PREFIX} \\
             -use \\
             -splitindex \\
             -windowtitle 'javadocs for $BUILD_PREFIX' \\
             -sourcepath ${CLASS}/$BUILD_PREFIX:${SRC}/src/$BUILD_PREFIX:$ENV{'CLASSPATH'} \\
             @package_list
           ";
    print ("\n=> $cmd \n");
    system ($cmd);

    
    ## - zip the javadoc tree.
    chdir ("$JAVADOC")  || die("Can't change to $JAVADOC... exiting $!");
    $cmd = "zip -rq $BUILD_PREFIX-javadoc.zip $BUILD_PREFIX";
    print ("\n=> $cmd \n");
    system($cmd);

    ## - Remove the javadoc tree.
    chdir ("$JAVADOC")  || die("Can't change to $JAVADOC... exiting $!");
    $cmd = "\\rm -rf $BUILD_PREFIX";  #!!!!! Guarentee safety here!!!!
    print ("\n=> Removing javadoc tree. \n");
    print ("\n=> $cmd \n");
    system($cmd);
    
} # sub create_javadoc()




##
## Zip the source tree. Store it at
## ${DIST}/src/${BUILD_PREFIX}.
##
## args:     none.
## returns:  nothing.
##
## TTD: Skip this step if package isn't core or alpine
##
sub zip_sources()
{
    print("\n=> zip_sources() ... \n") if ($BUILD_VERBOSE eq "TRUE");

    my $cmd;

    chdir("$SRC/") || die ("ERROR!! Can't change to $SRC/src/ $!");

    #mkdir ("$DIST/src", 0755) || die("ERROR!! Can't create $DIST/src  $!");

#    $cmd = "zip -rq ${DIST}/src/$BUILD_PREFIX.zip       \\
#             `find ./ -name \"*java\"      -print`      \\
#             `find ./ -name \"*def\"       -print`      \\
#             `find ./ -name \"*html\"      -print`      \\
#             `find ./ -name \"*dtd\"       -print`      \\
#             `find ./ -name \"ChangeLog\"  -print`  
#           "; 

    $cmd = "zip -rq ${DIST}/src/$BUILD_PREFIX.zip  `find ./ -type f -a -name \"*\" -print` ";
    print("\n=> $cmd \n");
    system($cmd);

} # zip_sources()



##
## 
## args:     none.
## returns:  nothing.
##
sub zip_distribution()
{
    print("\n=> zip_distribution() ... \n") if ($BUILD_VERBOSE eq "TRUE");
   
    my $cmd;

    # core?
    if ( $BUILD_PREFIX eq 'core' ) {
	chdir($BUILD_DIR) || die("ERROR!! Cannot change to $BUILD_DIR  $!");
        # move the 'core' direcotry to 'alp-<date>'
	$cmd = "mv $BUILD_PREFIX alp-$BUILD_DATE";
	print("\n=> $cmd \n");
	system($cmd);
        # zip it.
	$cmd = "zip -rq ${BUILD_DIR}/${BUILD_NAME}.zip alp-$BUILD_DATE";
	print("\n=> $cmd \n");
	system($cmd);
        # move it back to 'core' so that other builds can find it.
	$cmd = "mv alp-$BUILD_DATE $BUILD_PREFIX";
	print("\n=> $cmd \n");
	system($cmd);
    } # if
##    # alpine, tools, tutorial, or utility?
##    elsif ( ($BUILD_PREFIX eq 'alpine') || ($BUILD_PREFIX eq 'tools') || ($BUILD_PREFIX eq 'tutorial') || ($BUILD_PREFIX eq 'utility') ) {
##	chdir("$BUILD_DIR/$BUILD_PREFIX") || die("ERROR!! Cannot change to $BUILD_DIR/$BUILD_PREFIX  $!");
##	$cmd = "zip -rq ${BUILD_DIR}/${BUILD_NAME}.zip *";
##	print("\n=> $cmd \n");
##	system($cmd);
##   } # elsif
##   # plugin?
    else {
	chdir("$BUILD_DIR/$BUILD_PREFIX") || die("ERROR!! Cannot change to $BUILD_DIR/$BUILD_PREFIX  $!");
	$cmd = "zip -rq ${BUILD_DIR}/${BUILD_NAME}.zip *";
	print("\n=> $cmd \n");
	system($cmd);
    } # else


    # Move the zip file to the distribution location.
    chdir($BUILD_DIR) || die("ERROR!! Cannot change to $BUILD_DIR  $!");
    $cmd = "cp ${BUILD_NAME}.zip $BUILD_DIST_LOCATION";
    print("\n=> $cmd \n");
    system($cmd);

} # zip_distribution()




##
## args:     none.
## returns:  nothing.
##
##
sub check_for_errors()
{
    print("\n=> check_for_errors() ... \n") if ($BUILD_VERBOSE eq "TRUE");

    my $cmd;

    $cmd  = "/usr/local/bin/Scan4Errors $BUILD_LOG";
    print("\n=> $cmd \n");
    system($cmd);

} # check_for_errors()



## Move the log files.
## TTD: close and reopen STDOUT/STDIN
##
sub move_log_files()
{ 
    print("\n=> move_log_files() ...   \n") if ($BUILD_VERBOSE eq "TRUE");

    mkdir ("$BUILD_LOCATION/$BUILD_NAME/log",0755);

    my $cmd = "mv ${BUILD_LOG}* $BUILD_LOCATION/$BUILD_NAME/log";
    print("\n=> $cmd \n");
    system($cmd);

    $cmd = "cp $BUILD_LOCATION/$BUILD_NAME/log/* /build/dist/build-logs";
    print("\n=> $cmd \n");
    system($cmd);

} # move_log_files()



## Takes a command, which is echo'd to the screen before execution.
##
sub doit()
{
   my $cmd = (@_)[0];

   if ($cmd eq '') {
      print("\n=> doit()  passed a null command. \n");
   } # if

   print("\n=> $cmd \n");
   system($cmd);

} # sub doit()




#   Fragments:

#   Mikes redirection code:
#   print "Redirecting STDOUT to $logfile\n" if $verbose;
#   open(NEWOUT, "|tee $logfile"); select(NEWOUT); $| = 1;
#   close(STDOUT);
#   close(STDERR);
#   open(STDOUT, ">&NEWOUT"); select(STDOUT); $| = 1;
#   open(STDERR, ">&NEWOUT"); select(STDERR); $| = 1;


#=pod
#=head1 NAME
#=head1 SYNOPSIS
#=head1 DESCRIPTION
#=head1 OPTIONS
#=head1 OPERANDS
#=head1 USAGE
#=head1 ENVIRONMENT
#=head1 EXIT STATUS
#=head1 FILES 
#=head1 ATTRIBUTES
#=head1 SEE ALSO
#=head1 NOTES
#=head1 BUGS

#=head1 AUTHOR
#=head1 VERSION

#=cut


=pod

=head1 NAME

=head1 SYNOPSIS

=head1 DESCRIPTION

=head1 OPTIONS

=head1 USAGE

=head1 BUGS

=cut
