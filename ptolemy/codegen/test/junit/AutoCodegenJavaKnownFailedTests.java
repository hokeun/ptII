/* Run the Ptolemy model tests in the auto/knownFailedTests/ directory using codegen Java language code generation under JUnit.

   Copyright (c) 2012 The Regents of the University of California.
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

package ptolemy.codegen.test.junit;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

///////////////////////////////////////////////////////////////////
//// AutoCodegenCKnownFailedTests

/**
 * Run the Ptolemy model tests in the auto/knownFailedTests directory
 * using codegen Java language code generation under JUnit.
 * 
 * <p> This test must be run from the directory that contains the
 * auto/knowFailedTests/ directory, for example: </p>
 * 
 * <pre>
 * (cd $PTII/ptolemy/codegen/adapter/generic/program/procedural/java/adapters/ptolemy/actor/lib/test; java -classpath ${PTII}:${PTII}/lib/junit-4.8.2.jar:${PTII}/lib/JUnitParams-0.3.0.jar org.junit.runner.JUnitCore ptolemy.util.test.junit.AutoCodegenCKnownFailedTests)
 * </pre>
 * 
 * <p>
 * This test uses JUnitParams from 
 * <a href="http://code.google.com/p/junitparams/#in_browser">http://code.google.com/p/junitparams/</a>, which is released under
 * <a href="http://www.apache.org/licenses/LICENSE-2.0#in_browser">Apache License 2.0</a>.
 * </p>
 *
 * @author Christopher Brooks
 * @version $Id$
 * @since Ptolemy II 8.1
 * @Pt.ProposedRating Red (cxh)
 * @Pt.AcceptedRating Red (cxh)
 */
@RunWith(JUnitParamsRunner.class)
public class AutoCodegenJavaKnownFailedTests extends AutoCodegenKnownFailedTests {

    /**
     * Find the ptolemy.codegen.kernel.generic.GenericCodeGenerator class and its generateCode static
     * method that takes an array of strings.
     * 
     * @exception Throwable
     *                If the class or constructor cannot be found.
     */
    @Before
    public void setUp() throws Throwable {
        super.setUp();
    }

    /**
     * Generate, compile and run inline code for a model.
     * 
     * @param fullPath The full path to the model file to be
     * executed. If the fullPath ends with the value of the {@link
     * #THERE_ARE_NO_AUTO_TESTS}, then the method returns
     * immediately.
     * @exception Throwable If thrown while executing the model.
     */
    @Test
    @Parameters(method = "modelValues")
    public void runModelInline(String fullPath) throws Throwable {
        runModel(fullPath, "ptolemy.codegen.java",
                true /* inline */);
    }
         
    /**
     * Generate, compile and run code as if the model was very large.
     * 
     * @param fullPath The full path to the model file to be
     * executed. If the fullPath ends with the value of the {@link
     * #THERE_ARE_NO_AUTO_TESTS}, then the method returns
     * immediately.
     * @exception Throwable If thrown while executing the model.
     */
    @Test
    @Parameters(method = "modelValues")
    public void runModelLarge(String fullPath) throws Throwable {
        runModel(fullPath, "ptolemy.codegen.java",
                false /* inline */);
    }
}
