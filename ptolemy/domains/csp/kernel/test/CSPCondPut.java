/* CSPCondPut

 Copyright (c) 1998-1999 The Regents of the University of California.
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

@ProposedRating Red (davisj@eecs.berkeley.edu)

*/

package ptolemy.domains.csp.kernel.test;

import ptolemy.domains.csp.kernel.*;
import ptolemy.actor.*;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.data.Token;
import ptolemy.data.StringToken;


//////////////////////////////////////////////////////////////////////////
//// CSPCondPut
/**

@author John S. Davis II
@version $Id$

*/

public class CSPCondPut extends CSPPutToken {

    /**
     */
    public CSPCondPut(CompositeActor cont, String name, int numTokens, int rcvrCnt)
            throws IllegalActionException, NameDuplicationException {
         super(cont, name, numTokens);
         
	 _rcvrCnt = rcvrCnt;
	 _truth = new boolean[rcvrCnt]; 
	 _winningBrch = new boolean[rcvrCnt]; 

	for( int i = 0; i < rcvrCnt; i++ ) {
	    _winningBrch[i] = false;
	    _truth[i] = false;
	}
	
    }

    ////////////////////////////////////////////////////////////////////////
    ////                         public methods                         ////

    /**
     */
    public void fire() throws IllegalActionException {
	int numRcvrs = 0;
        Receiver[][] rcvrs = outputPort.getRemoteReceivers(); 
	for( int i = 0; i < rcvrs.length; i++ ) {
	    for( int j = 0; j < rcvrs[i].length; j++ ) {
		numRcvrs++;
            }
	}

	if( _rcvrCnt != numRcvrs ) {
	    throw new IllegalActionException(getName()+": Error in the number"
		    + " of receivers. Be sure constructor is properly"
		    + " instantiated.");
	}

	ConditionalBranch[] brchs = new ConditionalBranch[numRcvrs]; 

	Token token = new Token();

	for( int i = 0; i < numRcvrs; i++ ) {
	    brchs[i] = new ConditionalSend(_truth[i], outputPort, i, i, token);
        }

	int winner = chooseBranch(brchs);

	_winningBrch[winner] = true;
    }

    /**
     */
    public boolean isWinner(int index) {
	return _winningBrch[index];
    }

    /**
     */
    public void setTruth(int index, boolean val) {
	_truth[index] = val;
    }

    ////////////////////////////////////////////////////////////////////////
    ////                        private variables                       ////

    private int _rcvrCnt;
    private boolean[] _truth;
    private boolean[] _winningBrch; 

}
