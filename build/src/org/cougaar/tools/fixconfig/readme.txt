Options

The tools in this package have a number of options listed below:

-role <rolename> includes <rolename> in the set of relationships that
 are retained in the prototype-ini and relationships files. These are
 also the relationships used to construct the closure of all clusters
 needed to specify the base set of clusters.

-log <logfilename> writes the detail log to the specified file. This
 output is written to stderr if omitted.

-ini <node ini> writes a node .ini file of all the nodes implied by
 the rest of the args. Written to stdout if omitted.

-ammo -construction -consumable -food -medical -pol -sra -transport
 cause removal of the relationships and plugins supporting each supply
 thread.

