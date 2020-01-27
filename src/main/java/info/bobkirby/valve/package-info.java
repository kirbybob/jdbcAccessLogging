/*
 * Use Javadoc command line option
 * -doctitle "JDBC Access Log Filter and Valve files" first
 * and then "-tag copyright" to display copyright.
 */
/**
 * Highly configurable Filter and Valves log access server database rows
 * for each HTTP access.
 * <code>JDBCAccessLogFilter</code> explains in detail.
 * <p>
 * Depending on which libraries and patches are available,
 * some source files may not compile,
 * which expected when supporting multiple versions.<ul>
 * <li><code>FacadeJDBCAccessLogFilter</code> may only work as a Filter
 * with older versions of JBoss or its Tomcat,
 * where <code>org.apache.catalina.connector.ResponseFacade</code>
 * did not expose several useful values and had <code>Response</code> names
 * for those values that are different from those used in
 * Java EE 7 and later.</li>
 * <li><code>ResponseJDBCAccessLogValve</code> may only work as a Valve
 * with older versions of JBoss or its Tomcat,
 * where <code>org.apache.catalina.connector.ResponseFacade</code>
 * did not expose several useful values and had <code>Response</code> names
 * for those values that are different from those used in
 * Java EE 7 and later.</li>
 * <li><code>EE7JDBCAccessLogFilter</code> may only work with environments
 * that support Java EE 7 or later due to
 * <code>javax.servlet.http.*</code> interface name changes.</li>
 * </ul>
 * <p>
 * Use Java 1.5 (Java 5) or later to compile since the code has generics.
 * Communicate with a SQL3 or higher
 * relational management database system (RDBMS)
 * for many features.
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
 */
package info.bobkirby.valve;
