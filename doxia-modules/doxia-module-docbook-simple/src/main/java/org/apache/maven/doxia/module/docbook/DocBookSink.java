package org.apache.maven.doxia.module.docbook;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;

import org.apache.maven.doxia.sink.AbstractXmlSink;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.util.DoxiaUtils;
import org.apache.maven.doxia.util.HtmlTools;

import org.codehaus.plexus.util.FileUtils;

/**
 * Docbook Sink implementation.
 * <br/>
 * <b>Note</b>: The encoding used is UTF-8.
 *
 * @version $Id$
 * @since 1.0
 */
public class DocBookSink
    extends AbstractXmlSink
    implements DocbookMarkup
{
    /** DocBook V4.4 SGML public id: "-//OASIS//DTD DocBook V4.4//EN" */
    public static final String DEFAULT_SGML_PUBLIC_ID = "-//OASIS//DTD DocBook V4.4//EN";

    /** DocBook XML V4.4 XML public id: "-//OASIS//DTD DocBook XML V4.4//EN" */
    public static final String DEFAULT_XML_PUBLIC_ID = "-//OASIS//DTD DocBook V4.4//EN";

    /** DocBook XML V4.4 XML system id: "http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd" */
    public static final String DEFAULT_XML_SYSTEM_ID = "http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd";

    /** DocBook XML V4.4 SGML system id: "http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd" */
    public static final String DEFAULT_SGML_SYSTEM_ID = "http://www.oasis-open.org/docbook/sgml/4.4/docbookx.dtd";

    /** The output writer. */
    private PrintWriter out;

    /** xmlMode. */
    private boolean xmlMode = false;

    /** styleSheet. */
    private String styleSheet = null;

    /** language. */
    private String lang = null;

    /** publicId. */
    private String publicId = null;

    /** systemId. */
    private String systemId = null;

    /** italicBegin. */
    private String italicBeginTag;

    /** italicEnd. */
    private String italicEndTag;

    /** boldBegin. */
    private String boldBeginTag;

    /** boldEnd. */
    private String boldEndTag;

    /** monospacedBegin. */
    private String monospacedBeginTag;

    /** monospacedEnd. */
    private String monospacedEndTag;

    /** horizontalRule. */
    private String horizontalRuleElement;

    /** pageBreak. */
    private String pageBreakElement;

    /** lineBreak. */
    private String lineBreakElement;

    /** An image source file. */
    private String graphicsFileName;

    /** hasTitle. */
    private boolean hasTitle;

    /** authorDate. */
    private boolean authorDateFlag;

    /** verbatim. */
    private boolean verbatimFlag;

    /** externalLink. */
    private boolean externalLinkFlag;

    /** tableHasCaption. */
    private boolean tableHasCaption;

    /** Used for table rows. */
    private PrintWriter savedOut;

    /** tableRows. */
    private String tableRows;

    /** tableRows writer. */
    private StringWriter tableRowsWriter;

    /** tableHasGrid. */
    private boolean tableHasGrid;

    private boolean skip;

    /**
     * Constructor, initialize the Writer.
     *
     * @param writer not null writer to write the result. <b>Should</b> be an UTF-8 Writer.
     * You could use <code>newXmlWriter</code> methods from {@link org.codehaus.plexus.util.WriterFactory}.
     */
    public DocBookSink( Writer writer )
    {
        this.out = new PrintWriter( writer );
        setItalicElement( "<emphasis>" );
        setBoldElement( "<emphasis role=\"bold\">" );
        setMonospacedElement( "<literal>" );
        setHorizontalRuleElement( "<!-- HR -->" );
        setPageBreakElement( "<!-- PB -->" );
        setLineBreakElement( "<!-- LB -->" );
    }

    /**
     * @param text The text to escape.
     * @param xmlMode xmlMode.
     * @return The escaped text.
     * @deprecated Use HtmlTools#escapeHTML(String,boolean).
     */
    public static final String escapeSGML( String text, boolean xmlMode )
    {
        int length = text.length();
        StringBuffer buffer = new StringBuffer( length );

        for ( int i = 0; i < length; ++i )
        {
            char c = text.charAt( i );
            switch ( c )
            {
                case '<':
                    buffer.append( "&lt;" );
                    break;
                case '>':
                    buffer.append( "&gt;" );
                    break;
                case '&':
                    buffer.append( "&amp;" );
                    break;
                case '"':
                    buffer.append( "&quot;" );
                    break;
                default:
                    if ( xmlMode )
                    {
                        buffer.append( c );
                    }
                    else
                    {
                        if ( c <= 0x7E )
                        {
                            // ASCII.
                            buffer.append( c );
                        }
                        else
                        {
                            buffer.append( "&#" );
                            buffer.append( (int) c );
                            buffer.append( ';' );
                        }
                    }
            }
        }

        return buffer.toString();
    }

    /**
     * Sets the xml mode.
     *
     * @param mode the mode to set.
     */
    public void setXMLMode( boolean mode )
    {
        this.xmlMode = mode;
    }

    /**
     * Returns the current xmlMode.
     *
     * @return the current xmlMode.
     */
    public boolean isXMLMode()
    {
        return xmlMode;
    }

    /**
     * Sets the encoding.
     *
     * @param enc the encoding to set.
     * @deprecated since 1.0, this method as no more effect. The encoding is always UTF-8.
     */
    public void setEncoding( String enc )
    {
        // nop
    }

    /**
     * Returns the UTF-8 encoding.
     *
     * @return always return UTF-8.
     * @deprecated since 1.0, this method as no more effect. The encoding is always UTF-8.
     */
    public String getEncoding()
    {
        return "UTF-8";
    }

    /**
     * Sets the styleSheet.
     *
     * @param sheet the styleSheet to set.
     */
    public void setStyleSheet( String sheet )
    {
        this.styleSheet = sheet;
    }

    /**
     * Returns the current styleSheet.
     *
     * @return the current styleSheet.
     */
    public String getStyleSheet()
    {
        return styleSheet;
    }

    /**
     * Sets the publicId.
     *
     * @param id the publicId to set.
     */
    public void setPublicId( String id )
    {
        this.publicId = id;
    }

    /**
     * Returns the current publicId.
     *
     * @return the current publicId.
     */
    public String getPublicId()
    {
        return publicId;
    }

    /**
     * Sets the systemId.
     *
     * @param id the systemId to set.
     */
    public void setSystemId( String id )
    {
        this.systemId = id;
    }

    /**
     * Returns the current systemId.
     *
     * @return the current systemId.
     */
    public String getSystemId()
    {
        return systemId;
    }

    /**
     * Sets the language.
     *
     * @param language the language to set.
     */
    public void setLanguage( String language )
    {
        this.lang = language;
    }

    /**
     * Returns the current language.
     *
     * @return the current language.
     */
    public String getLanguage()
    {
        return lang;
    }

    /**
     * Sets the current italicBeginTag and constructs the corresponding end tag from it.
     *
     * @param tag the tag to set. If tag is null, the empty string is used.
     */
    public void setItalicElement( String tag )
    {
        if ( tag == null )
        {
            tag = "";
        }
        this.italicBeginTag = tag;
        italicEndTag = makeEndTag( italicBeginTag );
    }

    /**
     * Constructs the corresponding end tag from the given begin tag.
     *
     * @param beginTag the begin tag to set. If null, the empty string is returned.
     * @return the corresponding end tag.
     */
    private String makeEndTag( String beginTag )
    {
        int length = beginTag.length();
        if ( length == 0 )
        {
            return "";
        }

        if ( beginTag.charAt( 0 ) != '<' || beginTag.charAt( length - 1 ) != '>' )
        {
            throw new IllegalArgumentException( "'" + beginTag + "', not a tag" );
        }

        StringTokenizer tokens = new StringTokenizer( beginTag, "<> \t\n\r\f" );
        if ( !tokens.hasMoreTokens() )
        {
            throw new IllegalArgumentException( "'" + beginTag + "', invalid tag" );
        }

        return "</" + tokens.nextToken() + ">";
    }

    /**
     * Returns the current italicBeginTag.
     *
     * @return the current italicBeginTag. Defaults to "<emphasis>".
     */
    public String getItalicElement()
    {
        return italicBeginTag;
    }

    /**
     * Sets the current boldBeginTag and constructs the corresponding end tag from it.
     *
     * @param tag the tag to set. If tag is null, the empty string is used.
     */
    public void setBoldElement( String tag )
    {
        if ( tag == null )
        {
            tag = "";
        }
        this.boldBeginTag = tag;
        boldEndTag = makeEndTag( boldBeginTag );
    }

    /**
     * Returns the current boldBeginTag.
     *
     * @return the current boldBeginTag. Defaults to "<emphasis role=\"bold\">".
     */
    public String getBoldElement()
    {
        return boldBeginTag;
    }

    /**
     * Sets the current monospacedBeginTag and constructs the corresponding end tag from it.
     *
     * @param tag the tag to set. If tag is null, the empty string is used.
     */
    public void setMonospacedElement( String tag )
    {
        if ( tag == null )
        {
            tag = "";
        }
        this.monospacedBeginTag = tag;
        monospacedEndTag = makeEndTag( monospacedBeginTag );
    }

    /**
     * Returns the current monospacedBeginTag.
     *
     * @return the current monospacedBeginTag. Defaults to "<literal>>".
     */
    public String getMonospacedElement()
    {
        return monospacedBeginTag;
    }

    /**
     * Sets the current horizontalRuleElement.
     *
     * @param element the element to set.
     */
    public void setHorizontalRuleElement( String element )
    {
        this.horizontalRuleElement = element;
    }

    /**
     * Returns the current horizontalRuleElement.
     *
     * @return the current horizontalRuleElement. Defaults to "<!-- HR -->".
     */
    public String getHorizontalRuleElement()
    {
        return horizontalRuleElement;
    }

    /**
     * Sets the current pageBreakElement.
     *
     * @param element the element to set.
     */
    public void setPageBreakElement( String element )
    {
        this.pageBreakElement = element;
    }

    /**
     * Returns the current pageBreakElement.
     *
     * @return the current pageBreakElement. Defaults to "<!-- PB -->".
     */
    public String getPageBreakElement()
    {
        return pageBreakElement;
    }

    /**
     * Sets the current lineBreakElement.
     *
     * @param element the element to set.
     */
    public void setLineBreakElement( String element )
    {
        this.lineBreakElement = element;
    }

    /**
     * Returns the current lineBreakElement.
     *
     * @return the current lineBreakElement. Defaults to "<!-- LB -->".
     */
    public String getLineBreakElement()
    {
        return lineBreakElement;
    }

    /**
     * Reset all variables.
     */
    protected void resetState()
    {
        hasTitle = false;
        authorDateFlag = false;
        verbatimFlag = false;
        externalLinkFlag = false;
        graphicsFileName = null;
        tableHasCaption = false;
        savedOut = null;
        tableRows = null;
        tableHasGrid = false;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @see #DEFAULT_XML_PUBLIC_ID
     * @see #DEFAULT_SGML_PUBLIC_ID
     * @see #DEFAULT_XML_SYSTEM_ID
     * @see DocbookMarkup#ARTICLE_TAG
     */
    public void head()
    {
        resetState();

        MutableAttributeSet att = writeXmlHeader( "article" );

        writeStartTag( ARTICLE_TAG, att );
    }

    protected MutableAttributeSet writeXmlHeader( String root )
    {
        if ( xmlMode )
        {
            if ( styleSheet != null )
            {
                markup( "<?xml-stylesheet type=\"text/css\" href=\"" + styleSheet + "\" ?>" );
            }
        }

        String pubId;
        markup( "<!DOCTYPE " + root + " PUBLIC" );

        if ( publicId == null )
        {
            if ( xmlMode )
            {
                pubId = DEFAULT_XML_PUBLIC_ID;
            }
            else
            {
                pubId = DEFAULT_SGML_PUBLIC_ID;
            }
        }
        else
        {
            pubId = publicId;
        }
        markup( " \"" + pubId + "\"" );
        String sysId = systemId;
        if ( sysId == null )
        {
            if ( xmlMode )
            {
                sysId = DEFAULT_XML_SYSTEM_ID;
            }
            else
            {
                sysId = DEFAULT_SGML_SYSTEM_ID;
            }
        }
        markup( " \"" + sysId + "\">" );

        MutableAttributeSet att = new SimpleAttributeSet();
        if ( lang != null )
        {
            att.addAttribute( Attribute.LANG, lang );
        }
        return att;
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ARTICLEINFO_TAG
     */
    public void head_()
    {
        if ( hasTitle )
        {
            writeEndTag( ARTICLEINFO_TAG );
            hasTitle = false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ARTICLEINFO_TAG
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void title()
    {
        writeStartTag( ARTICLEINFO_TAG );
        writeStartTag( Tag.TITLE );
        hasTitle = true;
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void title_()
    {
        writeEndTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#CORPAUTHOR_TAG
     */
    public void author()
    {
        authorDateFlag = true;
        writeStartTag( CORPAUTHOR_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#CORPAUTHOR_TAG
     */
    public void author_()
    {
        writeEndTag( CORPAUTHOR_TAG );
        authorDateFlag = false;
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#DATE_TAG
     */
    public void date()
    {
        authorDateFlag = true;
        writeStartTag( DATE_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#DATE_TAG
     */
    public void date_()
    {
        writeEndTag( DATE_TAG );
        authorDateFlag = false;
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ARTICLE_TAG
     */
    public void body_()
    {
        writeEndTag( ARTICLE_TAG );
        out.flush();
        resetState();
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#SECTION_TAG
     */
    public void section1()
    {
        writeStartTag( SECTION_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#SECTION_TAG
     */
    public void section1_()
    {
        writeEndTag( SECTION_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#SECTION_TAG
     */
    public void section2()
    {
        writeStartTag( SECTION_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#SECTION_TAG
     */
    public void section2_()
    {
        writeEndTag( SECTION_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#SECTION_TAG
     */
    public void section3()
    {
        writeStartTag( SECTION_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#SECTION_TAG
     */
    public void section3_()
    {
        writeEndTag( SECTION_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#SECTION_TAG
     */
    public void section4()
    {
        writeStartTag( SECTION_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#SECTION_TAG
     */
    public void section4_()
    {
        writeEndTag( SECTION_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#SECTION_TAG
     */
    public void section5()
    {
        writeStartTag( SECTION_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#SECTION_TAG
     */
    public void section5_()
    {
        writeEndTag( SECTION_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle()
    {
        writeStartTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle_()
    {
        writeEndTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle1()
    {
        writeStartTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle1_()
    {
        writeEndTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle2()
    {
        writeStartTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle2_()
    {
        writeEndTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle3()
    {
        writeStartTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle3_()
    {
        writeEndTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle4()
    {
        writeStartTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle4_()
    {
        writeEndTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle5()
    {
        writeStartTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void sectionTitle5_()
    {
        writeEndTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ITEMIZEDLIST_TAG
     */
    public void list()
    {
        writeStartTag( ITEMIZEDLIST_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ITEMIZEDLIST_TAG
     */
    public void list_()
    {
        writeEndTag( ITEMIZEDLIST_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#LISTITEM_TAG
     */
    public void listItem()
    {
        writeStartTag( LISTITEM_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#LISTITEM_TAG
     */
    public void listItem_()
    {
        writeEndTag( LISTITEM_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ORDEREDLIST_TAG
     * @see DocbookMarkup#NUMERATION_ATTRIBUTE
     */
    public void numberedList( int numbering )
    {
        String numeration;
        switch ( numbering )
        {
            case NUMBERING_UPPER_ALPHA:
                numeration = UPPERALPHA_STYLE;
                break;
            case NUMBERING_LOWER_ALPHA:
                numeration = LOWERALPHA_STYLE;
                break;
            case NUMBERING_UPPER_ROMAN:
                numeration = UPPERROMAN_STYLE;
                break;
            case NUMBERING_LOWER_ROMAN:
                numeration = LOWERROMAN_STYLE;
                break;
            case NUMBERING_DECIMAL:
            default:
                numeration = ARABIC_STYLE;
        }

        MutableAttributeSet att = new SimpleAttributeSet();
        att.addAttribute( NUMERATION_ATTRIBUTE, numeration );

        writeStartTag( ORDEREDLIST_TAG, att );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ORDEREDLIST_TAG
     */
    public void numberedList_()
    {
        writeEndTag( ORDEREDLIST_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#LISTITEM_TAG
     */
    public void numberedListItem()
    {
        writeStartTag( LISTITEM_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#LISTITEM_TAG
     */
    public void numberedListItem_()
    {
        writeEndTag( LISTITEM_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#VARIABLELIST_TAG
     */
    public void definitionList()
    {
        writeStartTag( VARIABLELIST_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#VARIABLELIST_TAG
     */
    public void definitionList_()
    {
        writeEndTag( VARIABLELIST_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#VARLISTENTRY_TAG
     */
    public void definitionListItem()
    {
        writeStartTag( VARLISTENTRY_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#VARLISTENTRY_TAG
     */
    public void definitionListItem_()
    {
        writeEndTag( VARLISTENTRY_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#TERM_TAG
     */
    public void definedTerm()
    {
        writeStartTag( TERM_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#TERM_TAG
     */
    public void definedTerm_()
    {
        writeEndTag( TERM_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#LISTITEM_TAG
     */
    public void definition()
    {
        writeStartTag( LISTITEM_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#LISTITEM_TAG
     */
    public void definition_()
    {
        writeEndTag( LISTITEM_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#PARA_TAG
     */
    public void paragraph()
    {
        writeStartTag( PARA_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#PARA_TAG
     */
    public void paragraph_()
    {
        writeEndTag( PARA_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#PROGRAMLISTING_TAG
     */
    public void verbatim( boolean boxed )
    {
        verbatimFlag = true;
        writeStartTag( PROGRAMLISTING_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#PROGRAMLISTING_TAG
     */
    public void verbatim_()
    {
        writeEndTag( PROGRAMLISTING_TAG );
        verbatimFlag = false;
    }

    /** {@inheritDoc} */
    public void horizontalRule()
    {
        markup( horizontalRuleElement );
    }

    /** {@inheritDoc} */
    public void pageBreak()
    {
        markup( pageBreakElement );
    }

    /** {@inheritDoc} */
    public void figure_()
    {
        graphicElement();
    }

    /**
     * @see DocbookMarkup#MEDIAOBJECT_TAG
     * @see DocbookMarkup#IMAGEOBJECT_TAG
     * @see DocbookMarkup#IMAGEDATA_TAG
     * @see DocbookMarkup#FORMAT_ATTRIBUTE
     * @see DocbookMarkup#FILEREF_ATTRIBUTE
     */
    protected void graphicElement()
    {
        if ( graphicsFileName != null )
        {
            String format = FileUtils.extension( graphicsFileName ).toUpperCase( Locale.ENGLISH );
            if ( format.length() == 0 )
            {
                format = "JPEG";
            }

            writeStartTag( MEDIAOBJECT_TAG );
            writeStartTag( IMAGEOBJECT_TAG );

            MutableAttributeSet att = new SimpleAttributeSet();
            att.addAttribute( FORMAT_ATTRIBUTE, format );
            att.addAttribute( FILEREF_ATTRIBUTE, escapeSGML( graphicsFileName, xmlMode ) );

            // TODO: why?
            if ( xmlMode )
            {
                writeSimpleTag( IMAGEDATA_TAG, att );
            }
            else
            {
                writeStartTag( IMAGEDATA_TAG, att );
                writeEndTag( IMAGEDATA_TAG );
            }

            writeEndTag( IMAGEOBJECT_TAG );
            writeEndTag( MEDIAOBJECT_TAG );
            graphicsFileName = null;
        }
    }

    /** {@inheritDoc} */
    public void figureGraphics( String name )
    {
        // TODO: extension?
        graphicsFileName = name + ".jpeg";
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#FIGURE_TAG
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void figureCaption()
    {
        writeStartTag( FIGURE_TAG );
        writeStartTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#FIGURE_TAG
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void figureCaption_()
    {
        writeEndTag( Tag.TITLE );
        graphicElement();
        writeEndTag( FIGURE_TAG );
    }

    /** {@inheritDoc} */
    public void table()
    {
        tableHasCaption = false;
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#INFORMALTABLE_TAG
     * @see DocbookMarkup#FRAME_ATTRIBUTE
     * @see DocbookMarkup#ROWSEP_ATTRIBUTE
     * @see DocbookMarkup#COLSEP_ATTRIBUTE
     * @see javax.swing.text.html.HTML.Tag#TABLE
     */
    public void table_()
    {
        if ( tableHasCaption )
        {
            tableHasCaption = false;
            // Formal table+title already written to original destination ---

            out.write( tableRows  );
            writeEndTag( Tag.TABLE );
        }
        else
        {
            String frame;
            int sep;
            if ( tableHasGrid )
            {
                frame = "all";
                sep = 1;
            }
            else
            {
                frame = "none";
                sep = 0;
            }

            MutableAttributeSet att = new SimpleAttributeSet();
            att.addAttribute( FRAME_ATTRIBUTE, frame );
            att.addAttribute( ROWSEP_ATTRIBUTE, String.valueOf( sep ) );
            att.addAttribute( COLSEP_ATTRIBUTE, String.valueOf( sep ) );

            writeStartTag( INFORMALTABLE_TAG, att );

            out.write( tableRows  );

            writeEndTag( INFORMALTABLE_TAG );
        }

        tableRows = null;
        tableHasGrid = false;
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#TGROUP_TAG
     * @see DocbookMarkup#COLS_ATTRIBUTE
     * @see DocbookMarkup#COLSPEC_TAG
     */
    public void tableRows( int[] justification, boolean grid )
    {
        tableHasGrid = grid;

        // Divert output to a string ---
        out.flush();
        savedOut = out;
        tableRowsWriter = new StringWriter();
        out = new PrintWriter( tableRowsWriter );

        MutableAttributeSet att = new SimpleAttributeSet();
        att.addAttribute( COLS_ATTRIBUTE, String.valueOf( justification.length ) );

        writeStartTag( TGROUP_TAG, att );

        for ( int i = 0; i < justification.length; ++i )
        {
            String justif;
            switch ( justification[i] )
            {
                case Sink.JUSTIFY_LEFT:
                    justif = "left";
                    break;
                case Sink.JUSTIFY_RIGHT:
                    justif = "right";
                    break;
                case Sink.JUSTIFY_CENTER:
                default:
                    justif = "center";
                    break;
            }

            att = new SimpleAttributeSet();
            att.addAttribute( Attribute.ALIGN.toString(), justif );

            // TODO: why?
            if ( xmlMode )
            {
                writeSimpleTag( COLSPEC_TAG, att );
            }
            else
            {
                writeStartTag( COLSPEC_TAG, att );
                writeEndTag( COLSPEC_TAG );
            }
        }

        writeStartTag( TBODY_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#TGROUP_TAG
     * @see DocbookMarkup#TBODY_TAG
     */
    public void tableRows_()
    {
        writeEndTag( TBODY_TAG );
        writeEndTag( TGROUP_TAG );

        // Remember diverted output and restore original destination ---
        out.flush();
        if ( tableRowsWriter == null )
        {
            throw new IllegalArgumentException( "tableRows( int[] justification, boolean grid ) was not called before." );
        }

        tableRows = tableRowsWriter.toString();
        tableRowsWriter = null;
        out = savedOut;
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ROW_TAG
     */
    public void tableRow()
    {
        writeStartTag( ROW_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ROW_TAG
     */
    public void tableRow_()
    {
        writeEndTag( ROW_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ENTRY_TAG
     */
    public void tableCell()
    {
        writeStartTag( ENTRY_TAG );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ENTRY_TAG
     */
    public void tableCell_()
    {
        writeEndTag( ENTRY_TAG );
    }

    /**
     * {@inheritDoc}
     * @see DocbookMarkup#ENTRY_TAG
     */
    public void tableHeaderCell()
    {
        writeStartTag( ENTRY_TAG );
    }

    /**
     * {@inheritDoc}
     * @see DocbookMarkup#ENTRY_TAG
     */
    public void tableHeaderCell_()
    {
        writeEndTag( ENTRY_TAG );
    }

    /**
     * {@inheritDoc}
     * @see javax.swing.text.html.HTML.Tag#TABLE
     * @see DocbookMarkup#FRAME_ATTRIBUTE
     * @see DocbookMarkup#ROWSEP_ATTRIBUTE
     * @see DocbookMarkup#COLSEP_ATTRIBUTE
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void tableCaption()
    {
        tableHasCaption = true;

        String frame;
        int sep;
        if ( tableHasGrid )
        {
            frame = "all";
            sep = 1;
        }
        else
        {
            frame = "none";
            sep = 0;
        }

        MutableAttributeSet att = new SimpleAttributeSet();
        att.addAttribute( FRAME_ATTRIBUTE, frame );
        att.addAttribute( ROWSEP_ATTRIBUTE, String.valueOf( sep ) );
        att.addAttribute( COLSEP_ATTRIBUTE, String.valueOf( sep ) );

        writeStartTag( Tag.TABLE, att );

        writeStartTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.swing.text.html.HTML.Tag#TITLE
     */
    public void tableCaption_()
    {
        writeEndTag( Tag.TITLE );
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ANCHOR_TAG
     */
    public void anchor( String name )
    {
        if ( name == null )
        {
            throw new NullPointerException( "Anchor name cannot be null!" );
        }

        if ( authorDateFlag )
        {
            return;
        }

        String id = name;

        if ( !DoxiaUtils.isValidId( id ) )
        {
            id = DoxiaUtils.encodeId( name );

            getLog().warn( "Modified invalid anchor name: " + name );
        }

        MutableAttributeSet att = new SimpleAttributeSet();
        att.addAttribute( Attribute.ID, id );

        // TODO: why?
        if ( xmlMode )
        {
            writeSimpleTag( ANCHOR_TAG, att );
        }
        else
        {
            writeStartTag( ANCHOR_TAG, att );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ANCHOR_TAG
     */
    public void anchor_()
    {
        if ( !authorDateFlag )
        {
            if ( !xmlMode )
            {
                writeEndTag( ANCHOR_TAG );
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ULINK_TAG
     * @see DocbookMarkup#URL_ATTRIBUTE
     * @see DocbookMarkup#LINK_TAG
     * @see DocbookMarkup#LINKEND_ATTRIBUTE
     */
    public void link( String name )
    {
        if ( name == null )
        {
            throw new NullPointerException( "Link name cannot be null!" );
        }

        if ( DoxiaUtils.isExternalLink( name ) )
        {
            externalLinkFlag = true;
            MutableAttributeSet att = new SimpleAttributeSet();
            att.addAttribute( URL_ATTRIBUTE, HtmlTools.escapeHTML( name, xmlMode ) );

            writeStartTag( ULINK_TAG, att );
        }
        else
        {
            MutableAttributeSet att = new SimpleAttributeSet();
            att.addAttribute( LINKEND_ATTRIBUTE, HtmlTools.escapeHTML( name ) );

            writeStartTag( LINK_TAG, att );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see DocbookMarkup#ULINK_TAG
     * @see DocbookMarkup#LINK_TAG
     */
    public void link_()
    {
        if ( externalLinkFlag )
        {
            writeEndTag( ULINK_TAG );
            externalLinkFlag = false;
        }
        else
        {
            writeEndTag( LINK_TAG );
        }
    }

    /** {@inheritDoc} */
    public void italic()
    {
        markup( italicBeginTag );
    }

    /** {@inheritDoc} */
    public void italic_()
    {
        markup( italicEndTag );
    }

    /** {@inheritDoc} */
    public void bold()
    {
        markup( boldBeginTag );
    }

    /** {@inheritDoc} */
    public void bold_()
    {
        markup( boldEndTag );
    }

    /** {@inheritDoc} */
    public void monospaced()
    {
        if ( !authorDateFlag )
        {
            markup( monospacedBeginTag );
        }
    }

    /** {@inheritDoc} */
    public void monospaced_()
    {
        if ( !authorDateFlag )
        {
            markup( monospacedEndTag );
        }
    }

    /** {@inheritDoc} */
    public void lineBreak()
    {
        markup( lineBreakElement );
    }

    /** {@inheritDoc} */
    public void nonBreakingSpace()
    {
        markup( "&#x00A0;" );
        //markup( "&nbsp;" );
    }

    /** {@inheritDoc} */
    public void text( String text )
    {
        if ( verbatimFlag )
        {
            verbatimContent( text );
        }
        else
        {
            content( text );
        }
    }

    /** {@inheritDoc} */
    public void comment( String comment )
    {
        StringBuffer buffer = new StringBuffer( comment.length() + 9 );

        buffer.append( LESS_THAN ).append( BANG ).append( MINUS ).append( MINUS ).append( SPACE );

        buffer.append( comment );

        buffer.append( SPACE ).append( MINUS ).append( MINUS ).append( GREATER_THAN );

        markup( buffer.toString() );
    }

    /**
     * Unkown events just log a warning message but are ignored otherwise.
     *
     * @param name The name of the event.
     * @param requiredParams not used.
     * @param attributes not used.
     * @see org.apache.maven.doxia.sink.Sink#unknown(String,Object[],SinkEventAttributes)
     */
    public void unknown( String name, Object[] requiredParams, SinkEventAttributes attributes )
    {
        getLog().warn( "Unknown Sink event in DocBookSink: " + name + ", ignoring!" );
    }

    // -----------------------------------------------------------------------

    /**
     * Write text to output, preserving white space.
     *
     * @param text The text to write.
     */
    protected void markup( String text )
    {
        if ( !skip )
        {
            out.write( text );
        }
    }

    /**
     * Write SGML escaped text to output, not preserving white space.
     *
     * @param text The text to write.
     */
    protected void content( String text )
    {
        if ( !skip )
        {
            out.write( escapeSGML( text, xmlMode ) );
        }
    }

    /**
     * Write SGML escaped text to output, preserving white space.
     *
     * @param text The text to write.
     */
    protected void verbatimContent( String text )
    {
        if ( !skip )
        {
            out.write( escapeSGML( text, xmlMode ) );
        }
    }

    // -----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void flush()
    {
        out.flush();
    }

    /** {@inheritDoc} */
    public void close()
    {
        out.close();
    }

    /** {@inheritDoc} */
    protected void write( String text )
    {
        markup( unifyEOLs( text ) );
    }

    /**
     * @param skip the skip to set.
     */
    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }
}
