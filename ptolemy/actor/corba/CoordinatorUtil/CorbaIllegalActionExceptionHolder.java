package ptolemy.actor.corba.CoordinatorUtil;

/**
* ptolemy/actor/corba/CoordinatorUtil/CorbaIllegalActionExceptionHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.1"
* from Coordinator.idl
*
*/

public final class CorbaIllegalActionExceptionHolder implements org.omg.CORBA.portable.Streamable
{
  public ptolemy.actor.corba.CoordinatorUtil.CorbaIllegalActionException value = null;

  public CorbaIllegalActionExceptionHolder ()
  {
  }

  public CorbaIllegalActionExceptionHolder (ptolemy.actor.corba.CoordinatorUtil.CorbaIllegalActionException initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = ptolemy.actor.corba.CoordinatorUtil.CorbaIllegalActionExceptionHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    ptolemy.actor.corba.CoordinatorUtil.CorbaIllegalActionExceptionHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return ptolemy.actor.corba.CoordinatorUtil.CorbaIllegalActionExceptionHelper.type ();
  }

}
