package ptolemy.actor.corba.CoordinatorUtil;


/**
* ptolemy/actor/corba/CoordinatorUtil/ClientOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.1"
* from Coordinator.idl
*
*/


/* A CORBA compatible interface for a consumer.
	 */
public interface ClientOperations
{

  /* this method is intended to be called remotely to
  	     * send data to it.
  	     */
  void push (org.omg.CORBA.Any data) throws ptolemy.actor.corba.CoordinatorUtil.CorbaIllegalActionException;

  /* this method is intended to be called remotely to start the application
           * for the consumer.
           */
  void start ();

  /* this method is intended to be called remotely to stop the application
           * for the consumer.
           */
  void stop ();
} // interface ClientOperations
