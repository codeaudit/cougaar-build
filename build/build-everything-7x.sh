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
cd /usr/local/bin
./build-alp-7x.pl -p build        -v COUGAAR_70 
./build-alp-7x.pl -p core         -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p javaiopatch  -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p toolkit      -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p planserver   -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p glm          -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p contract     -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p scalability  -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p server       -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p tutorial     -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p configgen    -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p aggagent     -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p uiframework -v COUGAAR_70   -w COUGAAR_70-`date +\%Y\%m\%d` -W /build/dist/builds/aggagent-COUGAAR_70-`date +\%Y\%m\%d`/aggagent/lib

./build-alp-7x.pl -r /cvs/alp/internal -p vishnu -t HEAD -v COUGAAR_70 -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -r /cvs/alp/internal -p tops   -t HEAD -v COUGAAR_70 -w COUGAAR_70-`date +\%Y\%m\%d` -W /build/dist/builds/vishnu-COUGAAR_70-`date +\%Y\%m\%d`/vishnu/lib

./build-alp-7x.pl -r /cvs/alp/internal -p delta      -t BJ_Mar2001 -T HEAD -v BJ_Mar2001 -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -r /cvs/alp/internal -p delta      -t HEAD -v COUGAAR_70 -w COUGAAR_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -r /cvs/alp/internal -p blackjack  -t HEAD -v COUGAAR_70  -w COUGAAR_70-`date +\%Y\%m\%d` -W /build/dist/builds/vishnu-COUGAAR_70-`date +\%Y\%m\%d`/vishnu/lib

./build-alp-7x.pl -r /cvs/alp/internal  -S -p ants     -t COUGAAR_70 -T HEAD -v COUGAAR_70 -w COUGAAR_70-`date +\%Y\%m\%d`
#---



# Usage: ./build-alp-7x.pl <options> 
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
