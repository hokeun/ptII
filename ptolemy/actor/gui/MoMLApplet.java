/* Basic applet that constructs a Ptolemy II model from a MoML file.

 Copyright (c) 1998-2000 The Regents of the University of California.
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

package ptolemy.actor.gui;

// Java imports.
import java.net.URL;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Iterator;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

// XML imports
import com.microstar.xml.XmlException;

// Ptolemy imports
import ptolemy.actor.CompositeActor;
import ptolemy.actor.IOPort;
import ptolemy.actor.Manager;
import ptolemy.actor.CompositeActor;
import ptolemy.gui.*;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.Documentation;
import ptolemy.moml.MoMLParser;

//////////////////////////////////////////////////////////////////////////
//// MoMLApplet
/**
This is an applet that constructs a Ptolemy II model from a MoML file.
"MoML" stands for "Modeling Markup Language." It is an XML schema for
constructing Ptolemy II models.
<p>
This class offers a number of alternatives that control the visual
appearance of the applet. By default, the applet places on the screen
a set of control buttons that can be used to start, stop, pause, and
resume the model.  Below those buttons, it places the visual elements
of any actors in the model that implement the Placeable interface,
such as plotters or textual output.
<p>
The applet parameters are:
<ul>
<li>
<i>background</i>: The background color, typically given as a hex
number of the form "#<i>rrggbb</i>" where <i>rr</i> gives the red
component, <i>gg</i> gives the green component, and <i>bb</i> gives
the blue component.
<li>
<i>controls</i>:
This gives a comma-separated list
of any subset of the words "buttons", "topParameters", and
"directorParameters" (case insensitive), or the word "none".
If this parameter is not given, then it is equivalent to
giving "buttons", and only the control buttons mentioned above
will be displayed.  If the parameter is given, and its value is "none",
then no controls are placed on the screen.  If the word "topParameters"
is included in the comma-separated list, then controls for the
top-level parameters of the model are placed on the screen, below
the buttons.  If the word "directorParameters" is included,
then controls for the director parameters are also included.
<li>
<i>modelURL</i>: The name of a URI (or URL) containing the
MoML file that defines the model.
<li>
<i>orientation</i>: This can have value "horizontal", "vertical", or
"controls_only" (case insensitive).  If it is "vertical", then the
controls are placed above the visual elements of the Placeable actors.
This is the default.  If it is "horizontal", then the controls
are placed to the left of the visual elements.  If it is "controls_only"
then no visual elements are placed.
</ul>
<p>
To create a model in a different way, say without a <i>modelClass</i>
applet parameter, you may extend this class and override the
protected method _createModel().  If you wish to alter the way
that the model is represented on the screen, you can extend this
class an override the _createView() method.  The rendition in this class
is an instance of ModelPane.

@author  Edward A. Lee
@version $Id$
*/
public class MoMLApplet extends PtolemyApplet {

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Return applet information. If the top-level model element
     *  contains a <i>doc</i> element, then the contents of that element
     *  is included in the applet information.
     *  @return A string giving information about the applet.
     */
    public String getAppletInfo() {
	// Include the release and build number to aid in user support.
	String version = new String("Ptolemy II "
				    + PtolemyApplication.RELEASE_VERSION);
	String build = new String("\n(Build: $Id$)");
        if (_toplevel != null) {
            String tip = Documentation.consolidate(_toplevel);
            if (tip != null) {
                return version 
		    + " model given in MoML:\n" + tip
		    + build;
            } else {
                return version
		    + " model given in MoML."
		    + build;
            }
        }
        return "MoML applet for " + version
            + "\nPtolemy II comes from UC Berkeley, Department of EECS.\n"
            + "See http://ptolemy.eecs.berkeley.edu/ptolemyII"
	    + build;
    }

    /** Describe the applet parameters.
     *  @return An array describing the applet parameters.
     */
    public String[][] getParameterInfo() {
        String newinfo[][] = {
            {"modelURL", "", "URL for the MoML file"},
        };
        return _concatStringArrays(super.getParameterInfo(), newinfo);
    }

    /** Read the model from the <i>modelURL</i> applet parameter.
     *  @param workspace The workspace in which to create the model.
     *  @return A model.
     *  @throws Exception If something goes wrong.
     */
    protected CompositeActor _createModel(Workspace workspace)
            throws Exception {
        String modelURL = getParameter("modelURL");
        if (modelURL == null) {
            // For backward compatibility, try name "model".
            modelURL = getParameter("model");
            if (modelURL == null) {
                throw new Exception("Applet does not not specify a modelURL.");
            }
        }
        MoMLParser parser = new MoMLParser();
        URL docBase = getDocumentBase();
        URL xmlFile = new URL(docBase, modelURL);
        _manager = null;
        CompositeActor result = null;
        NamedObj toplevel = parser.parse(docBase, xmlFile);
        _workspace = toplevel.workspace();
        if (toplevel instanceof CompositeActor) {
            result = (CompositeActor)toplevel;
            _manager = result.getManager();
            if (_manager == null) {
                _manager = new Manager(_workspace, "manager");
                result.setManager(_manager);
            }
            _manager.addExecutionListener(this);
        } else {
            throw new Exception("Model is not an instance of CompositeActor");
        }
        return result;
    }
}
