#!/bin/sh

# <copyright>
#  Copyright 2001 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
# 
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the Cougaar Open Source License as published by
#  DARPA on the Cougaar Open Source Website (www.cougaar.org).
# 
#  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
#  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
#  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
#  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
#  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
#  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
#  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
#  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
#  PERFORMANCE OF THE COUGAAR SOFTWARE.
# </copyright>


#DATE=`date +\%Y\%m\%d`
# `date  +\%Y\%m\%d` used below for 'cut and paste' builds.



#---

# Remaining FGI builds
/usr/local/bin/build-release-12  -t FGI_MB60_Integ -p fgi  -v FGI_MB60_Integ 


#---

# 6.4.x BRANCH builds...
/usr/local/bin/build-org.cougaar.pl -V -p core      -t COUGAAR_6_4 -v COUGAAR_6_4                               > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p utility   -t COUGAAR_6_4 -v COUGAAR_6_4 -w COUGAAR_6_4-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p alpine    -t COUGAAR_6_4 -v COUGAAR_6_4 -w COUGAAR_6_4-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p tools     -t COUGAAR_6_4 -v COUGAAR_6_4 -w COUGAAR_6_4-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p tutorial  -t COUGAAR_6_4 -v COUGAAR_6_4 -w COUGAAR_6_4-`date +\%Y\%m\%d`  > /dev/null 2>&1

# 6.4.x plugin BRANCH builds.
#
# - Plugin branch against alp branch
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p ants  -t COUGAAR_6_4  -v COUGAAR_6_4 -w COUGAAR_6_4-`date +\%Y\%m\%d` > /dev/null 2>&1
#
# - Plugin head, against alp branch
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p delta -t HEAD     -v COUGAAR_6_4 -w COUGAAR_6_4-`date +\%Y\%m\%d` > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p TOPS  -t STABLE -T COUGAAR_6_4 -v COUGAAR_6_4 -w COUGAAR_6_4-`date +\%Y\%m\%d` > /dev/null 2>&1


#--

# 6.6.x BRANCH builds...
/usr/local/bin/build-org.cougaar.pl -V -p core      -t COUGAAR_6_6 -v COUGAAR_6_6                               > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p utility   -t COUGAAR_6_6 -v COUGAAR_6_6 -w COUGAAR_6_6-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p alpine    -t COUGAAR_6_6 -v COUGAAR_6_6 -w COUGAAR_6_6-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p tools     -t COUGAAR_6_6 -v COUGAAR_6_6 -w COUGAAR_6_6-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p tutorial  -t COUGAAR_6_6 -v COUGAAR_6_6 -w COUGAAR_6_6-`date +\%Y\%m\%d`  > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -V -p server    -t COUGAAR_6_6 -v COUGAAR_6_6 -w COUGAAR_6_6-`date +\%Y\%m\%d`  > /dev/null 2>&1

# 6.6.x plugin BRANCH builds.
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p blackjack -t HEAD     -v COUGAAR_6_6 -w COUGAAR_6_6-`date +\%Y\%m\%d` > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p delta     -t blackjack_oct2000  -T COUGAAR_6_6   -v COUGAAR_6_6 -w COUGAAR_6_6-`date +\%Y\%m\%d` > /dev/null 2>&1
/usr/local/bin/build-org.cougaar.pl -r /cvs/alp/internal -V -p ants      -t COUGAAR_6_6  -v COUGAAR_6_6 -w COUGAAR_6_6-`date +\%Y\%m\%d` > /dev/null 2>&1


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
