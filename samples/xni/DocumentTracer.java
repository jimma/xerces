/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2000,2001 The Apache Software Foundation.  All rights 
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
 * originally based on software copyright (c) 1999, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package xni;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.xerces.parsers.XMLDocumentParser;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLDTDHandler;
import org.apache.xerces.xni.XMLDTDContentModelHandler;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLParserConfiguration;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

/**
 * Provides a complete trace of XNI document and DTD events for 
 * files parsed.
 *
 * @author Andy Clark, IBM
 * @author Arnaud Le Hors, IBM
 *
 * @version $Id$
 */
public class DocumentTracer 
    extends XMLDocumentParser
    implements ErrorHandler {

    //
    // Constants
    //

    // feature ids

    /** Namespaces feature id (http://xml.org/sax/features/namespaces). */
    protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
    
    /** Validation feature id (http://xml.org/sax/features/validation). */
    protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

    /** Schema validation feature id (http://apache.org/xml/features/validation/schema). */
    protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";

    /** Character ref notification feature id (http://apache.org/xml/features/scanner/notify-char-refs). */
    protected static final String NOTIFY_CHAR_REFS_FEATURE_ID = "http://apache.org/xml/features/scanner/notify-char-refs";

    // default settings

    /** Default parser configuration (org.apache.xerces.parsers.StandardParserConfiguration). */
    protected static final String DEFAULT_PARSER_CONFIG = "org.apache.xerces.parsers.StandardParserConfiguration";

    /** Default namespaces support (true). */
    protected static final boolean DEFAULT_NAMESPACES = true;

    /** Default validation support (false). */
    protected static final boolean DEFAULT_VALIDATION = false;
    
    /** Default Schema validation support (true). */
    protected static final boolean DEFAULT_SCHEMA_VALIDATION = true;

    /** Default character notifications (false). */
    protected static final boolean DEFAULT_NOTIFY_CHAR_REFS = false; 
    
    //
    // Data
    //

    /** Temporary QName. */
    private QName fQName = new QName();

    /** Print writer. */
    protected PrintWriter fOut;

    /** Indent level. */
    protected int fIndent;

    //
    // Constructors
    //

    /** Default constructor. */
    public DocumentTracer() {
        this(null);
    } // <init>()

    /** Default constructor. */
    public DocumentTracer(XMLParserConfiguration config) {
        super(config);
        setOutput(new PrintWriter(System.out));
        setErrorHandler(this);
    } // <init>(XMLParserConfiguration)

    //
    // Public methods
    //

    /** Sets the output stream for printing. */
    public void setOutput(OutputStream stream, String encoding)
        throws UnsupportedEncodingException {

        if (encoding == null) {
            encoding = "UTF8";
        }

        Writer writer = new OutputStreamWriter(stream, encoding);
        fOut = new PrintWriter(writer);

    } // setOutput(OutputStream,String)

    /** Sets the output writer. */
    public void setOutput(Writer writer) {
            
        fOut = writer instanceof PrintWriter
             ? (PrintWriter)writer : new PrintWriter(writer);

    } // setOutput(Writer)

    //
    // XMLDocumentHandler methods
    //

    /**
     * The start of the document.
     *
     * @param systemId The system identifier of the entity if the entity
     *                 is external, null otherwise.
     * @param encoding The auto-detected IANA encoding name of the entity
     *                 stream. This value will be null in those situations
     *                 where the entity encoding is not auto-detected (e.g.
     *                 internal entities or a document entity that is
     *                 parsed from a java.io.Reader).
     *     
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startDocument(String systemId, String encoding) 
        throws XNIException {

        fIndent = 0;
        printIndent();
        fOut.print("startDocument(");
        fOut.print("systemId=");
        printQuotedString(systemId);
        fOut.print(',');
        fOut.print("encoding=");
        printQuotedString(encoding);
        fOut.println(')');
        fOut.flush();
        fIndent++;

    } // startDocument()

    /** XML Declaration. */
    public void xmlDecl(String version, String encoding, String actualEncoding,
                        String standalone) throws XNIException {

        printIndent();
        fOut.print("xmlDecl(");
        fOut.print("version=");
        printQuotedString(version);
        fOut.print(',');
        fOut.print("encoding=");
        printQuotedString(encoding);
        fOut.print(',');
        fOut.print("actualEncoding=");
        printQuotedString(actualEncoding);
        fOut.print(',');
        fOut.print("standalone=");
        printQuotedString(standalone);
        fOut.println(')');

    } // xmlDecl(String,String,String,String)

    /** Doctype declaration. */
    public void doctypeDecl(String rootElement, String publicId, 
                            String systemId) throws XNIException {

        printIndent();
        fOut.print("doctypeDecl(");
        fOut.print("rootElement=");
        printQuotedString(rootElement);
        fOut.print(',');
        fOut.print("publicId=");
        printQuotedString(publicId);
        fOut.print(',');
        fOut.print("systemId=");
        printQuotedString(systemId);
        fOut.println(')');
        fOut.flush();

    } // doctypeDecl(String,String,String)

    /** Start prefix mapping. */
    public void startPrefixMapping(String prefix, String uri)
        throws XNIException {

        printIndent();
        fOut.print("startPrefixMapping(");
        fOut.print("prefix=");
        printQuotedString(prefix);
        fOut.print(',');
        fOut.print("uri=");
        printQuotedString(uri);
        fOut.println(')');
        fOut.flush();

    } // startPrefixMapping(String,String)

    /** Start element. */
    public void startElement(QName element, XMLAttributes attributes)
        throws XNIException {

        printIndent();
        fOut.print("startElement(");
        printElement(element, attributes);
        fOut.println(')');
        fOut.flush();
        fIndent++;

    } // startElement(QName,XMLAttributes)

    /** Empty element. */
    public void emptyElement(QName element, XMLAttributes attributes)
        throws XNIException {

        printIndent();
        fOut.print("emptyElement(");
        printElement(element, attributes);
        fOut.println(')');
        fOut.flush();

    } // emptyElement(QName,XMLAttributes)

    /** Characters. */
    public void characters(XMLString text) throws XNIException {

        printIndent();
        fOut.print("characters(");
        fOut.print("text=");
        printQuotedString(text.ch, text.offset, text.length);
        fOut.println(')');
        fOut.flush();

    } // characters(XMLString)

    /** Ignorable whitespace. */
    public void ignorableWhitespace(XMLString text) throws XNIException {

        printIndent();
        fOut.print("ignorableWhitespace(");
        fOut.print("text=");
        printQuotedString(text.ch, text.offset, text.length);
        fOut.println(')');
        fOut.flush();

    } // ignorableWhitespace(XMLString)

    /** End element. */
    public void endElement(QName element) throws XNIException {

        fIndent--;
        printIndent();
        fOut.print("endElement(");
        fOut.print("element=");
        fOut.print('{');
        fOut.print("prefix=");
        printQuotedString(element.prefix);
        fOut.print(',');
        fOut.print("localpart=");
        printQuotedString(element.localpart);
        fOut.print(',');
        fOut.print("rawname=");
        printQuotedString(element.rawname);
        fOut.print(',');
        fOut.print("uri=");
        printQuotedString(element.uri);
        fOut.print('}');
        fOut.println(')');
        fOut.flush();

    } // endElement(QName)

    /** End prefix mapping. */
    public void endPrefixMapping(String prefix) throws XNIException {

        printIndent();
        fOut.print("endPrefixMapping(");
        fOut.print("prefix=");
        printQuotedString(prefix);
        fOut.println(')');
        fOut.flush();

    } // endPrefixMapping(String)

    /** Start CDATA section. */
    public void startCDATA() throws XNIException {

        printIndent();
        fOut.println("startCDATA()");
        fOut.flush();
        fIndent++;

    } // startCDATA()

    /** End CDATA section. */
    public void endCDATA() throws XNIException {

        fIndent--;
        printIndent();
        fOut.println("endCDATA()");
        fOut.flush();

    } //  endCDATA()

    /** End document. */
    public void endDocument() throws XNIException {

        fIndent--;
        printIndent();
        fOut.println("endDocument()");
        fOut.flush();

    } // endDocument();

    //
    // XMLDocumentHandler and XMLDTDHandler methods
    //

    /** Start entity. */
    public void startEntity(String name, String publicId, String systemId, 
                            String encoding) throws XNIException {

        printIndent();
        fOut.print("startEntity(");
        fOut.print("name=");
        printQuotedString(name);
        fOut.print(',');
        fOut.print("publicId=");
        printQuotedString(publicId);
        fOut.print(',');
        fOut.print("systemId=");
        printQuotedString(systemId);
        fOut.print(',');
        fOut.print("encoding=");
        printQuotedString(encoding);
        fOut.println(')');
        fOut.flush();
        fIndent++;

    } // startEntity(String,String,String,String)

    /** Text declaration. */
    public void textDecl(String version, String encoding) throws XNIException {

        printIndent();
        fOut.print("textDecl(");
        fOut.print("version=");
        printQuotedString(version);
        fOut.print(',');
        fOut.print("encoding=");
        printQuotedString(encoding);
        fOut.println(')');
        fOut.flush();

    } // textDecl(String,String)

    /** Comment. */
    public void comment(XMLString text) throws XNIException {

        printIndent();
        fOut.print("comment(");
        fOut.print("text=");
        printQuotedString(text.ch, text.offset, text.length);
        fOut.println(')');
        fOut.flush();

    } // comment(XMLText)

    /** Processing instruction. */
    public void processingInstruction(String target, XMLString data)
        throws XNIException {

        printIndent();
        fOut.print("processingInstruction(");
        fOut.print("target=");
        printQuotedString(target);
        fOut.print(',');
        fOut.print("data=");
        printQuotedString(data.ch, data.offset, data.length);
        fOut.println(')');
        fOut.flush();

    } // processingInstruction(String,XMLString)

    /** End entity. */
    public void endEntity(String name) throws XNIException {

        fIndent--;
        printIndent();
        fOut.print("endEntity(");
        fOut.print("name=");
        printQuotedString(name);
        fOut.println(')');
        fOut.flush();

    } // endEntity(String)

    //
    // XMLDTDHandler methods
    //

    /** Start DTD. */
    public void startDTD() throws XNIException {

        printIndent();
        fOut.println("startDTD()");
        fOut.flush();
        fIndent++;

    } // startDTD()

    /** Element declaration. */
    public void elementDecl(String name, String contentModel)
        throws XNIException {

        printIndent();
        fOut.print("elementDecl(");
        fOut.print("name=");
        printQuotedString(name);
        fOut.print(',');
        fOut.print("contentModel=");
        printQuotedString(contentModel);
        fOut.println(')');
        fOut.flush();

    } // elementDecl(String,String)

    /** Start attribute list. */
    public void startAttlist(String elementName) throws XNIException {

        printIndent();
        fOut.print("startAttlist(");
        fOut.print("elementName=");
        printQuotedString(elementName);
        fOut.println(')');
        fOut.flush();
        fIndent++;

    } // startAttlist(String)

    /** Attribute declaration. */
    public void attributeDecl(String elementName, String attributeName, 
                              String type, String[] enumeration, 
                              String defaultType, XMLString defaultValue)
        throws XNIException {

        printIndent();
        fOut.print("attributeDecl(");
        fOut.print("elementName=");
        printQuotedString(elementName);
        fOut.print(',');
        fOut.print("attributeName=");
        printQuotedString(attributeName);
        fOut.print(',');
        fOut.print("type=");
        printQuotedString(type);
        fOut.print(',');
        fOut.print("enumeration=");
        if (enumeration == null) {
            fOut.print("null");
        }
        else {
            fOut.print('{');
            for (int i = 0; i < enumeration.length; i++) {
                printQuotedString(enumeration[i]);
                if (i < enumeration.length - 1) {
                    fOut.print(',');
                }
            }
            fOut.print('}');
        }
        fOut.print(',');
        fOut.print("defaultType=");
        printQuotedString(defaultType);
        fOut.print(',');
        fOut.print("defaultValue=");
        if (defaultValue == null) {
            fOut.print("null");
        }
        else {
            printQuotedString(defaultValue.ch, defaultValue.offset,
                              defaultValue.length);
        }
        fOut.println(')');
        fOut.flush();

    } // attributeDecl(String,String,String,String[],String,XMLString)

    /** End attribute list. */
    public void endAttlist() throws XNIException {

        fIndent--;
        printIndent();
        fOut.println("endAttlist()");
        fOut.flush();

    } // endAttlist()

    /** Internal entity declaration. */
    public void internalEntityDecl(String name, XMLString text)
        throws XNIException {

        printIndent();
        fOut.print("internalEntityDecl(");
        fOut.print("name=");
        printQuotedString(name);
        fOut.print(',');
        fOut.print("text=");
        printQuotedString(text.ch, text.offset, text.length);
        fOut.println(')');
        fOut.flush();

    } // internalEntityDecl(String,XMLString)

    /** External entity declaration. */
    public void externalEntityDecl(String name, String publicId, 
                                   String systemId) throws XNIException {

        printIndent();
        fOut.print("externalEntityDecl(");
        fOut.print("name=");
        printQuotedString(name);
        fOut.print(',');
        fOut.print("publicId=");
        printQuotedString(publicId);
        fOut.print(',');
        fOut.print("systemId=");
        printQuotedString(systemId);
        fOut.println(')');
        fOut.flush();

    } // externalEntityDecl(String,String,String)

    /** Unparsed entity declaration. */
    public void unparsedEntityDecl(String name, String publicId, 
                                   String systemId, String notation)
        throws XNIException {

        printIndent();
        fOut.print("externalEntityDecl(");
        fOut.print("name=");
        printQuotedString(name);
        fOut.print(',');
        fOut.print("publicId=");
        printQuotedString(publicId);
        fOut.print(',');
        fOut.print("systemId=");
        printQuotedString(systemId);
        fOut.print(',');
        fOut.print("notation=");
        printQuotedString(notation);
        fOut.println(')');
        fOut.flush();

    } // unparsedEntityDecl(String,String,String,String)

    /** Notation declaration. */
    public void notationDecl(String name, String publicId, String systemId)
        throws XNIException {

        printIndent();
        fOut.print("notationDecl(");
        fOut.print("name=");
        printQuotedString(name);
        fOut.print(',');
        fOut.print("publicId=");
        printQuotedString(publicId);
        fOut.print(',');
        fOut.print("systemId=");
        printQuotedString(systemId);
        fOut.println(')');
        fOut.flush();

    } // notationDecl(String,String,String)

    /** Start conditional section. */
    public void startConditional(short type) throws XNIException {

        printIndent();
        fOut.print("startConditional(");
        fOut.print("type=");
        switch (type) {
            case XMLDTDHandler.CONDITIONAL_IGNORE: {
                fOut.print("CONDITIONAL_IGNORE");
                break;
            }
            case XMLDTDHandler.CONDITIONAL_INCLUDE: {
                fOut.print("CONDITIONAL_INCLUDE");
                break;
            }
            default: {
                fOut.print("??? ("+type+')');
            }
        }
        fOut.println(')');
        fOut.flush();
        fIndent++;

    } // startConditional(short)

    /** End conditional section. */
    public void endConditional() throws XNIException {

        fIndent--;
        printIndent();
        fOut.println("endConditional()");
        fOut.flush();

    } // endConditional()

    /** End DTD. */
    public void endDTD() throws XNIException {

        fIndent--;
        printIndent();
        fOut.println("endDTD()");
        fOut.flush();

    } // endDTD()

    //
    // XMLDTDContentModelHandler methods
    //

    /** Start content model. */
    public void startContentModel(String elementName, short type)
        throws XNIException {

        printIndent();
        fOut.print("startContentModel(");
        fOut.print("elementName=");
        printQuotedString(elementName);
        fOut.print(',');
        fOut.print("type=");
        switch (type) {
            case XMLDTDContentModelHandler.TYPE_ANY: {
                fOut.print("TYPE_ANY");
                break;
            }
            case XMLDTDContentModelHandler.TYPE_EMPTY: {
                fOut.print("TYPE_EMPTY");
                break;
            }
            case XMLDTDContentModelHandler.TYPE_MIXED: {
                fOut.print("TYPE_MIXED");
                break;
            }
            case XMLDTDContentModelHandler.TYPE_CHILDREN: {
                fOut.print("TYPE_CHILDREN");
                break;
            }
            default: {
                fOut.print("??? ("+type+')');
            }
        }
        fOut.println(')');
        fOut.flush();
        fIndent++;

    } // startContentModel(String,short)

    /** Mixed element. */
    public void mixedElement(String elementName) throws XNIException {

        printIndent();
        fOut.print("mixedElement(");
        fOut.print("elementName=");
        printQuotedString(elementName);
        fOut.println(')');
        fOut.flush();

    } // mixedElement(String)

    /** Children start group. */
    public void childrenStartGroup() throws XNIException {

        printIndent();
        fOut.println("childrenStartGroup()");
        fOut.flush();
        fIndent++;

    } // childrenStartGroup()

    /** Children element. */
    public void childrenElement(String elementName) throws XNIException {

        printIndent();
        fOut.print("childrenElement(");
        fOut.print("elementName=");
        printQuotedString(elementName);
        fOut.println(')');
        fOut.flush();

    } // childrenElement(String)

    /** Children separator. */
    public void childrenSeparator(short separator) throws XNIException {

        printIndent();
        fOut.print("childrenSeparator(");
        fOut.print("separator=");
        switch (separator) {
            case XMLDTDContentModelHandler.SEPARATOR_CHOICE: {
                fOut.print("SEPARATOR_CHOICE");
                break;
            }
            case XMLDTDContentModelHandler.SEPARATOR_SEQUENCE: {
                fOut.print("SEPARATOR_SEQUENCE");
                break;
            }
            default: {
                fOut.print("??? ("+separator+')');
            }
        }
        fOut.println(')');
        fOut.flush();

    } // childrenSeparator(short)

    /** Children occurrence. */
    public void childrenOccurrence(short occurrence) throws XNIException {

        printIndent();
        fOut.print("childrenOccurrence(");
        fOut.print("occurrence=");
        switch (occurrence) {
            case XMLDTDContentModelHandler.OCCURS_ONE_OR_MORE: {
                fOut.print("OCCURS_ONE_OR_MORE");
                break;
            }
            case XMLDTDContentModelHandler.OCCURS_ZERO_OR_MORE: {
                fOut.print("OCCURS_ZERO_OR_MORE");
                break;
            }
            case XMLDTDContentModelHandler.OCCURS_ZERO_OR_ONE: {
                fOut.print("OCCURS_ZERO_OR_ONE");
                break;
            }
            default: {
                fOut.print("??? ("+occurrence+')');
            }
        }
        fOut.println(')');
        fOut.flush();

    } // childrenOccurrence(short)

    /** Children end group. */
    public void childrenEndGroup() throws XNIException {

        fIndent--;
        printIndent();
        fOut.println("childrenEndGroup()");
        fOut.flush();

    } // childrenEndGroup()

    /** End content model. */
    public void endContentModel() throws XNIException {

        fIndent--;
        printIndent();
        fOut.println("endContentModel()");
        fOut.flush();

    } // endContentModel()

    //
    // ErrorHandler methods
    //

    /** Warning. */
    public void warning(SAXParseException ex) throws SAXException {
        printError("Warning", ex);
    } // warning(SAXParseException)

    /** Error. */
    public void error(SAXParseException ex) throws SAXException {
        printError("Error", ex);
    } // error(SAXParseException)

    /** Fatal error. */
    public void fatalError(SAXParseException ex) throws SAXException {
        printError("Fatal Error", ex);
        throw ex;
    } // fatalError(SAXParseException)

    //
    // Protected methods
    //

    /** Prints an element. */
    protected void printElement(QName element, XMLAttributes attributes) {

        fOut.print("element=");
        fOut.print('{');
        fOut.print("prefix=");
        printQuotedString(element.prefix);
        fOut.print(',');
        fOut.print("localpart=");
        printQuotedString(element.localpart);
        fOut.print(',');
        fOut.print("rawname=");
        printQuotedString(element.rawname);
        fOut.print(',');
        fOut.print("uri=");
        printQuotedString(element.uri);
        fOut.print('}');
        fOut.print(',');
        fOut.print("attributes=");
        if (attributes == null) {
            fOut.println("null");
        }
        else {
            fOut.print('{');
            int length = attributes.getLength();
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    fOut.print(',');
                }
                attributes.getName(i, fQName);
                String attrType = attributes.getType(i);
                String attrValue = attributes.getValue(i);
                fOut.print("name=");
                fOut.print('{');
                fOut.print("prefix=");
                printQuotedString(fQName.prefix);
                fOut.print(',');
                fOut.print("localpart=");
                printQuotedString(fQName.localpart);
                fOut.print(',');
                fOut.print("rawname=");
                printQuotedString(fQName.rawname);
                fOut.print(',');
                fOut.print("uri=");
                printQuotedString(fQName.uri);
                fOut.print('}');
                fOut.print(',');
                fOut.print("type=");
                printQuotedString(attrType);
                fOut.print(',');
                fOut.print("value=");
                printQuotedString(attrValue);
                if (attributes.isSpecified(i) == false ) {
                   fOut.print("(default)");
                }
                int entityCount = attributes.getEntityCount(i);
                for (int j = 0; j < entityCount; j++) {
                    String entityName = attributes.getEntityName(i, j);
                    int entityOffset = attributes.getEntityOffset(i, j);
                    int entityLength = attributes.getEntityLength(i, j);
                    fOut.print(',');
                    fOut.print('[');
                    fOut.print("name=");
                    printQuotedString(entityName);
                    fOut.print(',');
                    fOut.print("offset=");
                    fOut.print(entityOffset);
                    fOut.print(',');
                    fOut.print("length=");
                    fOut.print(entityLength);
                    fOut.print(']');
                }
                fOut.print('}');
            }
            fOut.print('}');
        }

    } // printElement(QName,XMLAttributes)

    /** Print quoted string. */
    protected void printQuotedString(String s) {

        if (s == null) {
            fOut.print("null");
            return;
        }

        fOut.print('"');
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            normalizeAndPrint(c);
        }
        fOut.print('"');

    } // printQuotedString(String)

    /** Print quoted string. */
    protected void printQuotedString(char[] ch, int offset, int length) {

        fOut.print('"');
        for (int i = 0; i < length; i++) {
            normalizeAndPrint(ch[offset + i]);
        }
        fOut.print('"');

    } // printQuotedString(char[],int,int)

    /** Normalize and print. */
    protected void normalizeAndPrint(char c) {

        switch (c) {
            case '\n': {
                fOut.print("\\n");
                break;
            }
            case '\r': {
                fOut.print("\\r");
                break;
            }
            case '\t': {
                fOut.print("\\t");
                break;
            }
            case '\\': {
                fOut.print("\\\\");
                break;
            }
            case '"': {
                fOut.print("\\\"");
                break;
            }
            default: {
                fOut.print(c);
            }
        }

    } // normalizeAndPrint(char)

    /** Prints the error message. */
    protected void printError(String type, SAXParseException ex) {

        System.err.print("[");
        System.err.print(type);
        System.err.print("] ");
        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1)
                systemId = systemId.substring(index + 1);
            System.err.print(systemId);
        }
        System.err.print(':');
        System.err.print(ex.getLineNumber());
        System.err.print(':');
        System.err.print(ex.getColumnNumber());
        System.err.print(": ");
        System.err.print(ex.getMessage());
        System.err.println();
        System.err.flush();

    } // printError(String,SAXParseException)

    /** Prints the indent. */
    protected void printIndent() {

        for (int i = 0; i < fIndent; i++) {
            fOut.print(' ');
        }

    } // printIndent()

    //
    // MAIN
    //

    /** Main. */
    public static void main(String[] argv) throws Exception {
        
        // is there anything to do?
        if (argv.length == 0) {
            printUsage();
            System.exit(1);
        }

        // variables
        XMLDocumentParser parser = null;
        XMLParserConfiguration parserConfig = null;
        boolean namespaces = DEFAULT_NAMESPACES;
        boolean validation = DEFAULT_VALIDATION;
        boolean schemaValidation = DEFAULT_SCHEMA_VALIDATION;
        boolean notifyCharRefs = DEFAULT_NOTIFY_CHAR_REFS;

        // process arguments
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.startsWith("-")) {
                String option = arg.substring(1);
                if (option.equals("p")) {
                    // get parser name
                    if (++i == argv.length) {
                        System.err.println("error: Missing argument to -p option.");
                        continue;
                    }
                    String parserName = argv[i];

                    // create parser
                    try {
                        parserConfig = (XMLParserConfiguration)Class.forName(parserName).newInstance();
                        parser = null;
                    }
                    catch (Exception e) {
                        parserConfig = null;
                        System.err.println("error: Unable to instantiate parser configuration ("+parserName+")");
                    }
                    continue;
                }
                if (option.equalsIgnoreCase("n")) {
                    namespaces = option.equals("n");
                    continue;
                }
                if (option.equalsIgnoreCase("v")) {
                    validation = option.equals("v");
                    continue;
                }
                if (option.equalsIgnoreCase("s")) {
                    schemaValidation = option.equals("s");
                    continue;
                }
                if (option.equalsIgnoreCase("c")) {
                    notifyCharRefs = option.equals("c");
                    continue;
                }
                if (option.equals("h")) {
                    printUsage();
                    continue;
                }
            }

            // use default parser?
            if (parserConfig == null) {

                // create parser
                try {
                    parserConfig = (XMLParserConfiguration)Class.forName(DEFAULT_PARSER_CONFIG).newInstance();
                }
                catch (Exception e) {
                    System.err.println("error: Unable to instantiate parser configuration ("+DEFAULT_PARSER_CONFIG+")");
                    continue;
                }
            }
        
            // set parser features
            if (parser == null) {
                parser = new DocumentTracer(parserConfig);
            }
            try {
                parser.setFeature(NAMESPACES_FEATURE_ID, namespaces);
            }
            catch (SAXException e) {
                System.err.println("warning: Parser does not support feature ("+NAMESPACES_FEATURE_ID+")");
            }
            try {
                parser.setFeature(VALIDATION_FEATURE_ID, validation);
            }
            catch (SAXException e) {
                System.err.println("warning: Parser does not support feature ("+VALIDATION_FEATURE_ID+")");
            }
            try {
                parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, schemaValidation);
            }
            catch (SAXNotRecognizedException e) {
                // ignore
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+SCHEMA_VALIDATION_FEATURE_ID+")");
            }
            try {
                parser.setFeature(NOTIFY_CHAR_REFS_FEATURE_ID, notifyCharRefs);
            }
            catch (SAXNotRecognizedException e) {
                e.printStackTrace();
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+NOTIFY_CHAR_REFS_FEATURE_ID+")");
            }
    
            // parse file
            try {
                parser.parse(arg);
            }
            catch (SAXParseException e) {
                // ignore
            }
            catch (Exception e) {
                System.err.println("error: Parse error occurred - "+e.getMessage());
                if (e instanceof SAXException) {
                    e = ((SAXException)e).getException();
                }
                e.printStackTrace(System.err);
            }
        }

    } // main(String[])

    //
    // Private static methods
    //

    /** Prints the usage. */
    private static void printUsage() {

        System.err.println("usage: java xni.DocumentTracer (options) uri ...");
        System.err.println();
        
        System.err.println("options:");
        System.out.println("  -p name  Specify parser configuration by name.");
        System.err.println("  -n | -N  Turn on/off namespace processing.");
        System.err.println("  -v | -V  Turn on/off validation.");
        System.err.println("  -s | -S  Turn on/off Schema validation support.");
        System.err.println("  -c | -C  Turn on/off character notifications");
        System.err.println("  -h       This help screen.");
        System.err.println();

        System.err.println("defaults:");
        System.out.print("  Config:     "+DEFAULT_PARSER_CONFIG);
        System.out.print("  Namespaces: ");
        System.err.println(DEFAULT_NAMESPACES ? "on" : "off");
        System.out.print("  Validation: ");
        System.err.println(DEFAULT_VALIDATION ? "on" : "off");
        System.out.print("  Schema:     ");
        System.err.println(DEFAULT_SCHEMA_VALIDATION ? "on" : "off");
        System.out.print("  Char refs:  ");
        System.err.println(DEFAULT_NOTIFY_CHAR_REFS ? "on" : "off" );

    } // printUsage()

} // class DocumentTracer
