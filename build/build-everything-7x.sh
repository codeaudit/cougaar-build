#!/bin/sh

#DATE=`date +\%Y\%m\%d`
# `date  +\%Y\%m\%d` used below for 'cut and paste' builds.



#---
cd /usr/local/bin
./build-alp-7x.pl -p build        -v ALP_70 
./build-alp-7x.pl -p core         -v ALP_70  -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p javaiopatch  -v ALP_70  -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p toolkit      -v ALP_70  -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p planserver   -v ALP_70  -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p glm          -v ALP_70  -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p contract     -v ALP_70  -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p scalability  -v ALP_70  -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p server       -v ALP_70  -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p tutorial     -v ALP_70  -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p configgen    -v ALP_70  -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p aggagent     -v ALP_70  -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -p uiframework -v ALP_70   -w ALP_70-`date +\%Y\%m\%d` -W /build/dist/builds/aggagent-ALP_70-`date +\%Y\%m\%d`/aggagent/lib

./build-alp-7x.pl -r /cvs/alp/internal -p vishnu -t HEAD -v ALP_70 -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -r /cvs/alp/internal -p tops   -t HEAD -v ALP_70 -w ALP_70-`date +\%Y\%m\%d` -W /build/dist/builds/vishnu-ALP_70-`date +\%Y\%m\%d`/vishnu/lib

./build-alp-7x.pl -r /cvs/alp/internal -p delta      -t BJ_Mar2001 -T HEAD -v BJ_Mar2001 -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -r /cvs/alp/internal -p delta      -t HEAD -v ALP_70 -w ALP_70-`date +\%Y\%m\%d`
./build-alp-7x.pl -r /cvs/alp/internal -p blackjack  -t HEAD -v ALP_70  -w ALP_70-`date +\%Y\%m\%d` -W /build/dist/builds/vishnu-ALP_70-`date +\%Y\%m\%d`/vishnu/lib

./build-alp-7x.pl -r /cvs/alp/internal  -S -p ants     -t ALP_70 -T HEAD -v ALP_70 -w ALP_70-`date +\%Y\%m\%d`
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
