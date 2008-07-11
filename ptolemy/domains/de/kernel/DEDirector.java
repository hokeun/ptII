/* The DE domain director.

 Copyright (c) 1998-2008 The Regents of the University of California.
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
package ptolemy.domains.de.kernel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ptolemy.actor.Actor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.Director;
import ptolemy.actor.FiringEvent;
import ptolemy.actor.IOPort;
import ptolemy.actor.Receiver;
import ptolemy.actor.TimedDirector;
import ptolemy.actor.util.CausalityInterface;
import ptolemy.actor.util.Time;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.DebugListener;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.KernelException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// DEDirector

/**
 <p>This director implements the discrete-event (DE) model of computation (MoC).
 It should be used as the local director of a CompositeActor that is
 to be executed according to the DE MoC. This director maintains a totally
 ordered set of events and processes these events in the order defined on
 their tags and depths.
 </p><p>
 An event is associated with a tag, which is a tuple of timestamp and
 microstep. A timestamp indicates the model time when this event occurs. It
 is an object of the {@link ptolemy.actor.util.Time} class. A microstep is an
 integer which represents the index of the sequence of execution phases when
 this director processes events with the same timestamp. Two tags are equal
 if they have the same timestamp and microstep. If two events have the same
 tag, they are called simultaneous events.
 </p><p>
 Microsteps can only be increased by calling the fireAt() method. For example,
 when an actor requests to be fired again at the current model time, a
 new event with the same timestamp but a bigger microstep (incremented by 1)
 will be generated.
 </p><p>
 An event is also associated with a depth reflecting its priority, based
 on which a DE director chooses the execution order for simultaneous events.
 A depth is an integer and a larger value of depth indicates a lower priority.
 The depth of an event is determined by topologically sorting all the ports
 of actors according to their data dependencies over which there is no time
 delay.
 </p><p>
 The order of events is defined as follows. An event A is said to be earlier
 than another event B if A's timestamp is smaller than B's; or if A's
 timestamp is the same as B's, and A's microstep is smaller than B's; or if
 A's tag is the same as B's, and A's depth is smaller than B's. By giving
 events this well-defined order, this director can handle simultaneous events
 in a deterministic way.
 </p><p>
 The bottleneck in a typical DE simulator is in the maintenance of the
 global event queue. This director uses the calendar queue as the global
 event queue. This is an efficient algorithm with O(1) time complexity in
 both enqueue and dequeue operations. Sorting in the
 {@link ptolemy.actor.util.CalendarQueue} class is done according to the
 order defined above.
 </p><p>
 The complexity of the calendar algorithm is sensitive to the length of the
 event queue. When the size of the event queue becomes too long or changes
 very often, the simulation performance suffers from the penalties of queuing
 and dequeuing events. A few mechanisms are implemented to reduce such
 penalties by keeping the event queue short. The first mechanism is to only
 store in the event queue <i>pure</i> events and the <i>trigger</i> events
 with the same timestamp and microstep as those of the director. See
 {@link DEEvent} for explanation of these two types of events. What is more,
 no duplicate trigger events are allowed in the event queue. Another mechanism
 is that in a hierarchical model, each level keeps a local event queue.
 A lower level only reports the earliest event to its upper level
 to schedule a future firing. The last mechanism is to maintain a list which
 records all actors that are disabled. Any triggers sent to the actors in
 this list are discarded.
 </p><p>
 In the initialize() method, depths of actors and IO ports are statically
 analyzed and calculated. They are not calculated in the preinitialize()
 method because hierarchical models may change their structures during their
 preinitialize() method. For example, a modal model does not specify its
 initial state (and its refinement) until the end of its preinitialize()
 method. See {@link ptolemy.domains.fsm.kernel.FSMActor}. In order to support
 mutation, this director recalculates the depths at the beginning of its next
 iteration.
 </p><p>
 There are two types of depths: one is associated with IO ports, which
 reflects the order of trigger events; the other one is associated with
 actors, which is for pure events. The relationship between the depths of IO
 ports and actors is that the depth of an actor is the smallest of the depths
 of its IO ports. Pure events can only be produced by calling the fireAt()
 method, and trigger events can only be produced by actors that produce
 outputs. See {@link ptolemy.domains.de.kernel.DEReceiver#put(Token)}.
 </p><p>
 Directed loops of IO ports with no delay are not permitted because it is
 impossible to do a topological sort to assign depths. Such a loop can be
 broken by inserting some special actors, such as the <i>TimedDelay</i> actor.
 If zero delay in the loop is truly required, then set the <i>delay</i>
 parameter of those actors to zero. This zero-delay actor plays the same
 role as that of delta delay in VHDL. Note that the detection of directed
 loops are based on port connections rather than data dependencies between
 actors because port connections reflect the data dependencies more
 accurately. The information of port connections are stored in the
 nonpersistent attribute <i>FunctionDependency</i>.
 </p><p>
 An input port in a DE model contains an instance of DEReceiver.
 When a token is put into a DEReceiver, that receiver posts a trigger
 event to the director. This director sorts trigger events in a global event
 queue.
 </p><p>
 An iteration, in the DE domain, is defined as processing all the events
 whose tags are equal to the current tag of the director (also called the
 model tag). At the beginning of the fire() method, this director dequeues
 a subset of the earliest events (the ones with smallest timestamp, microstep,
 and depth) from the global event queue. These events have the same
 destination actor. Then, this director invokes that actor to iterate.
 This actor must consume tokens from its input port(s),
 and usually produces new events on its output port(s). These new events will
 be trigger the receiving actors to fire. It is important that the actor
 actually consumes tokens from its inputs, even if the tokens are solely
 used to trigger reactions. This is how polymorphic actors are used in the
 DE domain. The actor will be fired repeatedly until there are no more tokens
 in its input ports, or the actor returns false in its prefire() method. Then,
 this director keeps dequeuing and processing the earliest events from the
 event queue until no more events have the same tag as the model tag.
 After calling the postfire() method, this director finishes an iteration.
 This director is responsible to advance the model tag to perform another
 iteration.
 </p><p>
 A model starts from the time specified by <i>startTime</i>, which
 has default value 0.0. The stop time of the execution can be set
 using the <i>stopTime</i> parameter. The parameter has a default value
 <i>Infinity</i>, which means the execution runs forever.
 </p><p>
 Execution of a DE model ends when the timestamp of the earliest event
 exceeds the stop time. This stopping condition is checked inside
 the postfire() method of this director. By default, execution also ends
 when the global event queue becomes empty. Sometimes, the desired
 behaviour is for the director to wait on an empty queue until another
 thread makes new events available. For example, a DE actor may produce
 events when a user hits a button on the screen. To prevent ending the
 execution when there are no more events, set the
 <i>stopWhenQueueIsEmpty</i> parameter to <code>false</code>.
 </p><p>
 Parameters <i>isCQAdaptive</i>, <i>minBinCount</i>, and
 <i>binCountFactor</i>, are used to configure the calendar queue.
 Changes to these parameters are ignored when the model is running.
 </p><p>
 If the parameter <i>synchronizeToRealTime</i> is set to <code>true</code>,
 then the director will not process events until the real time elapsed
 since the model started matches the timestamp of the event.
 This ensures that the director does not get ahead of real time. However,
 of course, this does not ensure that the director keeps up with real time.
 </p><p>
 This director tolerates changes to the model during execution.
 The change should be queued with a component in the hierarchy using
 requestChange().  While invoking those changes, the method
 invalidateSchedule() is expected to be called, notifying the director
 that the topology it used to calculate the priorities of the actors
 is no longer valid.  This will result in the priorities (depths of actors)
 being recalculated the next time prefire() is invoked.</p>

 @author Lukito Muliadi, Edward A. Lee, Jie Liu, Haiyang Zheng
 @version $Id$
 @since Ptolemy II 0.2
 @Pt.ProposedRating Green (hyzheng)
 @Pt.AcceptedRating Yellow (hyzheng)
 */
public class DEDirector extends Director implements TimedDirector {
    /** Construct a director in the default workspace with an empty string
     *  as its name. The director is added to the list of objects in
     *  the workspace. Increment the version number of the workspace.
     */
    public DEDirector() {
        super();
        _initParameters();
    }

    /** Construct a director in the workspace with an empty name.
     *  The director is added to the list of objects in the workspace.
     *  Increment the version number of the workspace.
     *  @param workspace The workspace of this object.
     */
    public DEDirector(Workspace workspace) {
        super(workspace);
        _initParameters();
    }

    /** Construct a director in the given container with the given name.
     *  The container argument must not be null, or a
     *  NullPointerException will be thrown.
     *  If the name argument is null, then the name is set to the
     *  empty string. Increment the version number of the workspace.
     *  @param container Container of the director.
     *  @param name Name of this director.
     *  @exception IllegalActionException If the
     *   director is not compatible with the specified container.
     *  @exception NameDuplicationException If the container not a
     *   CompositeActor and the name collides with an entity in the container.
     */
    public DEDirector(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        _initParameters();
        // _verbose = true;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         parameters                        ////

    /** The factor when adjusting the bin number.
     *  This parameter must contain an IntToken.
     *  Changes to this parameter are ignored when the model is running.
     *  The value defaults to 2.
     */
    public Parameter binCountFactor;

    /** Specify whether the calendar queue adjusts its bin number
     *  at run time. This parameter must contain a BooleanToken.
     *  If this parameter is true, the calendar queue will adapt
     *  its bin number with respect to the distribution of events.
     *  Changes to this parameter are ignored when the model is running.
     *  The value defaults to true.
     */
    public Parameter isCQAdaptive;

    /** The minimum (initial) number of bins in the calendar queue.
     *  This parameter must contain an IntToken.
     *  Changes to this parameter are ignored when the model is running.
     *  The value defaults to 2.
     */
    public Parameter minBinCount;

    /** The start time of model. This parameter must contain a
     *  DoubleToken.  The value defaults to 0.0.
     */
    public Parameter startTime;

    /** The stop time of the model.  This parameter must contain a
     *  DoubleToken. The value defaults to Infinity.
     */
    public Parameter stopTime;

    /** Specify whether the execution stops when the queue is empty.
     *  This parameter must contain a
     *  BooleanToken. If this parameter is true, the
     *  execution of the model will be stopped when the queue is empty.
     *  The value defaults to true.
     */
    public Parameter stopWhenQueueIsEmpty;

    /** Specify whether the execution should synchronize to the
     *  real time. This parameter must contain a BooleanToken.
     *  If this parameter is true, then do not process events until the
     *  elapsed real time matches the time stamp of the events.
     *  The value defaults to false.
     */
    public Parameter synchronizeToRealTime;

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Append the specified listener to the current set of debug listeners.
     *  If an event queue has been created, register the listener to that queue.
     *  @param listener The listener to be added to the list of listeners
     *  to which debug messages are sent.
     *  @see #removeDebugListener(DebugListener)
     */
    public void addDebugListener(DebugListener listener) {
        if (_eventQueue != null) {
            _eventQueue.addDebugListener(listener);
        }

        super.addDebugListener(listener);
    }
    
    /** Update the director parameters when attributes are changed.
     *  Changes to <i>isCQAdaptive</i>, <i>minBinCount</i>, and
     *  <i>binCountFactor</i> parameters will only be effective on
     *  the next time when the model is executed.
     *  @param attribute The changed parameter.
     *  @exception IllegalActionException If the parameter set is not valid.
     *  Not thrown in this class.
     */
    public void attributeChanged(Attribute attribute)
            throws IllegalActionException {
        if (attribute == startTime) {
            double startTimeValue = ((DoubleToken) startTime.getToken())
                    .doubleValue();
            _startTime = new Time(this, startTimeValue);
        } else if (attribute == stopTime) {
            double stopTimeValue = ((DoubleToken) stopTime.getToken())
                    .doubleValue();
            _stopTime = new Time(this, stopTimeValue);
        } else if (attribute == stopWhenQueueIsEmpty) {
            _stopWhenQueueIsEmpty = ((BooleanToken) stopWhenQueueIsEmpty
                    .getToken()).booleanValue();
        } else if (attribute == synchronizeToRealTime) {
            _synchronizeToRealTime = ((BooleanToken) synchronizeToRealTime
                    .getToken()).booleanValue();
        } else {
            super.attributeChanged(attribute);
        }
    }

    /** Clone the object into the specified workspace. The new object is
     *  <i>not</i> added to the directory of that workspace (you must do this
     *  yourself if you want it there).
     *  The result is an attribute with no container.
     *  @param workspace The workspace for the cloned object.
     *  @exception CloneNotSupportedException Not thrown in this base class
     *  @return The new Attribute.
     */
    public Object clone(Workspace workspace) throws CloneNotSupportedException {
        DEDirector newObject = (DEDirector)super.clone(workspace);
        newObject._actorToDepth = null;
        newObject._disabledActors = null;
        newObject._eventQueue = null;
        newObject._exceedStopTime = false;
        newObject._isInitializing = false;
        newObject._microstep = 0;
        newObject._noMoreActorsToFire = false;
        newObject._portToDepth = null;
        newObject._realStartTime = 0;
        newObject._actorDepthVersion = -1;
        newObject._stopFireRequested = false;
        return newObject;
    }
    
    /** Return a string that describes the depths of actors and their ports.
     *  These depths are used to prioritize firings, where lower depths
     *  result in higher priorities.
     *  @exception IllegalActionException If there is a causality loop.
     */
    public String describePriorities() throws IllegalActionException {
        _computeActorDepth();
        StringBuffer result = new StringBuffer();
        List<Actor> actors = ((CompositeEntity)getContainer()).deepEntityList();
        for (Actor actor : actors) {
            result.append(actor.getFullName());
            result.append(": ");
            result.append(_actorToDepth.get(actor));
            result.append("\n");
            List<IOPort> ports = ((Entity)actor).portList();
            for (IOPort port : ports) {
                result.append("   ");
                result.append(port.getName());
                result.append(": ");
                result.append(_portToDepth.get(port));
                result.append("\n");
            }
        }
        return result.toString();
    }

    /** <p>Advance the current model tag to that of the earliest event in
     *  the event queue, and fire all actors that have requested or
     *  are triggered to be fired at the current tag. If
     *  <i>synchronizeToRealTime</i> is true, then before firing, wait
     *  until real time matches or exceeds the timestamp of the
     *  event. Note that the default unit for time is seconds.
     *  </p><p>
     *  Each actor is iterated repeatedly (prefire(), fire(), postfire()),
     *  until either it has no more input tokens, or its prefire() method
     *  returns false.
     *  </p><p>
     *  If there are no events in the event queue, then the behavior
     *  depends on the <i>stopWhenQueueIsEmpty</i> parameter. If it is
     *  false, then this thread will stall until events become
     *  available in the event queue. Otherwise, time will advance to
     *  the stop time and the execution will halt.</p>
     *
     *  @exception IllegalActionException If the firing actor throws it, or
     *  event queue is not ready, or an event is missed, or time is set
     *  backwards.
     */
    public void fire() throws IllegalActionException {
        // NOTE: This fire method does not call super.fire()
        // because this method is very different from that of the super class.
        // A BIG while loop that handles all events with the same tag.
        while (true) {
            // Find the next actor to be fired.
            Actor actorToFire = _getNextActorToFire();

            // Check whether the actor to be fired is null.
            // -- If the actor to be fired is null,
            // There are two conditions that the actor to be fired
            // can be null.
            if (actorToFire == null) {
                if (_isTopLevel()) {
                    // Case 1:
                    // If this director is an executive director at
                    // the top level, a null actor means that there are
                    // no events in the event queue.
                    if (_debugging) {
                        _debug("No more events in the event queue.");
                    }

                    // Setting the following variable to true makes the
                    // postfire method return false.
                    // Do not do this if _stopFireRequested is true,
                    // since there may in fact be actors to fire, but
                    // their firing has been deferred.
                    if (!_stopFireRequested) {
                        _noMoreActorsToFire = true;
                    }
                } else {
                    // Case 2:
                    // If this director belongs to an opaque composite model,
                    // which is not at the top level, the director may be
                    // invoked by an update of an external parameter port.
                    // Therefore, no actors contained by the composite model
                    // need to be fired.
                    // NOTE: There may still be events in the event queue
                    // of this director that are scheduled for future firings.
                    if (_debugging) {
                        _debug("No actor requests to be fired "
                                + "at the current tag.");
                    }
                }

                // Nothing more needs to be done in the current iteration.
                // Simply return.
                // Since we are now actually stopping the firing, we can set this false.
                _stopFireRequested = false;
                return;
            }

            // -- If the actor to be fired is not null.
            // If the actor to be fired is the container of this director,
            // the next event to be processed is in an inside receiver of
            // an output port of the container. In this case, this method
            // simply returns, and gives the outside domain a chance to react
            // to that event.
            // NOTE: Topological sort always assigns the composite actor the
            // lowest priority. This guarantees that all the inside actors
            // have fired (reacted to their triggers) before the composite
            // actor fires.
            if (actorToFire == getContainer()) {
                // Since we are now actually stopping the firing, we can set this false.
                _stopFireRequested = false;
                return;
            }

            if (_debugging) {
                _debug("DE director fires at " + getModelTime()
                        + "  with microstep as " + _microstep);
            }

            // Keep firing the actor to be fired until there are no more input
            // tokens available in any of its input ports, or its prefire()
            // method returns false.
            boolean refire;

            do {
                refire = false;

                // NOTE: There are enough tests here against the
                // _debugging variable that it makes sense to split
                // into two duplicate versions.
                if (_debugging) {
                    // Debugging. Report everything.
                    if (((Nameable) actorToFire).getContainer() == null) {
                        // If the actor to be fired does not have a container,
                        // it may just be deleted. Put this actor to the
                        // list of disabled actors.
                        _debug("Actor has no container. Disabling actor.");
                        _disableActor(actorToFire);
                        break;
                    }

                    _debug(new FiringEvent(this, actorToFire,
                            FiringEvent.BEFORE_PREFIRE));

                    if (!actorToFire.prefire()) {
                        _debug("*** Prefire returned false.");
                        break;
                    }

                    _debug(new FiringEvent(this, actorToFire,
                            FiringEvent.AFTER_PREFIRE));

                    _debug(new FiringEvent(this, actorToFire,
                            FiringEvent.BEFORE_FIRE));
                    actorToFire.fire();
                    _debug(new FiringEvent(this, actorToFire,
                            FiringEvent.AFTER_FIRE));

                    _debug(new FiringEvent(this, actorToFire,
                            FiringEvent.BEFORE_POSTFIRE));

                    if (!actorToFire.postfire()) {
                        _debug("*** Postfire returned false:",
                                ((Nameable) actorToFire).getName());

                        // This actor requests not to be fired again.
                        _disableActor(actorToFire);
                        break;
                    }

                    _debug(new FiringEvent(this, actorToFire,
                            FiringEvent.AFTER_POSTFIRE));
                } else {
                    // No debugging.
                    if (((Nameable) actorToFire).getContainer() == null) {
                        // If the actor to be fired does not have a container,
                        // it may just be deleted. Put this actor to the
                        // list of disabled actors.
                        _disableActor(actorToFire);
                        break;
                    }

                    if (!actorToFire.prefire()) {
                        break;
                    }

                    actorToFire.fire();

                    if (!actorToFire.postfire()) {
                        // This actor requests not to be fired again.
                        _disableActor(actorToFire);
                        break;
                    }
                }

                // Check all the input ports of the actor to see whether there
                // are more input tokens to be processed.
                Iterator inputPorts = actorToFire.inputPortList().iterator();

                while (inputPorts.hasNext() && !refire) {
                    IOPort port = (IOPort) inputPorts.next();

                    // iterate all the channels of the current input port.
                    for (int i = 0; i < port.getWidth(); i++) {
                        if (port.hasToken(i)) {
                            refire = true;

                            // Found a channel that has input data,
                            // jump out of the for loop.
                            break;
                        }
                    }
                }
            } while (refire); // close the do {...} while () loop
            // NOTE: On the above, it would be nice to be able to
            // check _stopFireRequested, but this doesn't actually work.
            // In particular, firing an actor may trigger a call to stopFire(),
            // for example if the actor makes a change request, as for example
            // an FSM actor will do.  This will prevent subsequent firings,
            // incorrectly.

            // The following code enforces that a firing of a
            // DE director only handles events with the same tag.
            // If the earliest event in the event queue is in the future,
            // this code terminates the current iteration.
            // This code is applied on both embedded and top-level directors.
            synchronized (_eventQueue) {
                if (!_eventQueue.isEmpty()) {
                    DEEvent next = _eventQueue.get();

                    if ((next.timeStamp().compareTo(getModelTime()) > 0)) {
                        // If the next event is in the future time,
                        // jump out of the big while loop and
                        // proceed to postfire().
                        // NOTE: we reset the microstep to 0 because it is
                        // the contract that if the event queue has some events
                        // at a time point, the first event must have the
                        // microstep as 0. See the
                        // _enqueueEvent(Actor actor, Time time) method.
                        _microstep = 0;
                        break;
                    } else if (next.microstep() > _microstep) {
                        // If the next event is has a bigger microstep,
                        // jump out of the big while loop and
                        // proceed to postfire().
                        break;
                    } else if ((next.timeStamp().compareTo(getModelTime()) < 0)
                            || (next.microstep() < _microstep)) {
                        throw new IllegalActionException(
                                "The tag of the next event ("
                                        + next.timeStamp() + "."
                                        + next.microstep()
                                        + ") can not be less than"
                                        + " the current tag (" + getModelTime()
                                        + "." + _microstep + ") !");
                    } else {
                        // The next event has the same tag as the current tag,
                        // indicating that at least one actor is going to be
                        // fired at the current iteration.
                        // Continue the current iteration.
                    }
                }
            }
        } // Close the BIG while loop.

        // Since we are now actually stopping the firing, we can set this false.
        _stopFireRequested = false;

        if (_debugging) {
            _debug("DE director fired!");
        }
    }

    /** Schedule an actor to be fired at the specified time by posting
     *  a pure event to the director. If the requested time is in the past
     *  relative to the current time, then replace it with the current
     *  model time.
     *  @param actor The scheduled actor to fire.
     *  @param time The scheduled time to fire.
     *  @exception IllegalActionException If event queue is not ready.
     */
    public void fireAt(Actor actor, Time time) throws IllegalActionException {
        if (_eventQueue == null) {
            throw new IllegalActionException(this,
                    "Calling fireAt() before preinitialize().");
        }
        if (_debugging) {
            _debug("DEDirector: Actor " + actor.getFullName()
                    + " requests refiring at " + time);
        }

        // We want to keep event queues at all levels in hierarchy
        // as short as possible. So, this pure event is not reported
        // to the higher level in hierarchy immediately. The postfire
        // method of this director is responsible to report the next
        // earliest event in the event queue to the higher level.
        synchronized (_eventQueue) {
            _enqueueEvent(actor, time);
            _eventQueue.notifyAll();
        }
        // If this is occurring between iterations within
        // an opaque composite actor, then postfire() will not
        // be invoked again to pass this fireAt() request up
        // the hierarchy to the executive director.  We
        // have to pass it up here.
        if (_delegateFireAt) {
            CompositeActor container = (CompositeActor) getContainer();
            if (_debugging) {
                _debug("DEDirector: Requests refiring of: "
                        + container.getName() + " at time " + time);
            }
            // Enqueue a pure event to fire the container of this director.
            container.getExecutiveDirector().fireAt(container, time);
        }
    }

    /** Schedule a firing of the given actor at the current time.
     *  @param actor The actor to be fired.
     *  @exception IllegalActionException If event queue is not ready.
     */
    public void fireAtCurrentTime(Actor actor) throws IllegalActionException {
        if (_eventQueue == null) {
            throw new IllegalActionException(this,
                    "Calling fireAt() before preinitialize().");
        }
        // This has to be synchronized at this level to make sure
        // that time does not advance between getModelTime() and
        // the enqueueing of the event.
        synchronized (_eventQueue) {
            fireAt(actor, getModelTime());
        }
    }

    /** Schedule an actor to be fired in the specified time relative to
     *  the current model time.
     *  @param actor The scheduled actor to fire.
     *  @param time The scheduled time to fire.
     *  @exception IllegalActionException If the specified time contains
     *  a negative time value, or event queue is not ready.
     */
    public void fireAtRelativeTime(Actor actor, Time time)
            throws IllegalActionException {
        fireAt(actor, time.add(getModelTime()));
    }

    /** Return the event queue. Note that this method is not synchronized.
     *  Any further accesses to this event queue needs synchronization.
     *  @return The event queue.
     */
    public DEEventQueue getEventQueue() {
        return _eventQueue;
    }

    /** Return the timestamp of the next event in the queue.
     *  The next iteration time, for example, is used to estimate the
     *  run-ahead time, when a continuous time composite actor is embedded
     *  in a DE model. If there is no event in the event queue, a positive
     *  infinity object is returned.
     *  @return The time stamp of the next event in the event queue.
     */
    public Time getModelNextIterationTime() {
        Time aFutureTime = Time.POSITIVE_INFINITY;

        // Record the model next iteration time as the tag of the the earliest
        // event in the queue.
        if (_eventQueue.size() > 0) {
            aFutureTime = _eventQueue.get().timeStamp();
        }

        // Iterate the event queue to find the earliest event with a bigger tag
        // ((either timestamp or microstop). If such an event exists,
        // use its time as the model next iteration time. If no such event
        // exists, it means that the model next iteration time still needs to
        // be resolved. In other words, the model next iteration time is
        // just the current time.
        Object[] events = _eventQueue.toArray();
        for (int i = 0; i < events.length; i++) {
            DEEvent event = (DEEvent) events[i];
            Time eventTime = event.timeStamp();
            int eventMicrostep = event.microstep();
            if (eventTime.compareTo(getModelTime()) > 0
                    || eventMicrostep > _microstep) {
                aFutureTime = eventTime;
                break;
            }
        }

        // Go through hierarchy to find the minimum step.
        Director executiveDirector = ((CompositeActor) getContainer())
                .getExecutiveDirector();
        if (executiveDirector != null) {
            Time aFutureTimeOfUpperLevel = executiveDirector
                    .getModelNextIterationTime();
            if (aFutureTime.compareTo(aFutureTimeOfUpperLevel) > 0) {
                aFutureTime = aFutureTimeOfUpperLevel;
            }
        }

        return aFutureTime;
    }

    /** Return the start time parameter value.
     *  @return the start time parameter value.
     */
    public final Time getModelStartTime() {
        // This method is final for performance reason.
        return _startTime;
    }

    /** Return the stop time parameter value.
     *  @return the stop time parameter value.
     */
    public final Time getModelStopTime() {
        // This method is final for performance reason.
        return _stopTime;
    }

    /** Return the system time at which the model begins executing.
     *  That is, the system time (in milliseconds) when the initialize()
     *  method of the director is called.
     *  The time is in the form of milliseconds counting
     *  from 1/1/1970 (UTC).
     *  @return The real start time of the model.
     */
    public long getRealStartTimeMillis() {
        return _realStartTime;
    }

    /** Return the start time parameter value.
     *  <p>
     *  When the start time is too big, the double representation loses
     *  the specified time resolution. To avoid this loss, use the
     *  {@link #getModelStartTime()} instead.</p>
     *  @return the start time.
     *  @deprecated As Ptolemy II 4.1, use {@link #getModelStartTime()}
     *  instead.
     */
    public final double getStartTime() {
        // This method is final for performance reason.
        return getModelStartTime().getDoubleValue();
    }

    /** Return the stop time.
     *  <p>
     *  When the stop time is too big, the double representation loses
     *  the specified time resolution. To avoid this loss, use the
     *  {@link #getModelStopTime()} instead.</p>
     *  @return the stop time.
     *  @deprecated As Ptolemy II 4.1, use {@link #getModelStopTime()}
     *  instead.
     */
    public final double getStopTime() {
        // This method is final for performance reason.
        return getModelStopTime().getDoubleValue();
    }

    /** Initialize all the contained actors by invoke the initialize() method
     *  of the super class. If any events are generated during the
     *  initialization, and the container is not at the top level, request a
     *  refiring.
     *  <p>
     *  The real start time of the model is recorded when this method
     *  is called. This method is <i>not</i> synchronized on the workspace,
     *  so the caller should be.</p>
     *
     *  @exception IllegalActionException If the initialize() method of
     *   the super class throws it.
     */
    public void initialize() throws IllegalActionException {
        _isInitializing = true;
        _eventQueue.clear();

        // Reset the following private variables.
        _disabledActors = null;
        _exceedStopTime = false;
        _microstep = 0;
        _noMoreActorsToFire = false;
        _realStartTime = System.currentTimeMillis();
        _stopFireRequested = false;

        super.initialize();

        // Register the stop time as an event such that the model is
        // guaranteed to stop at that time. This event also serves as
        // a guideline for an embedded Continuous model to know how much
        // further to integrate into future. But only do this if the
        // stop time is finite.
        if (!_stopTime.isPositiveInfinite()) {
            fireAt((Actor) getContainer(), _stopTime);
        }

        if (_isEmbedded() && !_eventQueue.isEmpty()) {
            // If the event queue is not empty and the container is not at
            // the top level, ask the upper level director in the
            // hierarchy to refire the container at the timestamp of
            // the earliest event of the local event queue.
            // This design allows the upper level director to keep a
            // relatively short event queue.
            _requestFiring();
            // Indicate that fireAt() request should be passed
            // up the chain if they are made before the next iteration.
            _delegateFireAt = true;
        } else {
            _delegateFireAt = false;
        }

        _isInitializing = false;
    }

    /** Indicate that the topological sort of the model may no longer be valid.
     *  This method should be called when topology changes are made.
     *  It sets a flag which will cause the topological
     *  sort to be redone next time when an event is enqueued.
     */
    public void invalidateSchedule() {
        _actorDepthVersion = -1;
    }

    /** Return a new receiver of the type DEReceiver.
     *  @return A new DEReceiver.
     */
    public Receiver newReceiver() {
        if (_debugging && _verbose) {
            _debug("Creating a new DE receiver.");
        }

        return new DEReceiver();
    }

    /** Return false if there are no more actors to be fired or the stop()
     *  method has been called. Otherwise, if the director is an embedded
     *  director and the local event queue is not empty, request the executive
     *  director to refire the container of this director at the timestamp of
     *  the first event in the event queue.
     *  @return True If this director will be fired again.
     *  @exception IllegalActionException If the postfire method of the super
     *  class throws it, or the stopWhenQueueIsEmpty parameter does not contain
     *  a valid token, or refiring can not be requested.
     */
    public boolean postfire() throws IllegalActionException {
        boolean result = super.postfire();
        boolean stop = ((BooleanToken) stopWhenQueueIsEmpty.getToken())
                .booleanValue();

        // There are two conditions to stop the model.
        // 1. There are no more actors to be fired (i.e. event queue is
        // empty), and either of the following conditions is satisfied:
        //     a. the stopWhenQueueIsEmpty parameter is set to true.
        //     b. the current model time equals the model stop time.
        // 2. The event queue is not empty, but the current time exceeds
        // the stop time.
        if (_noMoreActorsToFire
                && (stop || (getModelTime().compareTo(getModelStopTime()) == 0))) {
            _exceedStopTime = true;
            result = false;
        } else if (_exceedStopTime) {
            // If the current time is bigger than the stop time,
            // stop the model execution.
            result = false;
        } else if (_isEmbedded() && !_eventQueue.isEmpty()) {
            // If the event queue is not empty and the container is an
            // embedded model, ask the upper level director in the
            // hierarchy to refire the container at the timestamp of the
            // first event of the local event queue.
            // This design allows the upper level director (actually all
            // levels in hierarchy) to keep a relatively short event queue.
            _requestFiring();
        }
        if (_isEmbedded()) {
            // Indicate that fireAt() requests should be passed up the
            // hierarchy if they are made before the next iteration.
            _delegateFireAt = true;
        }
        // NOTE: The following commented block enforces that no events with
        // different tags can exist in the same receiver.
        // This is a quite different semantics from the previous designs,
        // and its effects are still under investigation and debate.
        //        // Clear all of the contained actor's input ports.
        //        for (Iterator actors = ((CompositeActor)getContainer())
        //                .entityList(Actor.class).iterator();
        //                actors.hasNext();) {
        //            Entity actor = (Entity)actors.next();
        //            Iterator ports = actor.portList().iterator();
        //            while (ports.hasNext()) {
        //                IOPort port = (IOPort)ports.next();
        //                if (port.isInput()) {
        //                    // Clear all receivers.
        //                    Receiver[][] receivers = port.getReceivers();
        //                    if (receivers == null) {
        //                        throw new InternalErrorException(this, null,
        //                                "port.getReceivers() returned null! "
        //                                + "This should never happen. "
        //                                + "port was '" + port + "'");
        //                    }
        //                    for (int i = 0; i < receivers.length; i++) {
        //                        Receiver[] receivers2 = receivers[i];
        //                        for (int j = 0; j < receivers2.length; j++) {
        //                            receivers2[j].clear();
        //                        }
        //                    }
        //                }
        //            }
        //        }
        return result;
    }

    /** Set the model timestamp to the outside timestamp if this director is
     *  not at the top level. Check the timestamp of the next event to decide
     *  whether to fire. Return true if there are inputs to this composite
     *  actor, or the timestamp of the next event is equal to the current model
     *  timestamp. Otherwise, return false.
     *  <p>
     *  Note that microsteps are not synchronized.
     *  </p><p>
     *  Throw an exception if the current model time is greater than the next
     *  event timestamp.
     *  @return True if the composite actor is ready to fire.
     *  @exception IllegalActionException If there is a missed event,
     *  or the prefire method of the super class throws it, or can not
     *  query the tokens of the input ports of the container of this
     *  director.</p>
     */
    public boolean prefire() throws IllegalActionException {
        // NOTE: The inside model does not need to have the same
        // microstep as that of the outside model (if it has one.)
        // However, an increment of microstep of the inside model must
        // trigger an increment of microstep of the outside model.
        // Set the model timestamp to the outside timestamp,
        // if this director is not at the top level.
        boolean result = super.prefire();

        if (_debugging) {
            _debug("Current time is: " + getModelTime());
        }

        // A top-level DE director is always ready to fire.
        if (_isTopLevel()) {
            if (_debugging) {
                _debug("Prefire returns: " + result);
            }
            return result;
        }

        // If embedded, check the timestamp of the next event to decide
        // whether this director is ready to fire.
        Time modelTime = getModelTime();
        Time nextEventTime = Time.POSITIVE_INFINITY;

        if (!_eventQueue.isEmpty()) {
            nextEventTime = _eventQueue.get().timeStamp();
        }

        // If the model time is larger (later) than the first event
        // in the queue, catch up with the current model time by discarding
        // the old events. This can occur, for example, if we are in a
        // modal model and this director was in an inactive mode before
        // we reached the time of the event.
        while (modelTime.compareTo(nextEventTime) > 0) {
            _eventQueue.take();

            if (!_eventQueue.isEmpty()) {
                nextEventTime = _eventQueue.get().timeStamp();
            } else {
                nextEventTime = Time.POSITIVE_INFINITY;
            }
        }

        // If model time is strictly less than the next event time,
        // then there are no events on the event queue with this
        // model time, and hence, if there are also no input events,
        // then there is nothing to do, and we can return false.
        if (!nextEventTime.equals(modelTime)) {
            // If the event timestamp is greater than the model timestamp,
            // we check if there's any external input.
            CompositeActor container = (CompositeActor) getContainer();
            Iterator inputPorts = container.inputPortList().iterator();
            boolean hasInput = false;

            while (inputPorts.hasNext() && !hasInput) {
                IOPort port = (IOPort) inputPorts.next();

                for (int i = 0; i < port.getWidth(); i++) {
                    if (port.hasToken(i)) {
                        hasInput = true;
                        break;
                    }
                }
            }

            if (!hasInput) {
                // If there is no internal event, it is not the correct
                // time to fire.
                // NOTE: This may happen because the container is statically
                // scheduled by its director to fire at this time.
                // For example, a DE model in a Giotto model.
                result = false;
            }
        }

        if (_debugging) {
            _debug("Prefire returns: " + result);
        }
        if (result) {
            _delegateFireAt = false;
        } else {
            _delegateFireAt = true;
        }
        return result;
    }

    /** Set the current timestamp to the model start time, invoke the
     *  preinitialize() methods of all actors deeply contained by the
     *  container.
     *  <p>
     *  This method should be invoked once per execution, before any
     *  iteration. Actors cannot produce output data in their preinitialize()
     *  methods. If initial events are needed, e.g. pure events for source
     *  actor, the actors should do so in their initialize() method.
     *  </p><p>
     *  This method is <i>not</i> synchronized on the workspace, so the
     *  caller should be.</p>
     *
     *  @exception IllegalActionException If the preinitialize() method of the
     *  container or one of the deeply contained actors throws it, or the
     *  parameters, minBinCount, binCountFactor, and isCQAdaptive, do not have
     *  valid tokens.
     */
    public void preinitialize() throws IllegalActionException {
        // Initialize an event queue.
        _eventQueue = new DECQEventQueue(((IntToken) minBinCount.getToken())
                .intValue(), ((IntToken) binCountFactor.getToken()).intValue(),
                ((BooleanToken) isCQAdaptive.getToken()).booleanValue());

        // Add debug listeners.
        if (_debugListeners != null) {
            Iterator listeners = _debugListeners.iterator();

            while (listeners.hasNext()) {
                DebugListener listener = (DebugListener) listeners.next();
                _eventQueue.addDebugListener(listener);
            }
        }

        // Call the preinitialize method of the super class.
        super.preinitialize();

        // Do this here so that performance measurements
        // clearly indicate that the cost is in static analysis
        // done in preinitialize.
        _computeActorDepth();
    }

    /** Unregister a debug listener.  If the specified listener has not
     *  been previously registered, then do nothing.
     *  @param listener The listener to remove from the list of listeners
     *   to which debug messages are sent.
     *  @see #addDebugListener(DebugListener)
     */
    public void removeDebugListener(DebugListener listener) {
        if (_eventQueue != null) {
            _eventQueue.removeDebugListener(listener);
        }

        super.removeDebugListener(listener);
    }

    /** Request the execution of the current iteration to stop.
     *  This is similar to stopFire(), except that the current iteration
     *  is not allowed to complete.  This is useful if there is actor
     *  in the model that has a bug where it fails to consume inputs.
     *  An iteration will never terminate if such an actor receives
     *  an event.
     *  If the director is paused waiting for events to appear in the
     *  event queue, then it stops waiting, and calls stopFire() for all actors
     *  that are deeply contained by the container of this director.
     */
    public void stop() {
        if (_eventQueue != null) {
            synchronized (_eventQueue) {
                _stopRequested = true;
                _eventQueue.notifyAll();
            }
        }

        super.stop();
    }

    /** Request the execution of the current iteration to complete.
     *  If the director is paused waiting for events to appear in the
     *  event queue, then it stops waiting,
     *  and calls stopFire() for all actors
     *  that are deeply contained by the container of this director.
     */
    public void stopFire() {
        if (_eventQueue != null) {
            synchronized (_eventQueue) {
                _stopFireRequested = true;
                _eventQueue.notifyAll();
            }
        }

        super.stopFire();
    }

    // FIXME: it is questionable whether the multirate FSMActor and FSMDirector
    // should be used in DE as the default? I will say NO.

    /** Return an array of suggested directors to use with
     *  ModalModel. Each director is specified by its full class
     *  name.  The first director in the array will be the default
     *  director used by a modal model.
     *  @return An array of suggested directors to be used with ModalModel.
     *  @see ptolemy.actor.Director#suggestedModalModelDirectors()
     */
    public String[] suggestedModalModelDirectors() {
        String[] defaultSuggestions = new String[2];
        defaultSuggestions[1] = "ptolemy.domains.fsm.kernel.MultirateFSMDirector";
        defaultSuggestions[0] = "ptolemy.domains.fsm.kernel.FSMDirector";
        return defaultSuggestions;
    }

    // NOTE: Why do we need an overridden transferOutputs method?
    // This director needs to transfer ALL output tokens at boundary of
    // hierarchy to outside. Without this overriden method, only one
    // output token is produced. See de/test/auto/transferInputsandOutputs.xml.
    // Do we need an overridden transferInputs method?
    // No. Because the DEDirector will keep firing an actor until it returns
    // false from its prefire() method, meaning that the actor has not enough
    // input tokens.

    /** Override the base class method to transfer all the available
     *  tokens at the boundary output port to outside.
     *  No data remains at the boundary after the model has been fired.
     *  This facilitates building multirate DE models.
     *  The port argument must be an opaque output port. If any channel
     *  of the output port has no data, then that channel is ignored.
     *
     *  @exception IllegalActionException If the port is not an opaque
     *   output port.
     *  @param port The port to transfer tokens from.
     *  @return True if data are transferred.
     */
    public boolean transferOutputs(IOPort port) throws IllegalActionException {
        boolean anyWereTransferred = false;
        boolean moreTransfersRemaining = true;

        while (moreTransfersRemaining) {
            moreTransfersRemaining = super.transferOutputs(port);
            anyWereTransferred |= moreTransfersRemaining;
        }

        return anyWereTransferred;
    }

    /** Invoke the wrapup method of the super class. Reset the private
     *  state variables.
     *  @exception IllegalActionException If the wrapup() method of
     *  one of the associated actors throws it.
     */
    public void wrapup() throws IllegalActionException {
        super.wrapup();
        _disabledActors = null;
        _eventQueue.clear();
        _noMoreActorsToFire = false;
        _microstep = 0;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Disable the specified actor.  All events destined to this actor
     *  will be ignored. If the argument is null, then do nothing.
     *  @param actor The actor to disable.
     */
    protected void _disableActor(Actor actor) {
        if (actor != null) {
            if (_debugging) {
                _debug("Actor ", ((Nameable) actor).getName(), " is disabled.");
            }

            if (_disabledActors == null) {
                _disabledActors = new HashSet();
            }

            _disabledActors.add(actor);
        }
    }

    /** Put a pure event into the event queue to schedule the given actor to
     *  fire at the specified timestamp.
     *  <p>
     *  The default microstep for the queued event is equal to zero,
     *  unless the time is equal to the current time, where the microstep
     *  will be the current microstep plus one.
     *  </p><p>
     *  The depth for the queued event is the minimum of the depths of
     *  all the ports of the destination actor.
     *  </p><p>
     *  If there is no event queue or the given actor is disabled, then
     *  this method does nothing.</p>
     *
     *  @param actor The actor to be fired.
     *  @param time The timestamp of the event.
     *  @exception IllegalActionException If the time argument is less than
     *  the current model time, or the depth of the actor has not be calculated,
     *  or the new event can not be enqueued.
     */
    protected void _enqueueEvent(Actor actor, Time time)
            throws IllegalActionException {
        if ((_eventQueue == null)
                || ((_disabledActors != null) && _disabledActors
                        .contains(actor))) {
            return;
        }

        // Adjust the microstep.
        int microstep = 0;

        if (time.compareTo(getModelTime()) == 0) {
            // If during initialization, do not increase the microstep.
            // This is based on the assumption that an actor only requests
            // one firing during initialization. In fact, if an actor requests
            // several firings at the same time,
            // only the first request will be granted.
            if (_isInitializing) {
                microstep = _microstep;
            } else {
                microstep = _microstep + 1;
            }
        } else if (time.compareTo(getModelTime()) < 0) {
            throw new IllegalActionException(actor,
                    "Attempt to queue an event in the past:"
                            + " Current time is " + getModelTime()
                            + " while event time is " + time);
        }

        int depth = _getDepthOfActor(actor);

        if (_debugging) {
            _debug("enqueue a pure event: ", ((NamedObj) actor).getName(),
                    "time = " + time + " microstep = " + microstep
                            + " depth = " + depth);
        }

        DEEvent newEvent = new DEEvent(actor, time, microstep, depth);
        _eventQueue.put(newEvent);
    }

    /** Put a trigger event into the event queue.
     *  <p>
     *  The trigger event has the same timestamp as that of the director.
     *  The microstep of this event is always equal to the current microstep
     *  of this director. The depth for the queued event is the
     *  depth of the destination IO port.
     *  </p><p>
     *  If the event queue is not ready or the actor contains the destination
     *  port is disabled, do nothing.</p>
     *
     *  @param ioPort The destination IO port.
     *  @exception IllegalActionException If the time argument is not the
     *  current time, or the depth of the given IO port has not be calculated,
     *  or the new event can not be enqueued.
     */
    protected void _enqueueTriggerEvent(IOPort ioPort)
            throws IllegalActionException {
        Actor actor = (Actor) ioPort.getContainer();

        if ((_eventQueue == null)
                || ((_disabledActors != null) && _disabledActors
                        .contains(actor))) {
            return;
        }

        int depth = _getDepthOfIOPort(ioPort);

        if (_debugging) {
            _debug("enqueue a trigger event for ",
                    ((NamedObj) actor).getName(), " time = " + getModelTime()
                            + " microstep = " + _microstep + " depth = "
                            + depth);
        }

        // Register this trigger event.
        DEEvent newEvent = new DEEvent(ioPort, getModelTime(), _microstep,
                depth);
        _eventQueue.put(newEvent);
    }

    /** Return the depth of an actor.
     *  @param actor An actor whose depth is requested.
     *  @return An integer indicating the depth of the given actor.
     *  @exception IllegalActionException If the actor depth has
     *   not been computed (this should not occur if the ioPort is under the control
     *   of this director).
     */
    protected int _getDepthOfActor(Actor actor) throws IllegalActionException {
        _computeActorDepth();
        Integer depth = _actorToDepth.get(actor);
        if (depth != null) {
            return depth.intValue();
        }
        throw new IllegalActionException("Attempt to get depth of actor "
                + ((NamedObj) actor).getFullName()
                + " that was not sorted.");
    }

    /** Return the depth of an ioPort.
     *  @param ioPort A port whose depth is requested.
     *  @return An integer representing the depth of the specified ioPort.
     *  @exception IllegalActionException If the ioPort does not have
     *   a depth (this should not occur if the ioPort is under the control
     *   of this director).
     */
    protected int _getDepthOfIOPort(IOPort ioPort) throws IllegalActionException {
        _computeActorDepth();
        Integer depth = _portToDepth.get(ioPort);

        if (depth != null) {
            return depth.intValue();
        }
        throw new IllegalActionException("Attempt to get depth of ioPort "
                + ((NamedObj) ioPort).getFullName()
                + " that was not sorted.");
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** Compute the depth of ports and actors.
     *  The actor depth is used to prioritize firings in response
     *  to pure events (fireAt() calls). The port depth is used
     *  to prioritize firings due to input events. Lower depths
     *  translate into higher priorities, with the lowest
     *  value being zero.
     *  The depth of an actor is the minimum depth of the output ports.
     *  This causes the actor to fire as early as possible
     *  to produce an output on those output ports in response
     *  to a pure event, but no earlier than the firings of
     *  actors that may source it data that the firing may
     *  depend on. If there are no output ports, then the depth of
     *  the actor it is the maximum depth of the input ports.
     *  This delays the response to fireAt() until all input
     *  events with the same tag have arrived.
     *  If there are no input ports or output ports, the depth is zero.
     *  @throws IllegalActionException If a zero-delay loop is found.
     */
    private void _computeActorDepth() throws IllegalActionException {
        if (_actorDepthVersion == workspace().getVersion()) {
            return;
        }
        _portToDepth = new HashMap<IOPort,Integer>();
        _actorToDepth = new HashMap<Actor,Integer>();
        
        // To ensure that the container actor has the lowest priority,
        // assign it the largest integer.
        CompositeEntity container = (CompositeEntity)getContainer();
        _actorToDepth.put((Actor)container, Integer.MAX_VALUE);
        
        // First iterate over all actors, ensuring that their
        // ports all have depths. Note that each time an actor
        // is visited, the depths of all upstream ports will be
        // set, so by the time we get to the end of the list
        // of actors, there will be nothing to do on them.
        // But we have to visit all of them to support disconnected
        // graphs. If the actors happen to be ordered in topological
        // sort order, then this will be quite efficient.
        // Otherwise, many actors will be visited twice.
        List<Actor> actors = container.deepEntityList();
        for (Actor actor : actors) {
            // If the actor already has a depth, skip it.
            Integer actorDepth = _actorToDepth.get(actor);
            if (actorDepth != null) {
                continue;
            }
            // First compute the depth of the input ports,
            // recording the maximum value.
            Integer maximumInputDepth = Integer.valueOf(0);
            Iterator inputPorts = actor.inputPortList().iterator();
            while (inputPorts.hasNext()) {
                IOPort inputPort = (IOPort)inputPorts.next();
                Integer inputPortDepth = _portToDepth.get(inputPort);
                if (inputPortDepth == null) {
                    // Keep track of ports that have been visited in
                    // to detect causality loops.
                    Set<IOPort> visited = new HashSet<IOPort>();
                    _computeInputDepth(inputPort, visited);
                    inputPortDepth = _portToDepth.get(inputPort);
                }
                // Record the maximum and continue to the next port.
                if (inputPortDepth.compareTo(maximumInputDepth) > 0) {
                    maximumInputDepth = inputPortDepth;
                }
            }
            
            // Next set the depth of the output ports.
            Integer minimumOutputDepth = null;
            Iterator outputPorts = actor.outputPortList().iterator();
            while (outputPorts.hasNext()) {
                IOPort outputPort = (IOPort)outputPorts.next();
                Integer outputPortDepth = _portToDepth.get(outputPort);
                if (outputPortDepth == null) {
                    // Keep track of ports that have been visited in
                    // to detect causality loops.
                    Set<IOPort> visited = new HashSet<IOPort>();
                    _computeOutputPortDepth(outputPort, visited);
                    outputPortDepth = _portToDepth.get(outputPort);
                }
                // Record the minimum and continue to the next port.
                if (minimumOutputDepth == null
                        || outputPortDepth.compareTo(minimumOutputDepth) < 0) {
                    minimumOutputDepth = outputPortDepth;
                }
            }
            
            // Finally, set the depth of the actor.
            if (minimumOutputDepth != null) {
                // There are output ports.
                _actorToDepth.put(actor, minimumOutputDepth);
            } else {
                _actorToDepth.put(actor, maximumInputDepth);
            }
        }
        // Next need to set the depth of all output ports of the
        // actor.
        List<IOPort> outputPorts = ((Actor)container).outputPortList();
        for (IOPort outputPort : outputPorts) {
            // The depth of each output port is one plus the maximum
            // depth of all the output ports (or input ports) that source
            // it data, or zero if there are no such ports.
            int depth = 0;
            List<IOPort> sourcePorts = outputPort.insideSourcePortList();
            for (IOPort sourcePort : sourcePorts) {
                Integer sourceDepth = _portToDepth.get(sourcePort);
                if (sourceDepth == null) {
                    if (sourcePort.isInput()) {
                        // This is another external input.
                        sourceDepth = Integer.valueOf(0);
                        _portToDepth.put(sourcePort, sourceDepth);
                    } else {
                        // Source port is an output (should this be checked?)
                        Set<IOPort> visited = new HashSet<IOPort>();
                        _computeOutputPortDepth(sourcePort, visited);
                        sourceDepth = _portToDepth.get(sourcePort);
                        if (sourceDepth == null) {
                            throw new InternalErrorException(
                                    "Failed to compute port depth for "
                                    + sourcePort.getFullName());
                        }
                    }
                }
                int newDepth = sourceDepth.intValue() + 1;
                if (newDepth > depth) {
                    depth = newDepth;
                }
            }
            _portToDepth.put(outputPort, Integer.valueOf(depth));
        }
        _actorDepthVersion = workspace().getVersion();
        // NOTE: Must call this after setting _actorDepthVersion.
        if (_debugging && _verbose) {
            _debug("## Depths assigned to actors and ports:");
            _debug(describePriorities());
        }
    }

    /** Compute the depth of the specified input port.
     *  The depth of an input port is maximum of the
     *  "source depths" of all the input ports in the
     *  same equivalence class with the specified port.
     *  An equivalence class is defined as follows.
     *  If ports X and Y each have a dependency not equal to the
     *  default depenency's oPlusIdentity(), then they
     *  are in an equivalence class. That is,
     *  there is a causal dependency. They are also in
     *  the same equivalence class if there is a port Z
     *  in an equivalence class with X and in an equivalence
     *  class with Y. Otherwise, they are not in the same
     *  equivalence class. If there are no
     *  output ports, then all the input ports
     *  are in a single equivalence class.
     *  <p>
     *  The "source depth" of an input port is one plus the maximum
     *  depth of all output ports that directly source data
     *  to it, or zero if there are no such ports.
     *  This delays the firing of this actor until
     *  all source actors have fired. As a side
     *  effect, this method also sets the depth of all the
     *  input ports in the same equivalence class, as well
     *  as all upstream ports up to a break with non-causal
     *  relationship. It also detects and reports dependency
     *  cycles.  The entry point
     *  to this method is _computeActorDepth().
     *  @see #_computeActorDepth()
     *  @param inputPort The port to compute the depth of.
     *  @param visited The set of ports that have been visited
     *   in this round.
     *  @throws IllegalActionException If a zero-delay loop is found.
     */
    private void _computeInputDepth(IOPort inputPort, Set<IOPort> visited)
             throws IllegalActionException {
        // NOTE: The definition of equivalence class, which comes
        // from CausalityInterface, is not quite what we want if
        // we use RealDependency instead of BooleanDependency
        // and represent metric delays.  In that case, we want
        // the equivalence class to include only ports that
        // have exactly zero delay dependency on some output port.
        // So if we switch to using RealDependency, this implementation
        // will have to change.
        int depth = 0;
        // Iterate over all the ports in the equivalence class.
        Actor actor = (Actor)inputPort.getContainer();
        CausalityInterface causality = actor.getCausalityInterface();
        Collection<IOPort> equivalentPorts = causality.equivalentPorts(inputPort);
        for (IOPort equivalentPort: equivalentPorts) {
            visited.add(equivalentPort);
            // Iterate over the source ports to compute the source depths.
            List<IOPort> sourcePorts = equivalentPort.sourcePortList();
            for (IOPort sourcePort : sourcePorts) {
                // NOTE: source port may be an input port, in which case
                // the port is an external input and should have depth 0.
                // The input port depends directly on this output port.
                // Find the depth of the output port.
                Integer sourcePortDepth = _portToDepth.get(sourcePort);
                if (sourcePortDepth == null) {
                    // Have to compute the depth of the output port.
                    if (visited.contains(sourcePort)) {
                        // Found a causality loop.
                        throw new IllegalActionException(this,
                                "Found a zero delay loop containing "
                                + actor.getFullName());
                    }
                    if (sourcePort.isInput()) {
                        // This had better be an external input.
                        // Should this be checked?
                        // Set the depth to zero.
                        sourcePortDepth = Integer.valueOf(0);
                        _portToDepth.put(sourcePort, sourcePortDepth);
                    } else {
                        // Source port is an output (should this be checked?)
                        _computeOutputPortDepth(sourcePort, visited);
                        sourcePortDepth = _portToDepth.get(sourcePort);
                        if (sourcePortDepth == null) {
                            throw new InternalErrorException(
                                    "Failed to compute port depth for "
                                    + sourcePort.getFullName());
                        }
                    }
                }
                // The depth needs to be one greater than any
                // output port it depends on.
                int newDepth = sourcePortDepth.intValue() + 1;
                if (depth < newDepth) {
                    depth = newDepth;
                }
            }
        }
        // We have now found the depth for the equivalence class.
        // Set it.
        for (IOPort equivalentPort: equivalentPorts) {
            _portToDepth.put(equivalentPort, Integer.valueOf(depth));
        }
    }

    /** Compute the depth of the specified output port.
     *  The depth of an output port is
     *  the maximum of the depth of all the input ports
     *  it directly depends on, or zero if there are no such
     *  input ports.  The entry point
     *  to this method is _computeActorDepth().
     *  @see #_computeActorDepth()
     *  @param outputPort The actor to compute the depth of.
     *  @param visited The set of ports that have been visited
     *   in this round.
     *  @throws IllegalActionException If a zero-delay loop is found.
     */
    private void _computeOutputPortDepth(IOPort outputPort, Set<IOPort> visited)
             throws IllegalActionException {
        visited.add(outputPort);
        int depth = 0;
        // Iterate over the input ports of the same actor that
        // this output directly depends on.
        Actor actor = (Actor)outputPort.getContainer();
        CausalityInterface causality = actor.getCausalityInterface();
        for (IOPort inputPort : causality.dependentPorts(outputPort)) {
            // The output port depends directly on this input port.
            // Find the depth of the input port.
            Integer inputPortDepth = _portToDepth.get(inputPort);
            if (inputPortDepth == null) {
                // Have to compute the depth of the input port.
                if (visited.contains(inputPort)) {
                    // Found a causality loop.
                    throw new IllegalActionException(this, actor,
                            "Found a zero delay loop containing "
                            + actor.getFullName());
                }
                // The following computes only the source depth,
                // which may not be the final depth. As a consequence,
                // _computeActorDepth(), the output depth that
                // we calculate here may need to be modified.
                _computeInputDepth(inputPort, visited);
                inputPortDepth = _portToDepth.get(inputPort);
                if (inputPortDepth == null) {
                    throw new InternalErrorException(
                            "Failed to compute port depth for "
                            + inputPort.getFullName());
                }
            }
            int newDepth = inputPortDepth.intValue();
            if (depth < newDepth) {
                depth = newDepth;
            }
        }
        _portToDepth.put(outputPort, Integer.valueOf(depth));
    }
    
    /** Dequeue the events that have the smallest tag from the event queue.
     *  Return their destination actor. Advance the model tag to their tag.
     *  If the timestamp of the smallest tag is greater than the stop time
     *  then return null. If there are no events in the event queue, and
     *  the stopWhenQueueIsEmpty parameter is set to true, then return null.
     *  Both cases will have the effect of stopping the simulation.
     *  <p>
     *  If the stopWhenQueueIsEmpty parameter is false and the queue is empty,
     *  then stall the current thread by calling wait() on the _eventQueue
     *  until there are new events available.  If the synchronizeToRealTime
     *  parameter is true, then this method may suspend the calling thread
     *  by using Object.wait(long) to let elapsed real time catch up with the
     *  current model time.</p>
     *  @return The next actor to be fired, which can be null.
     *  @exception IllegalActionException If event queue is not ready, or
     *  an event is missed, or time is set backwards.
     */
    private Actor _getNextActorToFire() throws IllegalActionException {
        if (_eventQueue == null) {
            throw new IllegalActionException(
                    "Fire method called before the preinitialize method.");
        }

        Actor actorToFire = null;
        DEEvent lastFoundEvent = null;
        DEEvent nextEvent = null;

        // Keep taking events out until there are no more events that have the
        // same tag and go to the same destination actor, or until the queue is
        // empty, or until a stop is requested.
        // LOOPLABEL::GetNextEvent
        while (!_stopRequested) {
            // Get the next event from the event queue.
            if (_stopWhenQueueIsEmpty) {
                if (_eventQueue.isEmpty()) {
                    // If the event queue is empty,
                    // jump out of the loop: LOOPLABEL::GetNextEvent
                    break;
                }
            }

            if (!_isTopLevel()) {
                // If the director is not at the top level.
                if (_eventQueue.isEmpty()) {
                    // This could happen if the container simply fires
                    // this composite at times it chooses. Most directors
                    // do this (SDF, SR, Continuous, etc.). It can also
                    // happen if an input is provided to a parameter port
                    // and the container is DE.
                    // In all these cases, no actors inside need to be
                    // fired.
                    break;
                }
                // For an embedded DE director, the following code prevents
                // the director from reacting to future events with bigger
                // time values in their tags.
                // For a top-level DE director, there is no such constraint
                // because the top-level director is responsible to advance
                // simulation by increasing the model tag.
                nextEvent = _eventQueue.get();

                // An embedded director should process events
                // that only happen at the current tag.
                // If the event is in the past, that is an error,
                // because the event should have been consumed in prefire().
                if ((nextEvent.timeStamp().compareTo(getModelTime()) < 0)) {
                    // missed an event
                    throw new IllegalActionException(
                            "Fire: Missed an event: the next event tag "
                            + nextEvent.timeStamp()
                            + " :: "
                            + nextEvent.microstep()
                            + " is earlier than the current model tag "
                            + getModelTime() + " :: " + _microstep
                            + " !");
                }

                // If the event is in the future time, it is ignored
                // and will be processed later.
                // Note that it is fine for the new event to have a bigger
                // microstep. This indicates that the embedded director is
                // going to advance microstep.
                // Note that conceptually, the outside and inside DE models
                // share the same microstep and the current design and
                // implementation assures that. However, the embedded DE
                // director does ask for the microstep of the upper level
                // DE director. They keep their own count of their
                // microsteps. The reason for this is to avoid the
                // difficulties caused by passing information across modal
                // model layers.
                if ((nextEvent.timeStamp().compareTo(getModelTime()) > 0)) {
                    // reset the next event
                    nextEvent = null;

                    // jump out of the loop: LOOPLABEL::GetNextEvent
                    break;
                }
            } else { // if (!topLevel)
                // If the director is at the top level
                // If the event queue is empty, normally
                // a blocking read is performed on the queue.
                // However, there are two conditions that the blocking
                // read is not performed, which are checked below.
                if (_eventQueue.isEmpty()) {
                    // The two conditions are:
                    // 1. An actor to be fired has been found; or
                    // 2. There are no more events in the event queue,
                    // and the current time is equal to the stop time.
                    if ((actorToFire != null)
                            || (getModelTime().equals(getModelStopTime()))) {
                        // jump out of the loop: LOOPLABEL::GetNextEvent
                        break;
                    }
                }

                // Otherwise, if the event queue is empty,
                // a blocking read is performed on the queue.
                // stopFire() needs to also cause this to fall out!
                while (_eventQueue.isEmpty() && !_stopRequested
                        && !_stopFireRequested) {
                    if (_debugging) {
                        _debug("Queue is empty. Waiting for input events.");
                    }

                    Thread.yield();

                    synchronized (_eventQueue) {
                        // Need to check _stopFireRequested again inside
                        // the synchronized block, because it may have changed.
                        if (_eventQueue.isEmpty() && !_stopRequested
                                && !_stopFireRequested) {
                            try {
                                // NOTE: Release the read access held
                                // by this thread to prevent deadlocks.
                                // NOTE: If a ChangeRequest has been requested,
                                // then _eventQueue.notifyAll() is called
                                // and stopFire() is called, so we will stop
                                // waiting for events. However,
                                // CompositeActor used to call stopFire() before
                                // queuing the change request, which created the risk
                                // that the below wait() would be terminated by
                                // a notifyAll() on _eventQueue with _stopFireRequested
                                // having been set, but before the change request has
                                // actually been filed.  See CompositeActor.requestChange().
                                // Does this matter? It means that on the next invocation
                                // of the fire() method, we could resume waiting on an empty queue
                                // without having filed the change request. That filing will
                                // no longer succeed in interrupting this wait, since
                                // stopFire() has already been called. Only on the next
                                // instance of change request would the first change
                                // request get a chance to execute.
                                workspace().wait(_eventQueue);
                            } catch (InterruptedException e) {
                                // If the wait is interrupted,
                                // then stop waiting.
                                break;
                            }
                        }
                    } // Close synchronized block
                } // Close the blocking read while loop

                // To reach this point, either the event queue is not empty,
                // or _stopRequested or _stopFireRequested is true, or an interrupted exception
                // happened.
                if (_eventQueue.isEmpty()) {
                    // Stop is requested or this method is interrupted.
                    // This can occur, for example, if a change has been requested.
                    // jump out of the loop: LOOPLABEL::GetNextEvent
                    return null;
                }
                // At least one event is found in the event queue.
                nextEvent = _eventQueue.get();
            }

            // This is the end of the different behaviors of embedded and
            // top-level directors on getting the next event.
            // When this point is reached, the nextEvent can not be null.
            // In the rest of this method, this is not checked any more.

            // If the actorToFire is null, find the destination actor associated
            // with the event just found. Store this event as lastFoundEvent and
            // go back to continue the GetNextEvent loop.
            // Otherwise, check whether the event just found goes to the
            // same actor to be fired. If so, dequeue that event and continue
            // the GetNextEvent loop. Otherwise, jump out of the GetNextEvent
            // loop.
            // TESTIT
            if (actorToFire == null) {
                // If the actorToFire is not set yet,
                // find the actor associated with the event just found,
                // and update the current tag with the event tag.
                Time currentTime;

                if (_synchronizeToRealTime) {
                    // If synchronized to the real time.
                    synchronized (_eventQueue) {
                        while (!_stopRequested && !_stopFireRequested) {
                            lastFoundEvent = _eventQueue.get();
                            currentTime = lastFoundEvent.timeStamp();

                            long elapsedTime = System.currentTimeMillis()
                                    - _realStartTime;

                            // NOTE: We assume that the elapsed time can be
                            // safely cast to a double.  This means that
                            // the DE domain has an upper limit on running
                            // time of Double.MAX_VALUE milliseconds.
                            double elapsedTimeInSeconds = elapsedTime / 1000.0;
                            ptolemy.actor.util.Time elapsed
                                    = new ptolemy.actor.util.Time(this, elapsedTimeInSeconds);
                            if (currentTime.compareTo(elapsed) <= 0) {
                                break;
                            }
                            
                            // NOTE: We used to do the following, but it had a limitation.
                            // In particular, if any user code also calculated the elapsed
                            // time and then constructed a Time object to post an event
                            // on the event queue, there was no assurance that the quantization
                            // would be the same, and hence it was possible for that event
                            // to be in the past when posted, even if done in the same thread.
                            // To ensure that the comparison of current time against model time
                            // always yields the same result, we have to do the comparison using
                            // the Time class, which is what the event queue does.
                            /*
                            if (currentTime.getDoubleValue() <= elapsedTimeInSeconds) {
                                break;
                            }*/

                            long timeToWait = (long) (currentTime.subtract(
                                    elapsed).getDoubleValue() * 1000.0);

                            if (timeToWait > 0) {
                                if (_debugging) {
                                    _debug("Waiting for real time to pass: "
                                            + timeToWait);
                                }

                                try {
                                    // NOTE: The built-in Java wait() method
                                    // does not release the
                                    // locks on the workspace, which would block
                                    // UI interactions and may cause deadlocks.
                                    // SOLUTION: workspace.wait(object, long).
                                    _workspace.wait(_eventQueue, timeToWait);
                                } catch (InterruptedException ex) {
                                    // Continue executing?
                                    // No, because this could be a problem if any
                                    // actor assumes that model time always exceeds
                                    // real time when synchronizeToRealTime is set.
                                    throw new IllegalActionException(this, ex,
                                            "Thread interrupted when waiting for" +
                                            " real time to match model time.");
                                }
                            }
                        } // while
                        // If stopFire() has been called, then the wait for real
                        // time above was interrupted by a change request. Hence,
                        // real time will not have reached the time of the first
                        // event in the event queue. If we allow this method to
                        // proceed, it will set model time to that event time,
                        // which is in the future. This violates the principle
                        // of synchronize to real time.  Hence, we must return
                        // without processing the event or incrementing time.
                        
                        // NOTE: CompositeActor used to call stopFire() before
                        // queuing the change request, which created the risk
                        // that the above wait() would be terminated by
                        // a notifyAll() on _eventQueue with _stopFireRequested
                        // having been set, but before the change request has
                        // actually been filed.  See CompositeActor.requestChange().
                        // Does this matter? It means that on the next invocation
                        // of the fire() method, we could resume processing the
                        // same event, waiting for real time to elapse, without
                        // having filed the change request. That filing will
                        // no longer succeed in interrupting this wait, since
                        // stopFire() has already been called. Alternatively,
                        // before we get to the wait for real time in the next
                        // firing, the change request could complete and be
                        // executed.
                        if (_stopRequested || _stopFireRequested) {
                            return null;
                        }
                    } // sync
                } // if (_synchronizeToRealTime)

                // Consume the earliest event from the queue. The event must be
                // obtained here, since a new event could have been enqueued
                // into the queue while the queue was waiting. Note however
                // that this would usually be an error. Any other thread that
                // posts events in the event queue should do so in a change request,
                // which will not be executed during the above wait.
                // Nonetheless, we are conservative here, and take the earliest
                // event in the event queue.
                synchronized (_eventQueue) {
                    lastFoundEvent = _eventQueue.take();
                    currentTime = lastFoundEvent.timeStamp();
                    actorToFire = lastFoundEvent.actor();

                    // NOTE: The _enqueueEvent method discards the events
                    // for disabled actors.
                    if ((_disabledActors != null)
                            && _disabledActors.contains(actorToFire)) {
                        // This actor has requested not to be fired again.
                        if (_debugging) {
                            _debug("Skipping disabled actor: ",
                                    ((Nameable) actorToFire).getFullName());
                        }

                        actorToFire = null;

                        // start a new iteration of the loop:
                        // LOOPLABEL::GetNextEvent
                        continue;
                    }

                    // Advance the current time to the event time.
                    // NOTE: This is the only place that the model time changes.
                    setModelTime(currentTime);

                    // Advance the current microstep to the event microstep.
                    _microstep = lastFoundEvent.microstep();
                }

                // Exceeding stop time means the current time is strictly
                // bigger than the model stop time.
                if (currentTime.compareTo(getModelStopTime()) > 0) {
                    if (_debugging) {
                        _debug("Current time has passed the stop time.");
                    }

                    _exceedStopTime = true;
                    return null;
                }
            } else { // i.e., actorToFire != null
                // In a previous iteration of this while loop,
                // we have already found an event and the actor to react to it.
                // Check whether the newly found event has the same tag
                // and destination actor. If so, they are 
                // handled at the same time. For example, a pure
                // event and a trigger event that go to the same actor.
                if (nextEvent.hasTheSameTagAs(lastFoundEvent)
                        && nextEvent.actor() == actorToFire) {
                    // Consume the event from the queue and discard it.
                    // In theory, there should be no event with the same depth
                    // as well as tag because
                    // the DEEvent class equals() method returns true in this
                    // case, and the CalendarQueue class does not enqueue an
                    // event that is equal to one already on the queue.
                    // Note that the Repeat actor, for one, produces a sequence
                    // of outputs, each of which will have the same microstep.
                    // These reduce to a single event in the event queue.
                    // The DEReceiver in the downstream port, however,
                    // contains multiple tokens. When the one event on
                    // event queue is encountered, then the actor will
                    // be repeatedly fired until it has no more input tokens.
                    // However, there could be events with the same tag
                    // and different depths, e.g. a trigger event and a pure
                    // event going to the same actor.
                    _eventQueue.take();
                } else {
                    // Next event has a future tag or a different destination.
                    break;
                }
            }
        } // close the loop: LOOPLABEL::GetNextEvent

        // Note that the actor to be fired can be null.
        return actorToFire;
    }

    /** initialize parameters. Set all parameters to their default values.
     */
    private void _initParameters() {
        try {
            startTime = new Parameter(this, "startTime");
            startTime.setExpression("0.0");
            startTime.setTypeEquals(BaseType.DOUBLE);

            stopTime = new Parameter(this, "stopTime");
            stopTime.setExpression("Infinity");
            stopTime.setTypeEquals(BaseType.DOUBLE);

            stopWhenQueueIsEmpty = new Parameter(this, "stopWhenQueueIsEmpty");
            stopWhenQueueIsEmpty.setExpression("true");
            stopWhenQueueIsEmpty.setTypeEquals(BaseType.BOOLEAN);

            synchronizeToRealTime = new Parameter(this, "synchronizeToRealTime");
            synchronizeToRealTime.setExpression("false");
            synchronizeToRealTime.setTypeEquals(BaseType.BOOLEAN);

            isCQAdaptive = new Parameter(this, "isCQAdaptive");
            isCQAdaptive.setExpression("true");
            isCQAdaptive.setTypeEquals(BaseType.BOOLEAN);
            isCQAdaptive.setVisibility(Settable.EXPERT);

            minBinCount = new Parameter(this, "minBinCount");
            minBinCount.setExpression("2");
            minBinCount.setTypeEquals(BaseType.INT);
            minBinCount.setVisibility(Settable.EXPERT);

            binCountFactor = new Parameter(this, "binCountFactor");
            binCountFactor.setExpression("2");
            binCountFactor.setTypeEquals(BaseType.INT);
            binCountFactor.setVisibility(Settable.EXPERT);

            timeResolution.setVisibility(Settable.FULL);
            timeResolution.moveToLast();
        } catch (KernelException e) {
            throw new InternalErrorException("Cannot set parameter:\n"
                    + e.getMessage());
        }
    }

    /** Request that the container of this director be refired in some
     *  future time specified by the first event of the local event queue.
     *  This method is used when the director is embedded inside an opaque
     *  composite actor. If the queue is empty, then throw an
     *  IllegalActionException.
     *  @exception IllegalActionException If the queue is empty.
     */
    private void _requestFiring() throws IllegalActionException {
        DEEvent nextEvent = null;
        nextEvent = _eventQueue.get();

        CompositeActor container = (CompositeActor) getContainer();

        if (_debugging) {
            _debug("DEDirector: Requests refiring of: " + container.getName()
                    + " at time " + nextEvent.timeStamp());
        }

        // Enqueue a pure event to fire the container of this director.
        container.getExecutiveDirector().fireAt(container,
                nextEvent.timeStamp());
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    /** Workspace version when actor depth was last computed. */
    private long _actorDepthVersion = -1;
    
    /** A table giving the depths of actors. */
    private Map<Actor,Integer> _actorToDepth = null;

    /** Indicator that calls to fireAt() should be delegated
     *  to the executive director.
     */
    private boolean _delegateFireAt = false;

    /** The set of actors that have returned false in their postfire()
     *  methods. Events destined for these actors are discarded and
     *  the actors are  never fired.
     */
    private Set _disabledActors;

    /** The queue used for sorting events. */
    private DEEventQueue _eventQueue;

    /** Set to true when the time stamp of the token to be dequeue
     *  has exceeded the stopTime.
     */
    private boolean _exceedStopTime = false;

    /** A local boolean variable indicating whether this director is in
     *  initialization phase execution.
     */
    private boolean _isInitializing = false;

    /** The current microstep. */
    private int _microstep = 0;

    /** Set to true when it is time to end the execution. */
    private boolean _noMoreActorsToFire = false;

    /** A table giving the depths of ports. */
    private Map<IOPort,Integer> _portToDepth = null;

    /** The real time at which the model begins executing. */
    private long _realStartTime = 0;

    /** Start time. */
    private Time _startTime;

    /** Flag that stopFire() has been called. */
    private boolean _stopFireRequested = false;

    /** Stop time. */
    private Time _stopTime;

    /** Decide whether the simulation should be stopped when there's no more
     *  events in the global event queue. By default, its value is 'true',
     *  meaning that the simulation will stop under that circumstances.
     *  Setting it to 'false', instruct the director to wait on the queue
     *  while some other threads might enqueue events in it.
     */
    private boolean _stopWhenQueueIsEmpty = true;

    /** Specify whether the director should wait for elapsed real time to
     *  catch up with model time.
     */
    private boolean _synchronizeToRealTime;
}
