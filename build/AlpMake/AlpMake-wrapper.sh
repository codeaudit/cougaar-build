#!/bin/sh

#
# Wrapper to set up environment for the AlpMake utility.
#
# Mark Damish  16 Jun 1998
#

#
# Typical Usage:   sh AlpMake-wrapper.sh  /path/to/source /path/to/destination
#
# The program compiles AlpMake.java, and then runs the AlpMake.java
# AlpMake.java currently creates a .sh and a .bat file for compiling
# a tree of java files. 
#



# The following variables need to be set...

# Location of COUGAAR source tree.
SRC=$1

# Location to put .class files. 
DST=$2


# The following variables contain the path, arguments, and name of the alp build util.


## # Arguments to AlpMake.class
MY_ARGUMENTS="-s $SRC -d $DST   $3 $4 $5 $6 $7 $8 $9" 

# Name of class to execute.
MY_CLASSES=AlpMake



#
# Remove old AlpMake generated files
#
if [ -f alp_compile.sh ]
then
  rm alp_compile.*
fi


#
# Change to the directory containing the build program, compile it, then execute it.
#
## cd $MY_EXE_DIR
echo ""
echo "wrapper: `date`"
echo ""
if [ ! -f MakeHelper.class ]
then
  echo "wrapper: compiling $MY_CLASS..."
  javac -classpath $CLASSPATH *.java
  echo "wrapper: ...done"
  echo ""
fi
#
echo ""
echo "wrapper: Starting $MY_CLASSES..."
java -mx96M  -classpath $CLASSPATH $MY_CLASSES $MY_ARGUMENTS
echo "wrapper: Starting compile..."
/bin/sh alp_compile.sh
echo ""
echo "wrapper: `date`"
echo ""
echo "wrapper: ...Done"
echo ""

