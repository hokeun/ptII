# Makefile for gui/demo directory
#
# @Authors: Christopher Hylands
#
# @Version: $Id$
#
# @Copyright (c) 1998-2008 The Regents of the University of California.
# All rights reserved.
#
# Permission is hereby granted, without written agreement and without
# license or royalty fees, to use, copy, modify, and distribute this
# software and its documentation for any purpose, provided that the
# above copyright notice and the following two paragraphs appear in all
# copies of this software.
#
# IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
# FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
# ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
# THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
# THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
# PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
# CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
# ENHANCEMENTS, OR MODIFICATIONS.
#
# 						PT_COPYRIGHT_VERSION_2
# 						COPYRIGHTENDKEY

ME =		ptolemy/gui/demo

# Root of the Java directory
ROOT =		../../..

CLASSPATH =	$(ROOT)

# Get configuration info
CONFIG =	$(ROOT)/mk/ptII.mk
include $(CONFIG)

TCL_SRCS = \


JSRCS = \
	FileChooserQuery.java \
	QueryApplet.java \
	ShowRawDocumentApplet.java


EXTRA_SRCS =	$(TCL_SRCS) $(JSRCS) $(JGRAPHICAL_TESTS) \
	QueryApplet.htm \
	ShowRawDocumentApplet.htm \
	FileChooserQuery.java

# Sources that may or may not be present, but if they are present, we don't
# want make checkjunk to barf on them.
MISC_FILES =	# doc

# make checkjunk will not report OPTIONAL_FILES as trash
# make realclean removes OPTIONAL_FILES
OPTIONAL_FILES = \
	'FileChooserQuery$$1.class' \
	'FileChooserQuery$$1$$1.class' \
	FileChooserQuery.class 

JCLASS = $(JSRCS:%.java=%.class)

# Don't include all or install rules here, we want the user
# to run 'make demos' to run the tests.
all: $(JCLASS)
	@echo "To run all the demos, run 'make demo'"

install: all

demos: demo
demo:	all
	CLASSPATH=$(CLASSPATH) appletviewer QueryApplet.htm
	CLASSPATH=$(CLASSPATH) java ptolemy.gui.demo.FileChooserQuery
	CLASSPATH=$(CLASSPATH) appletviewer ShowRawDocumentApplet.htm

# Get the rest of the rules
include $(ROOT)/mk/ptcommon.mk
