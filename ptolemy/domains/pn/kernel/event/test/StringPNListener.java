/* A PNProcessListener that listens for events generated
   by BasePNDirector and converts it to a string.

Copyright (c) 1997-2002 The Regents of the University of California.
All rights reserved.
Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the above
copyright notice and the following two paragraphs appear in all copies
of this software.

IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
ENHANCEMENTS, OR MODIFICATIONS.

                                        PT_COPYRIGHT_VERSION_2
                                        COPYRIGHTENDKEY

@ProposedRating Yellow (mudit@eecs.berkeley.edu)
@AcceptedRating Red
*/

package ptolemy.domains.pn.kernel.event.test;
import ptolemy.domains.pn.kernel.*;
import ptolemy.domains.pn.kernel.event.*;

//////////////////////////////////////////////////////////////////////////
//// StringPNListener
/**
A PNProcessListener that listens for events generated by
BasePNDirector and converts it to a string.
An StringPNListener is able to receive PNProcessEvents that are issued
during the execution of a process by a ProcessThread or director in PN.

@author Mudit Goel
@version $Id$
@since Ptolemy II 0.3
*/

public class StringPNListener implements PNProcessListener {

    /** Called to report that the execution of a process finished. The
     *  wrapup sequence may or may not have completed normally.   The
     *  execution event will contain a reference to the actor corresponding
     *  to the process that finished and the reason for finishing.
     *
     *  @param event A PNProcessEvent that contains a reference to an
     *  actor.
     */
    public void processFinished(PNProcessEvent event) {
        _profile += event.toString() + "\n";
    }

    /** Called to report that a process has changed its state (i.e. started,
     *  or blocked or unblocked, etc.). The PNProcessEvent
     *  will contain a reference to the actor corresponding to the process.
     *  The event will also indicate the new state and blocking cause, etc.
     *
     *  @param event A PNProcessEvent that contains a reference to an actor.
     */
    public void processStateChanged(PNProcessEvent event) {
        _profile += event.toString() + "\n";
    }

    /** Return a list of all the activities recorded by the listener.
     */
    public String getProfile() {
        return _profile;
    }

    private String _profile = "";

}
