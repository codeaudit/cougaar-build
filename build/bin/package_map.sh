#! /bin/sh

# <copyright>
#  
#  Copyright 2003-2004 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects
#  Agency (DARPA).
# 
#  You can redistribute this software and/or modify it under the
#  terms of the Cougaar Open Source License as published on the
#  Cougaar Open Source Website (www.cougaar.org).
# 
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
#  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
#  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
#  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
#  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
#  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
#  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
#  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
#  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#  
# </copyright>

#
# ugly script that generates the cougaar package map
#

# directory to find CVS checkouts
DIR=$1

# current cougaar version
VERSION=$2

if [ -z $DIR ] || [ -z $VERSION ]; then
  echo "Usage: $0 cvs_checkout_directory cougaar_version"
  exit 1
fi

# Expecting $DIR to either
#
# A) directly contains the CVS modules:
#
#   `ls`
#   core, glm, util, ...
#
# or,
# B) contains the modules as second-level subdirectories:
#
#   `find . -type d -maxdepth 2`
#   ./foo/core, ./foo/glm, ..., ./bar/albbn, ./bar/alsra, ...
#

cd $DIR || exit -1

cat << EOF
<html>
<title>Cougaar $VERSION Java Package Map</title>
<body>
<h2>Cougaar $VERSION Java Package Map</h2>
<table border=0>
<tr>
  <th align=left>Package:</th>
  <th align=left>CVS Module(s):</th>
</tr>
EOF

# not pretty!
#
# overview:
#   find all subdirectories with "src" or "examples" paths
#   exclude CVS directories
#   parse each line into (package, dir) pairs
#       (e.g. org/cougaar/core/agent, core)
#   sort by package
#   use perl to merge into (package, dir1, dir2, .., dirN) lines

find . -type d -regex "^\.\(\/\w+\)?\/\w+\/\(src\|examples\)\/.*" |\
  grep -v CVS |\
  sed -e 's/^\.\/\(\w*\/\)\?\(\w*\)\/\(src\|examples\)\/\(.*\)$/\4 \2/' |\
  sort -k 1,1 |\
  perl -e \
  '
  while(<>) {
    next unless (/^([^\s]+)\s(.+)$/o);
    if ($prevPath eq $1) {
      unless ($2 eq $prevName) {
        print ", $2";
        $prevName=$2;
      }
    } else {
      if ($needsTREnd) {
        print "</td></tr>\n";
      } else {
        $needsTREnd=1;
      }
      $prevPath=$1;
      $prevName=$2;
      $pkg = $prevPath;
      $pkg =~ s/\//\./go;
      print "<tr><td>$pkg</td><td>$prevName";
    }
  }
  if ($needsTREnd) {
    print "</td></tr>\n";
    $needsTREnd=0;
  }
  '

cat << EOF
</table>
</body></html>
EOF

# may require some hand editing to fix odd module layouts,
# e.g. community/examples/config

