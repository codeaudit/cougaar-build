#!/usr/bin/env perl
# The following was copied from http://coding.debuntu.org/perl-calculate-md5-sum-file on March 1, 2012
# with minor changes to more closely match the Mac OS X md5 program.

use warnings;
use strict;

use Digest::MD5;

sub md5sum {
    my $file = shift;
    my $digest = "";
    eval {
	die "md5.pl: $file: Is a directory\n" if ( -d $file );
	open(FILE, $file) or die "md5.pl: $file: $!\n";
	my $ctx = Digest::MD5->new;
	$ctx->addfile(*FILE);
	$digest = $ctx->hexdigest;
	close(FILE);
    };

    if ($@) {
	print $@;
	return "";
    }
    return $digest;
}

sub usage {
    print "usage: ./md5.pl filenames\n";
    exit 1;
}

if ($#ARGV == -1) {
    # use stdin
    my $fname = "-";
    my $md5 =  md5sum($fname);
    if ($md5 ne "") {
	print $md5 . "\n";
    } else {
	exit 1;
    }
    exit 0;
}

# use list of files
for (@ARGV) {
    my $fname = $_;
    my $md5 =  md5sum($fname);
    if ($md5 ne "") {
	print "MD5 (" . $fname . ") = " . $md5 . "\n";
    } else {
	exit 1;
    }
}

exit 0;
