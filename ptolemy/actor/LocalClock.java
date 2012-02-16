/* A clock that keeps track of model time at a level of the model hierarchy.

 Copyright (c) 1999-2010 The Regents of the University of California.
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
package ptolemy.actor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import ptolemy.actor.util.Time;
import ptolemy.data.DoubleToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.util.AbstractSettableAttribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.ValueListener;
import ptolemy.kernel.util.Workspace;

/** A clock that keeps track of model time at a level of the model hierarchy
 *  and relates it to the time of the enclosing model, if there is one. The time
 *  of the enclosing model is referred to as the environment time. This
 *  clock has a notion of local time and committed time. The committed time 
 *  is "simultaneous" with the environment time.  
 *  
 *  The local time is
 *  not allowed to move backwards past the committed time, but ahead
 *  of that time, it can move around at will. 
 *  <p>
 *  There is no way of explicitly committing time, but 
 *  several methods have the side effect of committing the current
 *  local time. For example, {@link #setClockDrift(double)} will commit
 *  the current local time and change the clock drift.  So will
 *  {@link #start()} and {@link #stop()}
 *  <p>
 *  The value of the clock is exposed as an attribute that, by default, 
 *  is non editable. The clock drift is a contained attribute that can 
 *  be modified.
 *  FIXME: Setting of clock drift must be controlled because it commits
 *  time. 
 @author Ilge Akkaya, Patricia Derler, Edward A. Lee, Christos Stergiou, Michael Zimmer
 @version $Id$
 @since Ptolemy II 8.0
 @Pt.ProposedRating yellow (eal)
 @Pt.AcceptedRating red (eal)
 */

public class LocalClock extends AbstractSettableAttribute implements ValueListener {

    /** Construct an attribute with the given name contained by the specified
     *  entity. The container argument must not be null, or a
     *  NullPointerException will be thrown.  This attribute will use the
     *  workspace of the container for synchronization and version counts.
     *  If the name argument is null, then the name is set to the empty string.
     *  Increment the version of the workspace.
     *  @param container The container.
     *  @param name The name of this attribute.
     *  @exception IllegalActionException If the attribute is not of an
     *   acceptable class for the container, or if the name contains a period.
     *  @exception NameDuplicationException If the name coincides with
     *   an attribute already in the container.
     */
    public LocalClock(NamedObj container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        _init((Director)container);
    }
    
    /** The drift of the local clock with respect to the environment
     *  clock. If this is a top level director the clock drift has no
     *  consequence. The value is a double that is initialized to 
     *  1.0 which means that the local clock drift matches the one
     *  of the environment.
     */
    public Parameter clockDrift;

    ///////////////////////////////////////////////////////////////////
    ////                         public method                     ////
    
    /** Add a listener to be notified when the value of this variable changes.
     *  @param listener The listener to add.
     *  @see #removeValueListener(ValueListener)
     */
    public void addValueListener(ValueListener listener) {
        if (_valueListeners == null) {
            _valueListeners = new LinkedList();
        }

        if (!_valueListeners.contains(listener)) {
            _valueListeners.add(listener);
        }
    }
    
    /** Clone the object into the specified workspace for the specified
     *  director. 
     *  @param workspace The workspace for the cloned object.
     *  @param director The director for the cloned object.
     *  @return The cloned object.
     *  @throws CloneNotSupportedException Thrown by super class.
     */
    public Object clone(Workspace workspace, Director director) throws CloneNotSupportedException { 
        LocalClock newObject = (LocalClock) super.clone(workspace);
        newObject._director = director; 
        newObject._localTime = Time.NEGATIVE_INFINITY; 
        newObject._offset = _director._zeroTime;
        newObject._drift = 1.0;
        return newObject;
    }
    
    /** Get clock drift. 
     *  @return The clock drift.
     */
    public double getClockDrift() { 
        return _drift;
    }  
    
    /** Return the display name.
     *  @return The display name.
     */
    public String getDisplayName() {
        return "LocalClock";
    }
    
    /** Get the expression currently used by this settable. The expression
     *  is either the current value of local time.
     *  @return The current local time. 
     */
    public String getExpression() {
        return _localTime.toString();
    }
    
    /** Get the environment time that corresponds to the given local time.
     *  The given local time is required to be either equal to or
     *  greater than the committed time when this method is called.
     *  @param time The local Time.
     *  @return The corresponding environment Time.
     *  @throws IllegalActionException If the specified local time
     *   is in the past, or if Time objects cannot be created.
     */
    public Time getEnvironmentTimeForLocalTime(Time time) throws IllegalActionException {
        if (time.compareTo(_lastCommitLocalTime) < 0) {
            throw new IllegalActionException(
                    "Cannot compute environment time for local time " 
                    + time + " because "
                    + "the last commit of the local time occured at " 
                    + "local time " + _lastCommitLocalTime);
        } 
        Time localTimePassedSinceCommit = time.subtract(_lastCommitLocalTime);
        Time environmentTimePassedSinceCommit = localTimePassedSinceCommit;
        if (_drift != 1.0) {
            double environmentTimePassedSinceCommitDoubleValue = environmentTimePassedSinceCommit.getDoubleValue();
            environmentTimePassedSinceCommitDoubleValue =  environmentTimePassedSinceCommitDoubleValue / _drift;
            environmentTimePassedSinceCommit = new Time(_director, environmentTimePassedSinceCommitDoubleValue);
        }
        Time environmentTime = _lastCommitEnvironmentTime.add(environmentTimePassedSinceCommit); 
        return environmentTime;
    }
    
    /** Get current local time. If it has never been set, then this will return
     *  Time.NEGATIVE_INFINITY. The returned value may have been set by
     *  {@link #setLocalTime(Time)}.
     */
    public Time getLocalTime() {
        return _localTime;
    }
    
    /** Get the local time that corresponds to the current environment time.
     *  The current environment time is required to be greater than or equal
     *  to the environment time corresponding to the last committed local time.
     *  @return The corresponding local time.
     *  @throws IllegalActionException If Time objects cannot be created, or
     *   if the current environment time is less than the time
     *   corresponding to the last committed local time.
     */
    public Time getLocalTimeForCurrentEnvironmentTime() throws IllegalActionException {   
        return getLocalTimeForEnvironmentTime(_director.getEnvironmentTime()); 
    }
    
    /** Get the local time that corresponds to the given environment time.
     *  The given environment time is required to be greater than or equal
     *  to the environment time corresponding to the last committed local time.
     *  @param time The environment time.
     *  @return The corresponding local time.
     *  @throws IllegalActionException If the specified environment time
     *   is less than the environment time corresponding to the last
     *   committed local time, or if Time objects cannot be created.
     */
    public Time getLocalTimeForEnvironmentTime(Time time) throws IllegalActionException {
        if (time.compareTo(_lastCommitEnvironmentTime) < 0) {
            throw new IllegalActionException(
                    "Cannot compute local time for environment time " 
                    + time + " because "
                    + "the last commit of the local time occured at " 
                    + "local time " + _lastCommitLocalTime + " which " 
                    + "corresponds to environment time " 
                    + _lastCommitEnvironmentTime);
        } 
        
        Time environmentTimePassedSinceCommit = time.subtract(_lastCommitEnvironmentTime);
        Time localTimePassedSinceCommit = environmentTimePassedSinceCommit;
        if (_drift != 1.0) {
            double localTimePassedSinceCommitDoubleValue = environmentTimePassedSinceCommit.getDoubleValue();
            localTimePassedSinceCommitDoubleValue = localTimePassedSinceCommitDoubleValue * _drift;
            localTimePassedSinceCommit = new Time(_director, localTimePassedSinceCommitDoubleValue);
        }
        Time localTime = _lastCommitEnvironmentTime.subtract(_offset).add(localTimePassedSinceCommit);  
        return localTime;
    }
    
    /** Return visibility of local clock. If visibility hasn't been
     *  set it returns {@link Settable.NOT_EDITABLE}.
     */
    public Visibility getVisibility() {
        if (_visibility == null) {
            return Settable.NOT_EDITABLE;
        }
        return _visibility;
    }
    
    /** Remove a listener from the list of listeners that is
     *  notified when the value of this variable changes.  If no such listener
     *  exists, do nothing.
     *  @param listener The listener to remove.
     *  @see #addValueListener(ValueListener)
     */
    public void removeValueListener(ValueListener listener) {
        if (_valueListeners != null) {
            _valueListeners.remove(listener);
        }
    }
    
    /** Set local time without committing.
     *  This is allowed to set
     *  time earlier than the last committed local time.
     *  @param time The new local time. 
     */
    public void resetLocalTime(Time time) {
        _localTime = time;
    }
    
    /** Set the new clock drift but do not commit. The commit
     *  is done via {@link #commitClockDriftAndValue()}.
     *  @param drift New clock drift.  
     *  @throws IllegalActionException If the specified drift is
     *   non-positive.
     */
    public void setClockDrift(double drift) throws IllegalActionException {
        Time environmentTime = _director.getEnvironmentTime(); 
        if (drift <= 0.0) {
            throw new IllegalActionException(_director,
                    "Illegal clock drift: "
                    + drift
                    + ". Clock drift is required to be positive.");
        }
        _drift = drift;
        _commit();
    }
    
    /** Set local time without committing.
     *  This is not allowed to set
     *  time earlier than the last committed local time.
     *  @param time The new local time.
     *  @throws IllegalActionException If the specified time is
     *   earlier than the current time.
     */
    public void setLocalTime(Time time) throws IllegalActionException {
        if (_lastCommitLocalTime != null && time.compareTo(_lastCommitLocalTime) < 0) {
            throw new IllegalActionException(_director,
                    "Cannot set local time to "
                    + time
                    + ", which is earlier than the last committed current time "
                    + _lastCommitLocalTime);
        }
        _localTime = time; 
    }
    
    /** Set the visibility of this Settable. The argument should be one
     *  of the public static instances in Settable.
     *  @param visibility The visibility of this variable.
     *  @see #getVisibility()
     */
    public void setVisibility(Visibility visibility) {
        _visibility = visibility;
    }
    
    /** Start the clock with the current drift as specified by the
     *  last call to {@link #setClockDrift(double)}.
     *  If {@link #setClockDrift(double)} has never been called, then
     *  the drift is 1.0.
     *  This method commits current local time.
     */
    public void start() {
        _commit();
    }
    
    /** Stop the clock. The current time will remain the
     *  same as its current value until the next call to
     *  {@link #start()}.
     *  This method commits current local time.
     */
    public void stop() {
        _commit();
    } 
    
    /** FIXME: is there anything that needs to be done here?
     *  @return Nothing.
     */
    public Collection validate() throws IllegalActionException { 
        return null;
    }
    
    /** React to the change in the clock drift parameter.
     *  @param settable The object that has changed value.
     */
    public void valueChanged(Settable settable) {
        if (settable == clockDrift) {
            double drift;
            try {
                drift = ((DoubleToken) clockDrift.getToken()).doubleValue();
                if (drift != getClockDrift()) {
                    setClockDrift(drift);
                }
            } catch (IllegalActionException e) {
                // This should not happen. 
                e.printStackTrace();
            } 
        }
        
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////
    
    /** Commit the current local time. 
     */
    private void _commit() {
        // skip if local time has never been set.
        if (_localTime != Time.NEGATIVE_INFINITY) {
            Time environmentTime = _director.getEnvironmentTime();  
            _offset = environmentTime.subtract(_localTime);  
            _lastCommitEnvironmentTime = environmentTime; 
            _lastCommitLocalTime = _localTime;
        }
    }
    
    /** Create a local clock. 
     *  @param director The associated director.
     * @return 
     * @throws NameDuplicationException 
     * @throws IllegalActionException 
     */
    private void _init(Director director) throws IllegalActionException, NameDuplicationException {
        _director = director;
        
        // Make sure getCurrentTime() never returns null.
        _localTime = Time.NEGATIVE_INFINITY;
        
        _offset = _director._zeroTime;
        _drift = 1.0;
        
        clockDrift = new Parameter(this, "clockDrift");
        clockDrift.setExpression("1.0");
        clockDrift.setTypeEquals(BaseType.DOUBLE);
        clockDrift.addValueListener(this);
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                         private variable                  ////
    
    /** The current time of this clock. */
    private Time _localTime;
    
    /** The associated director. */
    private Director _director;
    
    /** The current clock drift.
     *  The drift is initialized to 1.0 which means that the
     *  local time matches to the environment time.
     */
    private double _drift;

    /** The environment time at which a change to local time, drift,
     *  or resumption occurred.
     */
    private Time _lastCommitEnvironmentTime;

    /** The local time at which a change to local time, drift,
     *  or resumption occurred.
     */
    private Time _lastCommitLocalTime;
    
    /** Name of the clock. */
    private String _name;
        
    /** The environment time minus the local time at the the point
     *  at which a commit occurred.
     *  By default, the offset is zero.
     */
    private Time _offset;
    
    /** Listeners for changes in value. */
    private List<ValueListener> _valueListeners;
    
    /** Visibility of local clock value. */
    private Visibility _visibility;

}
