#!/bin/sh

#DATE=`date +\%Y\%m\%d`
# `date  +\%Y\%m\%d` used below for 'cut and paste' builds.



#---

# Remaining FGI builds
/usr/local/bin/build-release-12  -t FGI_MB60_Integ -p fgi  -v FGI_MB60_Integ 


#---

# 6.4.x BRANCH builds...
/usr/local/bin/build-org.cougaar.pl -V -p core      -t ALP_6_4 -v ALP_6_4                               > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p utility   -t ALP_6_4 -v ALP_6_4 -w ALP_6_4-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p alpine    -t ALP_6_4 -v ALP_6_4 -w ALP_6_4-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p tools     -t ALP_6_4 -v ALP_6_4 -w ALP_6_4-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p tutorial  -t ALP_6_4 -v ALP_6_4 -w ALP_6_4-`date +\%Y\%m\%d`  > /dev/null 2>&1

# 6.4.x plugin BRANCH builds.
#
# - Plugin branch against alp branch
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p ants  -t ALP_6_4  -v ALP_6_4 -w ALP_6_4-`date +\%Y\%m\%d` > /dev/null 2>&1
#
# - Plugin head, against alp branch
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p delta -t HEAD     -v ALP_6_4 -w ALP_6_4-`date +\%Y\%m\%d` > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p TOPS  -t STABLE -T ALP_6_4 -v ALP_6_4 -w ALP_6_4-`date +\%Y\%m\%d` > /dev/null 2>&1


#--

# 6.6.x BRANCH builds...
/usr/local/bin/build-org.cougaar.pl -V -p core      -t ALP_6_6 -v ALP_6_6                               > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p utility   -t ALP_6_6 -v ALP_6_6 -w ALP_6_6-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p alpine    -t ALP_6_6 -v ALP_6_6 -w ALP_6_6-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p tools     -t ALP_6_6 -v ALP_6_6 -w ALP_6_6-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p tutorial  -t ALP_6_6 -v ALP_6_6 -w ALP_6_6-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p server    -t ALP_6_6 -v ALP_6_6 -w ALP_6_6-`date +\%Y\%m\%d`  > /dev/null 2>&1

# 6.6.x plugin BRANCH builds.
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p blackjack -t HEAD     -v ALP_6_6 -w ALP_6_6-`date +\%Y\%m\%d` > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p delta     -t blackjack_oct2000  -T ALP_6_6   -v ALP_6_6 -w ALP_6_6-`date +\%Y\%m\%d` > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p ants      -t ALP_6_6  -v ALP_6_6 -w ALP_6_6-`date +\%Y\%m\%d` > /dev/null 2>&1


#--


# 6.x.x builds HEAD builds...
/usr/local/bin/build-org.cougaar.pl -V -p core      -v HEAD                            > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p utility   -v HEAD -w HEAD-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p alpine    -v HEAD -w HEAD-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p tools     -v HEAD -w HEAD-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p tutorial  -v HEAD -w HEAD-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p server    -v HEAD -w HEAD-`date +\%Y\%m\%d`  > /dev/null 2>&1

# 6.x.x plugin HEAD builds...
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p ants  -v HEAD -w HEAD-`date +\%Y\%m\%d`   > /dev/null 2>&1

#---



# Usage: ./build-org.cougaar.pl <options> 
# 
#    -p  <Build name prefix>
#    -v  <Build name version>
#    -b  <Build location>
#    -d  <Distribution destination directory>
#    -r  <CVS root>
#    -t  <CVS tag of sources to build.>
#    -T  <CVS tag of build scripts.>
#    -w  <build name>   Build with jars in this build.
#    -W  <path1:path2>  Build with jars in this path.
#    -i  Interactive. Don't create log or send mail.
#    -s  Skip CVS checkout, and dir creation 
#    -S  Skip the recursive build process
#    -V  Add some verbocity to the messages. 
#    -h  Help. (This blurb)
# 
#    The build name is constucted to form:
#      prefix-version-date
#    The default prefix is:                    core
#    The default version is:                   HEAD
#    The default date is today (unchangable):  20000516
#    The default build name is:                core-HEAD-20000516
#    The default cvs repository is:            /cvs/alp/master
#    The default build location is:            /build/dist/builds
#    The default cvs tag for sources is:       HEAD
#    The default cvs tag for build scripts is: HEAD
#    The default distribution location is:     /build/dist/zips
