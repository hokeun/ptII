# Makefile for the Ptolemy II Attack Modeling paper
#
# Version: $Id$
# Makefile Author: Christopher Brooks
#
# Copyright (c) 2014 The Regents of the University of California.
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
##########################################################################

ME =		doc/papers/y14/attackModeling

# Sub directories to run make in.
# Don't include src directory, since we don't ship it with the release
# Don't need a test here, we test it elsewhere.
DIRS =		

# Root of Ptolemy II directory
ROOT =		../../../..

# Get configuration info
CONFIG =	$(ROOT)/mk/ptII.mk
include $(CONFIG)

PTPACKAGE = 	attackModeling
PTCLASSJAR = 	$(PTPACKAGE).jar
#PTCLASSALLJAR = $(PTPACKAGE).jar

# Include the .class files from these jars in PTCLASSALLJAR
#PTCLASSALLJARS = \

EXAMPLE_MODELS = \
	CarTrackingAttackModeling.xml

OTHER_FILES_TO_BE_JARED = $(EXAMPLE_MODELS)

EXTRA_SRCS = $(OTHER_FILES_TO_BE_JARED)

# Sources that may or may not be present, but if they are present, we don't
# want make checkjunk to barf on them.
MISC_FILES =	$(DIRS)

# make checkjunk will not report OPTIONAL_FILES as trash
# make distclean removes OPTIONAL_FILES
OPTIONAL_FILES = \
	index.html.in \
	toc.htm

all: jclass
install: jclass jars

# The main target for this file is "jnlps", which is defined in $PTII/mk/ptcommon.mk

# See http://chess.eecs.berkeley.edu/ptexternal/wiki/Main/HTMLExport
# For instructions about how to update http://ptolemy.org/constructive

# For an example that is in the ptII repo, see the $PTII/mk/jnlp.mk file.

# $PTII/mk/jnlp.mk calls "make echo_models"
echo_models:
	@echo "$(EXAMPLE_MODELS) $(EXAMPLE_SUBMODELS)" | awk '{for(i=1;i<=NF;i++) {print $$i}}' | sort

# We don't include common.mk since we don't use the compiler here
include $(ROOT)/mk/ptno-compile.mk
