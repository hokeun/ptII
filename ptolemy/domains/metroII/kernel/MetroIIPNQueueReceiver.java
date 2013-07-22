/*
Below is the copyright agreement for the Ptolemy II system.

Copyright (c) 1995-2013 The Regents of the University of California.
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

Ptolemy II includes the work of others, to see those copyrights, follow
the copyright link on the splash page or see copyright.htm.
*/
package ptolemy.domains.metroII.kernel;

import ptolemy.actor.Actor;
import ptolemy.actor.Director;
import ptolemy.actor.IOPort;
import ptolemy.actor.process.TerminateProcessException;
import ptolemy.data.Token;
import ptolemy.domains.pn.kernel.PNQueueReceiver;
import ptolemy.kernel.util.IllegalActionException;

// FIXME: Class Comment.  What does this class do?  Why is it necessary to subclass PNQueueReceiver.

public class MetroIIPNQueueReceiver extends PNQueueReceiver {

    // FIXME: FindBugs says:
    // "PNQueueReceiver.java:-1, MF_CLASS_MASKS_FIELD, Priority: Normal
    // Class defines field that masks a superclass field
    // This class defines a field with the same name as a visible
    // instance field in a superclass. This is confusing, and may
    // indicate an error if methods update or access one of the fields
    // when they wanted the other."
    // The issue here is that PNQueueReceiver has a _director field

    // FIXME: Put protected fields after public.
    /** The director in charge of this receiver. */
    protected MetroIIPNDirector _director;

    // FIXME: comment?
    public MetroIIPNDirector getDirector() {
        return _director;
    }

    // FIXME: comment?
    public Token get() {
        // FIXME: rename t to token.  We tend not to use single letter variable names except for things like i,j,k.
        Token t = super.get();
        try {
            _director.proposeMetroIIEvent(".get.end");
        } catch (InterruptedException e) {
            _terminate = true;
        }
        if (_terminate) {
            throw new TerminateProcessException(
                    "Interrupted when proposing MetroII events.");
        }
        return t;
    }

    // FIXME: comment?
    public void put(Token token) {
        try {
            _director.proposeMetroIIEvent(".put.begin");
        } catch (InterruptedException e) {
            _terminate = true;
        }
        if (_terminate) {
            throw new TerminateProcessException(
                    "Interrupted when proposing MetroII events.");
        }

        super.put(token);
    }

    // FIXME: comment?
    public void setContainer(IOPort port) throws IllegalActionException {
        super.setContainer(port);
        if (port == null) {
            _director = null;
        } else {
            Actor actor = (Actor) port.getContainer();
            Director director;

            // For a composite actor,
            // the receiver type of an input port is decided by
            // the executive director.
            // While the receiver type of an output is decided by the director.
            // NOTE: getExecutiveDirector() and getDirector() yield the same
            // result for actors that do not contain directors.
            if (port.isInput()) {
                director = actor.getExecutiveDirector();
            } else {
                director = actor.getDirector();
            }

            if (!(director instanceof MetroIIPNDirector)) {
                throw new IllegalActionException(port,
                        "Cannot use an instance of PNQueueReceiver "
                                + "since the director is not a PNDirector.");
            }

            _director = (MetroIIPNDirector) director;
        }
    }
}
