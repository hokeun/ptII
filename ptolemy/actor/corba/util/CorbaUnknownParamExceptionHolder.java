package ptolemy.actor.corba.util;

/**
* ptolemy/actor/corba/util/CorbaUnknownParamExceptionHolder.java
* Generated by the IDL-to-Java compiler (portable), version "3.0"
* from CorbaActor.idl
* Thursday, January 18, 2001 7:07:58 PM PST
*/

public final class CorbaUnknownParamExceptionHolder implements org.omg.CORBA.portable.Streamable
{
    public ptolemy.actor.corba.util.CorbaUnknownParamException value = null;

    public CorbaUnknownParamExceptionHolder ()
    {
    }

    public CorbaUnknownParamExceptionHolder (ptolemy.actor.corba.util.CorbaUnknownParamException initialValue)
    {
        value = initialValue;
    }

    public void _read (org.omg.CORBA.portable.InputStream i)
    {
        value = ptolemy.actor.corba.util.CorbaUnknownParamExceptionHelper.read (i);
    }

    public void _write (org.omg.CORBA.portable.OutputStream o)
    {
        ptolemy.actor.corba.util.CorbaUnknownParamExceptionHelper.write (o, value);
    }

    public org.omg.CORBA.TypeCode _type ()
    {
        return ptolemy.actor.corba.util.CorbaUnknownParamExceptionHelper.type ();
    }

}
