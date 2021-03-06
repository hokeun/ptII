# Makefile for Ptolemy II - HLA/CERTI demos
#
# @Authors: Gilles Lasnier, based on a file by Christopher H. Brooks
#
# @Version: $Id: makefile 181 2018-03-28 09:46:50Z gl.lasnier $
#
# @Copyright (c) 2013-2015 The Regents of the University of California.
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
#                       PT_COPYRIGHT_VERSION_2
#                       COPYRIGHTENDKEY

ME =        org/hlacerti/demo/f14HLA/AllFederatesNER

# Root of the Java directory
ROOT =      ../../../../..

CLASSPATH = $(ROOT)

# Get configuration info
CONFIG =    $(ROOT)/mk/ptII.mk
include $(CONFIG)

# Used to build jar files
PTPACKAGE =     TwoF14AircraftsNER
PTCLASSJAR =    $(PTPACKAGE).jar

JSRCS =

OTHER_FILES_TO_BE_JARED = \
	$(JSRCS) \
	f14HlaNER.xml \
	priseV2.fed \
	f14Aircraft2.xml \
	f14AutoPilot2.xml \
	f14PilotStick2.xml \
	f14Aircraft.xml \
	f14AutoPilot.xml \
	f14PilotStick.xml

EXTRA_SRCS = \
	$(OTHER_FILES_TO_BE_JARED)

# Sources that may or may not be present, but if they are present, we don't
# want make checkjunk to barf on them.
MISC_FILES =

# make checkjunk will not report OPTIONAL_FILES as trash
# make realclean removes OPTIONAL_FILES
OPTIONAL_FILES = RTIG.log

JCLASS = $(JSRCS:%.java=%.class)

# Don't include all or install rules here, we want the user
# to run 'make demos' to run the tests.
all: jclass $(PTCLASSJAR)n
	@echo "To run all the demos, run 'make demo'"

install: all jars

demo: demo_ptolemy demo_vergil

demo_ptolemy:
	"$(PTII)/bin/ptolemy" TwoF14AircraftsNER.xml

demo_vergil:
	"$(PTII)/bin/vergil" TwoF14AircraftsNER.xml

# Get the rest of the rules
include $(ROOT)/mk/ptcommon.mk
