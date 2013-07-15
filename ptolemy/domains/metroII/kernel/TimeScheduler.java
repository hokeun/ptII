/* TimeScheduler is a ConstraintSolver that handles the time quantity for MetroIIDirector. 

 Copyright (c) 2012-2013 The Regents of the University of California.
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

 */
package ptolemy.domains.metroII.kernel;


import ptolemy.domains.metroII.kernel.util.ProtoBuf.metroIIcomm.Event;
import ptolemy.domains.metroII.kernel.util.ProtoBuf.metroIIcomm.Event.Builder;
import ptolemy.domains.metroII.kernel.util.ProtoBuf.metroIIcomm.Event.Status;

///////////////////////////////////////////////////////////////////
////MetroIIActorGeneralWrapper

/** <p>
* TimeScheduler is a ConstraintSolver that handles the time quantity for MetroIIDirector.
* </p>
*
* @author Liangpeng Guo
* @version $Id$
* @since Ptolemy II 9.1
* @Pt.ProposedRating Red (glp)
* @Pt.AcceptedRating Red (glp)
*
*/
public class TimeScheduler implements ConstraintSolver, Cloneable {

    /**
     * Construct a time scheduler
     */
    public TimeScheduler() {
        initialize(); 
    }
    
    /**
     * Clone a time scheduler
     */
    public TimeScheduler clone() throws CloneNotSupportedException {
        TimeScheduler newObject = (TimeScheduler) super.clone();
        newObject._debugger = (MetroDebugger) _debugger.clone(); 
        return newObject;
    }

    /**
     * Initialize the current time value
     */
    public void initialize() {
        current_time = 0; 
    }

    /**
     * Turn on debugging printing
     */
    public void turnOnDebugging() {
        _debugger.turnOnDebugging();
    }

    /**
     * Turn off debugging printing
     */
    public void turnOffDebugging() {
        _debugger.turnOffDebugging();
    }
    
    /**
     * Resolve the time constraints. Notified only the next events with the most recent time tag.
     */
    @Override
    public void resolve(Iterable<Builder> metroIIEventList) {
        _debugger.printTitle("TimeScheduler Begins at Time " + current_time); 
        _debugger.printMetroEvents(metroIIEventList);

        long time = Long.MAX_VALUE;
        boolean hasEventWithoutTime = false;
        for (Builder event : metroIIEventList) {
            if (event.getStatus() == Status.PROPOSED) {
                if (event.hasTime()) {
                    if (event.getTime().getValue() < time) {
                        time = event.getTime().getValue();
                    }
                } else {
                    hasEventWithoutTime = true;
                }
            }
        }
        // System.out.println("Time Scheduler: " + (double) current_time
        //         / Double.valueOf("10000000000"));
        if (hasEventWithoutTime) {
            for (Builder event : metroIIEventList) {
                if (event.getStatus() == Status.PROPOSED) {
                    if (event.hasTime()) {
                        event.setStatus(Status.WAITING);
                    }
                }
            }
        } else {
            for (Builder event : metroIIEventList) {
                if (event.getStatus() == Status.PROPOSED) {
                    if (event.hasTime()) {
                        if (event.getTime().getValue() > time) {
                            event.setStatus(Status.WAITING);
                        }
                    }
                }
            }
        }

        // System.out.println("Time Scheduler: "+time); 
        for (Builder event : metroIIEventList) {
            if (event.getStatus() == Status.PROPOSED) {
                if (event.hasTime()) {
                    if (current_time < event.getTime().getValue()) {
                        current_time = event.getTime().getValue();
                    }
                }
            }
        }
        for (Builder event : metroIIEventList) {
            if (event.getStatus() == Status.PROPOSED) {
                if (!event.hasTime()) {
                    Event.Time.Builder builder = Event.Time.newBuilder();
                    builder.setValue(current_time); 
                    event.setTime(builder);
                }
            }
        }

        for (Builder event : metroIIEventList) {
            if (event.getStatus() == Status.PROPOSED) {
                event.setStatus(Status.NOTIFIED);
            }
        }
        
        _debugger.printMetroEvents(metroIIEventList);
        _debugger.printTitle("TimeScheduler Ends at Time " + current_time); 
    }

    ///////////////////////////////////////////////////////////////////
    ////                    private fields                         ////
    
    /**
     * debugger
     */
    private MetroDebugger _debugger = new MetroDebugger();

    /**
     * Current time value 
     */
    private long current_time;
    
}
