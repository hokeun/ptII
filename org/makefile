# Makefile org sources from outside of Ptolemy
#
# @Version: $Id$
# @Author: Christopher Brooks
#
# @Copyright (c) 2007-2015 The Regents of the University of California.
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

# configure sets PTBSH_MLC_DIR to mlc if BSH, the Java Bean Shell.
# configure sets PTJSON_DIR to json if JSON, the JavaScript Object Notation was found.
# configure sets PTHLACERTI_DIR to hlacerti.
DIRS = $(PTBSH_MLC_DIR) $(PTJSON_DIR) ptolemy terraswarm $(PTHLACERTI_DIR)

# Root of Ptolemy II directory
ROOT =		..

# $(ME) is used in ptdir.mk
ME =		org

# True source directory
VPATH =		$(ROOT)/$(ME)

# Get configuration info
CONFIG =	$(ROOT)/mk/ptII.mk
include $(CONFIG)

all: suball
install: subinstall

# Sources that may or may not be present, but if they are present, we don't
# want make checkjunk to report an error on them.
MISC_FILES = hlacerti json mlc ptolemy terraswarm

# Get the rest of the rules
include $(ROOT)/mk/ptcommon.mk
