# Makefile for vergil basic classes
#
# @Authors: Christopher Brooks
#
# @Version: $Id$
#
# @Copyright (c) 2008 The Regents of the University of California.
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

# Location of this directory, relative to the Ptolemy II directory
ME =		ptolemy/vergil/basic/test

# PTJUNIT_DIR is set to junit in ptII.mk by configure if JUnit is found.
DIRS =		$(PTJUNIT_DIR)

# Root of the Ptolemy II directory
ROOT =		../../../..

CLASSPATH =	$(ROOT)$(CLASSPATHSEPARATOR)$(DIVA_JAR)

# Get configuration info
CONFIG =	$(ROOT)/mk/ptII.mk
include $(CONFIG)

# Tcl sources that are part of the testing infrastructure
TCL_SRCS = \
	testDefs.tcl

JSRCS =

# Non-graphical Java tests written in Tcl
# If there are no tests, we use a dummy file so that the script that builds
# alljtests.tcl works.
JSIMPLE_TESTS = \
	DocAttribute.tcl \
	KeplerDocumentationAttribute.tcl

# Graphical Java tests that use Tcl.
# If there are no tests, we use a dummy file so that the script that builds
# alljtests.tcl works.
JGRAPHICAL_TESTS = \
	dummy.tcl

EXTRA_SRCS =	$(TCL_SRCS) $(JSRCS) $(JSIMPLE_TESTS) #$(JGRAPHICAL_TESTS) 

# Sources that may or may not be present, but if they are present, we don't
# want make checkjunk to barf on them.
MISC_FILES =	 junit \
	alljtests.tcl \
	alljsimpletests.tcl \
	junit

# make checkjunk will not report OPTIONAL_FILES as trash
# make distclean removes OPTIONAL_FILES
OPTIONAL_FILES =

JCLASS =	$(JSRCS:%.java=%.class)

# Don't include all or install rules here, we want the user
# to run 'make tests' to run the tests.

# The test rule is the first rule so if the user runs make test, then
# we get the graphical tests
test: $(EXTRA_SRCS) jclass test_java test_jsimple #test_jgraphical

# The nightly build runs make tests, so don't had graphical tests here
tests:: $(EXTRA_SRCS) jclass test_java test_jsimple

test_java: jclass

# PtolemyII-wide test definitions.
KERNEL_TESTDEFS = $(ROOT)/util/testsuite/testDefs.tcl

# These tests do not require a graphics terminal, but do use Jacl and Java
test_jsimple: $(EXTRA_SRCS) jclass $(KERNEL_TESTDEFS) alljsimpletests.tcl
	$(JTCLSH) alljsimpletests.tcl

# These tests do require a graphics terminal, but do use Jacl and Java
test_jgraphical: $(EXTRA_SRCS) jclass $(KERNEL_TESTDEFS) alljtests.tcl
	$(JTCLSH) alljtests.tcl

# Get the rest of the rules
include $(ROOT)/mk/ptcommon.mk
