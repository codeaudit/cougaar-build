#!/usr/local/bin/perl -w

# <copyright>
#  Copyright 2001-2003 BBNT Solutions, LLC
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


