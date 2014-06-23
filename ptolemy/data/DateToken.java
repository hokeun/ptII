/* A token that contains a date.

   @Copyright (c) 2008-2014 The Regents of the University of California.
   All rights reserved.

   Permission is hereby granted, without written agreement and without
   license or royalty fees, to use, copy, modify, and distribute this
   software and its documentation for any purpose, provided that the
   above copyright notice and the following two paragraphs appear in all
   copies of this software.

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

package ptolemy.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ptolemy.data.BooleanToken;
import ptolemy.data.Token;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.data.type.TypeLattice;
import ptolemy.graph.CPO;
import ptolemy.kernel.util.IllegalActionException;

/** A token that contains a date.
 * @author Patricia Derler, Christopher  based on DateToken in Kepler by Daniel Crawl and Christopher Brooks
 * @version $Id: DateToken.java 24000 2010-04-28 00:12:36Z berkley $
 * @since Ptolemy II 10.
 * @Pt.ProposedRating Red (cxh)
 * @Pt.AcceptedRating Red (cxh)
 */
public class DateToken extends AbstractConvertibleToken 
                               implements PartiallyOrderedToken {

    /** Construct a date token. The current time is used for the date.
     */
    public DateToken() {
        _isNil = false;
        _value = new Date();
    }

    /** Construct a token with a specified java.util.Date.
     *  @param value The specified java.util.Date type to construct the 
     *    token with.
     */
    public DateToken(Date value) {
        _isNil = false;
        _value = value;
    }
	
    /** Construct a DateToken that represents the time at the specified
     *  number of milliseconds since January 1, 1970.
     *  @param value The number of milliseconds since January 1, 1970.
     */
    public DateToken(long value) {
        _isNil = false;
        _value = new Date(value);
    }
	
    /** Construct a DateToken that represents the time specified as a
     *  string.  The string is first parsed by the default
     *  java.text.DateFormat parser, if that fails, the any leading
     *  and trailing double quotes are removed and a
     *  java.text.SimpleDateFormat with a parser with the value of
     *  {@link ptolemy.data.DateToken._SIMPLE_DATE_FORMAT} is used.
     *
     *  @param value The date specified in a format acceptable
     *  to java.text.DateFormat.
     *  @exception IllegalActionException If the date is not
     *  parseable by java.text.DateFormat.
     */
    public DateToken(String value) throws IllegalActionException {
        if (value == null) {
            _isNil = false;
            _value = null;
            return;
        }

        if (value.equals(_NIL)) {
            _isNil = true;
            _value = null;
            return;
        }


        DateFormat dateFormat = DateFormat.getDateInstance();
        try {
            // FIXME: One possibility would be to make this parser much
            // more robust so that it would parse almost any date.
            _value = dateFormat.parse(value);
        } catch (ParseException ex) {
            // Try parsing using the format generated by Date.toString().
            try {
                // See https://stackoverflow.com/questions/4713825/how-to-parse-output-of-new-date-tostring
                // FIXME: this is probably Locale.US-specific
                
                // Remove leading and trailing double quotes.
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1,value.length()-1);
                }
                _value = _simpleDateFormat.parse(value);
            } catch (ParseException ex2) {
                throw new IllegalActionException(null, ex, "The date value \"" + value + "\" could not be parsed to a Date."
                        + "Also tried parsing with the \""
                        + _simpleDateFormat.toPattern()
                        + "\" pattern, the exception was: " + ex2.getMessage());
            }
        }
        _isNil = false;

    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Convert the specified token into an instance of DateToken.
     *  This method does lossless conversion.
     *  If the argument is already an instance of DateToken,
     *  it is returned without any change.  If the argument is
     *  a nil token, then a new nil Token is returned, see {@link
     *  #NIL}.  Otherwise, if the argument is below DateToken in the
     *  type hierarchy, it is converted to an instance of DateToken or
     *  one of the subclasses of DateToken and returned. If none of
     *  the above condition is met, an exception is thrown.
     *
     *  @param token The token to be converted to a DateToken.
     *  @return A DateToken.
     *  @exception IllegalActionException If the conversion
     *   cannot be carried out.
     */
    public static DateToken convert(Token token) throws IllegalActionException {
        if (token instanceof DateToken) {
            return (DateToken) token;
        }

        if (token.isNil()) {
            return DateToken.NIL;
        }

        int compare = TypeLattice.compare(BaseType.DATE, token);

        if (compare == CPO.LOWER || compare == CPO.INCOMPARABLE) {
            // We could try to create a DateToken from a String here,
            // but not all Strings are convertible to Dates.  Marten wrote:
            // "This seems wrong to me. It is not generally possible to
            // convert a String into a Date. Also, the type lattice
            // doesn't permit that conversion. Type inference is
            // supposed to yield a typing of which the automatic type
            // conversions that it imposes during run time work
            // without exception. We should not misuse the conversion
            // method to build a customized parser."
            throw new IllegalActionException(
                    notSupportedIncomparableConversionMessage(token, "date"));
        }

        compare = TypeLattice.compare(BaseType.LONG, token);

        if (compare == CPO.SAME || compare == CPO.HIGHER) {
            LongToken longToken = LongToken.convert(token);
            DateToken result = new DateToken(longToken.longValue());
            return result;
        }

        compare = TypeLattice.compare(BaseType.STRING, token);

        if (compare == CPO.SAME || compare == CPO.HIGHER) {
            StringToken stringToken = StringToken.convert(token);
            DateToken result = new DateToken(stringToken.stringValue());
            return result;
        }

        throw new IllegalActionException(notSupportedConversionMessage(token,
                "date"));
    }

    /**
     * Return the type of this token.
     * @return {@link #DATE}, the least upper bound of all the date types.
     */
    public Type getType() {
        return BaseType.DATE;
    }

    /**
     * Return the java.util.Date value of this token.
     * @return The java.util.Date that this Token was created with.
     */
    public Date getValue() {
        return _value;
    }

    /** Check whether the value of this token is strictly greater than
     *  that of the argument token.  The argument and this token are
     *  converted to equivalent types, and then compared.  Generally,
     *  this is the higher of the type of this token and the argument
     *  type.  This method defers to the _isLessThan() method to perform
     *  a type-specific equality check.  Derived classes should
     *  implement that method to provide type specific actions for
     *  equality testing.
     *
     *  @param rightArgument The token to compare against.
     *  @return A boolean token with value true if this token has the
     *  same units as the argument, and is strictly greater than the
     *  argument.
     *  @exception IllegalActionException If the argument token and
     *  this token are of incomparable types, or have different units,
     *  or the operation does not make sense for the given types.
     */
    public final BooleanToken isGreaterThan(PartiallyOrderedToken rightArgument)
            throws IllegalActionException {
        // Similar to the same method in ScalarToken.
        int typeInfo = TypeLattice.compare(getType(), (Token)rightArgument);

        if (typeInfo == CPO.SAME) {
            return ((DateToken)rightArgument)._doIsLessThan(this);
        } else if (typeInfo == CPO.HIGHER) {
            // This line is different from ScalarToken and causes problems with StringTokens.
            PartiallyOrderedToken convertedArgument = (PartiallyOrderedToken) getType().convert((Token)rightArgument);
            try {
                return convertedArgument.isLessThan(this);
            } catch (IllegalActionException ex) {
                // If the type-specific operation fails, then create a
                // better error message that has the types of the
                // arguments that were passed in.
                throw new IllegalActionException(null, ex, notSupportedMessage(
                                "isGreaterThan", (Token)this, (Token)rightArgument));
            }
        } else if (typeInfo == CPO.LOWER) {
            return rightArgument.isLessThan(this);
        } else {
            throw new IllegalActionException(notSupportedIncomparableMessage(
                            "isGreaterThan", (Token)this, (Token)rightArgument));
        }
    }

    /** Check whether the value of this token is strictly less than that of the
     *  argument token.
     *
     *  Only a partial order is assumed, so !(a < b) need not imply (a >= b).
     *
     *  @param rightArgument The token on greater than side of the inequality.
     *  @return BooleanToken.TRUE, if this token is less than the
     *    argument token. BooleanToken.FALSE, otherwise.
     *  @exception IllegalActionException If the tokens are incomparable.
     */
    public BooleanToken isLessThan(PartiallyOrderedToken rightArgument)
            throws IllegalActionException {
        DateToken rightDateToken = null;
        try {
            rightDateToken = convert((Token)rightArgument);
        } catch (IllegalActionException ex) {
            //// FIXME: Since PartiallyOrderedToken is an interface, we cannot do:
            //throw new IllegalActionException(null, ex, notSupportedMessage(
            //        "isLessThan", this, rightArgument))
            //// and must do this instead:
            throw new IllegalActionException("Cannot compare ScalarToken with "
                    + rightArgument);
        }
        return isLessThan(rightDateToken);
    }

    /** Check whether the value of this token is strictly less than that of the
     *  argument token.
     *
     *  @param rightArgument The token to compare against.
     *  @return A boolean token with value true if this token is strictly
     *  less than the argument.
     *  @exception IllegalActionException If the argument token and
     *  this token are of incomparable types, or have different units,
     *  or the operation does not make sense for the given types.
     */
    public BooleanToken isLessThan(DateToken rightArgument)
            throws IllegalActionException {
        // FIXME: Copied from ScalarToken, but one line is different
        int typeInfo = TypeLattice.compare(getType(), rightArgument);

        if (typeInfo == CPO.SAME) {
            return _doIsLessThan(rightArgument);
        } else if (typeInfo == CPO.HIGHER) {
            DateToken convertedArgument = (DateToken) getType().convert(
                    rightArgument);
            try {
                return _doIsLessThan(convertedArgument);
            } catch (IllegalActionException ex) {
                // If the type-specific operation fails, then create a
                // better error message that has the types of the
                // arguments that were passed in.
                throw new IllegalActionException(null, ex, notSupportedMessage(
                        "isLessThan", this, rightArgument));
            }
        } else if (typeInfo == CPO.LOWER) {
            return rightArgument.isGreaterThan(this);
        } else {
            throw new IllegalActionException(notSupportedIncomparableMessage(
                    "isLessThan", this, rightArgument));
        }
    }

    /** Return true if the token is nil, (aka null or missing).
     *  Nil or missing tokens occur when a data source is sparsely populated.
     *  To create a nil DateToken, call new DateToken("nil");
     *  @return True if the token is the {@link #NIL} token.
     */
    public boolean isNil() {
        return _isNil;
    }

    /**
     * Return a String representation of the DateToken. The string is surrounded
     * by double-quotes; without them, the Ptolemy expression parser fails to
     * parse it.
     * 
     * <p>Unfortunately, the Java Date class has a fatal flaw in that 
     * Date.toString() does not return the value of the number of ms., so
     * we use a format that includes the number of ms.</p>
     *
     * @return A String representation of the DateToken.
     */
    public String toString() {
        if (isNil()) {
            return _NIL;
        }
        if (_value == null) {
            // FindBugs prefers that toString() not return null.
            return "null";
        } else {
            return "\"" + _simpleDateFormat.format(_value) + "\"";
        }
    }

    /** A token that represents a missing value.
     *  Null or missing tokens are common in analytical systems
     *  like R and SAS where they are used to handle sparsely populated data
     *  sources.  In database parlance, missing tokens are sometimes called
     *  null tokens.  Since null is a Java keyword, we use the term "nil".
     *  The toString() method on a nil token returns the string "nil".
     */
    public static final DateToken NIL;

    ///////////////////////////////////////////////////////////////////
    ////               protected methods                           ////

    /** Subtract is not supported for Dates.
     *  @param rightArgument The token to subtract from this token.
     *  @return A new token containing the result.
     *  @exception IllegalActionException Always thrown because 
     *  multiplying a Date does not make sense.
     */
    protected Token _add(Token rightArgument) throws IllegalActionException {
        throw new IllegalActionException(null, notSupportedMessage(
                        "add", this, rightArgument));
    }

    /** Subtract is not supported for Dates.
     *  @param rightArgument The token to subtract from this token.
     *  @return A new token containing the result.
     *  @exception IllegalActionException Always thrown because 
     *  dividing a Date does not make sense.
     */
    protected Token _divide(Token rightArgument) throws IllegalActionException {
        throw new IllegalActionException(null, notSupportedMessage(
                        "divide", this, rightArgument));
    }

    /** Test for ordering of the values of this Token and the argument
     *  Token.  It is guaranteed by the caller that the type and
     *  units of the argument is the same as the type of this class.
     *  This method may defer to the _isLessThan() method that takes a
     *  ScalarToken.  Derived classes should implement that method
     *  instead to provide type-specific operation.
     *  @param rightArgument The token with which to test ordering.
     *  @return A BooleanToken which contains the result of the test.
     *  @exception IllegalActionException If the units of the argument
     *  are not the same as the units of this token, or the method is
     *  not supported by the derived class or if either this token or
     *  the argument token is a nil token.
     */
    private BooleanToken _doIsLessThan(PartiallyOrderedToken rightArgument)
            throws IllegalActionException {
        if (isNil() || ((Token)rightArgument).isNil()) {
            throw new IllegalActionException(notSupportedMessage("isLessThan",
                            this, (Token)rightArgument) + " because one or the other is nil");
        }

        DateToken convertedArgument = (DateToken) rightArgument;

        return _isLessThan(convertedArgument);
    }

    /** The isCloseTo() method is not supported for Dates because
     *  epsilon is not losslessly convertable to Long.   
     *  @param token The token to compare to this token
     *  @return A new token containing the result.
     *  @exception IllegalActionException Always thrown because 
     *  isCloseTo() on a Date does not make sense.
     */
    protected BooleanToken _isCloseTo(Token token, double epsilon)
            throws IllegalActionException {
        // FIXME: If we convert the two tokens to longs and the
        // epsilon to a long, then this might make sense?
        // However, double is not losslessly convertible to long?
        // Probably throw an IllegalActionException here.

        throw new IllegalActionException(null, notSupportedMessage(
                        "isCloseTo", this, token));
    }

    /** Return true of the the value of this token is equal
     *  to the value of the argument according to java.util.Date.
     *  Two DateTokens are considered equal if the their values
     *  are non-null and the java.util.Date.equals() method returns
     *  true.
     *  It is assumed that the type of the argument is the
     *  same as the type of this class.
     *  @param rightArgument The token with which to test equality.
     *  @exception IllegalActionException Not thrown in this baseclass
     */
    protected BooleanToken _isEqualTo(Token rightArgument)
        throws IllegalActionException {

        // The caller of this method should convert
        // the rightArgument to a DateToken, but we check anyway.
        if (!(rightArgument instanceof DateToken)) {
            return BooleanToken.FALSE;            
        }

        if (isNil() || rightArgument.isNil()) {
            return BooleanToken.FALSE;
        }

        Date rightDate = ((DateToken) rightArgument).getValue();
        Date leftDate = getValue();
        if (rightDate == null || leftDate == null) {
            return BooleanToken.FALSE;
        }

        if (leftDate.compareTo(rightDate) == 0) {
            return BooleanToken.TRUE;
        }

        return BooleanToken.FALSE;
    }

    /** Test for ordering of the values of this Token and the argument
     *  Token.
     *  @param rightArgument The token to compare to this token.
     *  @exception IllegalActionException If this method is not
     *  supported by the derived class.
     *  @return A new Token containing the result.
     */
    protected BooleanToken _isLessThan(DateToken rightArgument)
            throws IllegalActionException {
        return BooleanToken.getInstance(_value.before(rightArgument.getValue()));
    }

    /** Modulo is not supported for Dates.
     *  @param rightArgument The token to divide into this token.
     *  @return A new token containing the result.
     *  @exception IllegalActionException Always thrown because 
     *  modulo of a Date does not make sense.
     */
    protected Token _modulo(Token rightArgument) throws IllegalActionException {
        // If we convert to longs then is does this make sense?
        // For example, say we had a date that represented a period of 
        // time and wanted to know if it was within a certain month, would
        //that be like modulo
        throw new IllegalActionException(null, notSupportedMessage(
                        "modulo", this, rightArgument));
    }

    /** Multiply is not supported for Dates.
     *  @param rightArgument The token to multiply this token by.
     *  @return A new token containing the result.
     *  @exception IllegalActionException Always thrown because 
     *  multiplying a Date does not make sense.
     */
    protected Token _multiply(Token rightArgument)
            throws IllegalActionException {
        throw new IllegalActionException(null, notSupportedMessage(
                        "multiply", this, rightArgument));
    }

    /** Subtract is not supported for Dates.
     *  @param rightArgument The token to subtract from this token.
     *  @return A new token containing the result.
     *  @exception IllegalActionException Always thrown because 
     *  subtracting a Date does not make sense.
     */
    protected Token _subtract(Token rightArgument)
            throws IllegalActionException {
        throw new IllegalActionException(null, notSupportedMessage(
                        "subtract", this, rightArgument));
    }

    ///////////////////////////////////////////////////////////////////
    ////               protected variables                         ////

    // This is protected so that the DateToken(String) javadoc can refer
    // to it.
    /** The format in which dates are reported.  Milliseconds are included
     *  so that the toString() method returns a string that can be parsed
     *  to the same Date.
     */
    protected static final String _SIMPLE_DATE_FORMAT = "EEE MMM dd HH:mm:ss.SSS zzz yyyy";


    ///////////////////////////////////////////////////////////////////
    ////               private variables                           ////

    /** True if the value of this Date is missing. */
    private boolean _isNil = false;

    /** The String value of a nil token. */
    private static final String _NIL = "nil";

    /** The format used to read and write dates. */
    private static final SimpleDateFormat _simpleDateFormat;

    /** The java.util.Date */
    private Date _value;

    static {
        try {
            NIL = new DateToken(_NIL);
            _simpleDateFormat = new SimpleDateFormat(_SIMPLE_DATE_FORMAT);

        } catch (IllegalActionException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
