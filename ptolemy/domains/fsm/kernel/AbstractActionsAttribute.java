/* A base class for actions with semicolon delimitted lists of commands.

 Copyright (c) 2000 The Regents of the University of California.
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
@ProposedRating Yellow (eal@eecs.berkeley.edu)
@AcceptedRating Red (eal@eecs.berkeley.edu)
*/

package ptolemy.domains.fsm.kernel;

import ptolemy.actor.NoRoomException;
import ptolemy.data.expr.Variable;
import ptolemy.kernel.util.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

//////////////////////////////////////////////////////////////////////////
//// AbstractActionsAttribute
/**
A base class for actions with semicolon delimitted lists of commands.
<p>
The value of this attribute is a semicolon separated list of commands,
where each command gives a destination to send data to and a value
to send. The actions are given by calling setExpression() with
a string of the form:
<pre>
     <i>command</i>; <i>command</i>; ...
</pre>
where each <i>command</i> has the form:
<pre>
     <i>destination</i> = <i>expression</i>
</pre>
where <i>destination</i> is either
<pre>
     <i>name</i>
</pre>
or
<pre>
     <i>name</i>(<i>number</i>)
</pre>
<p>
The <i>expression</i> is a string giving an expression in the usual
Ptolemy II expression language.  The expression may include references
to variables and parameters contained by the FSM actor.

@author Xiaojun Liu and Edward A. Lee
@version $Id$
@see CommitActionsAttribute
@see Transition
@see FSMActor
*/
public abstract class AbstractActionsAttribute extends Action {

    /** Construct an action with the given name contained
     *  by the specified transition. The <i>transition</i> argument must not
     *  be null, or a NullPointerException will be thrown. This action will
     *  use the workspace of the transition for synchronization and
     *  version counts. If the name argument is null, then the name is
     *  set to the empty string. A variable for expression evaluation is
     *  created in the transition. The name of the variable is obtained
     *  by prepending an underscore to the name of this action.
     *  This increments the version of the workspace.
     *  @param transition The transition that contains this action.
     *  @param name The name of this action.
     *  @exception IllegalActionException If the action is not of an
     *   acceptable class for the container, or if the name contains
     *   a period.
     *  @exception NameDuplicationException If the transition already
     *   has an attribute with the name or that obtained by prepending
     *   an underscore to the name.
     */
    public AbstractActionsAttribute(Transition transition, String name)
            throws IllegalActionException, NameDuplicationException {
        super(transition, name);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** For each destination identified in an action, find the corresponding
     *  Ptolemy II object (a variable or a port).
     *  @exception IllegalActionException If a destination is not found.
     */
    public void execute() throws IllegalActionException {
        if (_destinationsNeedUpdating) {
            _updateDestinations();
        }
    }

    /** Set the action and notify the container
     *  that the action has changed by calling attributeChanged(),
     *  and notify any listeners that have
     *  been registered using addValueListener().
     *  @param expression The action.
     *  @exception IllegalActionException If the change is not acceptable
     *   to the container, or if the action is syntactically incorrect.
     */
    public void setExpression(String expression)
             throws IllegalActionException {
        super.setExpression(expression);

        // Initialize the lists that store the commands to be executed.
        _destinationNames = new LinkedList();
        _numbers = new LinkedList();
        _variables = new LinkedList();

        // Indicate that the _destinations list is invalid.  We defer
        // determination of the destinations because the destinations
        // may not have been created yet.
        _destinationsNeedUpdating = true;

        StringTokenizer commands = new StringTokenizer(expression, ";");
        while (commands.hasMoreTokens()) {
            String command = commands.nextToken();

            // Parse the command.
            int equalSign = command.indexOf("=");
            if (equalSign < 1 || equalSign >= command.length()-1) {
                throw new IllegalActionException(this,
                "Malformed action: expected destination=expression."
                + " Got: "
                + command);
            }

            // Parse the destination specification first.
            String completeDestinationSpec
                    = command.substring(0, equalSign).trim();
            int openParen = completeDestinationSpec.indexOf("(");
            if (openParen > 0) {
                // A channel is being specified.
                int closeParen = completeDestinationSpec.indexOf(")");
                if (closeParen < openParen) {
                    throw new IllegalActionException(this,
                    "Malformed action: expected destination=expression. "
                    + "Got: "
                    + command);
                }
                _destinationNames.add(
                        completeDestinationSpec.substring(
                                0, openParen).trim());
                String channelSpec = completeDestinationSpec.substring(
                        openParen + 1, closeParen);
                try {
                    _numbers.add(new Integer(channelSpec));
                } catch (NumberFormatException ex) {
                    throw new IllegalActionException(this,
                    "Malformed action: expected destination=expression. "
                    + "Got: "
                    + command);
                }
            } else {
                // No channel is specified.
                _destinationNames.add(completeDestinationSpec);
                _numbers.add(null);
            }

            // Next, create a variable in the transition to
            // evaluate this expression with the appropriate scope.
            Transition transition = (Transition)getContainer();
            // Arcane name for variable to ensure we don't get collisions.
            String variableName = "_action_"
                    + getName()
                    + "_"
                    + completeDestinationSpec.replace('(','_')
                    .replace(')','_');
            Variable evalVariable = null;
            if (transition == null) {
                // Create the variable in the workspace.
                evalVariable = new Variable(workspace());
                try {
                    evalVariable.setName(variableName);
                } catch (NameDuplicationException ex) {
                    throw new InternalErrorException(ex.toString());
                }
            } else {
                Attribute attr = transition.getAttribute(variableName);
                if (attr instanceof Variable) {
                    evalVariable = (Variable)attr;
                } else {
                    try {
                        if (attr != null) {
                            attr.setContainer(null);
                        }
                        evalVariable = new Variable(transition, variableName);
                    } catch (NameDuplicationException ex) {
                        throw new InternalErrorException(ex.toString());
                    }
                }
            }
            evalVariable.setExpression(
                    command.substring(equalSign + 1).trim());
            _variables.add(evalVariable);
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Given a destination name, return a NamedObj that matches that
     *  destination.  An implementation of this method should never return
     *  null (throw an exception instead).
     *  @param name The name of the destination, or null if none is found.
     *  @return An object (like a port or a variable) with the specified name.
     *  @exception IllegalActionException If the associated FSMActor
     *   does not have a destination with the specified name.
     */
    protected abstract NamedObj _getDestination(String name)
             throws IllegalActionException;

    ///////////////////////////////////////////////////////////////////
    ////                         protected variables               ////

    // List of channels.
    protected List _numbers;

    // List of destinations.
    protected List _destinations;

    // List of destination names.
    protected List _destinationNames;

    // List of variables.
    protected List _variables;

    // Indicator that the _destinations list needs updating.
    protected boolean _destinationsNeedUpdating = false;

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** For each destination in the _destinationNames list,
     *  create a corresponding
     *  entry in the _destinations list that refers to the destination.
     *  @exception IllegalActionException If the associated FSMActor
     *   does not have a destination with the specified name.
     */
    private void _updateDestinations() throws IllegalActionException {
        try {
            workspace().getReadAccess();
            FSMActor fsm = (FSMActor)getContainer().getContainer();
            _destinations = new LinkedList();
            if(_destinationNames != null) {
                Iterator destinationNames = _destinationNames.iterator();
                while(destinationNames.hasNext()) {
                    String destinationName = (String)destinationNames.next();
                    NamedObj destination = _getDestination(destinationName);
                    _destinations.add(destination);
                }
            }
            _destinationsNeedUpdating = false;
        } finally {
            workspace().doneReading();
        }
    }
}
