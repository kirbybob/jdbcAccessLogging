package info.bobkirby.valve;

import java.util.Arrays;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ResponseFacade;

import info.bobkirby.valve.JDBCAccessLogFilter;

/**
 * A wrapper for <code>JDBCAccessLogFilter</code> that avoids reflection
 * when a few operationally important values of HTTP queries are exposed.
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
 * An included version of
 * <code>org.apache.catalina.connector.ResponseFacade</code>,
 * with the previously hidden values exposed with added accessors,
 * can be patched into older versions of JBoss and its included Tomcat.
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
 * with a patched version of <code>ResponseFacade</code> in JBoss 5.1GA
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
 * even if used internally or provided to others for commercial purposes.
 * Software is provided as-is with user assumption of all risks.
 **/

public class FacadeJDBCAccessLogFilter extends JDBCAccessLogFilter  {
    @SuppressWarnings("unused") private static final String copyright
        = "Copyright © 2019 Robert L. Kirby";

    /**
     * Wrapper to allow getting values that Tomcat supports
     * but are not part of the older standard interface.
     */
    public class FacadeFilterResponse
        extends JDBCAccessLogFilter.FilterResponse {

        private final ResponseFacade facade;

        protected FacadeFilterResponse (ResponseFacade facade) {
            super(facade);
            this.facade = facade;
        }

        @Override
            protected Long getCount () throws ServletException {

            long l = facade.getContentCount();
            return 0 < l ? Long.valueOf(l) : null;
        }

        @Override
            protected Long getLongLength () throws ServletException {

            long l = facade.getContentLength();
            return 0 < l ? Long.valueOf(l) : null;
        }

        @Override
            protected Integer getIntegerLength () throws ServletException {

            long l = facade.getContentLength();
            return 0 <= l && Integer.MAX_VALUE < l
                ? Integer.valueOf((int) l) : null;
        }

        @Override
            protected Integer getStatusInteger () throws ServletException {

            int i = facade.getStatus();
            return 0 < i ? Integer.valueOf(i) : null;
        }

        @Override
            protected String getStringHeader (String name) {

            return facade.getHeader(name);
        }

        @Override
            protected Collection<String> getStringHeaders (String name) {

            return Arrays.asList(facade.getHeaderValues(name));
        }

        @Override
            protected Collection<String> getStringHeaderNames () {

            return Arrays.asList(facade.getHeaderNames());
        }
    }

    /**
     * Create new FacadeFilterResponse specific to a patched
     * <code>org.apache.catalina.connector.ResponseFacade</code>.
     * Otherwise, uses slower, reflection techniques.
     * @param httpResponse HttpServletResponse,
     * which should be a patched or extended
     * <code>org.apache.catalina.connector.ResponseFacade</code>.
     * specialization of <code>HttpServletResponse</code>.
     */
    @Override public FilterResponse createFilterResponse
        (HttpServletResponse httpResponse) {
    
        if (httpResponse instanceof ResponseFacade
            && usableFilter(httpResponse)) {
    
            if (isDebugEnabled()) {
                debug("FacadeJDBCAccessLogFilter.FacadeFilterResponse " + this
                      + ".createFilterResponse(" + httpResponse + ")");
            }
            return new FacadeFilterResponse((ResponseFacade) httpResponse);
        }
        // Only logs this once.
        if (searchForMethods()) {
            log("FacadeJDBCAccessLogFilter.FacadeFilterResponse " + this
                + ".createFilterResponse(" + httpResponse
                + ") not given org.apache.catalina.connector.ResponseFacade"
                + " to create FacadeFilterResponse. Using reflection.");
        }
        return super.createFilterResponse(httpResponse);
    }

    private boolean usabilityChecked = false;

    private boolean usableFilter (HttpServletResponse response) {
        if (! usabilityChecked) {
            try {
                response.getClass().getMethod("getHeaderValues", String.class);
                usabilityChecked = true;
                if (isDebugEnabled()) {
                    debug("FacadeJDBCAccessLogFilter found getHeaderValues");
                }
            } catch (NoSuchMethodException nsme) {
                throw new UnsupportedOperationException
                    ("org.apache.catalina.connector.ResponseFacade"
                     + " has not been patched.", nsme);
            }
        }
        return true;
    }

    /**
     * Default ResponseFacade Tomcat Filter constructor assumes
     * ResponseFacade has been patched but drops back to default if not.
     */
    public FacadeJDBCAccessLogFilter () {
        super(false, false);
    }

    /**
     * The descriptive information about this implementation.
     */
    private static String info
        = "info.bobkirby.valve.FacadeJDBCAccessLogFilter/2.0";

    /**
     * Return descriptive information about this Filter implementation.
     */
    @Override public String getInfo() {
        return (info);
    }
}
