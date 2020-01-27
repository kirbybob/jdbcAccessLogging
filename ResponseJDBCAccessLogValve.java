package info.bobkirby.valve;

import java.util.Arrays;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

import info.bobkirby.valve.JDBCAccessLogFilter;
import info.bobkirby.valve.JDBCAccessLogValve;

/**
 * An extension of <code>JDBCAccessLogValve</code> that avoids reflection
 * when a few operationally important values of HTTP queries are exposed
 * to the Valves getting <code>org.apache.catalina.connector.Response</code>.
<h2>Development</h2>
 * This Valve was used with PostgreSQL 9.1 UTF8 on Mac OS X 10.6 (Snow Leopard)
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

public class ResponseJDBCAccessLogValve extends JDBCAccessLogValve  {
    @SuppressWarnings("unused") private static final String copyright
        = "Copyright © 2019 Robert L. Kirby";

    /**
     * FilterResponse using Valve implementation specifics,
     * which is created for each HTTP query.
     * This wrapper allows getting values that Tomcat supports
     * but are not part of the older standard interface.
     */
    public class ResponseFilterResponse
        extends JDBCAccessLogValve.ValveFilterResponse {

        private final Request request;
        private final Response response;

        protected ResponseFilterResponse (JDBCAccessLogFilter filter,
                                          Request request,
                                          Response response) {
            super(filter, request, response);
            this.request = request;
            this.response = response;
        }

        @Override
            protected Long getCount () throws ServletException {

            long l = response.getContentCount();
            return 0 < l ? Long.valueOf(l) : null;
        }

        @Override
            protected Long getLongLength () throws ServletException {

            long l = response.getContentLength();
            return 0 < l ? Long.valueOf(l) : null;
        }

        @Override
            protected Integer getIntegerLength () throws ServletException {

            long l = response.getContentLength();
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

        @Override @SuppressWarnings("unchecked")
            protected Collection<String> getStringHeaders (String name) {

            Object obj = response.getHeaderValues(name);
            return null == obj ? null
                : obj.getClass().isArray()
                ? Arrays.asList((String[]) obj) : (Collection<String>) obj;
        }

        @Override @SuppressWarnings("unchecked")
            protected Collection<String> getStringHeaderNames () {

            Object obj = response.getHeaderNames();
            return null == obj ? null
                : obj.getClass().isArray()
                ? Arrays.asList((String[]) obj) : (Collection<String>) obj;
        }

        /**
         * Determine the ServletContext from the Valve implementation.
         */
        @Override protected ServletContext getServletContext () {
            if (null != this.servletContext) {
                return this.servletContext; // cached below
            }
            Context context = response.getContext();
            if (null != context) {
                ServletContext servletContext = context.getServletContext();
                if (null != servletContext) {
                    this.servletContext = servletContext;
                    return servletContext;
                }
            }
            context = request.getContext();
            if (null != context) {
                ServletContext servletContext = context.getServletContext();
                if (null != servletContext) {
                    this.servletContext = servletContext;
                    return servletContext;
                }
            }
            return super.getServletContext();
        }
    }

    /**
     * Create new ResponseFilterResponse specific
     * to a <code>org.apache.catalina.connector.Response</code>.
     * If not, then uses slower, reflection techniques.
     * @param request A Request, which implements HttpServletRequest,
     * may determine the ServletContext from the implementation.
     * @param response A <code>org.apache.catalina.connector.Response</code>
     * which implements <code>javax.servlet.http.HttpServletResponse</code>
     * for parent implementations will wrap.
     */
    @Override public ResponseJDBCAccessLogValve.ResponseFilterResponse
        createFilterResponse (Request request, Response response) {

        if (getFilter().isDebugEnabled()) {
            getFilter().debug
                ("ResponseJDBCAccessLogValve.ResponseFilterResponse " + this
                 + ".createFilterResponse("
                 + request + ", " + response + ")");
        }
        return new ResponseFilterResponse(getFilter(), request, response);
    }

    /**
     * Default Tomcat Response Valve constructor.
     */
    public ResponseJDBCAccessLogValve () {
        this(true, false);
    }

    /**
     * Constructor for Tomcat Response Valve,
     * which extends JDBCAccessLogValve with direct Response handling.
     * @param allowCSVs Flag for handling init-param values,
     * which might accept more than one instance,
     * to parse comma-separated values, which may also have whitespace.
     * @param needMethods Flag to have the first FilterResponse instance
     * use reflection to find unadvertised Methods.
     */
    public ResponseJDBCAccessLogValve
        (boolean allowCSVs, boolean needMethods) {

        super(allowCSVs, needMethods);
    }

    private static String info
        = "info.bobkirby.valve.ResponseJDBCAccessLogValve/2.0";

    /**
     * Return descriptive information about this Filter implementation.
     */
    @Override public String getInfo() {
        return (info);
    }
}
