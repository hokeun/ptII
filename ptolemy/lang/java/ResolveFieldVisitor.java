/* Resolve fields, overloading, and do other random semantic checks.

Copyright (c) 1998-2001 The Regents of the University of California.
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

@ProposedRating Red (ctsay@eecs.berkeley.edu)
@AcceptedRating Red (ctsay@eecs.berkeley.edu)

*/

package ptolemy.lang.java;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ptolemy.lang.*;
import ptolemy.lang.java.nodetypes.*;

/** A visitor that does field and method resolution. Additionally,
ASTs that were previously loaded in shallow mode, but whose deep
versions are need, are loaded on demand (based on field/method/constructor
references.
<p>
Portions of this code were derived from sources developed under the
auspices of the Titanium project, under funding from the DARPA, DoE,
and Army Research Office.

@author Jeff Tsay, Shuvra S. Bhattacharyya 
@version $Id$
 */
public class ResolveFieldVisitor extends ReplacementJavaVisitor
    implements JavaStaticSemanticConstants {
    public ResolveFieldVisitor() {
        this(new TypeVisitor());
    }

    public ResolveFieldVisitor(TypeVisitor typeVisitor) {
        super(TM_CUSTOM);

        _typeID = typeVisitor.typeIdentifier();
        _typePolicy = typeVisitor.typePolicy();
        _typeVisitor = typeVisitor;
    }

    public Object visitCompileUnitNode(CompileUnitNode node, LinkedList args) {
        if (StaticResolution.debugLoading) {
            System.out.println("ResolveFieldVisitor.visitCompileUnitNode: "
                    + ASTReflect.getFullyQualifiedName(node));
        }

        _currentPackage = (PackageDecl) node.getDefinedProperty(PACKAGE_KEY);

        LinkedList childArgs = TNLManip.addFirst(new FieldContext());

        TNLManip.traverseList(this, childArgs, node.getDefTypes());

        //System.out.println("finished resolve field on " +
        //        node.getDefinedProperty(IDENT_KEY));

        return node;
    }

    public Object visitClassDeclNode(ClassDeclNode node, LinkedList args) {
        FieldContext subCtx = (FieldContext) ((FieldContext) args.get(0)).clone();
        ClassDecl d = (ClassDecl) JavaDecl.getDecl((NamedNode) node);

        subCtx.currentClass = d.getDefType();
        subCtx.currentClassDecl = d;

        LinkedList childArgs = TNLManip.addFirst(subCtx);

        TNLManip.traverseList(this, childArgs, node.getMembers());

        return node;
    }

    public Object visitFieldDeclNode(FieldDeclNode node, LinkedList args) {
        FieldContext subCtx = (FieldContext) ((FieldContext) args.get(0)).clone();
        subCtx.inStatic = ((node.getModifiers() & STATIC_MOD) != 0);

        LinkedList childArgs = TNLManip.addFirst(subCtx);

        node.setInitExpr((TreeNode) node.getInitExpr().accept(this, childArgs));

        return node;
    }

    public Object visitMethodDeclNode(MethodDeclNode node, LinkedList args) {
        FieldContext subCtx = (FieldContext) ((FieldContext) args.get(0)).clone();
        subCtx.inStatic = ((node.getModifiers() & STATIC_MOD) != 0);

        LinkedList childArgs = TNLManip.addFirst(subCtx);

        node.setParams(TNLManip.traverseList(this, childArgs, node.getParams()));
        node.setBody((TreeNode) node.getBody().accept(this, childArgs));

        return node;
    }

    public Object visitConstructorDeclNode(ConstructorDeclNode node, LinkedList args) {
        FieldContext ctx = (FieldContext) args.get(0);
        ClassDecl classDecl =
            (ClassDecl) JavaDecl.getDecl((NamedNode) ctx.currentClass);
        String className = classDecl.getName();

        if (!node.getName().getIdent().equals(className)) {
            throw new RuntimeException("constructor for " + className + " must be named " +
                    className);
        }

        node.setParams(TNLManip.traverseList(this, args, node.getParams()));
        node.setConstructorCall((ConstructorCallNode)
                node.getConstructorCall().accept(this, args));
        node.setBody((BlockNode) node.getBody().accept(this, args));

        return node;
    }

    public Object visitThisConstructorCallNode(ThisConstructorCallNode node, LinkedList args) {
        FieldContext ctx = (FieldContext) args.get(0);
        ClassDecl classDecl = ctx.currentClassDecl;
        Scope classScope = classDecl.getScope();

        ScopeIterator methods = classScope.lookupFirstLocal(classDecl.getName(),
                CG_CONSTRUCTOR);

        node.setArgs(TNLManip.traverseList(this, args, node.getArgs()));

        node.setProperty(DECL_KEY, resolveCall(methods, node.getArgs()));

        // checkFieldAccess omitted

        return node;
    }

    public Object visitSuperConstructorCallNode(SuperConstructorCallNode node, LinkedList args) {

        FieldContext ctx = (FieldContext) args.get(0);

        ClassDecl superDecl = ctx.currentClassDecl.getSuperClass();

        if (StaticResolution.debugLoading) {
            System.out.println("ResolveFieldVisitor.visitSuperConstructorCallNode: "
                    + ctx.currentClassDecl.getName() + "/" + superDecl.getName()); 
            System.out.println("Super's AST:"); 
            System.out.println(superDecl.getSource().toString()); 
        }

        if (superDecl == null) {
            // the class is 'Object'
            return AbsentTreeNode.instance;
        }

        // Make sure the super class has a deeply- or fully-loaded AST.
        ASTReflect.ensureDeepLoading(superDecl.getSource());

        Scope superScope = superDecl.getScope();

        ScopeIterator methods = superScope.lookupFirstLocal(superDecl.getName(),
                CG_CONSTRUCTOR);

        node.setArgs(TNLManip.traverseList(this, args, node.getArgs()));

        node.setProperty(DECL_KEY, resolveCall(methods, node.getArgs()));

        // checkFieldAccess(decl(), ctx->currentClass->decl()->superClass(),
        //true, true, true, ctx, position());
        return node;
    }

    public Object visitStaticInitNode(StaticInitNode node, LinkedList args) {
        FieldContext subCtx = (FieldContext) ((FieldContext) args.get(0)).clone();

        subCtx.inStatic = true;

        LinkedList childArgs = TNLManip.addFirst(subCtx);

        node.getBlock().accept(this, childArgs);

        return node;
    }

    public Object visitInterfaceDeclNode(InterfaceDeclNode node, LinkedList args) {
        FieldContext subCtx = (FieldContext) ((FieldContext) args.get(0)).clone();
        ClassDecl d = (ClassDecl) JavaDecl.getDecl((NamedNode) node);

        subCtx.currentClass = d.getDefType();

        LinkedList childArgs = TNLManip.addFirst(subCtx);

        TNLManip.traverseList(this, childArgs, node.getMembers());

        return node;
    }

    public Object visitThisNode(ThisNode node, LinkedList args) {
        FieldContext ctx = (FieldContext) args.get(0);
        if (ctx.inStatic) {
            throw new RuntimeException("cannot use 'this' in static code");
        }

        return node;
    }

    public Object visitObjectFieldAccessNode(ObjectFieldAccessNode node, LinkedList args) {
        FieldContext ctx = (FieldContext) args.get(0);

        FieldContext subCtx = (FieldContext) ctx.clone();

        subCtx.methodArgs = null;

        // FIXME : getObject() should return an expression after name resolution
        ExprNode expr = (ExprNode) node.getObject().accept(this, TNLManip.addFirst(subCtx));

        node.setObject(expr);

        TypeNode ot = _typeVisitor.type(expr);

        if (!(_typePolicy.isReferenceType(ot) || _typePolicy.isArrayType(ot))) {
            throw new RuntimeException("attempt to select from non-reference type " + ot);
        } else {
            resolveAField(node, false, false, ctx);
        }

        if (expr.classID() == THISNODE_ID) {
            ThisFieldAccessNode retval = new ThisFieldAccessNode(node.getName());

            retval.setProperty(THIS_CLASS_KEY,
                    (TypeNameNode) expr.getDefinedProperty(THIS_CLASS_KEY));

            return retval.accept(this, args);
        }

        return node;
    }

    public Object visitSuperFieldAccessNode(SuperFieldAccessNode node, LinkedList args) {
        FieldContext ctx = (FieldContext) args.get(0);

        if (ctx.inStatic) {
            throw new RuntimeException("cannot use 'super' in static code");
        }

        resolveAField(node, true, true, ctx);

        return node;
    }

    public Object visitTypeFieldAccessNode(TypeFieldAccessNode node, LinkedList args) {
        FieldContext ctx = (FieldContext) args.get(0);

        resolveAField(node, true, false, ctx);

        JavaDecl d = JavaDecl.getDecl((NamedNode) node);

        if ((d.getModifiers() & STATIC_MOD) == 0) {
            ClassDecl typeDecl = (ClassDecl) JavaDecl.getDecl((NamedNode) node.getFType());

            if (!_typePolicy.isSubClass(ctx.currentClassDecl, typeDecl)) {
                throw new RuntimeException("access to non-static " + d.getName() +
                        " that does not exist in this class");
            }
        }

        return node;
    }

    public Object visitThisFieldAccessNode(ThisFieldAccessNode node, LinkedList args) {
        FieldContext ctx = (FieldContext) args.get(0);

        resolveAField(node, true, false, ctx);

        return node;
    }

    public Object visitMethodCallNode(MethodCallNode node, LinkedList args) {
        FieldContext ctx = (FieldContext) args.get(0);

        TNLManip.traverseList(this, args, node.getArgs());

        FieldContext subCtx = (FieldContext) ctx.clone();
        subCtx.methodArgs = node.getArgs();

        node.setMethod((ExprNode) node.getMethod().accept(this, TNLManip.addFirst(subCtx)));
        return node;
    }

    public Object visitOuterThisAccessNode(OuterThisAccessNode node, LinkedList args) {
        FieldContext ctx = (FieldContext) args.get(0);
        if (ctx.inStatic) {
            throw new RuntimeException("cannot use 'this' in static code");
        }
        return node;
    }

    public Object visitOuterSuperAccessNode(OuterSuperAccessNode node, LinkedList args) {
        FieldContext ctx = (FieldContext) args.get(0);
        if (ctx.inStatic) {
            throw new RuntimeException("cannot use 'super' in static code");
        }
        return node;
    }

    public Object visitAllocateNode(AllocateNode node, LinkedList args) {
        //  dtype()->resolveField (ctx, NULL);
        FieldContext ctx = (FieldContext) args.get(0);

        if (!ctx.inStatic &&
                (node.getEnclosingInstance() == AbsentTreeNode.instance)) {
            ThisNode thisNode = new ThisNode();

            // duplicates what's done by ResolveNameVisitor
            thisNode.setProperty(THIS_CLASS_KEY, ctx.currentClass);
            node.setEnclosingInstance((TreeNode) thisNode.accept(this, args));
        } else {
            node.setEnclosingInstance((TreeNode)
                    node.getEnclosingInstance().accept(this, args));
        }

        node.setArgs(TNLManip.traverseList(this, args, node.getArgs()));

        TypeNameNode typeName = node.getDtype();

        if (!_typeID.isClassKind(_typeID.kind(typeName))) {
            throw new RuntimeException("can't allocate something of non-class type " +
                    typeName.getName().getIdent());
        } else {
            ClassDecl typeDecl = (ClassDecl) JavaDecl.getDecl((NamedNode) typeName);

            if (StaticResolution.traceLoading) {
                System.out.println("ResolveFieldVisitor.visitAllocateNode with type "
                        + "name: " + typeName.getName().getIdent());
            }

            ASTReflect.ensureDeepLoading(typeDecl.getSource());

            if ((typeDecl.getModifiers() & ABSTRACT_MOD) != 0) {
                throw new RuntimeException("cannot allocate abstract " +
                        typeName.getName().getIdent());
            }

            ScopeIterator methods = typeDecl.getScope().lookupFirstLocal(
                    typeDecl.getName(), CG_CONSTRUCTOR);

            MethodDecl constructor = resolveCall(methods, node.getArgs());

            node.setProperty(DECL_KEY, constructor);
        }

        // checkFieldAccess(constructor, dtype()->decl(), false, false, true,
    	//	     ctx, position());

        return node;
    }

    public Object visitAllocateAnonymousClassNode(AllocateAnonymousClassNode node, LinkedList args) {
        FieldContext ctx = (FieldContext) args.get(0);

        if (!ctx.inStatic &&
                (node.getEnclosingInstance() == AbsentTreeNode.instance)) {
            ThisNode thisNode = new ThisNode();

            // duplicates what's done by ResolveNameVisitor
            thisNode.setProperty(THIS_CLASS_KEY, ctx.currentClass);
            node.setEnclosingInstance((TreeNode) thisNode.accept(this, args));
        } else {
            node.setEnclosingInstance((TreeNode)
                    node.getEnclosingInstance().accept(this, args));
        }

        node.setSuperArgs(TNLManip.traverseList(this, args, node.getSuperArgs()));

        ClassDecl superDecl = (ClassDecl) node.getDefinedProperty(SUPERCLASS_KEY);

        ScopeIterator methods = superDecl.getScope().lookupFirstLocal(
                superDecl.getName(), CG_CONSTRUCTOR);

        MethodDecl constructor = resolveCall(methods, node.getSuperArgs());

        node.setProperty(CONSTRUCTOR_KEY, constructor);

        FieldContext subCtx = new FieldContext();
        subCtx.currentClass = (TypeNameNode) node.getDefinedProperty(TYPE_KEY);
        subCtx.currentClassDecl = (ClassDecl) node.getDefinedProperty(DECL_KEY);
        subCtx.inStatic = false;
        subCtx.methodArgs = null;

        node.setMembers(TNLManip.traverseList(this, TNLManip.addFirst(subCtx),
                node.getMembers()));

        return node;
    }

    // default visit is from ResolveVisitorBase


    /** Return true iff MethodDecl m1 is more specific
     *  (in the sense of 14.11.2.2) than MethodDecl m2.  Actually, the right term
     *  should be "no less specific than", but the Reference Manual is the
     *  Reference Manual.
     */
    public boolean isMoreSpecific(final MethodDecl m1, final MethodDecl m2) {

        List params1 = m1.getParams();
        List params2 = m2.getParams();

        if (params2.size() != params2.size()) {
            return false;
        }

        ClassDecl container2 = (ClassDecl) m2.getContainer();
        TypeNameNode classType2 = container2.getDefType();

        ClassDecl container1 = (ClassDecl) m1.getContainer();
        TypeNameNode classType1 = container1.getDefType();

        if (!_typePolicy.isAssignableFromType(classType2, classType1)) {
            return false;
        }

        Iterator params1Itr = params1.iterator();
        Iterator params2Itr = params2.iterator();

        while (params1Itr.hasNext()) {
            TypeNode param2 = (TypeNode) params2Itr.next();
            TypeNode param1 = (TypeNode) params1Itr.next();

            if (!_typePolicy.isAssignableFromType(param2, param1)) {
                return false;
            }
        }
        return true;
    }

    public boolean isCallableWith(MethodDecl m, List argTypes) {
        List formalTypes = m.getParams();

        if (argTypes.size() != formalTypes.size()) {
            return false;
        }

        Iterator formalItr = formalTypes.iterator();
        Iterator argItr = argTypes.iterator();

        while (formalItr.hasNext()) {
            TypeNode formalType = (TypeNode) formalItr.next();
            TypeNode argType = (TypeNode) argItr.next();

            if (!_typePolicy.isAssignableFromType(formalType, argType)) {
                return false;
            }
        }
        return true;
    }

    protected void resolveAField(FieldAccessNode node, boolean thisAccess, boolean isSuper,
            FieldContext ctx) {
        ScopeIterator resolutions;
        TypeNode oType = _typeVisitor.accessedObjectType(node);
        ClassDecl typeDecl;

        if (oType.classID() == ARRAYTYPENODE_ID) {
            typeDecl = StaticResolution.ARRAY_CLASS_DECL;
        } else {
            typeDecl = (ClassDecl) JavaDecl.getDecl((NamedNode) oType);
        }

        JavaDecl d = null;
        List methodArgs = ctx.methodArgs;
        String nameString = node.getName().getIdent();

        if (methodArgs == null) {
            d = JavaDecl.getDecl((NamedNode) node);
            if (d == null) { // don't repeat work
                resolutions = typeDecl.getScope().lookupFirstLocal(nameString, CG_FIELD);

                if (!resolutions.hasNext()) {
                    throw new RuntimeException ("no " + nameString + " field in " +
                            typeDecl.getName());
                } else {
         	    d = (JavaDecl) resolutions.nextDecl();
        	    if (resolutions.hasNext()) {
                        throw new RuntimeException("ambiguous reference to " + d.getName());
                    }
                }
            }
        } else {
            resolutions = typeDecl.getScope().lookupFirstLocal(nameString,
                    CG_METHOD);

            if (!resolutions.hasNext()) {
                throw new RuntimeException("no " + nameString + " method in " +
                        typeDecl.getName());
            } else {
                d = resolveCall(resolutions, methodArgs);
            }
        }

        node.getName().setProperty(DECL_KEY, d);
        //checkFieldAccess(d, typeDecl, thisAccess, isSuper, false, ctx, position());
    }

    public MethodDecl resolveCall(ScopeIterator methods, List args) {

        Decl aMethod = methods.peek();
        Decl d;

        LinkedList types = new LinkedList();

        LinkedList argTypes = new LinkedList();

        Iterator argsItr = args.iterator();

        while (argsItr.hasNext()) {
            ExprNode expr = (ExprNode) argsItr.next();
            argTypes.addLast(_typeVisitor.type(expr));
        }

        LinkedList matches = new LinkedList();


	// FIXME: remove debugMatches when we are done debugging
        StringBuffer debugMatches = new StringBuffer();

        while (methods.hasNext()) {
            MethodDecl method = (MethodDecl) methods.next();


	    debugMatches.append("-" + method.toString() + " " + method.getName());
	    List formalTypes = method.getParams();
	    Iterator formalItr2 = formalTypes.iterator();
	    while (formalItr2.hasNext()) {
		debugMatches.append(formalItr2.next().toString());
	    }

            if (isCallableWith(method, argTypes)) {
                matches.addLast(method);
            }
        }

        if (matches.size() == 0) {
            throw new RuntimeException("no matching method" + aMethod.getName() +
                    "(" + TNLManip.toString(argTypes) + ")" + debugMatches);
        }

        Iterator matchesItr1 = matches.iterator();

        while (matchesItr1.hasNext()) {
            MethodDecl m1 = (MethodDecl) matchesItr1.next();
            Iterator matchesItr2 = matches.iterator();
            boolean thisOne = true;

            while (matchesItr2.hasNext()) {
                MethodDecl m2 = (MethodDecl) matchesItr2.next();
                if (m1 == m2) {
                    continue; // get out of this inner loop
                }
                if (!isMoreSpecific(m1, m2) || isMoreSpecific(m2, m1)) {
                    thisOne = false; // keep looking
                    continue; // get out of this inner loop
                }
            }

            if (thisOne) {
                return m1;
            }
        }

        throw new RuntimeException ("ambiguous method call to " + aMethod.getName());
        //return null;
    }

    protected static class FieldContext implements Cloneable {
        public FieldContext() {}

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException cnse) {
                throw new InternalError("clone of FieldContext not supported");
            }
        }

        /** The type representing the class we are currently in. */
        public TypeNameNode currentClass = null;

        /** The declaration representing the class we are currently in. */
        public ClassDecl currentClassDecl = null;

        /** A flag indicating that we are in static code. */
        public boolean inStatic = false;

        /** A list of the method arguments. */
        public List methodArgs = null;
    }

    protected final TypeIdentifier _typeID;
    protected final TypePolicy _typePolicy;
    protected final TypeVisitor _typeVisitor;

    /** The current package. */
    protected PackageDecl _currentPackage = null;


}
