#!/bin/sh
jade -d /usr/lib/sgml/stylesheets/nwalsh-modular/print/docbook.dsl -t tex /usr/share/doc/openjade-1.3/xml.dcl freehost3270_dc.xml
jadetex freehost3270_dc.tex
dvips -o freehost3270.ps freehost3270_dc.dvi
