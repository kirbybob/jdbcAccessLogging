package info.bobkirby.valve;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import info.bobkirby.valve.JDBCAccessLogFilter;

/**
 * A wrapper for <code>JDBCAccessLogFilter</code> that avoids reflection
 * when a few operationally important values of HTTP queries are exposed
 * with later versions
 * of the <code>javax.servlet.http.HttpServletResponse</code> interface.
 * Wraps <code>JDBCAccessLogFilter.FilterResponse</code>.
 * Some early versions of JBoss, which uses a Tomcat implementation,
 * hide potential HTTPServletResponse values:
 * <ul>
 * <li><code>getStatus</code>,
 * which gives the 3-digit integer status code</li>
 * <li><code>getContentCount</code> and <code>getContentCountLong</code>,
 * which are Tomcat specific octet counts taken from
 * <code>org.apache.catalina.connector.OutputBuffer</code>.</li>
 * <li><code>getContentLength</code>,
 * which gives the <code>CONTENT-LENGTH</code> field value,
 * which is returned in the query response header
 * estimating of the eventually expected octet count.</li>
 * <li><code>getHeader</code>,
 * which gives the first value for the named response header.</li>
 * <li><code>getHeaderValues</code>,
 * which gives all of the value for the named response header.
 * This name and its return type, <code>String[]</code>,
 * varies from <code>getHeaders</code> name,
 * which the <code>javax.servlet.http.HttpServletResponse</code> interface
 * exposed for Java EE 7 and later
 * with its return type <code>Collection&lt;String&gt;</code>.</li>
 * <li><code>getStringHeaderNames</code>,
 * which gives all of the header names of the response.
 * Its return type, <code>String[]</code>, varies from
 * the <code>javax.servlet.http.HttpServletResponse</code> interface
 * exposed for Java EE 7 and later
 * with its return type <code>Collection&lt;String&gt;</code>.</li>
 * </ul>
 * <p>
 * In older versions of JBoss,
 * Tomcat Valves, which use the Tomcat implementation
 * <code>org.apache.catalina.connector.Response</code>,
 * also expose the accessors without the need for patching.
 * A included replacement, extended version of
 * <code>org.apache.catalina.valves.JDBCAccessLogValve</code>,
 * calls this extended Filter.
 * <p>
 * Later versions of JBoss, now called Wildfly,
 * which no longer rely on Tomcat,
 * may also expose the accessors by default.
<h2>Development</h2>
 * This Filter was used with PostgreSQL 9.1 UTF8 on Mac OS X 10.6 (Snow Leopard)
 * in JBoss 5.1GA
 * and the Apple distribution of Oracle Java 1.6 originally.
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
 * even if used iternally or provided to others for commercial purposes.
 * Software is provided as-is with user assumption of all risks.
 **/

public class EE7JDBCAccessLogFilter extends JDBCAccessLogFilter  {
    @SuppressWarnings("unused") private static final String copyright
        = "Copyright © 2019 Robert L. Kirby";

    /**
     * Wrapper to allow getting values that Tomcat supports
     * but are not part of the older standard interface.
     */
    public class EE7FilterResponse
        extends JDBCAccessLogFilter.FilterResponse {

        private final HttpServletResponse response;

        protected EE7FilterResponse (HttpServletResponse response) {
            super(response);
            this.response = response;
        }

        @Override
            protected Long getLongLength () throws ServletException {

            String len = response.getHeader("Content-Length");
            if (null == len) {
                return super.getLongLength();
            }
            len = len.trim();
            if (0 == len.length()) {
                return super.getLongLength();
            }
            final long l;
            try {
                l = Long.parseLong(len);
            } catch (NumberFormatException nfe) {
                StringBuilder sb = logStringBuilder
                    ("Content-Length header=",
                     "EE7FilterResponse.getLongLength()");
                sb.append(len);
                throw new ServletException(sb.toString(), nfe);
            }
            return 0L < l ? Long.valueOf(l) : null;
        }

        @Override
            protected Integer getIntegerLength () throws ServletException {

            String len = response.getHeader("Content-Length");
            if (null == len) {
                return super.getIntegerLength();
            }
            len = len.trim();
            if (0 == len.length()) {
                return super.getIntegerLength();
            }
            final long l;
            try {
                l = Long.parseLong(len);
            } catch (NumberFormatException nfe) {
                StringBuilder sb = logStringBuilder
                    ("Content-Length header=",
                     "EE7FilterResponse.getIntegerLength()");
                sb.append(len);
                throw new ServletException(sb.toString(), nfe);
            }
            return 0 < l && Integer.MAX_VALUE < l
                ? Integer.valueOf((int) l) : null;
        }

        @Override
            protected Integer getStatusInteger () throws ServletException {

            int i = response.getStatus();
            return 0 < i ? Integer.valueOf(i) : null;
        }

        @Override
            protected String getStringHeader (String name) {

            return response.getHeader(name);
        }

        @Override
            protected Collection<String> getStringHeaders (String name) {

            return response.getHeaders(name);
        }

        @Override
            protected Collection<String> getStringHeaderNames () {

            return response.getHeaderNames();
        }

        @Override protected Array createArray (Connection connection,
                                               String stringElementSqlType,
                                               Collection<String> collection)
            throws SQLException {

            if (isDebugEnabled()) {
                debug("EE7FilterResponse.createArray stringElementSqlType="
                + stringElementSqlType + " Connection=" + connection);
            }
            return connection.createArrayOf(stringElementSqlType,
                                            collection.toArray());
        }
    }

    /**
     * Create new EE7FilterResponse specific to the Java EE 7 interface.
     * Falls back to slower, reflection techniques.
     * @param httpResponse Java EE 7 HttpServletResponse.
     */
    @Override public FilterResponse createFilterResponse
        (HttpServletResponse httpResponse) {
    
        if (isDebugEnabled()) {
            debug("EE7JDBCAccessLogFilter.EE7FilterResponse " + this
                  + ".createFilterResponse(" + httpResponse + ")");
        }
        return new EE7FilterResponse((HttpServletResponse) httpResponse);
    }

    /**
     * Default Filter constructor uses Java EE 7 interface and does not
     * accept comma separated values except for column assignments
     * and throwableattribute names.
     */
    public EE7JDBCAccessLogFilter () {
        this(false, false);
    }

    /**
     * Constructor for overriding Filters and Valves.
     * @param allowCSVs Flag for handling init-param values,
     * which might accept more than one instance,
     * to parse comma-separated values, which may also have whitespace.
     * @param needMethods Flag to have the first FilterResponse instance
     * use reflection to find unadvertised Methods.
     */
    public EE7JDBCAccessLogFilter (boolean allowCSVs, boolean needMethods) {
        super(allowCSVs, needMethods);
    }

    /**
     * The descriptive information about this implementation.
     */
    private static String info
        = "info.bobkirby.valve.EE7JDBCAccessLogFilter/2.0";

    /**
     * Return descriptive information about this Filter implementation.
     */
    @Override public String getInfo() {
        return (info);
    }
}
