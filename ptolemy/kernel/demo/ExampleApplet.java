/* ExampleApplet.java is a applet class which cotains a run button and a text area

 Copyright (c) 1997- The Regents of the University of California.
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
//    NOTE: Part of this file is automatically generated

package ptolemy.kernel.demo;

import java.awt.Color;
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Insets;
import java.lang.Math;
import java.lang.String;

//////////////////////////////////////////////////////////////////////////
//// ExampleApplet
/** 
ExapmleApplet is an applet contains a run button and a text area.
It's for the general perpose of convert the command line application
into AWT compatible applet. This applet is also contained in a frame
to make it run as an application
@author Jie Liu
@version $Id$
@see java.lang.applet
@see ExampleFrame
*/
class ExampleApplet extends java.applet.Applet
{
    /** Constructor
     */	
    public ExampleApplet() {
        super();
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Gerneral perpose method. Redefine the Rectangle to support full
     *  letter display.
     * @see java.awt.Rectangle
     * @param position of the rectangle, initial point(x,y), width and hight
     */	
    public Rectangle DURectangle( int x, int y, int w, int h )
        {
            String        alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            FontMetrics   fm = getFontMetrics( getFont() );
            double        fw = ( fm != null ) ? ( fm.stringWidth( alphabet ) / alphabet.length() ) : 0;
            double        fh = ( fm != null ) ? ( fm.getHeight() / 2.0 ) : 0.0;

            return new Rectangle( (int) Math.round( ( (double)fw * (double)x ) / 4.0 ),
                    (int) Math.round( ( (double)fh * (double)y ) / 4.0 ),
                    (int) Math.round( ( (double)fw * (double)w ) / 4.0 ),
                    (int) Math.round( ( (double)fh * (double)h ) / 4.0 ) );
        }
    
    /** Place the Rectangle in a component.
     * @see java.awt.Rectangle
     * @see java.awt.Component
     * @see java.awt.Insets
     */	
    public void DUPositionComponent( java.awt.Component comp, int x, int y, int w, int h, java.awt.Insets formInsets ) {
        Rectangle     rect = DURectangle( x, y, w, h );
        if( formInsets != null ) {
            rect.x += formInsets.left;
            rect.y += formInsets.top;
        }
        comp.reshape( rect.x, rect.y, rect.width, rect.height );
    }

    /** Create the applet. One label, one button "run" and one text area.
    *  @see java.lang.applet#create
    */	
    public boolean create() throws java.lang.Exception {
        setFont( new Font( "Dialog", Font.PLAIN, 12 ) );

        boolean retval = true;
        if( getPeer() == null ) {
            addNotify();
        }
        show();
        Insets formInsets = insets();
        hide();
        setBackground( Color.lightGray );
        setForeground( Color.black );

        add(_lb);
        add(_bnrun);
        add(_tarea);
        
        DUPositionComponent( this, 0, 0, 296, 225, null );

        DUPositionComponent( _lb, -1, 5, 231, 10, formInsets );
        _lb.setAlignment(java.awt.Label.CENTER);
        _lb.setText( "Results of Major Methods Applied to Ports and Relations" );
        _lb.setFont( new Font( "Dialog", Font.BOLD, 14 ) );
        _lb.setBackground( Color.lightGray );
        _lb.setForeground( Color.black );
        _lb.enable(true);
        _lb.show();

        DUPositionComponent( _bnrun, 235, 20, 50, 20, formInsets );
        _bnrun.setBackground( Color.lightGray );
        _bnrun.setForeground( Color.black );
        _bnrun.enable(true);
        _bnrun.show();
        _bnrun.setLabel("Run");

        DUPositionComponent( _tarea, 5, 20, 220, 185, formInsets );
        _tarea.setBackground( Color.lightGray );
        _tarea.setForeground( Color.black );
        _tarea.enable(true);
        _tarea.show();

        show();
        return retval;
    }
    
    /** Destroy the applet. 
    *  @see java.lang.applet#destroy
    */	
    public synchronized void destroy() {
        if( (java.awt.Container)this instanceof java.awt.Window ) {
            ((java.awt.Window)(java.awt.Container)this).dispose();
        } else {
            removeNotify();
        }
        System.gc();
        System.runFinalization();
        System.exit(0);

    }
    
    /** Handle the event passed to this applet. The event for the button is catched
    *  and all othe events are hendled by default.
    */	
    public boolean handleEvent(java.awt.Event event) {
        Object eventTarget = event.target;
        if( eventTarget == _bnrun && event.id == java.awt.Event.ACTION_EVENT ) {
            return buttonAction(event);
        }
        return super.handleEvent(event);
    }
    
    /** The action performed after clicking the button. By default it does nothing.
    *  To be override by the inherent class.
    */	
    public boolean buttonAction(java.awt.Event event) {
        printInTextArea("Hello World\n");
        return true;
    }
    
    /** Print the string in the text area. It does not add any extra charactors to
     *  the string, like "\n".
     *  @param st the string to be printed.
     */	
    public void printInTextArea (String st) {
        _tarea.appendText(st);
    }
    
    /** Reset the text area by an empty string.
     */
    public void clearTextArea() {
        _tarea.setText("");
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected variables                  ////

    /** A label, a text area and a button */
    
    private  java.awt.Button  _bnrun = new java.awt.Button();
    private  java.awt.TextArea  _tarea = new java.awt.TextArea();
    private  java.awt.Label  _lb = new java.awt.Label();


}

