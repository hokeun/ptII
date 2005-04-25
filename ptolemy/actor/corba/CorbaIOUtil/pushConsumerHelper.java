package ptolemy.actor.corba.CorbaIOUtil;


/**
 * ptolemy/actor/corba/CorbaIOUtil/pushConsumerHelper.java .
 * Generated by the IDL-to-Java compiler (portable), version "3.1"
 * from CorbaIO.idl
 * Wednesday, April 16, 2003 5:05:14 PM PDT
 */

/* A CORBA compatible interface for a push consumer.
 */
abstract public class pushConsumerHelper {
    private static String _id = "IDL:CorbaIOUtil/pushConsumer:1.0";

    public static void insert(org.omg.CORBA.Any a,
            ptolemy.actor.corba.CorbaIOUtil.pushConsumer that) {
        org.omg.CORBA.portable.OutputStream out = a.create_output_stream();
        a.type(type());
        write(out, that);
        a.read_value(out.create_input_stream(), type());
    }

    public static ptolemy.actor.corba.CorbaIOUtil.pushConsumer extract(
            org.omg.CORBA.Any a) {
        return read(a.create_input_stream());
    }

    private static org.omg.CORBA.TypeCode __typeCode = null;

    synchronized public static org.omg.CORBA.TypeCode type() {
        if (__typeCode == null) {
            __typeCode = org.omg.CORBA.ORB.init().create_interface_tc(ptolemy.actor.corba.CorbaIOUtil.pushConsumerHelper
                    .id(), "pushConsumer");
        }

        return __typeCode;
    }

    public static String id() {
        return _id;
    }

    public static ptolemy.actor.corba.CorbaIOUtil.pushConsumer read(
            org.omg.CORBA.portable.InputStream istream) {
        return narrow(istream.read_Object(_pushConsumerStub.class));
    }

    public static void write(org.omg.CORBA.portable.OutputStream ostream,
            ptolemy.actor.corba.CorbaIOUtil.pushConsumer value) {
        ostream.write_Object((org.omg.CORBA.Object) value);
    }

    public static ptolemy.actor.corba.CorbaIOUtil.pushConsumer narrow(
            org.omg.CORBA.Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof ptolemy.actor.corba.CorbaIOUtil.pushConsumer) {
            return (ptolemy.actor.corba.CorbaIOUtil.pushConsumer) obj;
        } else if (!obj._is_a(id())) {
            throw new org.omg.CORBA.BAD_PARAM();
        } else {
            org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl) obj)
                ._get_delegate();
            ptolemy.actor.corba.CorbaIOUtil._pushConsumerStub stub = new ptolemy.actor.corba.CorbaIOUtil._pushConsumerStub();
            stub._set_delegate(delegate);
            return stub;
        }
    }
}
