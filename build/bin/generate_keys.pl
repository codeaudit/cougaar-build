#!/usr/local/bin/perl -w

# <copyright>
#  
#  Copyright 2001-2004 BBNT Solutions, LLC
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


#-------- Notes -------#
#
#   author: Mark Damish
#     date: Feb 2000
# --------
#     file: generate_keys.pl
#  purpose: Generate a keyfile and certificate for signing .jar files.
#  expects: Cluster file. One per line.  For initial run, 
#           eiger.alpine.bbn.com:/usr/local/etc/clusterlist.dat
#    usage: ./generate_keys.pl
#           User prompted for cluster filename.
#    notes: Generated keys will be placed cwd
#


#------- Includes -------#
use strict;


#------- Globals -------#
my @clusters;


#------- Main -------#
get_cluster_names();
generate_keys();


#------- Subroutines -------#


sub get_cluster_names
{
   print "File containing cluster names: ";
   my $filename = <>;
   chomp $filename;

   open (FH, "<$filename")  || die "Can't open $filename for read.  $! \n";
   @clusters = <FH>;
   chomp @clusters;

   close FH;
   
} # END get_cluster_names



sub generate_keys 
{

   print "\n==> Generating key\n";
   `keytool -genkey -keystore .keystore -storepass alpalp -alias alpine -validity 1000 -dname  "cn=Alpine, o=Alpine c=US" -keypass alpalp`;

  
   print "\n==> Generating keys\n";
   foreach (@clusters) {
      print ".... $_ \n";
      `keytool -keystore .keystore -storepass alpalp -genkey -alias $_ -keypass alpalp -validity 1000 -dname "cn=$_"`;
   } # foreach

   print "\n==> Exporting Certificate\n";
   `keytool -export -alias alpine -file alpcertfile.cer -keystore .keystore -keypass alpalp -storepass alpalp`;

} # END generate_keys


