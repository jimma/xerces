/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xerces" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 2001, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.xerces.impl.v2;

import  org.apache.xerces.impl.v2.datatypes.*;
import  org.apache.xerces.impl.XMLErrorReporter;
import  org.apache.xerces.util.DOMUtil;
import  org.apache.xerces.util.XInt;
import  org.apache.xerces.util.XIntPool;
import  org.apache.xerces.xni.QName;
import  org.w3c.dom.Element;
import java.util.Hashtable;


/**
 * The element declaration schema component traverser.
 * <element
 *   abstract = boolean : false
 *   block = (#all | List of (extension | restriction | substitution))
 *   default = string
 *   final = (#all | List of (extension | restriction))
 *   fixed = string
 *   form = (qualified | unqualified)
 *   id = ID
 *   maxOccurs = (nonNegativeInteger | unbounded)  : 1
 *   minOccurs = nonNegativeInteger : 1
 *   name = NCName
 *   nillable = boolean : false
 *   ref = QName
 *   substitutionGroup = QName
 *   type = QName
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, ((simpleType | complexType)?, (unique | key | keyref)*))
 * </element>
 *
 * @version $Id$
 */
class XSDElementTraverser extends XSDAbstractTraverser{

    protected static final QName ANY_TYPE        = new QName(null,
                                                             SchemaSymbols.ATTVAL_ANYTYPE,
                                                             SchemaSymbols.ATTVAL_ANYTYPE,
                                                             SchemaSymbols.URI_SCHEMAFORSCHEMA);

    protected XSElementDecl  fTempElementDecl  = new XSElementDecl();
    protected XSParticleDecl fTempParticleDecl = new XSParticleDecl();
    
    // the initial size of the array to store deferred local elements
    static final int INIT_STACK_SIZE = 30;
    // the incremental size of the array to store deferred local elements
    static final int INC_STACK_SIZE  = 10;
    // current position of the array (# of deferred local elements)
    int fStackPos = 0;

    private int[] fParticleIdx = new int[INIT_STACK_SIZE];
    private Element[] fElementDecl = new Element[INIT_STACK_SIZE];
    private XSDocumentInfo[] fSchemaDoc = new XSDocumentInfo[INIT_STACK_SIZE];
    private SchemaGrammar[] fGrammar = new SchemaGrammar[INIT_STACK_SIZE];
    
    SubstitutionGroupHandler fSubGroupHandler;
    
    XSDElementTraverser (XSDHandler handler,
                         XMLErrorReporter errorReporter,
                         XSAttributeChecker gAttrCheck,
                         SubstitutionGroupHandler subGroupHandler) {
        super(handler, errorReporter, gAttrCheck);
        fSubGroupHandler = subGroupHandler;
    }

    /**
     * Traverse a locally declared element (or an element reference).
     *
     * To handle the recursive cases effeciently, we delay the traversal
     * and return an empty particle node. We'll fill in this particle node
     * later after we've done with all the global declarations.
     *
     * @param  elmDecl
     * @param  schemaDoc
     * @param  grammar
     * @return the particle node index
     */
    int traverseLocal(Element elmDecl,
                      XSDocumentInfo schemaDoc,
                      SchemaGrammar grammar) {

        // if the stack is full, increase the size
        if (fParticleIdx.length == fStackPos) {
            // increase size
            int[] newStackI = new int[fStackPos+INC_STACK_SIZE];
            System.arraycopy(fParticleIdx, 0, newStackI, 0, fStackPos);
            Element[] newStackE = new Element[fStackPos+INC_STACK_SIZE];
            System.arraycopy(fElementDecl, 0, newStackE, 0, fStackPos);
            XSDocumentInfo[] newStackX = new XSDocumentInfo[fStackPos+INC_STACK_SIZE];
            System.arraycopy(fSchemaDoc, 0, newStackX, 0, fStackPos);
            SchemaGrammar[] newStackS = new SchemaGrammar[fStackPos+INC_STACK_SIZE];
            System.arraycopy(fGrammar, 0, newStackS, 0, fStackPos);
        }

        fTempParticleDecl.clear();
        fTempParticleDecl.type = XSParticleDecl.PARTICLE_EMPTY;
        int particleIdx = grammar.addParticleDecl(fTempParticleDecl);

        fParticleIdx[fStackPos] = particleIdx;
        fElementDecl[fStackPos] = elmDecl;
        fSchemaDoc[fStackPos] = schemaDoc;
        fGrammar[fStackPos] = grammar;

        return particleIdx;
    }

    /**
     * Traverse all the deferred local elements. This method should be called
     * by the handler after we've done with all the global declarations.
     */
    void traverseLocalElements() {
        for (int i = 0; i < fStackPos; i++) {
            traverseLocal(i);
        }
    }
    
    /**
     * Traverse a locally declared element (or an element reference).
     *
     * This is the real traversal method. It's called after we've done with
     * all the global declarations.
     *
     * @param  index
     */
    private void traverseLocal(int index) {

        int particleIdx = fParticleIdx[index];
        Element elmDecl = fElementDecl[index];
        XSDocumentInfo schemaDoc = fSchemaDoc[index];
        SchemaGrammar grammar = fGrammar[index];

        // General Attribute Checking
        Object[] attrValues = fAttrChecker.checkAttributes(elmDecl, false, schemaDoc.fNamespaceSupport);

        QName   refAtt = (QName)   attrValues[XSAttributeChecker.ATTIDX_REF];
        XInt minAtt = (XInt) attrValues[XSAttributeChecker.ATTIDX_MINOCCURS];
        XInt maxAtt = (XInt) attrValues[XSAttributeChecker.ATTIDX_MAXOCCURS];

        int elemIdx = XSDHandler.I_EMPTY_DECL;
        if (refAtt != null) {
            elemIdx = fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.ELEMENT_TYPE, refAtt);
            if (elemIdx == XSDHandler.I_NOT_FOUND) {
                reportGenericSchemaError("element not found: "+refAtt.uri+","+refAtt.localpart);
                elemIdx = XSDHandler.I_EMPTY_DECL;
            }

            Element child = DOMUtil.getFirstChildElement(elmDecl);
            if(child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                traverseAnnotationDecl(child, attrValues, false, schemaDoc);
                child = DOMUtil.getNextSiblingElement(child);
            }
    
            if (child != null) {
                reportGenericSchemaError("src-element.0: the content of an element information item with 'ref' must match (annotation?)");
            }
            
        } else {
            elemIdx = traverseNamedElement(elmDecl, attrValues, schemaDoc, grammar, false);
        }

        fTempParticleDecl.clear();
        fTempParticleDecl.type = XSParticleDecl.PARTICLE_ELEMENT;
        fTempParticleDecl.uri = refAtt == null ? schemaDoc.fTargetNamespace : refAtt.uri;
        fTempParticleDecl.value = elemIdx;
        fTempParticleDecl.minOccurs = minAtt.intValue();
        fTempParticleDecl.maxOccurs = maxAtt.intValue();
        
        fAttrChecker.returnAttrArray(attrValues, schemaDoc.fNamespaceSupport);
    }

    /**
     * Traverse a globally declared element.
     *
     * @param  elmDecl
     * @param  schemaDoc
     * @param  grammar
     * @return the element declaration index
     */
    int traverseGlobal(Element elmDecl,
                       XSDocumentInfo schemaDoc,
                       SchemaGrammar grammar) {

        // General Attribute Checking
        Object[] attrValues = fAttrChecker.checkAttributes(elmDecl, true, schemaDoc.fNamespaceSupport);
        int elemIdx = traverseNamedElement(elmDecl, attrValues, schemaDoc, grammar, true);
        fAttrChecker.returnAttrArray(attrValues, schemaDoc.fNamespaceSupport);

        return elemIdx;
    }

    /**
     * Traverse a globally declared element.
     *
     * @param  elmDecl
     * @param  attrValues
     * @param  schemaDoc
     * @param  grammar
     * @param  isGlobal
     * @return the element declaration index
     */
    int traverseNamedElement(Element elmDecl,
                             Object[] attrValues,
                             XSDocumentInfo schemaDoc,
                             SchemaGrammar grammar,
                             boolean isGlobal) {

        Boolean abstractAtt  = (Boolean) attrValues[XSAttributeChecker.ATTIDX_ABSTRACT];
        XInt    blockAtt     = (XInt)    attrValues[XSAttributeChecker.ATTIDX_BLOCK];
        String  defaultAtt   = (String)  attrValues[XSAttributeChecker.ATTIDX_DEFAULT];
        XInt    finalAtt     = (XInt)    attrValues[XSAttributeChecker.ATTIDX_FINAL];
        String  fixedAtt     = (String)  attrValues[XSAttributeChecker.ATTIDX_FIXED];
        XInt    formAtt      = (XInt)    attrValues[XSAttributeChecker.ATTIDX_FORM];
        String  nameAtt      = (String)  attrValues[XSAttributeChecker.ATTIDX_NAME];
        Boolean nillableAtt  = (Boolean) attrValues[XSAttributeChecker.ATTIDX_NILLABLE];
        QName   subGroupAtt  = (QName)   attrValues[XSAttributeChecker.ATTIDX_SUBSGROUP];
        QName   typeAtt      = (QName)   attrValues[XSAttributeChecker.ATTIDX_TYPE];

        // Step 1: get declaration information
        
        // get 'target namespace'
        String namespace = XSDHandler.EMPTY_STRING;
        if (isGlobal) {
            namespace = schemaDoc.fTargetNamespace;
        }
        else if (formAtt != null) {
            if (formAtt.intValue() == SchemaSymbols.FORM_QUALIFIED)
                namespace = schemaDoc.fTargetNamespace;
        } else if (schemaDoc.fAreLocalElementsQualified) {
            namespace = schemaDoc.fTargetNamespace;
        }

        // get 'block', 'final', 'nillable', 'abstract'
        short blockSet = blockAtt == null ? SchemaSymbols.EMPTY_SET : blockAtt.shortValue();
        short finalSet = finalAtt == null ? SchemaSymbols.EMPTY_SET : finalAtt.shortValue();
        short elementMiscFlags = 0;
        if (nillableAtt.booleanValue())
            elementMiscFlags |= XSElementDecl.NILLABLE;
        if (abstractAtt.booleanValue())
            elementMiscFlags |= XSElementDecl.ABSTRACT;
        // make the property of the element's value being fixed also appear in elementMiscFlags
        if (fixedAtt != null)
            elementMiscFlags |= XSElementDecl.FIXED;

        // get 'value constraint'
        if (defaultAtt == null && fixedAtt != null) {
            defaultAtt = fixedAtt;
            fixedAtt = null;
        }

        // get 'substitutionGroup affiliation'
        String subGroupNS = null;
        int subGroupIndex = XSDHandler.I_EMPTY_DECL;
        if (subGroupAtt != null) {
            subGroupIndex = fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.ELEMENT_TYPE, subGroupAtt);
            if (subGroupIndex == XSDHandler.I_NOT_FOUND) {
                reportGenericSchemaError("substitutionGroup element not found: "+subGroupAtt.uri+","+subGroupAtt.localpart+" for element '"+nameAtt+"'");
                subGroupIndex = XSDHandler.I_EMPTY_DECL;
            } else {
                subGroupNS = subGroupAtt.uri;
            }
        }
        
        // get 'annotation'
        Element child = DOMUtil.getFirstChildElement(elmDecl);
        if(child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
			traverseAnnotationDecl(child, attrValues, false, schemaDoc);
            child = DOMUtil.getNextSiblingElement(child);
		}

        // get 'type definition'
        String typeNS = null;
        int elementType = XSDHandler.I_EMPTY_DECL;
        boolean haveAnonType = false;

        // Handle Anonymous type if there is one
        if (child != null) {
            String childName = DOMUtil.getLocalName(child);

            if (childName.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
                elementType = fSchemaHandler.fComplexTypeTraverser.traverseLocal(child, schemaDoc, grammar);
                if (elementType != XSDHandler.I_EMPTY_DECL)
                    typeNS = schemaDoc.fTargetNamespace;
                haveAnonType = true;
            	child = DOMUtil.getNextSiblingElement(child);
            }
            else if (childName.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                elementType = fSchemaHandler.fSimpleTypeTraverser.traverseLocal(child, schemaDoc, grammar);
                if (elementType != XSDHandler.I_EMPTY_DECL)
                    typeNS = schemaDoc.fTargetNamespace;
                haveAnonType = true;
            	child = DOMUtil.getNextSiblingElement(child);
            }
        }

        // Handler type attribute
        if (elementType == XSDHandler.I_EMPTY_DECL && typeAtt != null) {
            elementType = fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.TYPEDECL_TYPE, typeAtt);
            if (elementType == XSDHandler.I_NOT_FOUND) {
                reportGenericSchemaError("type not found: '"+typeAtt.uri+","+typeAtt.localpart+"' for element '"+nameAtt+"'");
                elementType = XSDHandler.I_EMPTY_DECL;
            } else {
                typeNS = typeAtt.uri;
            }
        }
        
        // Get it from the substitutionGroup declaration
        if (elementType == XSDHandler.I_EMPTY_DECL && subGroupIndex != XSDHandler.I_EMPTY_DECL) {
            fTempElementDecl = fSchemaHandler.getElementDecl(subGroupNS, subGroupIndex, fTempElementDecl);
            elementType = fTempElementDecl.fTypeIdx;
            if (elementType != XSDHandler.I_EMPTY_DECL)
                typeNS = fTempElementDecl.fTypeNS;
        }

        if (elementType == XSDHandler.I_EMPTY_DECL) {
            elementType = fSchemaHandler.getGlobalDecl(schemaDoc, fSchemaHandler.TYPEDECL_TYPE, ANY_TYPE);
            typeNS = SchemaSymbols.URI_SCHEMAFORSCHEMA;
        }

        // get 'identity constaint'
        
        // see if there's something here; it had better be key, keyref or unique.
        if (child != null) {
            String childName = DOMUtil.getLocalName(child);
            while (child != null &&
                   (childName.equals(SchemaSymbols.ELT_KEY) ||
                    childName.equals(SchemaSymbols.ELT_KEYREF) ||
                    childName.equals(SchemaSymbols.ELT_UNIQUE))) {
                child = DOMUtil.getNextSiblingElement(child);
                if (child != null) {
                    childName = DOMUtil.getLocalName(child);
                }
            }
        }
        
        //
        // REVISIT: key/keyref/unique processing
        //

        /*Element ic = XUtil.getFirstChildElementNS(elementDecl, IDENTITY_CONSTRAINTS);
        if (ic != null) {
            XInt elementIndexObj = XIntPool.getXInt(elementIndex);
            Vector identityConstraints = (Vector)fIdentityConstraints.get(elementIndexObj);
            if (identityConstraints == null) {
                identityConstraints = new Vector();
                fIdentityConstraints.put(elementIndexObj, identityConstraints);
            }
            while (ic != null) {
                if (DEBUG_IC_DATATYPES) {
                    System.out.println("<ICD>: adding ic for later traversal: "+ic);
                }
                identityConstraints.addElement(ic);
                ic = XUtil.getNextSiblingElementNS(ic, IDENTITY_CONSTRAINTS);
            }
        }*/

        // Step 2: create the declaration, and register it to the grammar
        fTempElementDecl.clear();
        fTempElementDecl.fName = nameAtt;
        fTempElementDecl.fTargetNamespace = namespace;
        fTempElementDecl.fTypeNS = typeNS;
        fTempElementDecl.fTypeIdx = elementType;
        fTempElementDecl.fElementMiscFlags = elementMiscFlags;
        fTempElementDecl.fBlock = blockSet;
        fTempElementDecl.fFinal = finalSet;
        fTempElementDecl.fDefault = defaultAtt;
        fTempElementDecl.fSubGroupNS = subGroupNS;
        fTempElementDecl.fSubGroupIdx = subGroupIndex;
        int elementIndex = grammar.addElementDecl(fTempElementDecl, isGlobal);

        // Step 3: check against schema for schemas
        
        // required attributes
        if (nameAtt == null) {
            if (isGlobal)
                reportGenericSchemaError("src-element.0: 'name' must be present in a global element declaration");
            else
                reportGenericSchemaError("src-element.2.1: One of 'ref' or 'name' must be present in a local element declaration");
        }
        
        // element
        if (child != null) {
            reportGenericSchemaError("src-element.0: the content of an element information item must match (annotation?, (simpleType | complexType)?, (unique | key | keyref)*))");
        }

        // Step 4: check 3.3.3 constraints
        
        // src-element
        
        // 1 default and fixed must not both be present. 
		if (defaultAtt != null && fixedAtt != null) {
			reportGenericSchemaError("src-element.1: 'default' and 'fixed' must not both be present in element declaration '" + nameAtt + "'");
        }

        // 2 If the item's parent is not <schema>, then all of the following must be true:
        // 2.1 One of ref or name must be present, but not both. 
        // This is checked in XSAttributeChecker
        
        // 2.2 If ref is present, then all of <complexType>, <simpleType>, <key>, <keyref>, <unique>, nillable, default, fixed, form, block and type must be absent, i.e. only minOccurs, maxOccurs, id are allowed in addition to ref, along with <annotation>. 
        // Attributes are checked in XSAttributeChecker, elements are checked in "traverse" method
        
        // 3 type and either <simpleType> or <complexType> are mutually exclusive. 
        if (haveAnonType && (typeAtt != null)) {
            reportGenericSchemaError( "src-element.3: Element '"+ nameAtt +
                                      "' have both a type attribute and a annoymous type child" );
        }

        // Step 5: check 3.3.6 constraints
        XSType typeInfo = fSchemaHandler.getXSTypeDecl(typeNS, elementType);
        // check for NOTATION type
        checkNotationType(nameAtt, typeInfo);

        // e-props-correct
        
        // 2 If there is a {value constraint}, the canonical lexical representation of its value must be �valid� with respect to the {type definition} as defined in Element Default Valid (Immediate) (�3.3.6). 
        if (defaultAtt != null) {
            if (!checkDefaultValid(defaultAtt, typeInfo, nameAtt)) {
                reportGenericSchemaError ("e-props-correct.2: invalid fixed or default value '" + defaultAtt + "' in element " + nameAtt);
            }
        }

        // 3 If there is an {substitution group affiliation}, the {type definition} of the element declaration must be validly derived from the {type definition} of the {substitution group affiliation}, given the value of the {substitution group exclusions} of the {substitution group affiliation}, as defined in Type Derivation OK (Complex) (�3.4.6) (if the {type definition} is complex) or as defined in Type Derivation OK (Simple) (�3.14.6) (if the {type definition} is simple). 
        if (subGroupIndex != XSDHandler.I_EMPTY_DECL) {
           if (!fSubGroupHandler.checkSubstitutionGroupOK(typeInfo, subGroupNS, subGroupIndex)) {
                reportGenericSchemaError ("e-props-correct.3: the {type definition} of element '"+nameAtt+"' must be validly derived from the {type definition} of the element '"+subGroupAtt.uri+","+subGroupAtt.localpart+"'");
           }
        }

        // 4 If the {type definition} or {type definition}'s {content type} is or is derived from ID then there must not be a {value constraint}. 
        if (defaultAtt != null) {
            if (typeInfo instanceof IDDatatypeValidator ||
                typeInfo instanceof XSComplexTypeDecl &&
                ((XSComplexTypeDecl)typeInfo).containsTypeID()) {
                reportGenericSchemaError ("e-props-correct.4: if the {type definition} or {type definition}'s {content type} is or is derived from ID then there must not be a {value constraint} -- element " + nameAtt);
            }
        }

        // Step 6: add substitutionGroup information to the handler

        if (subGroupIndex != XSDHandler.I_EMPTY_DECL) {
            fSubGroupHandler.addSubstitutionGroup(schemaDoc.fTargetNamespace, elementIndex,
                                                  subGroupNS, subGroupIndex);
        }

        return elementIndex;
    }

    //private help functions

    // return whether the constraint value is valid for the given type
    boolean checkDefaultValid(String defaultStr, XSType typeInfo, String referName) {

        DatatypeValidator dv = null;
        
        // e-props-correct
        // For a string to be a valid default with respect to a type definition the appropriate case among the following must be true:
        // 1 If the type definition is a simple type definition, then the string must be �valid� with respect to that definition as defined by String Valid (�3.14.4).
        if (typeInfo instanceof DatatypeValidator) {
            dv = (DatatypeValidator)typeInfo;
        }
        
        // 2 If the type definition is a complex type definition, then all of the following must be true:
        else {
            // 2.1 its {content type} must be a simple type definition or mixed. 
            XSComplexTypeDecl ctype = (XSComplexTypeDecl)typeInfo;            
            // 2.2 The appropriate case among the following must be true:
            // 2.2.1 If the {content type} is a simple type definition, then the string must be �valid� with respect to that simple type definition as defined by String Valid (�3.14.4).
            if (ctype.fContentType == XSComplexTypeDecl.CONTENTTYPE_SIMPLE) {
                dv = ctype.fDatatypeValidator;
            }
            // 2.2.2 If the {content type} is mixed, then the {content type}'s particle must be �emptiable� as defined by Particle Emptiable (�3.9.6).
            else if (ctype.fContentType == XSComplexTypeDecl.CONTENTTYPE_MIXED) {
                //REVISIT: to implement
                //if (!particleEmptiable(typeInfo.contentSpecHandle))
                //    reportGenericSchemaError ("e-props-correct.2.2.2: for element " + nameStr + ", the {content type} is mixed, then the {content type}'s particle must be emptiable");
            }
            else {
                reportGenericSchemaError ("e-props-correct.2.1: element " + referName + " has a fixed or default value and must have a mixed or simple content model");
            }
        }

        // get the simple type delaration, and validate
        boolean ret = true;
        if (dv != null) {
            try {
                dv.validate(defaultStr, null);
            } catch (InvalidDatatypeValueException ide) {
                ret = false;
            }
        }
        
        return ret;
    }

}
