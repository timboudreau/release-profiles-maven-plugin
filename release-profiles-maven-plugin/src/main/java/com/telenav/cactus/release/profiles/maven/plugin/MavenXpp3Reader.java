package com.telenav.cactus.release.profiles.maven.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.ConfigurationContainer;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Extension;
import org.apache.maven.model.FileSet;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Notifier;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Parent;
import org.apache.maven.model.PatternSet;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginConfiguration;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import static java.lang.Long.parseLong;
import static java.util.Locale.US;
import static org.codehaus.plexus.util.ReaderFactory.newXmlReader;
import static org.codehaus.plexus.util.xml.Xpp3DomBuilder.build;
import static org.codehaus.plexus.util.xml.pull.XmlPullParser.*;

/**
 * Copied from Maven sources and patched to read just the profiles element.
 */
@SuppressWarnings("all")
public class MavenXpp3Reader
{

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/
    /**
     * If set the parser will be loaded with all single characters from the
     * XHTML specification. The entities used:
     * <ul>
     * <li>http://www.w3.org/TR/xhtml1/DTD/xhtml-lat1.ent</li>
     * <li>http://www.w3.org/TR/xhtml1/DTD/xhtml-special.ent</li>
     * <li>http://www.w3.org/TR/xhtml1/DTD/xhtml-symbol.ent</li>
     * </ul>
     */
    private boolean addDefaultEntities = true;

    //-----------/
    //- Methods -/
    //-----------/
    /**
     * Method checkFieldWithDuplicate.
     *
     * @param parser
     * @param parsed
     * @param alias
     * @param tagName
     * @throws XmlPullParserException
     * @return boolean
     */
    private boolean checkFieldWithDuplicate(XmlPullParser parser, String tagName,
            String alias, java.util.Set parsed)
            throws XmlPullParserException
    {
        if (!(parser.getName().equals(tagName) || parser.getName().equals(alias)))
        {
            return false;
        }
        if (!parsed.add(tagName))
        {
            throw new XmlPullParserException("Duplicated tag: '" + tagName + "'",
                    parser, null);
        }
        return true;
    } //-- boolean checkFieldWithDuplicate( XmlPullParser, String, String, java.util.Set )

    /**
     * Method checkUnknownAttribute.
     *
     * @param parser
     * @param strict
     * @param tagName
     * @param attribute
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void checkUnknownAttribute(XmlPullParser parser, String attribute,
            String tagName, boolean strict)
            throws XmlPullParserException, IOException
    {
        // strictXmlAttributes = true for model: if strict == true, not only elements are checked but attributes too
        if (strict)
        {
            throw new XmlPullParserException(
                    "Unknown attribute '" + attribute + "' for tag '" + tagName + "'",
                    parser, null);
        }
    } //-- void checkUnknownAttribute( XmlPullParser, String, String, boolean )

    /**
     * Method checkUnknownElement.
     *
     * @param parser
     * @param strict
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void checkUnknownElement(XmlPullParser parser, boolean strict)
            throws XmlPullParserException, IOException
    {
        if (strict)
        {
            throw new XmlPullParserException("Unrecognised tag: '" + parser
                    .getName() + "'", parser, null);
        }

        for (int unrecognizedTagCount = 1; unrecognizedTagCount > 0;)
        {
            int eventType = parser.next();
            if (eventType == START_TAG)
            {
                unrecognizedTagCount++;
            }
            else if (eventType == END_TAG)
            {
                unrecognizedTagCount--;
            }
        }
    } //-- void checkUnknownElement( XmlPullParser, boolean )

    /**
     * Returns the state of the "add default entities" flag.
     *
     * @return boolean
     */
    public boolean getAddDefaultEntities()
    {
        return addDefaultEntities;
    } //-- boolean getAddDefaultEntities()

    /**
     * Method getBooleanValue.
     *
     * @param s
     * @param parser
     * @param attribute
     * @throws XmlPullParserException
     * @return boolean
     */
    private boolean getBooleanValue(String s, String attribute,
            XmlPullParser parser)
            throws XmlPullParserException
    {
        return getBooleanValue(s, attribute, parser, null);
    } //-- boolean getBooleanValue( String, String, XmlPullParser )

    /**
     * Method getBooleanValue.
     *
     * @param s
     * @param defaultValue
     * @param parser
     * @param attribute
     * @throws XmlPullParserException
     * @return boolean
     */
    private boolean getBooleanValue(String s, String attribute,
            XmlPullParser parser, String defaultValue)
            throws XmlPullParserException
    {
        if (s != null && s.length() != 0)
        {
            return Boolean.valueOf(s);
        }
        if (defaultValue != null)
        {
            return Boolean.valueOf(defaultValue);
        }
        return false;
    } //-- boolean getBooleanValue( String, String, XmlPullParser, String )

    /**
     * Method getByteValue.
     *
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     * @throws XmlPullParserException
     * @return byte
     */
    private byte getByteValue(String s, String attribute, XmlPullParser parser,
            boolean strict)
            throws XmlPullParserException
    {
        if (s != null)
        {
            try
            {
                return Byte.valueOf(s);
            }
            catch (NumberFormatException nfe)
            {
                if (strict)
                {
                    throw new XmlPullParserException(
                            "Unable to parse element '" + attribute + "', must be a byte",
                            parser, nfe);
                }
            }
        }
        return 0;
    } //-- byte getByteValue( String, String, XmlPullParser, boolean )

    /**
     * Method getCharacterValue.
     *
     * @param s
     * @param parser
     * @param attribute
     * @throws XmlPullParserException
     * @return char
     */
    private char getCharacterValue(String s, String attribute,
            XmlPullParser parser)
            throws XmlPullParserException
    {
        if (s != null)
        {
            return s.charAt(0);
        }
        return 0;
    } //-- char getCharacterValue( String, String, XmlPullParser )

    /**
     * Method getDateValue.
     *
     * @param s
     * @param parser
     * @param attribute
     * @throws XmlPullParserException
     * @return Date
     */
    private java.util.Date getDateValue(String s, String attribute,
            XmlPullParser parser)
            throws XmlPullParserException
    {
        return getDateValue(s, attribute, null, parser);
    } //-- java.util.Date getDateValue( String, String, XmlPullParser )

    /**
     * Method getDateValue.
     *
     * @param s
     * @param parser
     * @param dateFormat
     * @param attribute
     * @throws XmlPullParserException
     * @return Date
     */
    private java.util.Date getDateValue(String s, String attribute,
            String dateFormat, XmlPullParser parser)
            throws XmlPullParserException
    {
        if (s != null)
        {
            String effectiveDateFormat = dateFormat;
            if (dateFormat == null)
            {
                effectiveDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS";
            }
            if ("long".equals(effectiveDateFormat))
            {
                try
                {
                    return new java.util.Date(parseLong(s));
                }
                catch (NumberFormatException e)
                {
                    throw new XmlPullParserException(e.getMessage(), parser, e);
                }
            }
            else
            {
                try
                {
                    DateFormat dateParser = new java.text.SimpleDateFormat(
                            effectiveDateFormat, US);
                    return dateParser.parse(s);
                }
                catch (java.text.ParseException e)
                {
                    throw new XmlPullParserException(e.getMessage(), parser, e);
                }
            }
        }
        return null;
    } //-- java.util.Date getDateValue( String, String, String, XmlPullParser )

    /**
     * Method getDoubleValue.
     *
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     * @throws XmlPullParserException
     * @return double
     */
    private double getDoubleValue(String s, String attribute,
            XmlPullParser parser, boolean strict)
            throws XmlPullParserException
    {
        if (s != null)
        {
            try
            {
                return Double.valueOf(s);
            }
            catch (NumberFormatException nfe)
            {
                if (strict)
                {
                    throw new XmlPullParserException(
                            "Unable to parse element '" + attribute + "', must be a floating point number",
                            parser, nfe);
                }
            }
        }
        return 0;
    } //-- double getDoubleValue( String, String, XmlPullParser, boolean )

    /**
     * Method getFloatValue.
     *
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     * @throws XmlPullParserException
     * @return float
     */
    private float getFloatValue(String s, String attribute, XmlPullParser parser,
            boolean strict)
            throws XmlPullParserException
    {
        if (s != null)
        {
            try
            {
                return Float.valueOf(s);
            }
            catch (NumberFormatException nfe)
            {
                if (strict)
                {
                    throw new XmlPullParserException(
                            "Unable to parse element '" + attribute + "', must be a floating point number",
                            parser, nfe);
                }
            }
        }
        return 0;
    } //-- float getFloatValue( String, String, XmlPullParser, boolean )

    /**
     * Method getIntegerValue.
     *
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     * @throws XmlPullParserException
     * @return int
     */
    private int getIntegerValue(String s, String attribute, XmlPullParser parser,
            boolean strict)
            throws XmlPullParserException
    {
        if (s != null)
        {
            try
            {
                return Integer.valueOf(s);
            }
            catch (NumberFormatException nfe)
            {
                if (strict)
                {
                    throw new XmlPullParserException(
                            "Unable to parse element '" + attribute + "', must be an integer",
                            parser, nfe);
                }
            }
        }
        return 0;
    } //-- int getIntegerValue( String, String, XmlPullParser, boolean )

    /**
     * Method getLongValue.
     *
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     * @throws XmlPullParserException
     * @return long
     */
    private long getLongValue(String s, String attribute, XmlPullParser parser,
            boolean strict)
            throws XmlPullParserException
    {
        if (s != null)
        {
            try
            {
                return Long.valueOf(s);
            }
            catch (NumberFormatException nfe)
            {
                if (strict)
                {
                    throw new XmlPullParserException(
                            "Unable to parse element '" + attribute + "', must be a long integer",
                            parser, nfe);
                }
            }
        }
        return 0;
    } //-- long getLongValue( String, String, XmlPullParser, boolean )

    /**
     * Method getRequiredAttributeValue.
     *
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     * @throws XmlPullParserException
     * @return String
     */
    private String getRequiredAttributeValue(String s, String attribute,
            XmlPullParser parser, boolean strict)
            throws XmlPullParserException
    {
        if (s == null)
        {
            if (strict)
            {
                throw new XmlPullParserException(
                        "Missing required value for attribute '" + attribute + "'",
                        parser, null);
            }
        }
        return s;
    } //-- String getRequiredAttributeValue( String, String, XmlPullParser, boolean )

    /**
     * Method getShortValue.
     *
     * @param s
     * @param strict
     * @param parser
     * @param attribute
     * @throws XmlPullParserException
     * @return short
     */
    private short getShortValue(String s, String attribute, XmlPullParser parser,
            boolean strict)
            throws XmlPullParserException
    {
        if (s != null)
        {
            try
            {
                return Short.valueOf(s);
            }
            catch (NumberFormatException nfe)
            {
                if (strict)
                {
                    throw new XmlPullParserException(
                            "Unable to parse element '" + attribute + "', must be a short integer",
                            parser, nfe);
                }
            }
        }
        return 0;
    } //-- short getShortValue( String, String, XmlPullParser, boolean )

    /**
     * Method getTrimmedValue.
     *
     * @param s
     * @return String
     */
    private String getTrimmedValue(String s)
    {
        if (s != null)
        {
            s = s.trim();
        }
        return s;
    } //-- String getTrimmedValue( String )

    /**
     * Method initParser.
     *
     * @param parser
     * @throws XmlPullParserException
     */
    private void initParser(XmlPullParser parser)
            throws XmlPullParserException
    {
        if (addDefaultEntities)
        {
            // ----------------------------------------------------------------------
            // Latin 1 entities
            // ----------------------------------------------------------------------

            parser.defineEntityReplacementText("nbsp", "\u00a0");
            parser.defineEntityReplacementText("iexcl", "\u00a1");
            parser.defineEntityReplacementText("cent", "\u00a2");
            parser.defineEntityReplacementText("pound", "\u00a3");
            parser.defineEntityReplacementText("curren", "\u00a4");
            parser.defineEntityReplacementText("yen", "\u00a5");
            parser.defineEntityReplacementText("brvbar", "\u00a6");
            parser.defineEntityReplacementText("sect", "\u00a7");
            parser.defineEntityReplacementText("uml", "\u00a8");
            parser.defineEntityReplacementText("copy", "\u00a9");
            parser.defineEntityReplacementText("ordf", "\u00aa");
            parser.defineEntityReplacementText("laquo", "\u00ab");
            parser.defineEntityReplacementText("not", "\u00ac");
            parser.defineEntityReplacementText("shy", "\u00ad");
            parser.defineEntityReplacementText("reg", "\u00ae");
            parser.defineEntityReplacementText("macr", "\u00af");
            parser.defineEntityReplacementText("deg", "\u00b0");
            parser.defineEntityReplacementText("plusmn", "\u00b1");
            parser.defineEntityReplacementText("sup2", "\u00b2");
            parser.defineEntityReplacementText("sup3", "\u00b3");
            parser.defineEntityReplacementText("acute", "\u00b4");
            parser.defineEntityReplacementText("micro", "\u00b5");
            parser.defineEntityReplacementText("para", "\u00b6");
            parser.defineEntityReplacementText("middot", "\u00b7");
            parser.defineEntityReplacementText("cedil", "\u00b8");
            parser.defineEntityReplacementText("sup1", "\u00b9");
            parser.defineEntityReplacementText("ordm", "\u00ba");
            parser.defineEntityReplacementText("raquo", "\u00bb");
            parser.defineEntityReplacementText("frac14", "\u00bc");
            parser.defineEntityReplacementText("frac12", "\u00bd");
            parser.defineEntityReplacementText("frac34", "\u00be");
            parser.defineEntityReplacementText("iquest", "\u00bf");
            parser.defineEntityReplacementText("Agrave", "\u00c0");
            parser.defineEntityReplacementText("Aacute", "\u00c1");
            parser.defineEntityReplacementText("Acirc", "\u00c2");
            parser.defineEntityReplacementText("Atilde", "\u00c3");
            parser.defineEntityReplacementText("Auml", "\u00c4");
            parser.defineEntityReplacementText("Aring", "\u00c5");
            parser.defineEntityReplacementText("AElig", "\u00c6");
            parser.defineEntityReplacementText("Ccedil", "\u00c7");
            parser.defineEntityReplacementText("Egrave", "\u00c8");
            parser.defineEntityReplacementText("Eacute", "\u00c9");
            parser.defineEntityReplacementText("Ecirc", "\u00ca");
            parser.defineEntityReplacementText("Euml", "\u00cb");
            parser.defineEntityReplacementText("Igrave", "\u00cc");
            parser.defineEntityReplacementText("Iacute", "\u00cd");
            parser.defineEntityReplacementText("Icirc", "\u00ce");
            parser.defineEntityReplacementText("Iuml", "\u00cf");
            parser.defineEntityReplacementText("ETH", "\u00d0");
            parser.defineEntityReplacementText("Ntilde", "\u00d1");
            parser.defineEntityReplacementText("Ograve", "\u00d2");
            parser.defineEntityReplacementText("Oacute", "\u00d3");
            parser.defineEntityReplacementText("Ocirc", "\u00d4");
            parser.defineEntityReplacementText("Otilde", "\u00d5");
            parser.defineEntityReplacementText("Ouml", "\u00d6");
            parser.defineEntityReplacementText("times", "\u00d7");
            parser.defineEntityReplacementText("Oslash", "\u00d8");
            parser.defineEntityReplacementText("Ugrave", "\u00d9");
            parser.defineEntityReplacementText("Uacute", "\u00da");
            parser.defineEntityReplacementText("Ucirc", "\u00db");
            parser.defineEntityReplacementText("Uuml", "\u00dc");
            parser.defineEntityReplacementText("Yacute", "\u00dd");
            parser.defineEntityReplacementText("THORN", "\u00de");
            parser.defineEntityReplacementText("szlig", "\u00df");
            parser.defineEntityReplacementText("agrave", "\u00e0");
            parser.defineEntityReplacementText("aacute", "\u00e1");
            parser.defineEntityReplacementText("acirc", "\u00e2");
            parser.defineEntityReplacementText("atilde", "\u00e3");
            parser.defineEntityReplacementText("auml", "\u00e4");
            parser.defineEntityReplacementText("aring", "\u00e5");
            parser.defineEntityReplacementText("aelig", "\u00e6");
            parser.defineEntityReplacementText("ccedil", "\u00e7");
            parser.defineEntityReplacementText("egrave", "\u00e8");
            parser.defineEntityReplacementText("eacute", "\u00e9");
            parser.defineEntityReplacementText("ecirc", "\u00ea");
            parser.defineEntityReplacementText("euml", "\u00eb");
            parser.defineEntityReplacementText("igrave", "\u00ec");
            parser.defineEntityReplacementText("iacute", "\u00ed");
            parser.defineEntityReplacementText("icirc", "\u00ee");
            parser.defineEntityReplacementText("iuml", "\u00ef");
            parser.defineEntityReplacementText("eth", "\u00f0");
            parser.defineEntityReplacementText("ntilde", "\u00f1");
            parser.defineEntityReplacementText("ograve", "\u00f2");
            parser.defineEntityReplacementText("oacute", "\u00f3");
            parser.defineEntityReplacementText("ocirc", "\u00f4");
            parser.defineEntityReplacementText("otilde", "\u00f5");
            parser.defineEntityReplacementText("ouml", "\u00f6");
            parser.defineEntityReplacementText("divide", "\u00f7");
            parser.defineEntityReplacementText("oslash", "\u00f8");
            parser.defineEntityReplacementText("ugrave", "\u00f9");
            parser.defineEntityReplacementText("uacute", "\u00fa");
            parser.defineEntityReplacementText("ucirc", "\u00fb");
            parser.defineEntityReplacementText("uuml", "\u00fc");
            parser.defineEntityReplacementText("yacute", "\u00fd");
            parser.defineEntityReplacementText("thorn", "\u00fe");
            parser.defineEntityReplacementText("yuml", "\u00ff");

            // ----------------------------------------------------------------------
            // Special entities
            // ----------------------------------------------------------------------
            parser.defineEntityReplacementText("OElig", "\u0152");
            parser.defineEntityReplacementText("oelig", "\u0153");
            parser.defineEntityReplacementText("Scaron", "\u0160");
            parser.defineEntityReplacementText("scaron", "\u0161");
            parser.defineEntityReplacementText("Yuml", "\u0178");
            parser.defineEntityReplacementText("circ", "\u02c6");
            parser.defineEntityReplacementText("tilde", "\u02dc");
            parser.defineEntityReplacementText("ensp", "\u2002");
            parser.defineEntityReplacementText("emsp", "\u2003");
            parser.defineEntityReplacementText("thinsp", "\u2009");
            parser.defineEntityReplacementText("zwnj", "\u200c");
            parser.defineEntityReplacementText("zwj", "\u200d");
            parser.defineEntityReplacementText("lrm", "\u200e");
            parser.defineEntityReplacementText("rlm", "\u200f");
            parser.defineEntityReplacementText("ndash", "\u2013");
            parser.defineEntityReplacementText("mdash", "\u2014");
            parser.defineEntityReplacementText("lsquo", "\u2018");
            parser.defineEntityReplacementText("rsquo", "\u2019");
            parser.defineEntityReplacementText("sbquo", "\u201a");
            parser.defineEntityReplacementText("ldquo", "\u201c");
            parser.defineEntityReplacementText("rdquo", "\u201d");
            parser.defineEntityReplacementText("bdquo", "\u201e");
            parser.defineEntityReplacementText("dagger", "\u2020");
            parser.defineEntityReplacementText("Dagger", "\u2021");
            parser.defineEntityReplacementText("permil", "\u2030");
            parser.defineEntityReplacementText("lsaquo", "\u2039");
            parser.defineEntityReplacementText("rsaquo", "\u203a");
            parser.defineEntityReplacementText("euro", "\u20ac");

            // ----------------------------------------------------------------------
            // Symbol entities
            // ----------------------------------------------------------------------
            parser.defineEntityReplacementText("fnof", "\u0192");
            parser.defineEntityReplacementText("Alpha", "\u0391");
            parser.defineEntityReplacementText("Beta", "\u0392");
            parser.defineEntityReplacementText("Gamma", "\u0393");
            parser.defineEntityReplacementText("Delta", "\u0394");
            parser.defineEntityReplacementText("Epsilon", "\u0395");
            parser.defineEntityReplacementText("Zeta", "\u0396");
            parser.defineEntityReplacementText("Eta", "\u0397");
            parser.defineEntityReplacementText("Theta", "\u0398");
            parser.defineEntityReplacementText("Iota", "\u0399");
            parser.defineEntityReplacementText("Kappa", "\u039a");
            parser.defineEntityReplacementText("Lambda", "\u039b");
            parser.defineEntityReplacementText("Mu", "\u039c");
            parser.defineEntityReplacementText("Nu", "\u039d");
            parser.defineEntityReplacementText("Xi", "\u039e");
            parser.defineEntityReplacementText("Omicron", "\u039f");
            parser.defineEntityReplacementText("Pi", "\u03a0");
            parser.defineEntityReplacementText("Rho", "\u03a1");
            parser.defineEntityReplacementText("Sigma", "\u03a3");
            parser.defineEntityReplacementText("Tau", "\u03a4");
            parser.defineEntityReplacementText("Upsilon", "\u03a5");
            parser.defineEntityReplacementText("Phi", "\u03a6");
            parser.defineEntityReplacementText("Chi", "\u03a7");
            parser.defineEntityReplacementText("Psi", "\u03a8");
            parser.defineEntityReplacementText("Omega", "\u03a9");
            parser.defineEntityReplacementText("alpha", "\u03b1");
            parser.defineEntityReplacementText("beta", "\u03b2");
            parser.defineEntityReplacementText("gamma", "\u03b3");
            parser.defineEntityReplacementText("delta", "\u03b4");
            parser.defineEntityReplacementText("epsilon", "\u03b5");
            parser.defineEntityReplacementText("zeta", "\u03b6");
            parser.defineEntityReplacementText("eta", "\u03b7");
            parser.defineEntityReplacementText("theta", "\u03b8");
            parser.defineEntityReplacementText("iota", "\u03b9");
            parser.defineEntityReplacementText("kappa", "\u03ba");
            parser.defineEntityReplacementText("lambda", "\u03bb");
            parser.defineEntityReplacementText("mu", "\u03bc");
            parser.defineEntityReplacementText("nu", "\u03bd");
            parser.defineEntityReplacementText("xi", "\u03be");
            parser.defineEntityReplacementText("omicron", "\u03bf");
            parser.defineEntityReplacementText("pi", "\u03c0");
            parser.defineEntityReplacementText("rho", "\u03c1");
            parser.defineEntityReplacementText("sigmaf", "\u03c2");
            parser.defineEntityReplacementText("sigma", "\u03c3");
            parser.defineEntityReplacementText("tau", "\u03c4");
            parser.defineEntityReplacementText("upsilon", "\u03c5");
            parser.defineEntityReplacementText("phi", "\u03c6");
            parser.defineEntityReplacementText("chi", "\u03c7");
            parser.defineEntityReplacementText("psi", "\u03c8");
            parser.defineEntityReplacementText("omega", "\u03c9");
            parser.defineEntityReplacementText("thetasym", "\u03d1");
            parser.defineEntityReplacementText("upsih", "\u03d2");
            parser.defineEntityReplacementText("piv", "\u03d6");
            parser.defineEntityReplacementText("bull", "\u2022");
            parser.defineEntityReplacementText("hellip", "\u2026");
            parser.defineEntityReplacementText("prime", "\u2032");
            parser.defineEntityReplacementText("Prime", "\u2033");
            parser.defineEntityReplacementText("oline", "\u203e");
            parser.defineEntityReplacementText("frasl", "\u2044");
            parser.defineEntityReplacementText("weierp", "\u2118");
            parser.defineEntityReplacementText("image", "\u2111");
            parser.defineEntityReplacementText("real", "\u211c");
            parser.defineEntityReplacementText("trade", "\u2122");
            parser.defineEntityReplacementText("alefsym", "\u2135");
            parser.defineEntityReplacementText("larr", "\u2190");
            parser.defineEntityReplacementText("uarr", "\u2191");
            parser.defineEntityReplacementText("rarr", "\u2192");
            parser.defineEntityReplacementText("darr", "\u2193");
            parser.defineEntityReplacementText("harr", "\u2194");
            parser.defineEntityReplacementText("crarr", "\u21b5");
            parser.defineEntityReplacementText("lArr", "\u21d0");
            parser.defineEntityReplacementText("uArr", "\u21d1");
            parser.defineEntityReplacementText("rArr", "\u21d2");
            parser.defineEntityReplacementText("dArr", "\u21d3");
            parser.defineEntityReplacementText("hArr", "\u21d4");
            parser.defineEntityReplacementText("forall", "\u2200");
            parser.defineEntityReplacementText("part", "\u2202");
            parser.defineEntityReplacementText("exist", "\u2203");
            parser.defineEntityReplacementText("empty", "\u2205");
            parser.defineEntityReplacementText("nabla", "\u2207");
            parser.defineEntityReplacementText("isin", "\u2208");
            parser.defineEntityReplacementText("notin", "\u2209");
            parser.defineEntityReplacementText("ni", "\u220b");
            parser.defineEntityReplacementText("prod", "\u220f");
            parser.defineEntityReplacementText("sum", "\u2211");
            parser.defineEntityReplacementText("minus", "\u2212");
            parser.defineEntityReplacementText("lowast", "\u2217");
            parser.defineEntityReplacementText("radic", "\u221a");
            parser.defineEntityReplacementText("prop", "\u221d");
            parser.defineEntityReplacementText("infin", "\u221e");
            parser.defineEntityReplacementText("ang", "\u2220");
            parser.defineEntityReplacementText("and", "\u2227");
            parser.defineEntityReplacementText("or", "\u2228");
            parser.defineEntityReplacementText("cap", "\u2229");
            parser.defineEntityReplacementText("cup", "\u222a");
            parser.defineEntityReplacementText("int", "\u222b");
            parser.defineEntityReplacementText("there4", "\u2234");
            parser.defineEntityReplacementText("sim", "\u223c");
            parser.defineEntityReplacementText("cong", "\u2245");
            parser.defineEntityReplacementText("asymp", "\u2248");
            parser.defineEntityReplacementText("ne", "\u2260");
            parser.defineEntityReplacementText("equiv", "\u2261");
            parser.defineEntityReplacementText("le", "\u2264");
            parser.defineEntityReplacementText("ge", "\u2265");
            parser.defineEntityReplacementText("sub", "\u2282");
            parser.defineEntityReplacementText("sup", "\u2283");
            parser.defineEntityReplacementText("nsub", "\u2284");
            parser.defineEntityReplacementText("sube", "\u2286");
            parser.defineEntityReplacementText("supe", "\u2287");
            parser.defineEntityReplacementText("oplus", "\u2295");
            parser.defineEntityReplacementText("otimes", "\u2297");
            parser.defineEntityReplacementText("perp", "\u22a5");
            parser.defineEntityReplacementText("sdot", "\u22c5");
            parser.defineEntityReplacementText("lceil", "\u2308");
            parser.defineEntityReplacementText("rceil", "\u2309");
            parser.defineEntityReplacementText("lfloor", "\u230a");
            parser.defineEntityReplacementText("rfloor", "\u230b");
            parser.defineEntityReplacementText("lang", "\u2329");
            parser.defineEntityReplacementText("rang", "\u232a");
            parser.defineEntityReplacementText("loz", "\u25ca");
            parser.defineEntityReplacementText("spades", "\u2660");
            parser.defineEntityReplacementText("clubs", "\u2663");
            parser.defineEntityReplacementText("hearts", "\u2665");
            parser.defineEntityReplacementText("diams", "\u2666");

        }
    } //-- void initParser( XmlPullParser )

    /**
     * Method nextTag.
     *
     * @param parser
     * @throws IOException
     * @throws XmlPullParserException
     * @return int
     */
    private int nextTag(XmlPullParser parser)
            throws IOException, XmlPullParserException
    {
        int eventType = parser.next();
        if (eventType == TEXT)
        {
            eventType = parser.next();
        }
        if (eventType != START_TAG && eventType != END_TAG)
        {
            throw new XmlPullParserException(
                    "expected START_TAG or END_TAG not " + TYPES[eventType],
                    parser, null);
        }
        return eventType;
    } //-- int nextTag( XmlPullParser )

    /**
     * Method parseActivation.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Activation
     */
    private Activation parseActivation(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Activation activation = new Activation();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "activeByDefault", null, parsed))
            {
                activation.setActiveByDefault(getBooleanValue(getTrimmedValue(
                        parser.nextText()), "activeByDefault", parser, "false"));
            }
            else if (checkFieldWithDuplicate(parser, "jdk", null, parsed))
            {
                activation.setJdk(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "os", null, parsed))
            {
                activation.setOs(parseActivationOS(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "property", null, parsed))
            {
                activation.setProperty(parseActivationProperty(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "file", null, parsed))
            {
                activation.setFile(parseActivationFile(parser, strict));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return activation;
    } //-- Activation parseActivation( XmlPullParser, boolean )

    /**
     * Method parseActivationFile.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return ActivationFile
     */
    private ActivationFile parseActivationFile(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        ActivationFile activationFile = new ActivationFile();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "missing", null, parsed))
            {
                activationFile.setMissing(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "exists", null, parsed))
            {
                activationFile.setExists(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return activationFile;
    } //-- ActivationFile parseActivationFile( XmlPullParser, boolean )

    /**
     * Method parseActivationOS.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return ActivationOS
     */
    private ActivationOS parseActivationOS(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        ActivationOS activationOS = new ActivationOS();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                activationOS.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "family", null, parsed))
            {
                activationOS.setFamily(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "arch", null, parsed))
            {
                activationOS.setArch(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "version", null, parsed))
            {
                activationOS.setVersion(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return activationOS;
    } //-- ActivationOS parseActivationOS( XmlPullParser, boolean )

    /**
     * Method parseActivationProperty.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return ActivationProperty
     */
    private ActivationProperty parseActivationProperty(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        ActivationProperty activationProperty = new ActivationProperty();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                activationProperty.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "value", null, parsed))
            {
                activationProperty.setValue(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return activationProperty;
    } //-- ActivationProperty parseActivationProperty( XmlPullParser, boolean )

    /**
     * Method parseBuild.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Build
     */
    private Build parseBuild(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Build build = new Build();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "sourceDirectory", null, parsed))
            {
                build.setSourceDirectory(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "scriptSourceDirectory",
                    null, parsed))
            {
                build.setScriptSourceDirectory(
                        getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "testSourceDirectory", null,
                    parsed))
            {
                build.setTestSourceDirectory(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "outputDirectory", null,
                    parsed))
            {
                build.setOutputDirectory(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "testOutputDirectory", null,
                    parsed))
            {
                build.setTestOutputDirectory(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "extensions", null, parsed))
            {
                java.util.List extensions = new java.util.ArrayList/*<Extension>*/();
//                build.setExtensions(extensions);
                while (parser.nextTag() == START_TAG)
                {
                    if ("extension".equals(parser.getName()))
                    {
                        extensions.add(parseExtension(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "defaultGoal", null, parsed))
            {
                build.setDefaultGoal(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "resources", null, parsed))
            {
                java.util.List resources = new java.util.ArrayList/*<Resource>*/();
                build.setResources(resources);
                while (parser.nextTag() == START_TAG)
                {
                    if ("resource".equals(parser.getName()))
                    {
                        resources.add(parseResource(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "testResources", null,
                    parsed))
            {
                java.util.List testResources = new java.util.ArrayList/*<Resource>*/();
                build.setTestResources(testResources);
                while (parser.nextTag() == START_TAG)
                {
                    if ("testResource".equals(parser.getName()))
                    {
                        testResources.add(parseResource(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "directory", null, parsed))
            {
                build.setDirectory(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "finalName", null, parsed))
            {
                build.setFinalName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "filters", null, parsed))
            {
                java.util.List filters = new java.util.ArrayList/*<String>*/();
                build.setFilters(filters);
                while (parser.nextTag() == START_TAG)
                {
                    if ("filter".equals(parser.getName()))
                    {
                        filters.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "pluginManagement", null,
                    parsed))
            {
                build.setPluginManagement(parsePluginManagement(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "plugins", null, parsed))
            {
                java.util.List plugins = new java.util.ArrayList/*<Plugin>*/();
                build.setPlugins(plugins);
                while (parser.nextTag() == START_TAG)
                {
                    if ("plugin".equals(parser.getName()))
                    {
                        plugins.add(parsePlugin(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return build;
    } //-- Build parseBuild( XmlPullParser, boolean )

    /**
     * Method parseBuildBase.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return BuildBase
     */
    private BuildBase parseBuildBase(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        BuildBase buildBase = new BuildBase();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "defaultGoal", null, parsed))
            {
                buildBase.setDefaultGoal(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "resources", null, parsed))
            {
                java.util.List resources = new java.util.ArrayList/*<Resource>*/();
                buildBase.setResources(resources);
                while (parser.nextTag() == START_TAG)
                {
                    if ("resource".equals(parser.getName()))
                    {
                        resources.add(parseResource(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "testResources", null,
                    parsed))
            {
                java.util.List testResources = new java.util.ArrayList/*<Resource>*/();
                buildBase.setTestResources(testResources);
                while (parser.nextTag() == START_TAG)
                {
                    if ("testResource".equals(parser.getName()))
                    {
                        testResources.add(parseResource(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "directory", null, parsed))
            {
                buildBase.setDirectory(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "finalName", null, parsed))
            {
                buildBase.setFinalName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "filters", null, parsed))
            {
                java.util.List filters = new java.util.ArrayList/*<String>*/();
                buildBase.setFilters(filters);
                while (parser.nextTag() == START_TAG)
                {
                    if ("filter".equals(parser.getName()))
                    {
                        filters.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "pluginManagement", null,
                    parsed))
            {
                buildBase.setPluginManagement(parsePluginManagement(parser,
                        strict));
            }
            else if (checkFieldWithDuplicate(parser, "plugins", null, parsed))
            {
                java.util.List plugins = new java.util.ArrayList/*<Plugin>*/();
                buildBase.setPlugins(plugins);
                while (parser.nextTag() == START_TAG)
                {
                    if ("plugin".equals(parser.getName()))
                    {
                        plugins.add(parsePlugin(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return buildBase;
    } //-- BuildBase parseBuildBase( XmlPullParser, boolean )

    /**
     * Method parseCiManagement.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return CiManagement
     */
    private CiManagement parseCiManagement(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        CiManagement ciManagement = new CiManagement();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "system", null, parsed))
            {
                ciManagement.setSystem(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                ciManagement.setUrl(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "notifiers", null, parsed))
            {
                java.util.List notifiers = new java.util.ArrayList/*<Notifier>*/();
                ciManagement.setNotifiers(notifiers);
                while (parser.nextTag() == START_TAG)
                {
                    if ("notifier".equals(parser.getName()))
                    {
                        notifiers.add(parseNotifier(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return ciManagement;
    } //-- CiManagement parseCiManagement( XmlPullParser, boolean )

    /**
     * Method parseConfigurationContainer.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return ConfigurationContainer
     */
    private ConfigurationContainer parseConfigurationContainer(
            XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        ConfigurationContainer configurationContainer = new ConfigurationContainer();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "inherited", null, parsed))
            {
                configurationContainer.setInherited(getTrimmedValue(parser
                        .nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "configuration", null,
                    parsed))
            {
                configurationContainer.setConfiguration(build(
                        parser));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return configurationContainer;
    } //-- ConfigurationContainer parseConfigurationContainer( XmlPullParser, boolean )

    /**
     * Method parseContributor.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Contributor
     */
    private Contributor parseContributor(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Contributor contributor = new Contributor();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                contributor.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "email", null, parsed))
            {
                contributor.setEmail(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                contributor.setUrl(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "organization",
                    "organisation", parsed))
            {
                contributor.setOrganization(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "organizationUrl",
                    "organisationUrl", parsed))
            {
                contributor.setOrganizationUrl(
                        getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "roles", null, parsed))
            {
                java.util.List roles = new java.util.ArrayList/*<String>*/();
                contributor.setRoles(roles);
                while (parser.nextTag() == START_TAG)
                {
                    if ("role".equals(parser.getName()))
                    {
                        roles.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "timezone", null, parsed))
            {
                contributor.setTimezone(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "properties", null, parsed))
            {
                while (parser.nextTag() == START_TAG)
                {
                    String key = parser.getName();
                    String value = parser.nextText().trim();
                    contributor.addProperty(key, value);
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return contributor;
    } //-- Contributor parseContributor( XmlPullParser, boolean )

    /**
     * Method parseDependency.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Dependency
     */
    private Dependency parseDependency(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Dependency dependency = new Dependency();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "groupId", null, parsed))
            {
                dependency.setGroupId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "artifactId", null, parsed))
            {
                dependency.setArtifactId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "version", null, parsed))
            {
                dependency.setVersion(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "type", null, parsed))
            {
                dependency.setType(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "classifier", null, parsed))
            {
                dependency.setClassifier(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "scope", null, parsed))
            {
                dependency.setScope(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "systemPath", null, parsed))
            {
                dependency.setSystemPath(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "exclusions", null, parsed))
            {
                java.util.List exclusions = new java.util.ArrayList/*<Exclusion>*/();
                dependency.setExclusions(exclusions);
                while (parser.nextTag() == START_TAG)
                {
                    if ("exclusion".equals(parser.getName()))
                    {
                        exclusions.add(parseExclusion(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "optional", null, parsed))
            {
                dependency.setOptional(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return dependency;
    } //-- Dependency parseDependency( XmlPullParser, boolean )

    /**
     * Method parseDependencyManagement.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return DependencyManagement
     */
    private DependencyManagement parseDependencyManagement(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        DependencyManagement dependencyManagement = new DependencyManagement();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "dependencies", null, parsed))
            {
                java.util.List dependencies = new java.util.ArrayList/*<Dependency>*/();
                dependencyManagement.setDependencies(dependencies);
                while (parser.nextTag() == START_TAG)
                {
                    if ("dependency".equals(parser.getName()))
                    {
                        dependencies.add(parseDependency(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return dependencyManagement;
    } //-- DependencyManagement parseDependencyManagement( XmlPullParser, boolean )

    /**
     * Method parseDeploymentRepository.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return DeploymentRepository
     */
    private DeploymentRepository parseDeploymentRepository(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        DeploymentRepository deploymentRepository = new DeploymentRepository();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "uniqueVersion", null, parsed))
            {
                deploymentRepository.setUniqueVersion(getBooleanValue(
                        getTrimmedValue(parser.nextText()), "uniqueVersion",
                        parser, "true"));
            }
            else if (checkFieldWithDuplicate(parser, "releases", null, parsed))
            {
                deploymentRepository.setReleases(parseRepositoryPolicy(parser,
                        strict));
            }
            else if (checkFieldWithDuplicate(parser, "snapshots", null, parsed))
            {
                deploymentRepository.setSnapshots(parseRepositoryPolicy(parser,
                        strict));
            }
            else if (checkFieldWithDuplicate(parser, "id", null, parsed))
            {
                deploymentRepository.setId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                deploymentRepository.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                deploymentRepository.setUrl(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "layout", null, parsed))
            {
                deploymentRepository.setLayout(
                        getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return deploymentRepository;
    } //-- DeploymentRepository parseDeploymentRepository( XmlPullParser, boolean )

    /**
     * Method parseDeveloper.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Developer
     */
    private Developer parseDeveloper(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Developer developer = new Developer();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "id", null, parsed))
            {
                developer.setId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                developer.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "email", null, parsed))
            {
                developer.setEmail(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                developer.setUrl(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "organization",
                    "organisation", parsed))
            {
                developer.setOrganization(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "organizationUrl",
                    "organisationUrl", parsed))
            {
                developer.setOrganizationUrl(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "roles", null, parsed))
            {
                java.util.List roles = new java.util.ArrayList/*<String>*/();
                developer.setRoles(roles);
                while (parser.nextTag() == START_TAG)
                {
                    if ("role".equals(parser.getName()))
                    {
                        roles.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "timezone", null, parsed))
            {
                developer.setTimezone(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "properties", null, parsed))
            {
                while (parser.nextTag() == START_TAG)
                {
                    String key = parser.getName();
                    String value = parser.nextText().trim();
                    developer.addProperty(key, value);
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return developer;
    } //-- Developer parseDeveloper( XmlPullParser, boolean )

    /**
     * Method parseDistributionManagement.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return DistributionManagement
     */
    private DistributionManagement parseDistributionManagement(
            XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        DistributionManagement distributionManagement = new DistributionManagement();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "repository", null, parsed))
            {
                distributionManagement.setRepository(parseDeploymentRepository(
                        parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "snapshotRepository", null,
                    parsed))
            {
                distributionManagement.setSnapshotRepository(
                        parseDeploymentRepository(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "site", null, parsed))
            {
                distributionManagement.setSite(parseSite(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "downloadUrl", null, parsed))
            {
                distributionManagement.setDownloadUrl(getTrimmedValue(parser
                        .nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "relocation", null, parsed))
            {
                distributionManagement.setRelocation(parseRelocation(parser,
                        strict));
            }
            else if (checkFieldWithDuplicate(parser, "status", null, parsed))
            {
                distributionManagement.setStatus(getTrimmedValue(parser
                        .nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return distributionManagement;
    } //-- DistributionManagement parseDistributionManagement( XmlPullParser, boolean )

    /**
     * Method parseExclusion.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Exclusion
     */
    private Exclusion parseExclusion(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Exclusion exclusion = new Exclusion();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "artifactId", null, parsed))
            {
                exclusion.setArtifactId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "groupId", null, parsed))
            {
                exclusion.setGroupId(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return exclusion;
    } //-- Exclusion parseExclusion( XmlPullParser, boolean )

    /**
     * Method parseExtension.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Extension
     */
    private Extension parseExtension(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Extension extension = new Extension();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "groupId", null, parsed))
            {
                extension.setGroupId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "artifactId", null, parsed))
            {
                extension.setArtifactId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "version", null, parsed))
            {
                extension.setVersion(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return extension;
    } //-- Extension parseExtension( XmlPullParser, boolean )

    /**
     * Method parseFileSet.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return FileSet
     */
    private FileSet parseFileSet(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        FileSet fileSet = new FileSet();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "directory", null, parsed))
            {
                fileSet.setDirectory(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "includes", null, parsed))
            {
                java.util.List includes = new java.util.ArrayList/*<String>*/();
                fileSet.setIncludes(includes);
                while (parser.nextTag() == START_TAG)
                {
                    if ("include".equals(parser.getName()))
                    {
                        includes.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "excludes", null, parsed))
            {
                java.util.List excludes = new java.util.ArrayList/*<String>*/();
                fileSet.setExcludes(excludes);
                while (parser.nextTag() == START_TAG)
                {
                    if ("exclude".equals(parser.getName()))
                    {
                        excludes.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return fileSet;
    } //-- FileSet parseFileSet( XmlPullParser, boolean )

    /**
     * Method parseIssueManagement.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return IssueManagement
     */
    private IssueManagement parseIssueManagement(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        IssueManagement issueManagement = new IssueManagement();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "system", null, parsed))
            {
                issueManagement.setSystem(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                issueManagement.setUrl(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return issueManagement;
    } //-- IssueManagement parseIssueManagement( XmlPullParser, boolean )

    /**
     * Method parseLicense.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return License
     */
    private License parseLicense(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        License license = new License();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                license.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                license.setUrl(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "distribution", null,
                    parsed))
            {
                license.setDistribution(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "comments", null, parsed))
            {
                license.setComments(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return license;
    } //-- License parseLicense( XmlPullParser, boolean )

    /**
     * Method parseMailingList.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return MailingList
     */
    private MailingList parseMailingList(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        MailingList mailingList = new MailingList();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                mailingList.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "subscribe", null, parsed))
            {
                mailingList.setSubscribe(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "unsubscribe", null, parsed))
            {
                mailingList.setUnsubscribe(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "post", null, parsed))
            {
                mailingList.setPost(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "archive", null, parsed))
            {
                mailingList.setArchive(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "otherArchives", null,
                    parsed))
            {
                java.util.List otherArchives = new java.util.ArrayList/*<String>*/();
                mailingList.setOtherArchives(otherArchives);
                while (parser.nextTag() == START_TAG)
                {
                    if ("otherArchive".equals(parser.getName()))
                    {
                        otherArchives.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return mailingList;
    } //-- MailingList parseMailingList( XmlPullParser, boolean )

    /**
     * Method parseModel.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Model
     */
    private Model parseModel(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Model model = new Model();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else if ("xmlns".equals(name))
            {
                // ignore xmlns attribute in root class, which is a reserved attribute name
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "modelVersion", null, parsed))
            {
                model.setModelVersion(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "parent", null, parsed))
            {
                model.setParent(parseParent(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "groupId", null, parsed))
            {
                model.setGroupId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "artifactId", null, parsed))
            {
                model.setArtifactId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "version", null, parsed))
            {
                model.setVersion(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "packaging", null, parsed))
            {
                model.setPackaging(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                model.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "description", null, parsed))
            {
                model.setDescription(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                model.setUrl(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "inceptionYear", null,
                    parsed))
            {
                model.setInceptionYear(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "organization",
                    "organisation", parsed))
            {
                model.setOrganization(parseOrganization(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "licenses", null, parsed))
            {
                java.util.List licenses = new java.util.ArrayList/*<License>*/();
                model.setLicenses(licenses);
                while (parser.nextTag() == START_TAG)
                {
                    if ("license".equals(parser.getName()))
                    {
                        licenses.add(parseLicense(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "developers", null, parsed))
            {
                java.util.List developers = new java.util.ArrayList/*<Developer>*/();
                model.setDevelopers(developers);
                while (parser.nextTag() == START_TAG)
                {
                    if ("developer".equals(parser.getName()))
                    {
                        developers.add(parseDeveloper(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "contributors", null,
                    parsed))
            {
                java.util.List contributors = new java.util.ArrayList/*<Contributor>*/();
                model.setContributors(contributors);
                while (parser.nextTag() == START_TAG)
                {
                    if ("contributor".equals(parser.getName()))
                    {
                        contributors.add(parseContributor(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "mailingLists", null,
                    parsed))
            {
                java.util.List mailingLists = new java.util.ArrayList/*<MailingList>*/();
                model.setMailingLists(mailingLists);
                while (parser.nextTag() == START_TAG)
                {
                    if ("mailingList".equals(parser.getName()))
                    {
                        mailingLists.add(parseMailingList(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "prerequisites", null,
                    parsed))
            {
                model.setPrerequisites(parsePrerequisites(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "modules", null, parsed))
            {
                java.util.List modules = new java.util.ArrayList/*<String>*/();
                model.setModules(modules);
                while (parser.nextTag() == START_TAG)
                {
                    if ("module".equals(parser.getName()))
                    {
                        modules.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "scm", null, parsed))
            {
                model.setScm(parseScm(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "issueManagement", null,
                    parsed))
            {
                model.setIssueManagement(parseIssueManagement(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "ciManagement", null,
                    parsed))
            {
                model.setCiManagement(parseCiManagement(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "distributionManagement",
                    null, parsed))
            {
                model.setDistributionManagement(parseDistributionManagement(
                        parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "properties", null, parsed))
            {
                while (parser.nextTag() == START_TAG)
                {
                    String key = parser.getName();
                    String value = parser.nextText().trim();
                    model.addProperty(key, value);
                }
            }
            else if (checkFieldWithDuplicate(parser, "dependencyManagement",
                    null, parsed))
            {
                model.setDependencyManagement(parseDependencyManagement(parser,
                        strict));
            }
            else if (checkFieldWithDuplicate(parser, "dependencies", null,
                    parsed))
            {
                java.util.List dependencies = new java.util.ArrayList/*<Dependency>*/();
                model.setDependencies(dependencies);
                while (parser.nextTag() == START_TAG)
                {
                    if ("dependency".equals(parser.getName()))
                    {
                        dependencies.add(parseDependency(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "repositories", null,
                    parsed))
            {
                java.util.List repositories = new java.util.ArrayList/*<Repository>*/();
                model.setRepositories(repositories);
                while (parser.nextTag() == START_TAG)
                {
                    if ("repository".equals(parser.getName()))
                    {
                        repositories.add(parseRepository(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "pluginRepositories", null,
                    parsed))
            {
                java.util.List pluginRepositories = new java.util.ArrayList/*<Repository>*/();
                model.setPluginRepositories(pluginRepositories);
                while (parser.nextTag() == START_TAG)
                {
                    if ("pluginRepository".equals(parser.getName()))
                    {
                        pluginRepositories.add(parseRepository(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "build", null, parsed))
            {
                model.setBuild(parseBuild(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "reports", null, parsed))
            {
                model.setReports(build(parser));
            }
            else if (checkFieldWithDuplicate(parser, "reporting", null, parsed))
            {
                model.setReporting(parseReporting(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "profiles", null, parsed))
            {
                java.util.List profiles = new java.util.ArrayList/*<Profile>*/();
                model.setProfiles(profiles);
                while (parser.nextTag() == START_TAG)
                {
                    if ("profile".equals(parser.getName()))
                    {
                        profiles.add(parseProfile(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return model;
    } //-- Model parseModel( XmlPullParser, boolean )

    public List<Profile> parseProfiles(XmlPullParser parser, boolean strict)
            throws XmlPullParserException, IOException
    {
        List<Profile> profiles = new ArrayList<>();
        while (parser.nextTag() == START_TAG)
        {
            if ("profile".equals(parser.getName()))
            {
                profiles.add(parseProfile(parser, strict));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return profiles;
    }

    /**
     * Method parseModelBase.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return ModelBase
     */
    private ModelBase parseModelBase(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        ModelBase modelBase = new ModelBase();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "modules", null, parsed))
            {
                java.util.List modules = new java.util.ArrayList/*<String>*/();
                modelBase.setModules(modules);
                while (parser.nextTag() == START_TAG)
                {
                    if ("module".equals(parser.getName()))
                    {
                        modules.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "distributionManagement",
                    null, parsed))
            {
                modelBase.setDistributionManagement(parseDistributionManagement(
                        parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "properties", null, parsed))
            {
                while (parser.nextTag() == START_TAG)
                {
                    String key = parser.getName();
                    String value = parser.nextText().trim();
                    modelBase.addProperty(key, value);
                }
            }
            else if (checkFieldWithDuplicate(parser, "dependencyManagement",
                    null, parsed))
            {
                modelBase.setDependencyManagement(parseDependencyManagement(
                        parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "dependencies", null,
                    parsed))
            {
                java.util.List dependencies = new java.util.ArrayList/*<Dependency>*/();
                modelBase.setDependencies(dependencies);
                while (parser.nextTag() == START_TAG)
                {
                    if ("dependency".equals(parser.getName()))
                    {
                        dependencies.add(parseDependency(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "repositories", null,
                    parsed))
            {
                java.util.List repositories = new java.util.ArrayList/*<Repository>*/();
                modelBase.setRepositories(repositories);
                while (parser.nextTag() == START_TAG)
                {
                    if ("repository".equals(parser.getName()))
                    {
                        repositories.add(parseRepository(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "pluginRepositories", null,
                    parsed))
            {
                java.util.List pluginRepositories = new java.util.ArrayList/*<Repository>*/();
                modelBase.setPluginRepositories(pluginRepositories);
                while (parser.nextTag() == START_TAG)
                {
                    if ("pluginRepository".equals(parser.getName()))
                    {
                        pluginRepositories.add(parseRepository(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "reports", null, parsed))
            {
                modelBase.setReports(build(parser));
            }
            else if (checkFieldWithDuplicate(parser, "reporting", null, parsed))
            {
                modelBase.setReporting(parseReporting(parser, strict));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return modelBase;
    } //-- ModelBase parseModelBase( XmlPullParser, boolean )

    /**
     * Method parseNotifier.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Notifier
     */
    private Notifier parseNotifier(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Notifier notifier = new Notifier();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "type", null, parsed))
            {
                notifier.setType(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "sendOnError", null, parsed))
            {
                notifier.setSendOnError(getBooleanValue(getTrimmedValue(parser
                        .nextText()), "sendOnError", parser, "true"));
            }
            else if (checkFieldWithDuplicate(parser, "sendOnFailure", null,
                    parsed))
            {
                notifier.setSendOnFailure(getBooleanValue(getTrimmedValue(parser
                        .nextText()), "sendOnFailure", parser, "true"));
            }
            else if (checkFieldWithDuplicate(parser, "sendOnSuccess", null,
                    parsed))
            {
                notifier.setSendOnSuccess(getBooleanValue(getTrimmedValue(parser
                        .nextText()), "sendOnSuccess", parser, "true"));
            }
            else if (checkFieldWithDuplicate(parser, "sendOnWarning", null,
                    parsed))
            {
                notifier.setSendOnWarning(getBooleanValue(getTrimmedValue(parser
                        .nextText()), "sendOnWarning", parser, "true"));
            }
            else if (checkFieldWithDuplicate(parser, "address", null, parsed))
            {
                notifier.setAddress(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "configuration", null,
                    parsed))
            {
                while (parser.nextTag() == START_TAG)
                {
                    String key = parser.getName();
                    String value = parser.nextText().trim();
                    notifier.addConfiguration(key, value);
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return notifier;
    } //-- Notifier parseNotifier( XmlPullParser, boolean )

    /**
     * Method parseOrganization.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Organization
     */
    private Organization parseOrganization(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Organization organization = new Organization();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                organization.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                organization.setUrl(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return organization;
    } //-- Organization parseOrganization( XmlPullParser, boolean )

    /**
     * Method parseParent.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Parent
     */
    private Parent parseParent(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Parent parent = new Parent();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "groupId", null, parsed))
            {
                parent.setGroupId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "artifactId", null, parsed))
            {
                parent.setArtifactId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "version", null, parsed))
            {
                parent.setVersion(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "relativePath", null,
                    parsed))
            {
                parent.setRelativePath(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return parent;
    } //-- Parent parseParent( XmlPullParser, boolean )

    /**
     * Method parsePatternSet.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return PatternSet
     */
    private PatternSet parsePatternSet(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        PatternSet patternSet = new PatternSet();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "includes", null, parsed))
            {
                java.util.List includes = new java.util.ArrayList/*<String>*/();
                patternSet.setIncludes(includes);
                while (parser.nextTag() == START_TAG)
                {
                    if ("include".equals(parser.getName()))
                    {
                        includes.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "excludes", null, parsed))
            {
                java.util.List excludes = new java.util.ArrayList/*<String>*/();
                patternSet.setExcludes(excludes);
                while (parser.nextTag() == START_TAG)
                {
                    if ("exclude".equals(parser.getName()))
                    {
                        excludes.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return patternSet;
    } //-- PatternSet parsePatternSet( XmlPullParser, boolean )

    /**
     * Method parsePlugin.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Plugin
     */
    private Plugin parsePlugin(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Plugin plugin = new Plugin();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "groupId", null, parsed))
            {
                plugin.setGroupId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "artifactId", null, parsed))
            {
                plugin.setArtifactId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "version", null, parsed))
            {
                plugin.setVersion(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "extensions", null, parsed))
            {
                plugin.setExtensions(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "executions", null, parsed))
            {
                java.util.List executions = new java.util.ArrayList/*<PluginExecution>*/();
                plugin.setExecutions(executions);
                while (parser.nextTag() == START_TAG)
                {
                    if ("execution".equals(parser.getName()))
                    {
                        executions.add(parsePluginExecution(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "dependencies", null,
                    parsed))
            {
                java.util.List dependencies = new java.util.ArrayList/*<Dependency>*/();
                plugin.setDependencies(dependencies);
                while (parser.nextTag() == START_TAG)
                {
                    if ("dependency".equals(parser.getName()))
                    {
                        dependencies.add(parseDependency(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "goals", null, parsed))
            {
                plugin.setGoals(build(parser));
            }
            else if (checkFieldWithDuplicate(parser, "inherited", null, parsed))
            {
                plugin.setInherited(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "configuration", null,
                    parsed))
            {
                plugin.setConfiguration(build(parser));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return plugin;
    } //-- Plugin parsePlugin( XmlPullParser, boolean )

    /**
     * Method parsePluginConfiguration.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return PluginConfiguration
     */
    private PluginConfiguration parsePluginConfiguration(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        PluginConfiguration pluginConfiguration = new PluginConfiguration();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "pluginManagement", null, parsed))
            {
                pluginConfiguration.setPluginManagement(parsePluginManagement(
                        parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "plugins", null, parsed))
            {
                java.util.List plugins = new java.util.ArrayList/*<Plugin>*/();
                pluginConfiguration.setPlugins(plugins);
                while (parser.nextTag() == START_TAG)
                {
                    if ("plugin".equals(parser.getName()))
                    {
                        plugins.add(parsePlugin(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return pluginConfiguration;
    } //-- PluginConfiguration parsePluginConfiguration( XmlPullParser, boolean )

    /**
     * Method parsePluginContainer.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return PluginContainer
     */
    private PluginContainer parsePluginContainer(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        PluginContainer pluginContainer = new PluginContainer();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "plugins", null, parsed))
            {
                java.util.List plugins = new java.util.ArrayList/*<Plugin>*/();
                pluginContainer.setPlugins(plugins);
                while (parser.nextTag() == START_TAG)
                {
                    if ("plugin".equals(parser.getName()))
                    {
                        plugins.add(parsePlugin(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return pluginContainer;
    } //-- PluginContainer parsePluginContainer( XmlPullParser, boolean )

    /**
     * Method parsePluginExecution.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return PluginExecution
     */
    private PluginExecution parsePluginExecution(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        PluginExecution pluginExecution = new PluginExecution();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "id", null, parsed))
            {
                pluginExecution.setId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "phase", null, parsed))
            {
                pluginExecution.setPhase(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "goals", null, parsed))
            {
                java.util.List goals = new java.util.ArrayList/*<String>*/();
                pluginExecution.setGoals(goals);
                while (parser.nextTag() == START_TAG)
                {
                    if ("goal".equals(parser.getName()))
                    {
                        goals.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "inherited", null, parsed))
            {
                pluginExecution.setInherited(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "configuration", null,
                    parsed))
            {
                pluginExecution.setConfiguration(build(parser));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return pluginExecution;
    } //-- PluginExecution parsePluginExecution( XmlPullParser, boolean )

    /**
     * Method parsePluginManagement.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return PluginManagement
     */
    private PluginManagement parsePluginManagement(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        PluginManagement pluginManagement = new PluginManagement();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "plugins", null, parsed))
            {
                java.util.List plugins = new java.util.ArrayList/*<Plugin>*/();
                pluginManagement.setPlugins(plugins);
                while (parser.nextTag() == START_TAG)
                {
                    if ("plugin".equals(parser.getName()))
                    {
                        plugins.add(parsePlugin(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return pluginManagement;
    } //-- PluginManagement parsePluginManagement( XmlPullParser, boolean )

    /**
     * Method parsePrerequisites.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Prerequisites
     */
    private Prerequisites parsePrerequisites(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Prerequisites prerequisites = new Prerequisites();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "maven", null, parsed))
            {
                prerequisites.setMaven(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return prerequisites;
    } //-- Prerequisites parsePrerequisites( XmlPullParser, boolean )

    /**
     * Method parseProfile.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Profile
     */
    private Profile parseProfile(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Profile profile = new Profile();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "id", null, parsed))
            {
                profile.setId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "activation", null, parsed))
            {
                profile.setActivation(parseActivation(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "build", null, parsed))
            {
                profile.setBuild(parseBuildBase(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "modules", null, parsed))
            {
                java.util.List modules = new java.util.ArrayList/*<String>*/();
                profile.setModules(modules);
                while (parser.nextTag() == START_TAG)
                {
                    if ("module".equals(parser.getName()))
                    {
                        modules.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "distributionManagement",
                    null, parsed))
            {
                profile.setDistributionManagement(parseDistributionManagement(
                        parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "properties", null, parsed))
            {
                while (parser.nextTag() == START_TAG)
                {
                    String key = parser.getName();
                    String value = parser.nextText().trim();
                    profile.addProperty(key, value);
                }
            }
            else if (checkFieldWithDuplicate(parser, "dependencyManagement",
                    null, parsed))
            {
                profile.setDependencyManagement(
                        parseDependencyManagement(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "dependencies", null,
                    parsed))
            {
                java.util.List dependencies = new java.util.ArrayList/*<Dependency>*/();
                profile.setDependencies(dependencies);
                while (parser.nextTag() == START_TAG)
                {
                    if ("dependency".equals(parser.getName()))
                    {
                        dependencies.add(parseDependency(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "repositories", null,
                    parsed))
            {
                java.util.List repositories = new java.util.ArrayList/*<Repository>*/();
                profile.setRepositories(repositories);
                while (parser.nextTag() == START_TAG)
                {
                    if ("repository".equals(parser.getName()))
                    {
                        repositories.add(parseRepository(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "pluginRepositories", null,
                    parsed))
            {
                java.util.List pluginRepositories = new java.util.ArrayList/*<Repository>*/();
                profile.setPluginRepositories(pluginRepositories);
                while (parser.nextTag() == START_TAG)
                {
                    if ("pluginRepository".equals(parser.getName()))
                    {
                        pluginRepositories.add(parseRepository(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "reports", null, parsed))
            {
                profile.setReports(build(parser));
            }
            else if (checkFieldWithDuplicate(parser, "reporting", null, parsed))
            {
                profile.setReporting(parseReporting(parser, strict));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return profile;
    } //-- Profile parseProfile( XmlPullParser, boolean )

    /**
     * Method parseRelocation.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Relocation
     */
    private Relocation parseRelocation(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Relocation relocation = new Relocation();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "groupId", null, parsed))
            {
                relocation.setGroupId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "artifactId", null, parsed))
            {
                relocation.setArtifactId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "version", null, parsed))
            {
                relocation.setVersion(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "message", null, parsed))
            {
                relocation.setMessage(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return relocation;
    } //-- Relocation parseRelocation( XmlPullParser, boolean )

    /**
     * Method parseReportPlugin.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return ReportPlugin
     */
    private ReportPlugin parseReportPlugin(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        ReportPlugin reportPlugin = new ReportPlugin();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "groupId", null, parsed))
            {
                reportPlugin.setGroupId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "artifactId", null, parsed))
            {
                reportPlugin.setArtifactId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "version", null, parsed))
            {
                reportPlugin.setVersion(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "reportSets", null, parsed))
            {
                java.util.List reportSets = new java.util.ArrayList/*<ReportSet>*/();
                reportPlugin.setReportSets(reportSets);
                while (parser.nextTag() == START_TAG)
                {
                    if ("reportSet".equals(parser.getName()))
                    {
                        reportSets.add(parseReportSet(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "inherited", null, parsed))
            {
                reportPlugin.setInherited(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "configuration", null,
                    parsed))
            {
                reportPlugin.setConfiguration(build(parser));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return reportPlugin;
    } //-- ReportPlugin parseReportPlugin( XmlPullParser, boolean )

    /**
     * Method parseReportSet.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return ReportSet
     */
    private ReportSet parseReportSet(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        ReportSet reportSet = new ReportSet();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "id", null, parsed))
            {
                reportSet.setId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "reports", null, parsed))
            {
                java.util.List reports = new java.util.ArrayList/*<String>*/();
                reportSet.setReports(reports);
                while (parser.nextTag() == START_TAG)
                {
                    if ("report".equals(parser.getName()))
                    {
                        reports.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "inherited", null, parsed))
            {
                reportSet.setInherited(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "configuration", null,
                    parsed))
            {
                reportSet.setConfiguration(build(parser));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return reportSet;
    } //-- ReportSet parseReportSet( XmlPullParser, boolean )

    /**
     * Method parseReporting.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Reporting
     */
    private Reporting parseReporting(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Reporting reporting = new Reporting();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "excludeDefaults", null, parsed))
            {
                reporting.setExcludeDefaults(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "outputDirectory", null,
                    parsed))
            {
                reporting.setOutputDirectory(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "plugins", null, parsed))
            {
                java.util.List plugins = new java.util.ArrayList/*<ReportPlugin>*/();
                reporting.setPlugins(plugins);
                while (parser.nextTag() == START_TAG)
                {
                    if ("plugin".equals(parser.getName()))
                    {
                        plugins.add(parseReportPlugin(parser, strict));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return reporting;
    } //-- Reporting parseReporting( XmlPullParser, boolean )

    /**
     * Method parseRepository.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Repository
     */
    private Repository parseRepository(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Repository repository = new Repository();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "releases", null, parsed))
            {
                repository.setReleases(parseRepositoryPolicy(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "snapshots", null, parsed))
            {
                repository.setSnapshots(parseRepositoryPolicy(parser, strict));
            }
            else if (checkFieldWithDuplicate(parser, "id", null, parsed))
            {
                repository.setId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                repository.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                repository.setUrl(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "layout", null, parsed))
            {
                repository.setLayout(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return repository;
    } //-- Repository parseRepository( XmlPullParser, boolean )

    /**
     * Method parseRepositoryBase.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return RepositoryBase
     */
    private RepositoryBase parseRepositoryBase(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        RepositoryBase repositoryBase = new RepositoryBase();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "id", null, parsed))
            {
                repositoryBase.setId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                repositoryBase.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                repositoryBase.setUrl(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "layout", null, parsed))
            {
                repositoryBase.setLayout(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return repositoryBase;
    } //-- RepositoryBase parseRepositoryBase( XmlPullParser, boolean )

    /**
     * Method parseRepositoryPolicy.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return RepositoryPolicy
     */
    private RepositoryPolicy parseRepositoryPolicy(XmlPullParser parser,
            boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        RepositoryPolicy repositoryPolicy = new RepositoryPolicy();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "enabled", null, parsed))
            {
                repositoryPolicy.setEnabled(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "updatePolicy", null,
                    parsed))
            {
                repositoryPolicy.setUpdatePolicy(getTrimmedValue(parser
                        .nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "checksumPolicy", null,
                    parsed))
            {
                repositoryPolicy.setChecksumPolicy(getTrimmedValue(parser
                        .nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return repositoryPolicy;
    } //-- RepositoryPolicy parseRepositoryPolicy( XmlPullParser, boolean )

    /**
     * Method parseResource.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Resource
     */
    private Resource parseResource(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Resource resource = new Resource();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "targetPath", null, parsed))
            {
                resource.setTargetPath(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "filtering", null, parsed))
            {
                resource.setFiltering(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "directory", null, parsed))
            {
                resource.setDirectory(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "includes", null, parsed))
            {
                java.util.List includes = new java.util.ArrayList/*<String>*/();
                resource.setIncludes(includes);
                while (parser.nextTag() == START_TAG)
                {
                    if ("include".equals(parser.getName()))
                    {
                        includes.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else if (checkFieldWithDuplicate(parser, "excludes", null, parsed))
            {
                java.util.List excludes = new java.util.ArrayList/*<String>*/();
                resource.setExcludes(excludes);
                while (parser.nextTag() == START_TAG)
                {
                    if ("exclude".equals(parser.getName()))
                    {
                        excludes.add(getTrimmedValue(parser.nextText()));
                    }
                    else
                    {
                        checkUnknownElement(parser, strict);
                    }
                }
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return resource;
    } //-- Resource parseResource( XmlPullParser, boolean )

    /**
     * Method parseScm.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Scm
     */
    private Scm parseScm(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Scm scm = new Scm();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "connection", null, parsed))
            {
                scm.setConnection(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "developerConnection", null,
                    parsed))
            {
                scm.setDeveloperConnection(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "tag", null, parsed))
            {
                scm.setTag(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                scm.setUrl(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return scm;
    } //-- Scm parseScm( XmlPullParser, boolean )

    /**
     * Method parseSite.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Site
     */
    private Site parseSite(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        String tagName = parser.getName();
        Site site = new Site();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--)
        {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);

            if (name.indexOf(':') >= 0)
            {
                // just ignore attributes with non-default namespace (for example: xmlns:xsi)
            }
            else
            {
                checkUnknownAttribute(parser, name, tagName, strict);
            }
        }
        java.util.Set parsed = new java.util.HashSet();
        while ((strict
                ? parser.nextTag()
                : nextTag(parser)) == START_TAG)
        {
            if (checkFieldWithDuplicate(parser, "id", null, parsed))
            {
                site.setId(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "name", null, parsed))
            {
                site.setName(getTrimmedValue(parser.nextText()));
            }
            else if (checkFieldWithDuplicate(parser, "url", null, parsed))
            {
                site.setUrl(getTrimmedValue(parser.nextText()));
            }
            else
            {
                checkUnknownElement(parser, strict);
            }
        }
        return site;
    } //-- Site parseSite( XmlPullParser, boolean )

    /**
     * @see ReaderFactory#newXmlReader
     *
     * @param reader
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Model
     */
    public List<Profile> read(Reader reader, boolean strict)
            throws IOException, XmlPullParserException
    {
        XmlPullParser parser = new MXParser();

        parser.setInput(reader);

        initParser(parser);

        return read(parser, strict);
    } //-- Model read( Reader, boolean )

    /**
     * @see ReaderFactory#newXmlReader
     *
     * @param reader
     * @throws IOException
     * @throws XmlPullParserException
     * @return Model
     */
    public List<Profile> read(Reader reader)
            throws IOException, XmlPullParserException
    {
        return read(reader, true);
    } //-- Model read( Reader )

    /**
     * Method read.
     *
     * @param in
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Model
     */
    public List<Profile> read(InputStream in, boolean strict)
            throws IOException, XmlPullParserException
    {
        return read(newXmlReader(in), strict);
    } //-- Model read( InputStream, boolean )

    /**
     * Method read.
     *
     * @param in
     * @throws IOException
     * @throws XmlPullParserException
     * @return Model
     */
    public List<Profile> read(InputStream in)
            throws IOException, XmlPullParserException
    {
        return read(newXmlReader(in));
    } //-- Model read( InputStream )

    /**
     * Method read.
     *
     * @param parser
     * @param strict
     * @throws IOException
     * @throws XmlPullParserException
     * @return Model
     */
    private List<Profile> read(XmlPullParser parser, boolean strict)
            throws IOException, XmlPullParserException
    {
        int eventType = parser.getEventType();
        while (eventType != END_DOCUMENT)
        {
            if (eventType == START_TAG)
            {
                if (strict && !"profiles".equals(parser.getName()))
                {
                    throw new XmlPullParserException(
                            "Expected root element 'profiles' but found '" + parser
                                    .getName() + "'", parser, null);
                }
                List<Profile> model = parseProfiles(parser, strict);
                return model;
            }
            eventType = parser.next();
        }
        throw new XmlPullParserException(
                "Expected root element 'project' but found no element at all: invalid XML document",
                parser, null);
    } //-- Model read( XmlPullParser, boolean )

    /**
     * Sets the state of the "add default entities" flag.
     *
     * @param addDefaultEntities
     */
    public void setAddDefaultEntities(boolean addDefaultEntities)
    {
        this.addDefaultEntities = addDefaultEntities;
    } //-- void setAddDefaultEntities( boolean )

}
