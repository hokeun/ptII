/* Filter MoML without using a MoMLParser to parse xml.

 Copyright (c) 2011-2012 The Regents of the University of California.
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
package ptolemy.moml.filter;

import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLFilter;
import ptolemy.moml.MoMLParser;

///////////////////////////////////////////////////////////////////
//// MoMLFilterSimple

/** Filter MoML without using a MoMLParser to parse xml generated by the filter.

 @author Christopher Brooks
 @version $Id$
 @since Ptolemy II 8.1
 @Pt.ProposedRating Red (cxh)
 @Pt.AcceptedRating Red (cxh)
 */
public abstract class MoMLFilterSimple implements MoMLFilter {

    /** Return the old attribute value for properties that are not registered
     *  to be removed. Otherwise, return null to remove the property.
     *  @param container  The container for this attribute.
     *  @param element The XML element name.
     *  @param attributeName The name of the attribute.
     *  @param attributeValue The value of the attribute.
     *  @param xmlFile The file currently being parsed.
     *  @param parser The parser in which MoML is optionally evaluated.  Ignored
     *  in this method.
     *  @return The value of the attributeValue argument.
     */
    public String filterAttributeValue(NamedObj container, String element,
            String attributeName, String attributeValue, String xmlFile,
            MoMLParser parser) {
        // Ignore the MoMLParser argument for backward compatiblity.
        return filterAttributeValue(container, element, attributeName,
                attributeValue, xmlFile);
    }

    /** Reset private variables.
     *  @param container The object created by this element.
     *  @param elementName The element name.
     *  @param currentCharData The character data, which appears
     *   only in the doc and configure elements
     *  @param xmlFile The file currently being parsed.
     *  @param parser The parser in which MoML is optionally evaluated.
     *  @exception Exception if there is a problem substituting
     *  in the new value.
     */
    public void filterEndElement(NamedObj container, String elementName,
            StringBuffer currentCharData, String xmlFile, MoMLParser parser)
            throws Exception {
        filterEndElement(container, elementName, currentCharData, xmlFile);
    }
}
