/* A subscriber class to the Java Spaces.

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

@ProposedRating Red (liuj@eecs.berkeley.edu)
@AcceptedRating Red (yuhong@eecs.berkeley.edu)
*/

package ptolemy.actor.lib.jspaces;

import ptolemy.actor.lib.Source;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.data.expr.Parameter;
import ptolemy.data.*;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.kernel.util.*;
import ptolemy.data.type.BaseType;
import ptolemy.actor.lib.jspaces.util.SpaceFinder;
import net.jini.space.JavaSpace;
import net.jini.core.lease.Lease;
import net.jini.core.event.*;
import java.rmi.server.*;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.Iterator;

//////////////////////////////////////////////////////////////////////////
//// Subscriber
/**
A subscriber to the Java Spaces. This actor register a TokenEntry
of interest to the JavaSpaces. When get notified, it reads the
TokenEntry, and locally stores them in the order of the serial 
numbers. When this actor is fired, it output all the tokens it
has received from last firing.

If the parameter "blocking" is set to true, the actor will block
when it is fired but it has no receivings from last firing.
Otherwise, it will produce no output.

@author Jie Liu, Yuhong Xiong
@version $Id$
*/

public class Subscriber extends Source implements RemoteEventListener {

    /** Construct an actor with the given container and name.
     *  The output and trigger ports are also constructed.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the entity cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public Subscriber(TypedCompositeActor container, String name)
            throws NameDuplicationException, IllegalActionException  {
        super(container, name);
    	jspaceName = new Parameter(this, "jspaceName", 
                new StringToken("JaveSpaces"));
        jspaceName.setTypeEquals(BaseType.STRING);
        entryName = new Parameter(this, "entryName", 
                new StringToken(""));
        entryName.setTypeEquals(BaseType.STRING);
        minSerialNumber = new Parameter(this, "minSerialNumber", 
                new LongToken(0));
        minSerialNumber.setTypeEquals(BaseType.LONG);
        blocking = new Parameter(this, "blocking", 
                new BooleanToken(false));
        blocking.setTypeEquals(BaseType.BOOLEAN);
        defaultToken = new Parameter(this, "defaultToken", 
                new DoubleToken(0.0));
        defaultToken.setTypeEquals(BaseType.DOUBLE);
    }

    ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////

    /** The Java Space name. The default name is "JavaSpaces" of 
     *  type StringToken.
     */
    public Parameter jspaceName;

    /** The name for the subcribed entry. The default value is
     *  an empty string of type StringToken.
     */
    public Parameter entryName;

    /** The minimum serial number of the subsribed entry. All
     *  entries retrieved by this actor will have serial numbers
     *  no less than this number. The default value is 0 of 
     *  type LongToken. The serial number is usually greater
     *  than or equal to 0. If this number is negative, the 
     *  subscriber always get the entry with the largest 
     *  serial number (i.e. the latest entry).
     */  
    public Parameter minSerialNumber;

    /** Indicate whether the actor blocks when it can not read
     *  an entry from the space. The default value is false of
     *  type BooleanToken.
     */
    public Parameter blocking;

    /** The default initial token. If the actor is nonblocking
     *  and there is no matching entry in the space, then this
     *  token will be output. Default value is 0.0 of type 
     *  DoubleToken. The token and the type will be override
     *  by the first entry read from the space.
     */
    public Parameter defaultToken;
    

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Clone the actor into the specified workspace. This calls the
     *  base class and then sets the <code>output</code>
     *  variable to equal the new port.
     *  @param ws The workspace for the new object.
     *  @return A new actor.
     *  @exception CloneNotSupportedException If a derived class contains
     *   an attribute that cannot be cloned.
     */
    public Object clone(Workspace ws)
	    throws CloneNotSupportedException {
	try {
	    Subscriber newobj = (Subscriber)super.clone(ws);
	    newobj.jspaceName = (Parameter)newobj.getAttribute("jspaceName");
            newobj.entryName = (Parameter)newobj.getAttribute("entryName");
            newobj.minSerialNumber = 
                (Parameter)newobj.getAttribute("minSerialNumber");
            newobj.blocking = (Parameter)newobj.getAttribute("blocking");
            newobj.defaultToken = 
                (Parameter)newobj.getAttribute("defaultToken");
	    return newobj;
        } catch (CloneNotSupportedException ex) {
            // Errors should not occur here...
            throw new InternalErrorException(
                    "Clone failed: " + ex.getMessage());
        }
    }

    /** update parameters.
     */
    public void attributeChanged(Attribute attr) 
        throws IllegalActionException {
        if (attr == blocking) {
            _blocking = ((BooleanToken)blocking.getToken()).booleanValue();
        } 
    }
            

    /** Find the JavaSpaces and retrieve the first token. The type of
     *  the output is infered from the type of the token
     *  @exception IllegalActionException If the space cannot be found.
     */
    public void preinitialize() throws IllegalActionException {
        _entryName = ((StringToken)entryName.getToken()).stringValue();
        long minserialnumber = 
            ((LongToken)minSerialNumber.getToken()).longValue();

        _space = SpaceFinder.getSpace(
                ((StringToken)jspaceName.getToken()).stringValue());

        // export this object so that the space can call back
        try {
            UnicastRemoteObject.exportObject(this);
        } catch (RemoteException e) {
            throw new IllegalActionException( this,
                    "unable to export object. Please check if RMI is OK." +
                    e.getMessage());
        }
        
        //FIXME: First register to the space.
        try {
            IndexEntry indextemp = new IndexEntry(_entryName, "minimum", null);
            IndexEntry indexmin;
            if(_blocking) {
                indexmin = (IndexEntry)
                    _space.read(indextemp, null, Long.MAX_VALUE);
            } else {
                //read for 10 seconds. 
                // FIXME: should 10000 be a paramter?
                indexmin = (IndexEntry)_space.read(indextemp, null, 10000);
                if (indexmin == null) {
                    throw new IllegalActionException(this,
                            "the publisher is not ready. Please try again.");
                }
            }
            long minimum = indexmin.getPosition();
            indextemp = new IndexEntry(_entryName, "maximum", null);
            IndexEntry indexmax;
            if(_blocking) {
                indexmax = (IndexEntry)
                    _space.read(indextemp, null, Long.MAX_VALUE);
            } else {
                // read for 10 seconds. 
                // FIXME: should 10000 be a paramter?
                indexmax = (IndexEntry)_space.read(indextemp, null, 10000);
                if (indexmax == null) {
                    throw new IllegalActionException(this,
                            "the publisher is not ready. Please try again.");
                }
            }
            long maximum = indexmax.getPosition();
            
            // The template does not consider the serial number.
            TokenEntry template = new TokenEntry(_entryName, null, null);
            
            // depends on the where the minserialnumber is:
            // _lastRead serves as a lock.
            long lastread = minserialnumber -1 ;
            if( minserialnumber < minimum) {
                lastread = minimum -1;
            }
            _lastRead = new LastRead(lastread);
            
            // request for notification
            _eventReg = _space.notify(
                    template, null, this, Lease.FOREVER, null);
            _notificationSeq = _eventReg.getSequenceNumber();
            if(lastread < maximum) {
                // grab a lock and read all old entries.
                synchronized(_lastRead) {
                    boolean finished = false;
                    // don't need the lock on _tokenList, since
                    // no one can produce the outputs at this time.
                    while(!finished) {
                        TokenEntry entrytemp = new TokenEntry(_entryName, 
                                new Long(_lastRead.getSerialNumber()+1), null);
                        TokenEntry entry;
                        try {
                            entry = (TokenEntry)_space.readIfExists(
                                    entrytemp, null, Long.MAX_VALUE);
                        }catch (Exception e) {
                            throw new IllegalActionException(this,
                                    "error reading space." +
                                    e.getMessage());
                        }
                        if(entry == null) {
                            finished = true;
                        } else {
                            _lastRead.increment();
                            Token token = entry.token;
                            _tokenList.addLast(token);
                            // FIXME: set type?
                        }
                    }
                }
            }
        } catch (Exception e) {
                throw new IllegalActionException( this,
                        "error reading from the JavaSpace." +
                        e.getMessage());
        }
    }
    
    /** Fork a new thread to handle the notify event.
     */
    public void notify(RemoteEvent event) {
        NotifyHandler nh = new NotifyHandler(this, event);
        new Thread(nh).start();
    }
    
    
    /** Produce all tokens read so far, from last firing.
     */
    public void fire() throws IllegalActionException {
        // make sure no one is writing the token list.
        boolean waiting = true;
        while(waiting) {
            synchronized(_tokenList) {
                if (_tokenList.isEmpty()) {
                    if(_blocking) {
                        waiting = true;
                    } else {
                        waiting = false;
                        output.send(0, defaultToken.getToken());
                    }
                } else {
                    Iterator tokens = _tokenList.iterator();
                    while(tokens.hasNext()) {
                        output.send(0, (Token)tokens.next());
                    }
                    _tokenList.clear();
                    waiting = false;
                }
            }
            if(waiting) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new IllegalActionException(this,
                            "blocking interrupted." +
                            e.getMessage());
                }
            }
        }              
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////
    
    // The current serial number
    private String _entryName;

    // The space to read from.
    private JavaSpace _space;
    
    // cached value of whether blocks when no data
    private boolean _blocking;

    // The list of tokens that received.
    private LinkedList _tokenList;

    // The indicator the last read serial number
    private LastRead _lastRead;
    
    // Used to identify the event registration
    private EventRegistration _eventReg;
    
    // Notification sequence number
    private long _notificationSeq;
    
    ///////////////////////////////////////////////////////////////////
    ////                         inner class                       ////
    
    public class NotifyHandler implements Runnable {
        
        /** construct the notify handler
         */
        public NotifyHandler(TypedAtomicActor container, RemoteEvent event) {
            _container = container;
            _event = event;
        }

        //////////////////////////////////////////////////////////////
        ////                     public methods                   ////
                
        /** Read the entry token from the javaspaces.
         */
        public void run() {
            // check if it is the right notification
            if (_event.getSource().equals(_eventReg.getSource()) &&
                    _event.getID() == _eventReg.getID() && 
                    _event.getSequenceNumber() > _notificationSeq) {
                // grab a lock and read all new entries.
                synchronized(_lastRead) {
                    boolean finished = false;
                    // make sure the actor is not producing outputs.
                    synchronized(_tokenList) {
                        while(!finished) {
                            TokenEntry entrytemp = new TokenEntry(_entryName, 
                                new Long(_lastRead.getSerialNumber()+1), null);
                            TokenEntry entry;
                            try{
                                entry = (TokenEntry)_space.readIfExists(
                                        entrytemp, null, Long.MAX_VALUE);
                            } catch (Exception e) {
                                throw new InvalidStateException(_container,
                                        "error reading space." +
                                        e.getMessage());
                            }
                            if(entry == null) {
                                finished = true;
                            } else {
                                _lastRead.increment();
                                Token token = entry.token;
                                _tokenList.addLast(token);
                            }
                        }
                    }
                }
                notifyAll();
            }   
        }
        
        //////////////////////////////////////////////////////////////
        ////                     private variables                ////
        
        // the container
        private TypedAtomicActor _container;
        
        // the event
        private RemoteEvent _event;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         inner class                       ////
    /** A index for the last read token entry serial number.
     *  An instance of this class also serves as the lock for 
     *  reading token entries from the JavaSpaces.
     */
    public class LastRead {
        
        /** Construct LastRead with a given serial number.
         *  @param initserailnumber The initial serial number.
         */
        public LastRead(long initserialnumber) {
            _lastSerialNumber = initserialnumber;
        }
        
        //////////////////////////////////////////////////////////////
        ////                     public methods                   ////
        
        /** Return the serial number of the last token entry being 
         *  read.
         *  @return The last read serail number.
         */
        public long getSerialNumber() {
            return _lastSerialNumber;
        }

        /** Increase the serial number by one.
         */
        public void increment() {
            _lastSerialNumber ++;
        }
        
        //////////////////////////////////////////////////////////////
        ////                     private variables                ////
        
        // the last read serial number
        private long _lastSerialNumber;
    }

}
