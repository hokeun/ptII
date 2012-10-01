/* A merge actor whose output type is the union of input types.

 Copyright (c) 2006-2010 The Regents of the University of California.
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
package ptolemy.actor.lib;

import java.util.HashSet;
import java.util.Set;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.util.ConstructAssociativeType;
import ptolemy.actor.util.ExtractFieldType;
import ptolemy.data.UnionToken;
import ptolemy.data.type.UnionType;
import ptolemy.graph.Inequality;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

///////////////////////////////////////////////////////////////////
//// UnionMerge

/**
On each firing, read all tokens from every input port and wrap each token
into a UnionToken of which the label matches the name of the originating
input port. All produced UnionTypes are sent to the output port in a single
firing.

<p>
The type of the output port is a UnionType of which the labels much match
the names of the input ports. This is achieved using two type constraints:
The labels for the output UnionToken are the names of the input
ports. This is achieved using two type constraints:

<ul>
<li><tt>output >= {| x = typeOf(inputPortX), y = typeOf(inputPortY), .. |}
</tt>, which requires the types of the input ports to be compatible
with the corresponding types in the output union.
</li>
<li><tt>each input <= the type of the corresponding field inside the
output union</tt>, which is similar to the usual default constraints,
however this constraint establishes a dependency between the inputs of
this actor and the fields inside the output union, instead of just
between its inputs and outputs.
</li>
</ul>

Note that the output UnionType is required to contain a corresponding
field for every input. However, due to the subtyping relation of UnionType
that is opposite to the subtyping of RecordType, the type constraint
that the output port of this actor must be greater than or equal to the GLB
of the types of its receivers (implied by the connections), is always
satisfied.
</p>
<p>
To use this actor, instantiate it, and then add input ports
(instances of TypedIOPort).
</p>
@author Edward A. Lee, Haiyang Zheng, Yuhong Xiong, Marten Lohstroh
@version $Id: UnionMerge.java 63960 2012-07-13 22:59:42Z cxh $
@since Ptolemy II 5.2
@Pt.ProposedRating Red (yuhongx)
@Pt.AcceptedRating Red (yuhongx)
*/
public class UnionMerge extends TypedAtomicActor {

    /** Construct an actor in the specified container with the specified
     *  name.
     *  @param container The container.
     *  @param name The name.
     *  @exception NameDuplicationException If an actor
     *   with an identical name already exists in the container.
     *  @exception IllegalActionException If the actor cannot be contained
     *   by the proposed container.
     */
    public UnionMerge(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);

        output = new TypedIOPort(this, "output", false, true);

        _attachText("_iconDescription", "<svg>\n"
                + "<polygon points=\"-10,20 10,10 10,-10, -10,-20\" "
                + "style=\"fill:green\"/>\n" + "</svg>\n");
    }

    ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////

    /** The output port. The type of this port will be the union of the
     *  type of the input ports.
     */
    public TypedIOPort output;

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Read all available tokens from each input port, wrap each of them into
     *  a UnionToken of which the label matches the name of the originating
     *  input port. All produced UnionTypes are sent to the output port in a
     *  single firing.
     *  @exception IllegalActionException If there is no director, or
     *  the input can not be read, or the output can not be sent.
     */
    public void fire() throws IllegalActionException {
        super.fire();

        for (IOPort port : inputPortList()) {
            while (port.hasToken(0)) {
                output.send(0, new UnionToken(port.getName(), port.get(0)));
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Set up and return two type constraints.
     *  <ul>
     *  <li><tt>output >= {x = typeOf(inputPortX), y = typeOf(inputPortY), ..}
     *  </tt>, which requires the types of the input ports to be compatible
     *  with the corresponding types in the output union.
     *  </li>
     *  <li><tt>each input <= the type of the corresponding field inside the
     *  output union</tt>, which is similar to the usual default constraints,
     *  however this constraint establishes a dependency between the inputs of
     *  this actor and the fields inside the output union, instead of just
     *  between its inputs and outputs.
     *  </li>
     *  </ul>
     *  @return A set of type constraints
     *  @see ConstructAssociativeType
     *  @see ExtractFieldType
     */
    @Override
    protected Set<Inequality> _customTypeConstraints() {
        Set<Inequality> result = new HashSet<Inequality>();

        // constrain the fields in the output union to be greater than or
        // equal to the declared or resolved types of the input ports:
        // output >= {x = typeOf(outputPortX), y = typeOf(outputPortY), ..|}
        result.add(new Inequality(new ConstructAssociativeType(inputPortList(),
                UnionType.class), output.getTypeTerm()));

        for (TypedIOPort input : inputPortList()) {
            // constrain the type of every input to be >= the resolved type
            // of the corresponding field in the output union
            result.add(new Inequality(new ExtractFieldType(output, input
                    .getName()), input.getTypeTerm()));
        }

        // NOTE: refrain from using port.setTypeAtMost() or
        // port.setTypeAtLeast(), because after removing an output port, the
        // constraint referring to this removed port will remain to exist in
        // the input port, which will result in type errors.

        return result;
    }

    /** Do not establish the usual default type constraints.
     *  @return null
     */
    protected Set<Inequality> _defaultTypeConstraints() {
        return null;
    }

}
