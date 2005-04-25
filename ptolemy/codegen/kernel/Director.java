/* Code generator helper class associated with the Director class.

Copyright (c) 2005 The Regents of the University of California.
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
package ptolemy.codegen.kernel;

import ptolemy.actor.Actor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.IOPort;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


//////////////////////////////////////////////////////////////////
//// Director

/**
   Code generator helper associated with the Director class. This class
   is also associated with a code generator.

   @see CodeGenerator
   @author Ye Zhou
   @version $Id$
   @since Ptolemy II 5.0,
   @Pt.ProsedRating Red (zhouye)
   @Pt.AcceptedRating Red (zhouye)

*/
public class Director implements ActorCodeGenerator {
    /** Construct the code generator helper associated with the given director.
     *  @param director The associated director.
     */
    public Director(ptolemy.actor.Director director) {
        _director = director;
    }

    /////////////////////////////////////////////////////////////////
    ////                Public Methods                           ////

    /** Generatre the code for the firing of actors.
     *  In this base class, it is attempted to fire all the actors once.
     *  In subclasses such as helper for SDF and Giotto directors, the
     *  firings of actors observe the associated schedule. In addition,
     *  some special handling is needed, e.g., the iteration limit in
     *  SDF and time advancement in Giotto.
     *  @param code The string buffer that the generated code is appended to.
     *  @exception IllegalActionException If the helper associated with
     *   an actor throws it while generating wrapup code for the actor.
     */
    public void generateFireCode(StringBuffer code)
            throws IllegalActionException {
        code.append("/* The firing of the director. */\n");

        Iterator actors = ((CompositeActor) _codeGenerator.getContainer()).deepEntityList()
            .iterator();

        while (actors.hasNext()) {
            Actor actor = (Actor) actors.next();
            CodeGeneratorHelper helperObject = (CodeGeneratorHelper) _getHelper((NamedObj) actor);
            helperObject.generateFireCode(code);
        }
    }

    /** Generate the initialize code for this director. First set the buffer
     *  sizes of each port of the actors under the associated director.
     *  The initialize code for the director is generated by appending the
     *  intialize code for each actor.
     *  @return The generated initialize code.
     *  @exception IllegalActionException If the helper associated with
     *   an actor throws it while generating initialize code for the actor.
     */
    public String generateInitializeCode() throws IllegalActionException {
        StringBuffer code = new StringBuffer();
        code.append("/* The initialization of the director. */\n");

        List actorsList = ((CompositeActor) _codeGenerator.getContainer())
            .deepEntityList();
        Iterator actors = actorsList.iterator();

        while (actors.hasNext()) {
            // Set the buffer sizes of each channel of the actor before
            // generating initialize code.
            // FIXME: perhaps this part should be moved to preinitialize().
            // need an API that CodeGenerator will call preinitialize() before
            // generating initialize code.
            Actor actor = (Actor) actors.next();
            CodeGeneratorHelper helperObject = (CodeGeneratorHelper) _getHelper((NamedObj) actor);
            helperObject.createBufferAndOffsetMap();

            Set inputAndOutputPortsSet = new HashSet();
            inputAndOutputPortsSet.addAll(actor.inputPortList());
            inputAndOutputPortsSet.addAll(actor.outputPortList());

            Iterator inputAndOutputPorts = inputAndOutputPortsSet.iterator();

            while (inputAndOutputPorts.hasNext()) {
                IOPort port = (IOPort) inputAndOutputPorts.next();

                for (int i = 0; i < port.getWidth(); i++) {
                    int bufferSize = getBufferSize(port, i);
                    helperObject.setBufferSize(port, i, bufferSize);
                }
            }
        }

        actors = actorsList.iterator();

        while (actors.hasNext()) {
            Actor actor = (Actor) actors.next();
            CodeGeneratorHelper helperObject = (CodeGeneratorHelper) _getHelper((NamedObj) actor);
            code.append(helperObject.generateInitializeCode());
        }

        return code.toString();
    }

    /** Generate the wrapup code of the director associated with this helper
     *  class. For this base class, the wrapup code is just to generate
     *  the wrapup code for each actor.
     *  @param code The string buffer that the generated code is appended to.
     *  @exception IllegalActionException
     */
    public void generateWrapupCode(StringBuffer code)
            throws IllegalActionException {
        code.append("/* The wrapup of the director. */\n");

        Iterator actors = ((CompositeActor) _codeGenerator.getContainer()).deepEntityList()
            .iterator();

        while (actors.hasNext()) {
            Actor actor = (Actor) actors.next();
            ComponentCodeGenerator helperObject = _getHelper((NamedObj) actor);
            helperObject.generateWrapupCode(code);
        }
    }

    /** Return the buffer size of a given channel (i.e, a given port
     *  and a given channel number). In this base class, this method
     *  always returns 1.
     *  @param port The given port.
     *  @param channelNumber The given channel number.
     *  @return The buffer size of the given channel.
     *  @exception IllegalActionException Subclasses may throw it.
     */
    public int getBufferSize(IOPort port, int channelNumber)
            throws IllegalActionException {
        return 1;
    }

    /** Get the code generator associated with this helper class.
     *  @return The code generator associated with this helper class.
     */

    /*public ComponentCodeGenerator getCodeGenerator() {
      return _codeGenerator;
      }*/

    /** Return the director associated with this class.
     *  @return The director associated with this class.
     */
    public NamedObj getComponent() {
        return _director;
    }

    /** Set the code generator associated with this helper class.
     *  @param codeGenerator The code generator associated with this
     *   helper class.
     */
    public void setCodeGenerator(CodeGenerator codeGenerator) {
        _codeGenerator = codeGenerator;
    }

    /////////////////////////////////////////////////////////////////////
    ////                   protected methods                         ////

    /** Get the helper class associated with the given component.
     *  @return the helper class associated with the given component.
     *  @exception IllegalActionException If the code generator throws
     *   it when getting the helper associated with the given component.
     */
    protected ComponentCodeGenerator _getHelper(NamedObj component)
            throws IllegalActionException {
        return _codeGenerator._getHelper(component);
    }

    ////////////////////////////////////////////////////////////////////
    ////                     private variables                      ////
    // The associate director;
    private ptolemy.actor.Director _director;

    // The code generator containing this director helper.
    protected CodeGenerator _codeGenerator;
}
