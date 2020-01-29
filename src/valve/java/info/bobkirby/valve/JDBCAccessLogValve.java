package info.bobkirby.valve;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.valves.ValveBase;
import org.apache.catalina.connector.Constants;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.jboss.logging.Logger;

import info.bobkirby.valve.JDBCAccessLogFilter;

/**
 * This Tomcat Valve replaces an older
 * <code>org.apache.catalina.valves.JDBCAccessLogValve</code>,
 * allowing most of the simple values associated with processing an HTTP query
 * to be logged to a relational database management systems (RDMS) as selected.
 * <p>
 * Unlike the older <code>JDBCAccessLogValve</code>,
 * which only provisioned a single JDBC connection,
 * this version accepts <code>DataSource</code>s.
 * Classloading must make a <code>DataSource</code> available to the valve,
 * which typically may be configured with the <code>DataSource</code>
 * configuration file (*-DS.xml) being placed in the top level
 * of the <code>deploy</code> directory.
 * <p>
 * Typically Valve configuration uses XML attributes of the &lt;Valve&gt; tag
 * nested within &lt;Host&gt;, &lt;Engine&gt;, &lt;Service&gt;
 * and &lt;Server&gt; tags of a <code>server.xml</code> file
 * of a <code>jbossweb.sar</code> of a <code>deploy</code> subdirectory
 * of earlier JBoss implementations.
 * Since XML attributes may only appear at most once in each tag
 * and must have Java Bean names that the valve implements
 * with getters and setters,
 * while Filter configuration comes mostly through &lt;init-param&gt; pairs,
 * multiple values may be comma-separated for each attribute name.
 * <code>JDBCAccessLogFilter</code> documentation lists the attribute names.
 * <p>
 * Unlike typical camelCase names,
 * the XML attribute names for potential database columns (fields),
 * only capitalize their first letter and no interior letters.
 * This allows consistency with the all lowercase names that the filter uses
 * to match the conventions of database field (column) names.
 * Convenience functions with camelCase names are available
 * for detail parameters that do not specify database columns names.
 * <p>
 * For multiple, comma-separated values of an attribute,
 * values after the initial (zeroth) value may be processed with
 * attribute names suffixed with integers starting at one (1).
 * For example, a <code>header</code> attribute with
 * value <code>"Origin,&nbsp;Via"</code>
 * would create two processing requests "header" and header1",
 * which would process input HTTP header values
 * for "Origin"&nbsp;and&nbsp;"Via", respectively,
 * which then might be assigned to database fields "origin" and "via"
 * through comma-separated "columns" attribute assignment values,
 * <code>'origin=header,&nbsp;via=header1'</code>.
 * <p>
 * Nearly default Valve configuration tag attributes could be:<br><code>
&lt;Valve
className="valve.JDBCAccessLogValve"<br>
datasource="PostgresDS"<br>
tablename="Access"<br>
columns='"timestamp"=timestampkey,
remotehost(15)=remoteaddr,
query(2048)=requesturi,
"method"(8),
status,
username(16)=remoteuser,
bytes=contentcountlengthinteger,
virtualhost(64)=servername,
serverport,
referer(1028)=referertext,
useragent(512)=useragenttext,
rootcausetrace'
/&gt;</code>
<p>
* Single quotes (') are typically needed around
* the entire "columns" attribute value,
* which encompasses double quotes (&quot;) around column names
* that could conflict with SQL keywords.
* Although SQL uses single quotes (') for literal values,
* literal values should not be quoted in attributes other than for "columns".
<p>
The above default columns value would work with the following PostgreSQL DDL:
 * <pre>
CREATE TABLE access (
  "timestamp" timestamp with time zone NOT NULL DEFAULT now(),
  remotehost character(15) NOT NULL,
  query text NOT NULL,
  "method" character varying(8) NOT NULL,
  status smallint NOT NULL DEFAULT -1,
  username character(16),
  bytes integer NOT NULL DEFAULT -1,
  virtualhost character varying(64) NOT NULL,
  serverport integer,
  referer text,
  useragent text,
  rootcausetrace text[],
  CONSTRAINT access_timestamp_pkey PRIMARY KEY ("timestamp"),
  CONSTRAINT access_query_index UNIQUE (query, "timestamp" ),
  CONSTRAINT access_remotehost_index UNIQUE (remotehost, "timestamp" ),
  CONSTRAINT access_useragent_index UNIQUE (useragent, "timestamp")
) WITH (OIDS=FALSE);
 * </pre>
<h2>Development</h2>
 * This Valve was used with PostgreSQL 9.1 UTF8 on Mac OS X 10.6 (Snow Leopard)
 * with both a patched and unpatched JBoss 5.1GA
 * and the Apple distribution of Oracle Java 1.6 originally.
<p>
 * JDBCAccessLogValve version 1.1,
 * which Andre de Jesus and Peter Rossbach authored,
 * inspired development but only their examples and Lifecycle were extended.
<p>
 * @author Robert (Bob) L. Kirby
 * <a href="http://bobkirby.info/">http://bobkirby.info/</a><br>
 * kirby dot bob separated with an "at" sign from gmail dot com, avoiding spam.
 * @copyright Copyright &copy; 2019 Robert L. Kirby. All rights reserved.
 * <p>
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0
 * (<a href="http://opensource.org/licenses/eclipse-1.0.php"
 *  >http://opensource.org/licenses/eclipse-1.0.php</a>)
 * which can be found in the file epl-v10.html.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 * <p>
 * The intent of the use of this license is to keep this software
 * as open source with the author identity intact,
 * without contaminating other software with the license of this software,
 * even if used internally or provided to others for commercial purposes.
 * Software is provided as-is with user assumption of all risks.
 **/

public class JDBCAccessLogValve extends ValveBase implements Lifecycle  {
    @SuppressWarnings("unused") private static final String copyright
        = "Copyright © 2019 Robert L. Kirby";

    /**
     * Wrapper to allow getting values that Tomcat supports
     * but are not part of the older standard interface.
     */
    public class ValveFilterResponse
        extends JDBCAccessLogFilter.FilterResponse {

        private final Request request;

        /**
         * FilterResponse that determines the ServletContext without using
         * Valve implementation specifics, which may change.
         */
        protected ValveFilterResponse (JDBCAccessLogFilter filter,
                                       Request request,
                                       Response response) {
            filter.super(response);
            this.request = request;
        }

        /**
         * Determine the ServletContext without using
         * Valve implementation specifics, which may change.
         */
        @Override protected ServletContext getServletContext () {
            if (null != this.servletContext) {
                return this.servletContext; // cached below
            }
            HttpSession session = request.getSession(false);
            if (null != session) {
                ServletContext servletContext = session.getServletContext();
                if (null != servletContext) {
                    this.servletContext = servletContext;
                    return servletContext;
                }
            }
            return super.getServletContext();
        }
    }

    /**
     * Create new FilterResponse in a way that may be overridden.
     * The static needMethods should be true if reflection is expected.
     * If the Valve specifies a no more specialized HttpServletResponse,
     * the Filter wraps one instead.
     * @param request A Request, which implements HttpServletRequest,
     * may determine the ServletContext from the interface
     * without relying on the implementation, which might change.
     * @param response A Response,
     * which implements <code>javax.servlet.http.HttpServletResponse</code>
     * for parent implementations will wrap.
     */
    public JDBCAccessLogValve.ValveFilterResponse createFilterResponse
        (Request request, Response response) {

        if (filter.isDebugEnabled()) {
            filter.debug("JDBCAccessLogValve.ValveFilterResponse " + this
                         + ".createFilterResponse("
                         + request + ", " + response + ")");
        }
        return new ValveFilterResponse(filter, request, response);
    }

    private class ValveFilterConfig implements FilterConfig {

        public String getFilterName () {
            return JDBCAccessLogValve.class.getName();
        }

        public ServletContext getServletContext () {
            return null;
        }

        public String getInitParameter (String name) {
            return paramMap.get(name);
        }

        public Enumeration<String> getInitParameterNames () {
            return paramMap.keys();
        }
    }

    /**
     * The descriptive information about this implementation.
     */
    private static String info = "info.bobkirby.valve.JDBCAccessLogValve/2.0";

    /**
     * The lifecycle event support for this component.
     */
    private LifecycleSupport lifecycle = new LifecycleSupport(this);

    /**
     * The string manager for this package.
     */
    private StringManager sm = StringManager.getManager(Constants.Package);

    /**
     * Has this component been started yet?
     */
    private boolean started = false;

    private JDBCAccessLogFilter filter = null;
    private ValveFilterConfig filterConfig = new ValveFilterConfig();
    private final Hashtable<String, String> paramMap
        = new Hashtable<String, String>();

    protected JDBCAccessLogFilter getFilter () {
        return filter;
    }

    private String getParam (String name) {
        return paramMap.get(name);
    }

    private void setParam (String name, String value) {
        if (null == value) {
            paramMap.remove(name);
        } else {
            paramMap.put(name, value);
        }
    }

    public String getDataSource () {
        return getParam("datasource");
    }

    public void setDatasource (String value) {
        setParam("datasource", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setDataSource (String value) {
        setParam("datasource", value);
    }

    public String getConnectionURL () {
        return getParam("connectionurl");
    }

    public void setConnectionurl (String value) {
        setParam("connectionurl", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setConnectionURL (String value) {
        setParam("connectionurl", value);
    }

    public String getUserName () {
        return getParam("username");
    }

    public void setUsername (String value) {
        setParam("username", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setUserName (String value) {
        setParam("username", value);
    }

    public String getPassword () {
        return getParam("password");
    }

    public void setPassword (String value) {
        setParam("password", value);
    }

    public String getDriverClass () {
        return getParam("driverclass");
    }

    public void setDriverclass (String value) {
        setParam("driverclass", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setDriverClass (String value) {
        setParam("driverclass", value);
    }

    public String getArrayElementNull () {
        String v = getParam("arrayelementnull");
        return null != v ? v : JDBCAccessLogFilter.ARRAY_ELEMENT_NULL_DEFAULT;
    }

    public void setArrayelementnull (String value) {
        setParam("arrayelementnull", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setArrayElementNull (String value) {
        setParam("arrayelementnull", value);
    }

    public String getArrayBeginDelimiter () {
        String v = getParam("arraybegindelimiter");
        return null != v ? v : JDBCAccessLogFilter.ARRAY_BEGIN_DEFAULT;
    }

    public void setArraybegindelimiter (String value) {
        setParam("arraybegindelimiter", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setArrayBeginDelimiter (String value) {
        setParam("arraybegindelimiter", value);
    }

    public String getArrayEndDelimiter () {
        String v = getParam("arrayenddelimiter");
        return null != v ? v : JDBCAccessLogFilter.ARRAY_END_DEFAULT;
    }

    public void setArrayenddelimiter (String value) {
        setParam("arrayenddelimiter", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setArrayEndDelimiter (String value) {
        setParam("arrayenddelimiter", value);
    }

    public String getDebug () {
        String v = getParam("debug");
        return null != v ? v : "false";
    }

    /**
     * Set up Debugging as an initialization parameter.
     * By default, debugging is off.
     * @param value If equivalent to false, then turns off debugging;
     * otherwise, turns on debugging.
     */
    public void setDebug (String value) {
        setParam("debug", value);
    }

    public String getStringElementSqlType () {
        String v = getParam("stringelementsqltype");
        return null != v ? v
            : JDBCAccessLogFilter.STRING_ELEMENT_SQL_TYPE_DEFAULT;
    }

    public void setStringelementsqltype (String value) {
        setParam("stringelementsqltype", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setStringElementSqlType (String value) {
        setParam("stringelementsqltype", value);
    }

    public String getEllipsisString () {
        String v = getParam("ellipsisstring");
        return null != v ? v : JDBCAccessLogFilter.ELLIPSIS_STRING_DEFAULT;
    }

    public void setEllipsisstring (String value) {
        setParam("ellipsisstring", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setEllipsisString (String value) {
        setParam("ellipsisstring", value);
    }

    public String getLineSeparator () {
        String v = getParam("lineseparator");
        return null != v ? v : System.getProperty("line.separator");
    }

    public void setLineseparator (String value) {
        setParam("lineseparator", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setLineSeparator (String value) {
        setParam("lineseparator", value);
    }

    public String getTablename () {
        return getParam("tablename");
    }

    public void setTablename (String value) {
        setParam("tablename", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setTableName (String value) {
        setParam("tablename", value);
    }

    public String getThrowableAttribute () {
        return getParam("throwableattribute");
    }

    public void setThrowableattribute (String value) {
        setParam("throwableattribute", value);
    }

    /**
     * Potentially comma separated String of HttpServletRequest attribute names,
     * which may have a Throwable as a value.
     * Typically, such attributes would be set on HttpServlet error pages.
     * @param value HttpServletRequest attribute name
     * that may have a Throwable as a value.
     */
    public void setThrowableAttribute (String value) {
        setParam("throwableattribute", value);
    }

    public String getUniqueViolationSqlState () {
        String v = getParam("uniqueviolationsqlstate");
        return null != v ? v
            : JDBCAccessLogFilter.UNIQUE_VIOLATION_SQLSTATE_DEFAULT;
    }

    public void setUniqueviolationsqlstate (String value) {
        setParam("uniqueviolationsqlstate", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setUniqueViolationSqlState (String value) {
        setParam("uniqueviolationsqlstate", value);
    }

    public String getUniqueViolationRetries () {
        String v = getParam("uniqueviolationretries");
        return null != v ? v : Byte.toString
            (JDBCAccessLogFilter.UNIQUE_VIOLATION_RETRIES_DEFAULT);
    }

    public void setUniqueviolationretries (String value) {
        setParam("uniqueviolationretries", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setUniqueViolationRetries (String value) {
        setParam("uniqueviolationretries", value);
    }

    public String getInternalstringarray () {
        String v = getParam("internalstringarray");
        return null != v ? v : "false";
    }

    public void setInternalstringarray (String value) {
        setParam("internalstringarray", value);
    }

    /**
     * Convenience camelCase function name maps to lowercase processing.
     * @param value Value to set to header name in Filter implementation.
     */
    public void setInternalStringArray (String value) {
        setParam("internalstringarray", value);
    }

    // Parameterized SQL column producers
    public String getAttributestring () {
        return getParam("attributestring");
    }

    public String getAttributearraystring () {
        return getParam("attributearraystring");
    }

    public void setAttributearraystring (String value) {
        setParam("attributearraystring", value);
    }

    public String getAttributearraystringclob () {
        return getParam("attributearraystringclob");
    }

    public void setAttributearraystringclob (String value) {
        setParam("attributearraystringclob", value);
    }

    public void setAttributestring (String value) {
        setParam("attributestring", value);
    }

    public String getAttributestringclob () {
        return getParam("attributestringclob");
    }

    public void setAttributestringclob (String value) {
        setParam("attributestringclob", value);
    }

    public String getAttributetext () {
        return getParam("attributetext");
    }

    public void setAttributetext (String value) {
        setParam("attributetext", value);
    }

    public String getAttributetextclob () {
        return getParam("attributetextclob");
    }

    public void setAttributetextclob (String value) {
        setParam("attributetextclob", value);
    }

    public String getDateheader () {
        return getParam("dateheader");
    }

    public void setDateheader (String value) {
        setParam("dateheader", value);
    }

    public String getHeader () {
        return getParam("header");
    }

    public void setHeader (String value) {
        setParam("header", value);
    }

    public String getHeaders () {
        return getParam("headers");
    }

    public void setHeaders (String value) {
        setParam("headers", value);
    }

    public String getHeadersclob () {
        return getParam("headersclob");
    }

    public void setHeadersclob (String value) {
        setParam("headersclob", value);
    }

    public String getInitparameter () {
        return getParam("initparameter");
    }

    public void setInitparameter (String value) {
        setParam("initparameter", value);
    }

    public String getInitparametertext () {
        return getParam("initparametertext");
    }

    public void setInitparametertext (String value) {
        setParam("initparametertext", value);
    }

    public String getLiteral () {
        return getParam("literal");
    }

    public void setLiteral (String value) {
        setParam("literal", value);
    }

    public String getLiteralboolean () {
        return getParam("literalboolean");
    }

    public void setLiteralboolean (String value) {
        setParam("literalboolean", value);
    }

    public String getLiteralshort () {
        return getParam("literalshort");
    }

    public void setLiteralshort (String value) {
        setParam("literalshort", value);
    }

    public String getLiteralinteger () {
        return getParam("literalinteger");
    }

    public void setLiteralinteger (String value) {
        setParam("literalinteger", value);
    }

    public String getLiterallong () {
        return getParam("literallong");
    }

    public void setLiterallong (String value) {
        setParam("literallong", value);
    }

    public String getResponseheader () {
        return getParam("responseheader");
    }

    public void setResponseheader (String value) {
        setParam("responseheader", value);
    }

    public String getResponseheaders () {
        return getParam("responseheaders");
    }

    public void setResponseheaders (String value) {
        setParam("responseheaders", value);
    }

    public String getResponseheadersclob () {
        return getParam("responseheadersclob");
    }

    public void setResponseheadersclob (String value) {
        setParam("responseheadersclob", value);
    }

    public String getServletattributestring () {
        return getParam("servletattributestring");
    }

    public void setServletattributestring (String value) {
        setParam("servletattributestring", value);
    }

    public String getSessionattributestring () {
        return getParam("sessionattributestring");
    }

    public void setSessionattributestring (String value) {
        setParam("sessionattributestring", value);
    }

    public String getUserinrole () {
        return getParam("userinrole");
    }

    public void setUserinrole (String value) {
        setParam("userinrole", value);
    }

    // Column assignments from access values
    public String getColumns () {
        return getParam("columns");
    }

    public void setColumns (String value) {
        setParam("columns", value);
    }

    /**
     * Default Valve constructor creates a default
     * <code>JDBCAccessLogFilter</code>,
     * which supports comma-separated value (CSV) header values
     * and uses reflection to identify needed methods.
     * Extensions of the defaults may provide more efficient access.
     */
    public JDBCAccessLogValve () {
        this(true, true);
    }

    /**
     * Constructor for Valves where Filter parameters might be overridden.
     * @param allowCSVs Flag for handling init-param values,
     * which might accept more than one instance,
     * to parse comma-separated values, which may also have whitespace.
     * @param needMethods Flag to have the first FilterResponse instance
     * use reflection to find unadvertised Methods.
     */
    public JDBCAccessLogValve (boolean allowCSVs, boolean needMethods) {
        this(new JDBCAccessLogFilter(allowCSVs, needMethods));
    }

    /**
     * Constructor for Valves that gets a JDBCAccessLogFilter.
     * @param filter JDBCAccessLogFilter, whose process() methods
     * handles the processing of each invocation.
     */
    public JDBCAccessLogValve (JDBCAccessLogFilter filter) {
        this.filter = filter;
        final Logger log = Logger.getLogger(this.getClass());
        filter.setWarner(new JDBCAccessLogFilter.Warner() {
                public void warn (CharSequence message) {
                    log.warn(message);
                }
                public void warn (CharSequence message, Throwable throwable) {
                    log.warn(message, throwable);
                }
                public void debug (CharSequence message) {
                    log.warn(message);
                }
            });
    }

    /**
     * Invoked by Tomcat on startup. The database connection is set here.
     * 
     * @exception LifecycleException Can be thrown on lifecycle 
     * inconsistencies or on database errors (as a wrapped SQLException).
     */
    public void start() throws LifecycleException {
        // init (FilterConfig filterConfig) throws ServletException

        if (started)
            throw new LifecycleException
                (sm.getString("accessLogValve.alreadyStarted"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);

        try {
            filter.init(filterConfig);
        } catch (ServletException se) {
            throw new LifecycleException(se);
        }
        started = true;
    }

    /**
     * Invoked by Tomcat on shutdown. The database connection is closed here.
     * 
     * @exception LifecycleException Can be thrown on lifecycle 
     * inconsistencies or on database errors (as a wrapped SQLException).
     */
    public void stop () throws LifecycleException {

        if (!started)
            throw new LifecycleException
                (sm.getString("accessLogValve.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        filter.destroy();
    }

    /**
     * This Valve is invoked by Tomcat on each HTTP query.
     * 
     * @param request The catalina implementation Request object.
     * @param servletResponse The catalina implementation Response object.
     *
     * @exception IOException Could be thrown be next stacked Valve.
     * @exception ServletException Database SQLException is wrapped 
     * in a ServletException. Could be thrown be next stacked Valve too.
     * @exception NullPointerException If either parameter is null.
     */
    public void invoke (Request request, Response servletResponse) 
        throws IOException, ServletException {

        JDBCAccessLogFilter.FilterResponse response
            = createFilterResponse(request, servletResponse);

        // pass the request along the filter chain
        // using ValveBase.getNext()
        try {
            getNext().invoke(request, servletResponse);
        } catch (IOException e) {
            throw e;
        } catch (ServletException e) {
            throw e;
        } catch (RuntimeException e) {
            response.setThrowable(e);
            throw e;
        } catch (Error e) {
            response.setThrowable(e);
            throw e;
        } finally {
            try {
                filter.process(request, response);
            } catch (SQLException e) {
                System.out.print("Catching SQLException");
                // After the first SQLException prints its stack,
                // then just print the SQLException to avoid
                // overwhelming the log if access logging is unavailable.
                if (filter.isStackPrinted()) {
                    StringBuilder sb = filter.logStringBuilder
                        (null, this.getClass().getSimpleName() + ".invoke()");
                    response.log(sb);
                } else {
                    filter.setStackPrinted(true);
                    StringBuilder sb = filter.logStringBuilder
                        (null, this.getClass().getSimpleName() + ".invoke()");
                    response.log(sb, e);
                }
            }
        }
    }

    /**
     * Return descriptive information about this Valve implementation.
     */
    @Override public String getInfo() {
        return (info);
    }

    /**
     * Adds a Lifecycle listener.
     * 
     * @param listener The listener to add.
     */  
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * Get the lifecycle listeners associated with this lifecycle. If this 
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    /**
     * Removes a Lifecycle listener.
     * 
     * @param listener The listener to remove.
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }
}
