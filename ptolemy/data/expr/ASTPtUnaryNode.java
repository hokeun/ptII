/* Generated By:JJTree: Do not edit this line. ASTPtUnaryNode.java */

/* ASTPtUnaryNode represent the unary operator(!,-) nodes in the parse tree

 Copyright (c) 1998 The Regents of the University of California.
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

@ProposedRating Red (nsmyth@eecs.berkeley.edu)

Created : May 1998

*/

//////////////////////////////////////////////////////////////////////////
//// ASTPtUnaryNode
/**
 * The parse tree created from the expression string consists of a 
 * hierarchy of node objects. This class represents unary operator(!,-) 
 * nodes in the parse tree.
 * 
 * @author Neil Smyth
 * @version $Id$
 * @see pt.data.parser.ASTPtSimpleNode
 * @see pt.data.parser.PtParser 
 * @see pt.data.Token 
*/

package pt.data.parser;

import pt.data.*;

public class ASTPtUnaryNode extends ASTPtSimpleNode {
    protected boolean isMinus = false;
    protected boolean isNot = false;
    protected boolean isBitwiseNot = false;
       
     protected pt.data.Token  _resolveNode() throws IllegalArgumentException {
         if (jjtGetNumChildren() != 1) {
             String str = "More than one child of a Unary node";
             throw new IllegalArgumentException(str); 
         }
         pt.data.Token result = childTokens[0];
         try {
             if (isMinus == true) {
                 // Need to chose the type at the bottom of the hierarch
                 // so as to not do any upcasting. For now IntToken will do.
                 result = result.multiply(new pt.data.IntToken(-1));
             } else if (isNot == true) {
                 if (!(result instanceof BooleanToken)) {
                     String str = "Cannot negate a nonBoolean type: ";
                     throw new IllegalArgumentException(str + result.toString()); 
                 }
                 ((BooleanToken)result).negate();
             } else if (isBitwiseNot == true) {
                 if (result instanceof IntToken) {
                     int tmp = ~(((IntToken)result).getValue());
                     return new IntToken(tmp);
                     /*                 } else if (result instanceof LongToken) {
                     long tmp = ~(((LongToken)result).getValue());
                     return new LongToken(tmp);*/
                 } else { 
                     String str = "Cannot apply bitwise NOT \"~\" to  ";
                     str = str + "non-Integer type: " + result.toString();
                     throw new IllegalArgumentException(str);
                 }
             }
         } catch (Exception ex) {
             String str = "Invalid negation operation(!, ~, -) on ";
             str = str + childTokens[0].getClass().getName();
             throw new IllegalArgumentException(str);
         } 
         return result;
     }



             

  public ASTPtUnaryNode(int id) {
    super(id);
  }

  public ASTPtUnaryNode(PtParser p, int id) {
    super(p, id);
  }

  public static Node jjtCreate(int id) {
      return new ASTPtUnaryNode(id);
  }

  public static Node jjtCreate(PtParser p, int id) {
      return new ASTPtUnaryNode(p, id);
  }
}
