package info.bobkirby.valve;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.Principal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Binding;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import javax.sql.rowset.serial.SerialClob;

/**
 * This highly configurable Filter produces database rows for each HTTP access.
 * A <code>javax.servlet.Filter</code> deployment
 * is typically configured in <code>WEB-INF/web.xml</code>,
 * with XML tags and values within a high-level &lt;filter&gt; tag pair.
 * Within this pair, top-level configuration includes tag pairs:<ul>
<li>Full filter class name &lt;filter-class&gt;
for this <code>javax.servlet.Filter</code> implementation class.</li>
<li>Filter name &lt;filter-name&gt; of the filter, such as the container name,
which defaults to class simple name of this filter implementation.</li>
<li>Optional description &lt;description&gt; for the entire filter</li>
<li>Multiple initialization parameter &lt;init-param&gt; tag pairs,
which may each contain tag pairs:<ul>
<li>The name of an initialization parameter &lt;param-name&gt;</li>
<li>The value of the named initialization parameter &lt;param-value&gt;</li>
<li>An optional description of the initialization parameter
&lt;description&gt;</li>
</ul>
The contents of the name-value pairs are passed in a
<code>javax.servlet.FilterConfig</code> implementation
<code>java.util.Enumeration</code> described below.</li>
</ul>
<p>
<code>WEB-INF/web.xml</code> configuration should also contain high-level
<code>filter-mapping</code> tags, which includes tags:<ul>
<li><code>filter-name</code>,
whose value should match the <code>filter-name</code>
value within the <code>filter</code> tags.</li>
<li><code>url-pattern</code>,
which specifies URLs to which the filter should be applied,
such as <code>/*</code> to match everything.</li>
</ul>
<h2>Initialization parameter processing</h2>
 * The <code>FilterConfig</code> implementation,
 * which processes &lt;init-param&gt; name-value pairs
 * only accepts one instance of each initialization parameter
 * &lt;init-param&gt; name within its namespace.
 * This Filter implementation skips
 * hyphen (-), underscore (_), double quote (&quot;), and decimal digit (0-9)
 * characters when recognizing initialization configuration names,
 * which are identified in lower case,
 * but underscores and digits are recognized to distinguish column names,
 * which may be derived from a parameterized configuration name.
 * Unacceptable initialization parameter names produce error log entries.
 * Omitted or empty initialization parameter values are mostly ignored.
<h2>Initialization parameter configuration</h2>
 * There are several categories of configuration:<ul>
<li>JDBC compatible database connection parameters</li>
<li>Column detail parameters</li>
<li>Names of arguments to HTTP access functions</li>
<li>Assignment of database column names</li>
</ul>
<h3>Connection parameters</h3>
 * If both DataSource &lt;datasource&gt;
 * and JDBC connection values are specified
 * and JNDI returns a DataSource that provides a connection,
 * then the JDBC connection parameters are ignored.
 * The DataSource JNDI binding name might follow
 * the older convention like "jdbc/PostgresDS" or
 * use a shorter name like "PostgresDS" in configuration files such as
 * "server.xml", "context.xml", or "standalone.xml" establish the name.
 * The long form JNDI lookup name could be "java:comp/env/jdbc/PostgresDS".
 * For the older binding convention,
 * an <code>InitialContext.lookup(String)</code> may find
 * shorter names like "jdbc/PostgresDS"
 * or the global name "java:/jdbc/PostgresDS".
 * <p>
 * The container may provide the properties needed to establish
 * an InitialContext, but if not, then a "jndi.properties" file
 * may need to be available as a root file on the classpath,
 * possibly in the same JAR or exploded directory
 * as the class file of this Filter.
 * <p>
 * Later versions of Tomcat have a &lt;GlobalNamingResources&gt; tag
 * of the &lt;Server&gt; tag in the "services.xml" file,
 * which may specify a DataSource.
 * <p>
 * The JDBC connection parameter names are:<ul>
<li>JDBC connection URL &lt;connectionurl&gt;,
like <code>jdbc:postgresql://localhost:5432/dbname</code>,
which specifies the <code>jdbc</code> URL schema;
a PostgreSQL JDBC implementation;
a host name, <code>localhost</code>, which could be either a DNS or IP address;
an optional port number 5432, which is the default PostgreSQL port; and
<code>dbname</code>, which names a database on the PostgreSQL server.
<p>
For some databases, the connection URL may be quite long,
wrapping around its single line.
Although some databases may accept a user name and password
in the connection URL, others will only accept the user name and password
when provided as separate configuration items.
</li>
<li>Fully qualified JDBC driver class name string &lt;driver-class&gt;,
like <code>org.postgresql.Driver</code></li>
<li>Database user name string &lt;user-name&gt;, like <code>user</code></li>
<li>Database password string &lt;password&gt;, like <code>password</code></li>
</ul>
<h3>Standard HTTP access</h3>
Most of the documented access methods for
HTTP requests, responses, sessions, and server contexts
have actions to obtain a container value and
write a transformed value via JDBC to a relational database column.
Each access method has its "get" or "is" prefix stripped,
along with occasionally extraneous parts of the name,
and the remaining camel case string is converted to lower case,
which creates typical names for database columns.
Some names are suffixed with type names that show how values will be processed.
<ul>
<li><b>Boolean</b>: A mostly two (2) state value, true or false,
with the rare possiblity of a third state, unknown,
which could occur with named attribute access.</li>
<li><b>Short</b>: A 16-bit signed integer.</li>
<li><b>Integer</b>: A 32-bit signed integer.</li>
<li><b>Long</b>: A 64-bit signed integer.</li>
<li><b>Timestamp</b>: a 64-bit Java Timestamp with milliseconds,
which may not match the precision of the host machine
and which some databases round, perhaps to as coarse as a second.</li>
<li><b>String</b>: Characters without a terminator,
which some database fields, like CHAR without VAR, pad with blanks on the right.
A size assignment can truncate at the end for a positive size
and at the begin for a negative size 
when the absolute size is less than the number of characters.</li>
<li><b>Text</b>: Character handling like String except that
if a size assignment truncates characters,
an ellipsis string is added the end or beginning of the truncated characters
for positive or negative size, respectively.
Database field length declarations should consider ellipsis characters.</li>
<li><b>Clob</b>: an extension of String and Text that transports characters
as a CLOB, character large object,
which some data implementations required for even moderate lengths.</li>
<li><b>Array</b>: An indexed group of values of the same type.
Currently, the toString() converts values the strings
and wraps those strings in array literal syntax.
The protected method
<code>FilterResponse.stringArrayElement(String, StringBuffer)</code>,
which implements quoting for early PostgreSQL String array elements,
may be overridden for the quoting conventions of the few databases
that support arrays.
Where arrays are not sufficiently supported either CLOB or JSON fields
may partially stand in for arrays.
<p>
The PostgreSQL default of curly braces ({}) for outer array delimiters
may be changed in filter configuration to another character pair,
such as square brackets ([]), which might go to a JSON field,
which some relational databases support instead of native arrays.
Some databases may automatically convert the entire literal string
into a native value of a field declaration.</li>
<li><b>Key</b>: Suitable values for part of a unique key,
such as a primary key of a database table.
If a write fails with unique value collision,
the key may be advanced to retry the write for a configurable number of retries.
The SQL state string for a unique value collision, 23505,
may be replace with filter configuration.
Key fields may avoid using synthetic values,
such as a serialized number field,
for primary keys, particularly when derived from machine clocks,
which may be less accurate than their precision.</li>
</ul>
Accessed values that are unavailable are not sent as null
except for array and Collection members.
Instead, JDBC sends no value for the column,
which the relational database should fill with its default value.
<p>
Additionally, some accessed values have numerical defaults, like 0 or -1,
when an object such as a response buffer or <code>HttpSession</code>
are unavailable.
Such default accessed values are converted into applying database defaults,
typically <code>null</code>, to database fields.
If the original access value defaults were wanted,
the database field defaults could establish them.
Request parameter values,
which silently return <code>null</code> when a parsing error occurs
and return -1 unchanged even thought it may represent an unknown value,
are an exception.
<p>
The last types of a name indicate what is sent to the relational database.
Implemented access names are all lower case alphas (a-z),
which, if unquoted, the relational database may convert to its preferred case.
Two names, "method" and "timestamp", are surrounded with double quotes
to avoid interpretation as SQL keywords.
<p>
Implemented names of no-parameter or fixed-parameter access methods follow.
<table border="1" cellpadding="3" cellspacing="0">
<tr><th>Column parameter</th><th>HTTP accessor</th><th>SQL process</th></tr>
<tr><td>attributenames</td><td>request.getAttributeNames()</td>
 <td>ArrayString</td></tr>
<tr><td>authtype</td><td>request.getAuthType()</td><td>String</td></tr>
<tr><td>buffersize</td><td>response.getBufferSize()</td><td>Integer</td></tr>
<tr><td>characterencoding</td><td>request.getCharacterEncoding()</td>
 <td>String</td></tr>
<tr><td>contentcountlength<br>contentcountlengthinteger</td>
 <td>response.getContentCount() ||&nbsp;response.getContentLength()</td>
 <td>Long<br>Integer</td></tr>
<tr><td>contentlength</td><td>request.getContentLength()</td>
 <td>Integer</td></tr>
<tr><td>contenttype</td><td>request.getContentType()</td><td>String</td></tr>
<tr><td>contextpath</td><td>request.getContextPath()</td><td>String</td></tr>
<tr><td>elapsedmilli</td><td>System.currentTimeMillis()
 - response.getCurrentTimeMillis()</td><td>Long</td></tr>
<tr><td>epochmilli<br>epochmillikey</td>
 <td>response.getCurrentTimeMillis()</td><td>Long<br>LongKey</td></tr>
<tr><td>headerfirsts<br>headerfirstsclob</td>
 <td>e=request.getHeaderNames()<br>request.getHeader(e.nextElement())</td>
 <td>ArrayString<br>ArrayStringClob</td></tr>
<tr><td>headernames<br>headernamesclob</td><td>request.getHeaderNames()</td>
 <td>ArrayString<br>ArrayStringClob</td></tr>
<tr><td>idvalid</td><td>request.isRequestedSessionIdValid()</td>
 <td>Boolean</td></tr>
<tr><td>idfromcookie</td><td>request.isRequestedSessionIdFromCookie()</td>
 <td>Boolean</td></tr>
<tr><td>idfromurl</td><td>request.isRequestedSessionIdFromURL()</td>
 <td>Boolean</td></tr>
<tr><td>initparameternames</td><td>servletContext.getInitParameterNames()</td>
 <td>ArrayString</td></tr>
<tr><td>locale</td><td>response.getLocale().toString()</td>
 <td>String</td></tr>
<tr><td>localedisplayname</td> <!-- 200B is zero width space Unicode -->
 <td>response.getLocale().getDisplayName()</td>
 <td>String</td></tr>
<tr><td>localaddr</td><td>request.getLocalAddr()</td><td>String</td></tr>
<tr><td>localname</td><td>request.getLocalName()</td><td>String</td></tr>
<tr><td>localport</td><td>request.getLocalPort()</td><td>Integer</td></tr>
<tr><td>maxinactiveinterval</td><td>session.getMaxInactiveInterval()</td>
 <td>Integer</td></tr>
<tr><td>"method"</td><td>request.getMethod()</td><td>String</td></tr>
<tr><td>parameternames</td><td>request.getParameterNames()</td>
 <td>ArrayString</td></tr>
<tr><td>pathinfo</td><td>request.getPathInfo()</td><td>String</td></tr>
<tr><td>pathtranslated</td><td>request.getPathTranslated()</td>
 <td>String</td></tr>
<tr><td>principal</td><td>principal.toString()</td><td>String</td></tr>
<tr><td>protocol</td><td>request.getProtocol()</td><td>String</td></tr>
<tr><td>querystring<br>querystringclob<br>querytext<br>querytextclob</td>
 <td>request.getQueryString()</td>
 <td>String<br>StringClob<br>Text<br>TextClob</td></tr>
<tr><td>referer<br>refererclob<br>referertext<br>referertextclob</td>
 <td>request.getHeader("referer")</td>
 <td>String<br>StringClob<br>Text<br>TextClob</td></tr>
<tr><td>remoteaddr</td><td>request.getRemoteAddr()</td><td>String</td></tr>
<tr><td>remotehost<br>remotehosttext</td><td>request.getRemoteHost()</td>
 <td>String<br>Text</td></tr>
<tr><td>remoteport</td><td>request.getRemotePort()</td><td>Integer</td></tr>
<tr><td>remoteuser</td><td>request.getRemoteUser()</td><td>String</td></tr>
<tr><td>requestedsessionid</td><td>request.getRequestedSessionId()</td>
 <td>String</td></tr>
<tr><td>requesturi</td><td>request.getRequestURI()</td><td>String</td></tr>
<tr><td>requesturl</td><td>request.getRequestURL()</td><td>String</td></tr>
<tr><td>responsecharacterencoding</td><td>response.getCharacterEncoding()</td>
 <td>String</td></tr>
<tr><td>responsecontentcount<br>responsecontentcountinteger</td>
 <td>response.getIntegerContentLength()</td><td>Long<br>Integer</td></tr>
<tr><td>responsecontentlength<br>responsecontentlengthinteger</td>
 <td>response.getContentLength()<br>response.getContentLengthInteger()</td>
 <td>Long<br>Integer</td></tr>
<tr><td>responsecontenttype</td><td>response.getContentType()</td>
 <td>String</td></tr>
<tr><td>responseheaderfirsts<br>responseheaderfirstsclob</td>
 <td>i=response.getHeaderNames().iterator()<br>response.getHeader(i.next())</td>
 <td>ArrayString<br>ArrayStringClob</td></tr>
<tr><td>responseheadernames<br>responseheadernamesclob</td>
 <td>response.getHeaderNames()</td>
 <td>ArrayString<br>ArrayStringClob</td></tr>
<tr><td>rootcause<br>rootcausetext<br>rootcausetrace<br>rootcausetraceclob</td>
<td>catch(Throwable)<br>request.setAttribute(String,&nbsp;Throwable)<br>
for(Throwable.getCause())</td>
 <td>String<br>Text<br>ArrayString<br>ArrayStringClob</td></tr>
<tr><td>scheme</td><td>request.getScheme()</td><td>String</td></tr>
<tr><td>secure</td><td>request.isSecure()</td><td>Boolean</td></tr>
<tr><td>serverinfo<br>serverinfotext</td><td>servletContext.getServerInfo()</td>
 <td>String<br>Text</td></tr>
<tr><td>servername</td><td>request.getServerName()</td><td>String</td></tr>
<tr><td>serverport</td><td>request.getServerPort()</td><td>Integer</td></tr>
<tr><td>servletattributenames</td><td>servletContext.getAttributeNames()</td>
 <td>ArrayString</td></tr>
<tr><td>servletcontextname</td>
 <td>servletContext.getServletContextName()</td><td>String</td></tr>
<tr><td>servletcontextpath<br>servletcontextpathtext</td>
 <td>servletContext.getContextPath()</td>
 <td>String<br>Text</td></tr>
<tr><td>servletmajorversion</td><td>servletContext.getMajorVersion()</td>
 <td>Short</td></tr>
<tr><td>servletminorversion</td><td>servletContext.getMinorVersion()</td>
 <td>Short</td></tr>
<tr><td>servletpath</td><td>request.getServletPath()</td><td>String</td></tr>
<tr><td>sessionattributenames</td><td>session.getAttributeNames()</td>
 <td>ArrayString</td></tr>
<tr><td>sessioncreationtime</td><td>session.getCreationTime()</td>
 <td>Timestamp</td></tr>
<tr><td>sessionid</td><td>session.getId()</td><td>String</td></tr>
<tr><td>sessionisnew</td><td>session.isNew()</td><td>Boolean</td></tr>
<tr><td>status</td><td>response.getStatusInteger()</td><td>Integer</td></tr>
<tr><td>throwable<br>throwabletext<br>throwabletrace<br>throwabletraceclob</td>
<td>catch(Throwable)<br>request.setAttribute(String,&nbsp;Throwable)</td>
 <td>String<br>Text<br>ArrayString<br>ArrayStringClob</td></tr>
<tr><td>"timestamp"<br>timestampkey</td>
 <td>response.getCurrentTimeMillis()</td>
 <td>Timestamp<br>TimestampKey</td></tr>
<tr><td>useragent<br>useragentclob<br>useragenttextclob<br>useragenttext</td>
 <td>request.getHeader("user-agent")</td>
 <td>String<br>StringClob<br>Text<br>TextClob</td></tr>
</table>
The early developers of the <code>ServletResponse</code> interface had
a lack of foresight in not providing final sent content size and status,
which are important operational details that should be logged as needed.
Tomcat valves get around the issue by being provided with
implementation class objects but <code>Filter</code> implementations
may get a <code>ServletResponse</code> facade,
which reflection might be able to unmask.
An <code>HttpServletResponseWrapper</code>
implements <code>getStatusInteger</code>
through reflection on the implementations of the <code>ServletResponse</code>,
which typically include <code>getStatus</code>.
The wrapper also tries to heuristically implement <code>Content</code>
<code>Count</code> and <code>Length</code> octet methods with reflection,
which access priviledges may not always allow.
<p>
The <code>ArrayStep</code> provides reflection support for access
to createArray methods of <code>java.sql.Connection</code>
to create <code>java.sql.Array</code> instances,
which are needed to populate array database columns (fields).
With earlier JDBC versions where createArray methods are unavailable,
<code>SqlStringArray</code> supports earlier PostgreSQL versions.
<h3>Parameterized HTTP access</h3>
Parameterized HTTP access processing can be created with an init generator name
and a init value String that specifies the parameter.
Suffixing the same generator name with hyphens, underscores, or digits
may create multiple processing instances,
which can then specify database field names that omit hyphens.
<table border="1" cellpadding="3" cellspacing="0">
<tr><th>Column&nbsp;generator init&nbsp;parameter</th><th>HTTP accessor</th>
 <th>SQL process</th></tr>
<tr><td>attributearraystring<br>attributearraystringclob<br>
attributestring<br>attributestringclob<br>
attributetext<br>attributetextclob</td>
 <td>request.getAttribute(String)</td>
 <td>ArrayString<br>ArrayStringClob<br>
 String<br>Clob<br>Text<br>TextClob</td></tr>
<tr><td>dateheader<br>dateheadertimestamp</td>
 <td>request.getDateHeader(String)</td><td>Long<br>Timestamp</td></tr>
<tr><td>header</td><td>request.getHeader(String)</td><td>String</td></tr>
<tr><td>headers<br>headersclob</td><td>request.getHeaders(String)</td>
 <td>ArrayString<br>ArrayStringClob</td></tr>
<tr><td>initparameter<br>initparametertext</td>
 <td>servletContext.getInitParameter(String)</td>
 <td>String<br>Text</td></tr>
<tr><td>intheader</td><td>request.getIntHeader(String)</td><td>Integer</td></tr>
<tr><td>literal<br>literalboolean<br>
 literalshort<br>literalinteger<br>literallong</td>
 <td><code>'String'</code><br>Boolean.valueOf(String) <i>extended</i><br>
 Short.valueOf(String)<br>Integer.valueOf(String)<br>Long.valueOf(String)</td>
 <td>String<br>Boolean<br>Short<br>Integer<br>Long</td></tr>
<tr><td>parameter<br>parametertext</td><td>request.getParameter(String)</td>
 <td>String<br>Text</td></tr>
<tr><td>parametervalues<br>parametervaluesclob</td>
 <td>request.getParameterValues(String)</td>
 <td>ArrayString<br>ArrayStringClob</td></tr>
<tr><td>responseheader</td><td>response.getHeader(String)</td>
 <td>String</td></tr>
<tr><td>responseheaders<br>responseheadersclob</td>
 <td>response.getHeaders(String) ||&nbsp;response.getHeaderValues(String)</td>
 <td>ArrayString<br>ArrayStringClob</td></tr>
<tr><td>servletattributestring</td>
 <td>servletContext.getAttribute(String)</td><td>String</td></tr>
<tr><td>sessionattributestring</td><td>session.getAttribute(String)</td>
 <td>String</td></tr>
<tr><td>userinrole</td><td>request.isUserInRole(String)</td>
 <td>Boolean</td></tr>
</table>
<h3>Detail init parameters</h3>
Format details of values sent to database field
may be specified with init parameters.
Except for throwableattribute,
these parameters should only appear at most once during initialization.<ul>
<li><b>arraybegindelimiter</b>:
Overrides the PostgreSQL compatible default ({)
start bracketing for literal arrays.
Other characters may be provided such as an openningsquare bracket ([),
which might be used for JSON field types.
Some databases may also have a leading keyword like "ARRAY".</li>
<li><b>arrayelementnull</b>: String to use in place of the default (null)
for null array values.
For instance,
an empty string without quote characters might specify
that nothing is to placed between commas in an array literal.</li>
<li><b>arrayenddelimiter</b>:
Overrides the PostgreSQL compatible default (})
end bracketing for literal arrays.
Other characters may be provided such as a closing square bracket (]),
which might be used for JSON field types.</li>
<li><b>debug</b>: A string that is not equivalent to false
turns on debugging output in various logs.</li>
<li><b>ellipsisstring</b>: A string to use with Text types
when the number of characters
exceeds the absolute value of an optional column size.
The default is a Unicode horizontal ellipsis
(&hellip;,&nbsp;&bsol;u2026) character.
A multi-character ellipsis, such as three (3) dots (...),
could replace the ellipsis character for a relational database
that cannot properly handle Unicode.</li>
<li><b>internalstringarray</b>: Use internal Array for Strings implementation
<code>SqlStringArray</code> of the <code>java.sql.Array</code> interface
for marshalling an Array for Strings for
<code>Connection.setArray(int, java.sql.Array)</code>
when the standard implementat of <code>java.sql.Array</code> would fail.
Some client implementations fail compatibility while escaping Strings
where the server does not properly handle all escape techniques,
particularly earlier version PostgreSQL servers.</li>
<li><b>lineseparator</b>: Override the default line separator property
of the local Java implementation for some externalized strings.
On Unix&trade; or Linix&trade; based systems, including the Mac,
the line separator is a single newline (\n) character.</li>
<li><b>stringelementsqltype</b>: SQL type String for String members
sent to the database for a Java array or Collection.
Even though databases like PostgreSQL support the standard type,
"character varying", for arrays,
their catalogs only accept more restricted types,
non-standard SQL types like "varchar" and "text"
for the type of String members.</li>
<li><b>tablename</b>: Provides a database table name, such as "access",
which may include a database schema name,
to receive the access log rows.
In some databases, the schema name must be a database user name.
The schema name may override the default schema in case of conflicts.</li>
<li><b>throwableattribute</b>: Adds <code>HttpServletRequest</code>
attribute names that may have a <code>Throwable</code> as their value.
Such an attribute needs to be set during error handling
since servers intercept <code>java.lang.Throwable</code>s
before they can reach <code>Filter</code>s and <code>Valve</code>s.
<p>
A Java Server Page (JSP) with the page directives,<pre>
     &lt;%@page language="java" isErrorPage="true" %&gt;
</pre>
sometimes mentioned in <code>WEB-INF/web.xml</code>
with tags <code>&lt;location&gt;</code>
and <code>&lt;exception-type&gt;</code>
with value <code>java.lang.Throwable</code>
in the <code>&lt;error-page&gt;</code> tag,
defines implicit variables <code>request</code> and <code>exception</code>,
which contains any <code>Throwable</code>,
which embedded Java code<pre>
     &lt;% request.setAttribute(<i><u>name</u></i>, exception); %&gt;
</pre>can make available for access logging.
Attribute names must match exactly, including case.
</li>
<li><b>uniqueviolationsqlstate</b>: Replaces the PostgreSQL compatible default,
5-character SQL state string for a database unique value collision, 23505,
with another string value.</li>
<li><b>uniqueviolationretries</b>: An integer representing the maximum
of retry attempts on encountering a unique value collision (unique_violation),
such as repeating the primary key values with another row.
If no columns are configured with a Key type,
then no retries are attempted since all of the same values would be resent.</li>
</ul>
<h3>Column name assignments</h3>
The "columns" init parameter name specifies the assignment of processing
to database fields.
The init parameter name columns may be repeated with
hyphen, underscore, or digit character suffixes.
These columns names are handled in lexical order.
The columns values are comma-separated database field assignments,
which may have white space surrounding tokens,
in order: <ul>
<li>the database name for the field
that may be enclosed in double quotes (&quot;)
to preserve the quoted name as-is and avoid conflicts with SQL keywords.</li>
<li>An optional signed size integral number, enclosed in parenthesis,
which truncates excessively large values from the beginning or the end
according to a negative or positive sign.
The first size overrides later sizes,
including any attached to processing that is being assigned.
The size does not include ellipsis characters,
which the Text type may add to the field stored in the database.</li>
<li>An optional equals sign (=) is required if the first field name
is to be assigned processing associated with another name.</li>
<li>An optional second name, which many be enclosed in double quotes (&quot;)
that would distguish the quoted name from names without quotes,
that already has processing assigned to it either
from a default processing name or
from a parameterized name created through an init parameter name.
</li>
<li>An optional second place to specify a field truncation size
in double quotes (&quot;).</li>
</ul>
<p>
Unquoted names that begin with a question mark (?)
do not create a database field but do allow the assignment of processing.
Question mark names act like variables to allow name swaps.
<p>
An example,
which one behavior of an earlier version of JDBCAccessLogValve inspired,
could use the "columns" value String:<br>
"timestamp"=timestampkey,
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
rootcausetrace
<p>
This example string would work with the following PostgreSQL DDL:<pre>
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
 * The filter was used with PostgreSQL 9.1 UTF8 on Mac OS X 10.6 (Snow Leopard)
 * with JBoss 5.1GA and the Apple distribution of Oracle Java 1.6 originally.
 * However, accommodations, such as CLOB columns and JSON array delimiters,
 * are provided for other relational database management systems (RDMS)
 * implementations:
 * Yugabyte, Citus, MySQL, SQL Server, SyBase, DB2, Informix, Oracle RDBMS,
 * Splice Machine, Derby, SAP and others (probably all trademarked names).
<p>
 * JDBCAccessLogValve version 1.1,
 * which Andre de Jesus and Peter Rossbach authored,
 * inspired development but only their examples were extended.
<p>
 * @author Robert (Bob) L. Kirby
 * <a href="http://bobkirby.info/">http://bobkirby.info/</a><br>
 * kirby dot bob separated with an "at" sign from gmail dot com, avoiding spam.
<!-- Added option for processing Javadoc: -tag copyright -->
 * @copyright Copyright &copy; 2020 Robert L. Kirby. All rights reserved.
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

public class JDBCAccessLogFilter implements Filter {
    @SuppressWarnings("unused") private static final String copyright
        = "Copyright © 2020 Robert L. Kirby";

    /**
     * Default string to use when truncating the Text type
     * to fit within size limitations: an ellipsis character "&hellip;".
     */
    public static final String ELLIPSIS_STRING_DEFAULT = "\u2026";

    /**
     * Default 5-character SQL state code that SQL implementation may return
     * when detecting unique key violations such as for primary keys: 23505.
     */
    public static final String UNIQUE_VIOLATION_SQLSTATE_DEFAULT = "23505";

    /**
     * Default count of retries with adjusted key values after detecting
     * unique key violations such as for primary keys: 3.
     */
    public static final Byte UNIQUE_VIOLATION_RETRIES_DEFAULT = 3;

    /**
     * Default column names and sizes to use with processing names
     * for constructing database fields values
     * when no "columns" init-paramater name (or Valve attribute) is provided.
     *
     * Default column names and sizes can mask misconfiguration!
     */
    /*
    private static final String[][] COLUMN_NAME_SIZE_DEFAULTS
        = {{"\"timestamp\""}, {"remotehost", "16"}, {"query", "2048"},
           {"\"method\"", "8"}, {"status"}, {"username", "16"},
           {"bytes"}, {"virtualhost", "64"}, {"serverport"},
           {"referer", "1028"}, {"useragent", "512"}};
    */
    /**
     * Default alternate names to use in place of processing names
     * for database fields when no "columns" init-paramater name
     * (or Valve attribute) is provided.
     */
    /*
    private static final String[][] ALTERNAME_NAME_DEFAULTS
        = {{"\"timestamp\"", "timestampkey"}, {"remotehost", "remoteaddr"},
           {"query", "requesturi"}, {"username", "remoteuser"},
           {"bytes", "contentcountlengthinteger"}, {"virtualhost", "localname"},
           {"referer", "referertext"}, {"useragent", "useragenttext"}};
    */

    /**
     * Default beginning character for an array representation String
     * to be sent to a SQL database, such as PostgreSQL: "{".
     * JSON arrays are delimited with square brackets ([]) in some databases.
     */
    public static final String ARRAY_BEGIN_DEFAULT = "{";

    /**
     * Default ending character for an array representation String
     * to be sent to a SQL database, such as PostgreSQL: "}".
     * JSON arrays are delimited with square brackets ([]) in some databases.
     */
    public static final String ARRAY_END_DEFAULT = "}";

    /**
     * Default unquoted String to insert for null members sent to the database
     * in a String representation of a Java array or Collection: null.
     */
    public static final String ARRAY_ELEMENT_NULL_DEFAULT = "null";

    /**
     * Default unquoted SQL type String for String members
     * sent to the database for a Java array or Collection: "text".
     * Even though databases like PostgreSQL support the standard type,
     * "character varying", their catalogs only accept more restricted types,
     * non-standard SQL types like "varchar" and "text"
     * for the type of String members.
     */
    public static final String STRING_ELEMENT_SQL_TYPE_DEFAULT = "varchar";

    private static String info = "info.bobkirby.valve.JDBCAccessLogFilter/2.0";

    /**
     * Flags to OR with any other <code>java.util.regex.Pattern</code>
     * flags during Pattern compilation to allow the \w (word)
     * regular expression part to match all Unicode word characters.
     * Before Java version 7 this flag was not implemented,
     * and will be zero (0), giving \w only the older word character set,
     * which may, in turn, limit database field names
     * to the older word character set,
     */
    public static int UNICODE_CHARACTER_CLASS;
    static {
        try {       // Java version 7 allows \w pattern to be extended.
            Field field = Pattern.class.getField("UNICODE_CHARACTER_CLASS");
            UNICODE_CHARACTER_CLASS = field.getInt(null);
        } catch (NoSuchFieldException nsfe) {
            UNICODE_CHARACTER_CLASS = 0;
        } catch (Throwable e) {
            UNICODE_CHARACTER_CLASS = 0;
            e.printStackTrace();
        }
    }
    private static final Pattern ARRAY_ELEMENT_QUOTE_PATTERN
        = Pattern.compile("\\\\|,|\"|\\s");
    private static final Pattern INIT_PARAM_NAME_START_PATTERN
        = Pattern.compile("([a-z]+)");
    private static final Pattern INIT_PARAM_NAME_CONTINUATION_PATTERN
        = Pattern.compile("(?:[-0-9_]+([a-z]*))");
    private static final Pattern SQL_NAME_START_PATTERN
        = Pattern.compile("(\\p{Ll}+)(?:-)*");
    private static final Pattern SQL_NAME_CONTINUATION_PATTERN
        = Pattern.compile("([\\p{Ll}0-9_]+)(?:-)*");
    private static final Pattern ASSIGN_PATTERN = Pattern.compile
        ("(\"?)([?]?\\w+)(\"?)\\s*"
         + "(?:(\\(|\\[|\\{|\\<)\\s*(-?[0-9]+)\\s*(\\>|\\}|\\]|\\))\\s*)?"
         + "(?:(=)\\s*(\"?)([?]?\\w+)(\"?)\\s*"
         + "(?:(\\(|\\[|\\{|\\<)\\s*(-?[0-9]+)\\s*(\\>|\\}|\\]|\\))\\s*)?)?"
         + "(?:(,)\\s*)*",
         UNICODE_CHARACTER_CLASS);
    private static final Pattern CSV_PATTERN = Pattern.compile
        ("(?:,|;|\\s)*"
         + "([\\P{Space}&&[^,;]]+(?:\\p{Space}+[\\P{Space}&&[^,;]]+)*)"
         + "(?:,|;|\\s)*");

    private static final Set<String> TRUE_STRINGS = Collections.unmodifiableSet
        (new HashSet<String>(Arrays.asList
                             ("true", "t", "yes", "y", "on", "1")));
    private static final Set<String> FALSE_STRINGS = Collections.unmodifiableSet
        (new HashSet<String>(Arrays.asList
                             ("false", "f", "no", "n", "off", "0")));
    private static final Object[] EMPTY_OBJECTS = {};
    private static final Class<?>[] EMPTY_CLASSES = {};
    private static final Class<?>[] STRING_OBJECTS_CLASSES
        = {String.class, Object[].class};
    private static final Class<?>[] STRING_OBJECT_CLASSES
        = {String.class, Object.class};

    /**
     * Return descriptive information about this Filter implementation.
     */
    public String getInfo() {
        return (info);
    }

    /**
     * Implement java.sql.Array to pass to setArr
     * See Valentine Gogichashvili's tech blog
     * http://tech.valgog.com/2009/02/passing-arrays-to-postgresql-database.html
     * for an alternate approach for PostgreSQL
     * licensed under a Creative Commons Attribution 3.0 Unported License,
     * http://creativecommons.org/licenses/by/3.0/
     */
    private static class SqlStringArray implements Array {

        private Object[] arr;
        private CharSequence impl;
        private String baseTypeName;

        public SqlStringArray (Collection<String> cs,
                               CharSequence impl,
                               String baseTypeName) {
            arr = cs.toArray();
            this.impl = impl;
            this.baseTypeName = baseTypeName;
        }

        public String toString () {
            return impl.toString();
        }
    
        /* (non-Javadoc)
         * @see java.sql.Array#free()
         */
        public void free () {
        }
    
        /* (non-Javadoc)
         * @see java.sql.Array#getArray()
         */
        public Object[] getArray () {
            Object[] target = new Object[arr.length];
            for (int i = 0; i < arr.length; i++) {
                target[i] = arr[i];
            }
            return target;
        }
    
        /* (non-Javadoc)
         * @see java.sql.Array#getArray(java.util.Map)
         */
        public Object[] getArray (Map<String, Class<?>> m) {
            return getArray();
        }
    
        /* (non-Javadoc)
         * @see java.sql.Array#getArray(long, int)
         */
        public Object[] getArray (long first, int count) {
            int sz = first >= arr.length ? 0 : count <= 0 ? 0
                : Math.min((int) (arr.length - first), count);
            Object[] target = new Object[arr.length];
            for (int i = 0, j = (int) first; i < sz; i++, j++) {
                target[i] = arr[j];
            }
            return target;
        }
    
        /* (non-Javadoc)
         * @see java.sql.Array#getArray(long, int, java.util.Map)
         */
        public Object[] getArray (long first, int count,
                                  Map<String, Class<?>> m) {
            return getArray(first, count);
        }
    
        /* (non-Javadoc)
         * @see java.sql.Array#getBaseType()
         */
        public int getBaseType () {
            return java.sql.Types.VARCHAR;
        }

        /* (non-Javadoc)
         * @see java.sql.Array#getBaseTypeName()
         */
        public String getBaseTypeName () {
            return this.baseTypeName;
            //return "character varying";
        }

        /* (non-Javadoc)
         * @see java.sql.Array#getResultSet()
         */
        public ResultSet getResultSet () {
            throw new UnsupportedOperationException();
        }

        /* (non-Javadoc)
         * @see java.sql.Array#getResultSet(java.util.Map)
         */
        public ResultSet getResultSet (Map<String, Class<?>> m) {
            throw new UnsupportedOperationException();
        }

        /* (non-Javadoc)
         * @see java.sql.Array#getResultSet(long, int)
         */
        public ResultSet getResultSet (long first, int count) {
            throw new UnsupportedOperationException();
        }

        /* (non-Javadoc)
         * @see java.sql.Array#getResultSet(long, int, java.util.Map)
         */
        public ResultSet getResultSet (long first, int count,
                                       Map<String, Class<?>> m) {
            throw new UnsupportedOperationException();
        }
    }

    protected static enum NumericReturnType { // Support switch
        NULL_TYPE,
        LONG_TYPE,
        BOXED_LONG_TYPE,
        INT_TYPE,
        INTEGER_TYPE;
    }

    protected static NumericReturnType getReturnType (Method method) {
        Class<?> returnClass = method.getReturnType();
        if (Long.TYPE.equals(returnClass)) {
            return NumericReturnType.LONG_TYPE;
        } else if (Long.class.equals(returnClass)) {
            return NumericReturnType.BOXED_LONG_TYPE;
        } else if (Integer.TYPE.equals(returnClass)) {
            return NumericReturnType.INT_TYPE;
        } else if (Integer.class.equals(returnClass)) {
            return NumericReturnType.INTEGER_TYPE;
        }
        return NumericReturnType.NULL_TYPE;
    }

    private boolean needMethods = false;
    private boolean searchedForMethods = false;
    private Unmask responseUnmask = null;
    private Method countMethod = null;
    private Method lengthMethod = null;
    private Method statusMethod = null;
    private Method headerMethod = null;
    private Method headersMethod = null;
    private Method headerNamesMethod = null;
    private NumericReturnType countReturnType = NumericReturnType.NULL_TYPE;
    private NumericReturnType lengthReturnType = NumericReturnType.NULL_TYPE;
    private NumericReturnType statusReturnType = NumericReturnType.NULL_TYPE;

    private static abstract class Unmask
        implements Iterator<ServletResponse> {

        public abstract void unmask (ServletResponse response);
        public abstract boolean hasNext ();
        public abstract ServletResponse next ();
        public void remove () {
            throw new UnsupportedOperationException
                ("Unmask Iterator cannot remove.");
        }
        public abstract ServletResponse doImplementation
            (ServletResponse response)
            throws IllegalArgumentException, IllegalAccessException;
    }

    private void checkForMethods (final FilterResponse filterResponse,
                                  HttpServletResponse httpResponse) {

        List<Unmask> unmaskers = new LinkedList<Unmask>();
        // Tries the ServletResponse itself.
        unmaskers.add(new Unmask() {
                private boolean hasNext = false;
                private ServletResponse response;

                public void unmask (ServletResponse response) {
                    this.response = response;
                    hasNext = true;
                }

                public boolean hasNext () {
                    if (! hasNext) {
                        this.response = null;
                    }
                    return hasNext;
                }

                public ServletResponse next () {
                    if (! hasNext) {
                        throw new NoSuchElementException();
                    }
                    hasNext = false;
                    return this.response;
                }

                public ServletResponse doImplementation
                    (ServletResponse response) {

                    this.response = null;
                    return response;
                }

                public String toString () {
                    return "Unmask returns ServletResponse" + this.response
                        + " itself.";
                }
            });
        // Tries a Field of ServletResponse.
        unmaskers.add(new Unmask() {
                private ServletResponse response = null;
                private Field field = null;
                private Iterator<String> namesIter = null;
                private String name;
                public LinkedHashSet<String> implementationFieldNames
                    = new LinkedHashSet<String>(Arrays.asList("response"));

                public void unmask (ServletResponse response) {
                    this.response = response;
                    namesIter = implementationFieldNames.iterator();
                }

                public boolean hasNext () {
                    if (null == namesIter) {
                        return false;
                    } else if (! namesIter.hasNext()) {
                        this.response = null;
                        return false;
                    }
                    return true;
                }

                public ServletResponse next () {
                    if (null == namesIter || ! namesIter.hasNext()) {
                        throw new NoSuchElementException();
                    }
                    try {
                        name = namesIter.next();
                        field = response.getClass().getDeclaredField(name);
                        field.setAccessible(true);
                        Object obj = field.get(response);
                        if (obj instanceof ServletResponse) {
                            return (ServletResponse) obj;
                        }
                    } catch (NoSuchFieldException nsfe) {
                        return null;
                    } catch (SecurityException se) {
                        filterResponse.log("Unmask on field named " + name
                                           + " got " + se);
                        namesIter = null;
                        field = null;
                        return null;
                    } catch (IllegalArgumentException iae) {
                        filterResponse.log("Unmask on field named " + name
                                           + " got " + iae);
                        namesIter = null;
                        field = null;
                        return null;
                    } catch (IllegalAccessException iae) {
                        filterResponse.log("Unmask on field named " + name
                                           + " got " + iae);
                        field = null;
                        return null;
                    }
                    return null;
                }

                public ServletResponse doImplementation
                    (ServletResponse response) throws IllegalAccessException {

                    this.response = null;
                    return (ServletResponse) field.get(response);
                }

                public String toString () {
                    return "Unmask returns field "
                        + (null == field ? null
                           : field.getDeclaringClass() + "." + field.getName())
                        + " for name " + name + " and response " + response;
                }
            });

        for (Unmask unmask: unmaskers) {
            ServletResponse response;
            unmask.unmask(httpResponse);
            while (unmask.hasNext()) {
                response = unmask.next();
                if (null == response) {
                    continue;
                }
                try { // more accurate early JBoss or Tomcat Catalina response
                    countMethod
                        = response.getClass().getMethod("getContentCountLong");
                    countReturnType = getReturnType(countMethod);
                    if (debugEnabled) {
                        debug("Found getContentCountLong: " + countMethod
                              + " returns " + countReturnType);
                    }
                    responseUnmask = unmask;
                } catch (NoSuchMethodException nsme) {
                    // Try next Length method
                    try {      // Possible? JBoss or Tomcat Catalina response
                        countMethod
                            = response.getClass().getMethod("getContentCount");
                        lengthReturnType = getReturnType(countMethod);
                        if (debugEnabled) {
                            debug("Found getContentCount: " + countMethod
                                  + " returns " + countReturnType);
                        }
                        responseUnmask = unmask;
                    } catch (NoSuchMethodException e) {
                        // Use less accurate header length rather than count?
                    }
                }
                try { // Methods to return length sent in response header
                    lengthMethod
                        = response.getClass().getMethod("getContentLengthLong");
                    lengthReturnType = getReturnType(lengthMethod);
                    if (debugEnabled) {
                        debug("Found getContentLengthLong: " + lengthMethod
                              + " returns " + lengthReturnType);
                    }
                    responseUnmask = unmask;
                } catch (NoSuchMethodException nsme) {
                    // Try next Length method
                    try {      // Possible? JBoss or Tomcat Catalina response
                        lengthMethod
                            = response.getClass().getMethod("getContentLength");
                        lengthReturnType = getReturnType(lengthMethod);
                        if (debugEnabled) {
                            debug("Found getContentLength: " + lengthMethod
                                  + " returns " + lengthReturnType);
                        }
                        responseUnmask = unmask;
                    } catch (NoSuchMethodException e) {
                        // getContentLength[Long] still returns null
                    }
                }
                try {
                    statusMethod = response.getClass().getMethod("getStatus");
                    statusReturnType = getReturnType(statusMethod);
                    if (debugEnabled) {
                        debug("Found getStatus: " + statusMethod
                              + " returns " + statusReturnType);
                    }
                    responseUnmask = unmask;
                } catch (NoSuchMethodException nsme) {
                    // Status still returns null
                }
                try {
                    headerMethod = response.getClass().getMethod("getHeader",
                                                                 String.class);
                    responseUnmask = unmask;
                } catch (NoSuchMethodException nsme) {
                    // Header still returns null
                }
                try {
                    headersMethod = response.getClass().getMethod("getHeaders",
                                                                  String.class);
                    responseUnmask = unmask;
                } catch (NoSuchMethodException nsme) {
                    try {
                        headersMethod
                            = response.getClass().getMethod("getHeaderValues",
                                                            String.class);
                        responseUnmask = unmask;
                    } catch (NoSuchMethodException e) {
                        // Header still returns null
                    }
                }
                try {
                    headerNamesMethod
                        = response.getClass().getMethod("getHeaderNames");
                    responseUnmask = unmask;
                } catch (NoSuchMethodException nsme) {
                    // Header still returns null
                }
                if (null != responseUnmask) {
                    break;
                }
            }
            if (null != responseUnmask) {
                break;
            }
        }
        if (null == countMethod) {
            filterResponse.log
                ("ContentCount method not available for ServletResponse:"
                 + httpResponse + " with ClassLoader="
                 + httpResponse.getClass().getClassLoader());
        }
        if (null == lengthMethod) {
            filterResponse.log
                ("ContentLength method not available for ServletResponse:"
                 + httpResponse + " with ClassLoader="
                 + httpResponse.getClass().getClassLoader());
        }
        if (null == statusMethod) {
            filterResponse.log
                ("Status method not available for ServletResponse:"
                 + httpResponse + " with ClassLoader="
                 + httpResponse.getClass().getClassLoader());
        }
        if (null == headerMethod) {
            filterResponse.log
                ("Header method not available for ServletResponse:"
                 + httpResponse + " with ClassLoader="
                 + httpResponse.getClass().getClassLoader());
        }
        if (null == headersMethod) {
            filterResponse.log
                ("Headers method not available for ServletResponse:"
                 + httpResponse + " with ClassLoader="
                 + httpResponse.getClass().getClassLoader());
        }
        if (null == headerNamesMethod) {
            filterResponse.log
                ("Header names method not available for ServletResponse:"
                 + httpResponse + " with ClassLoader="
                 + httpResponse.getClass().getClassLoader());
        }
    }

    /**
     * If a child class to not get an appropriate HttpServletResponse subclass,
     * try searching once for Methods with reflection.
     */
    public boolean searchForMethods () {
        if (searchedForMethods) {
            return false;       // Do not search or warn again.
        }
        needMethods = true;
        return true;
    }

    /**
     * Wrapper to allow getting values
     * that are only supported in later environments
     * or are not exposed by the standard interface.
     * The names of the implementation functions differ
     * from the names of the potential functions of the interfaces
     * because their contracts differ.
     */
    public class FilterResponse extends HttpServletResponseWrapper {

        /**
         * The original response that was wrapped.
         */
        private final HttpServletResponse httpResponse;

        /**
         * The response calculated through reflection,
         * which may have creater access to fields.
         */
        private ServletResponse response = null;

        /**
         * GMT time in milliseconds from the epoch at the start of processing.
         * Other values are obtained after processing.
         */
        private long currentTimeMillis;

        /**
         * Save what was thrown during processing, otherwise null.
         */
        private Throwable throwable = null;

        private Connection cachedArrayConnection = null;

        /**
         * Cached ServletContext obtained from <code>FilterConfig</code>
         * for Filters but computed for Valves,
         * which may service several contexts.
         */
        protected ServletContext servletContext
            = JDBCAccessLogFilter.this.servletContext;

        /**
         * Constructor wraps response of HTTP query,
         * saves the current time near the beginning of the query, and
         * uses reflection to identify unadvertised methods
         * if a Filter flag requests it.
         */
        protected FilterResponse (HttpServletResponse httpResponse) {
            super(httpResponse);
            this.httpResponse = httpResponse;
            currentTimeMillis = System.currentTimeMillis();
            if (needMethods) {
                needMethods = false;
                searchedForMethods = true;
                checkForMethods(this, httpResponse);
            }
        }

        /**
         * If available, return a count of bytes that were sent,
         * which is typically only available in Valves,
         * not the standard interface.
         * Replaces a non-positive value with null.
         * @throws ServletException If reflection fails to notice
         * that a proposed Method is actually unavailable
         * or on an unexpected reflected return type.
         */
        protected Long getCount () throws ServletException {
            if (null == countMethod) {
                return null;
            }
            if (null == response) {
                try {
                    response = responseUnmask.doImplementation(httpResponse);
                } catch (IllegalAccessException iae) {
                    // This Exception should have been seen earlier
                    throw new ServletException(iae);
                }
            }
            try {
                switch (countReturnType) {
                case LONG_TYPE:
                case BOXED_LONG_TYPE:
                    Long box = (Long) countMethod.invoke(response);
                    return null == box ? null
                        : 0L < box.longValue() ? box : null;
                case INT_TYPE:
                case INTEGER_TYPE:
                    Integer iBox = (Integer) countMethod.invoke(response);
                    if (null == iBox) {
                        return null;
                    }
                    long count = iBox.longValue();
                    return 0L < count ? Long.valueOf(count) : null;
                default:
                    StringBuilder sb = logStringBuilder
                        ("Unexpected return type=",
                         "FilterResponse.getCount()");
                    sb.append(countReturnType);
                    throw new ServletException(sb.toString());
                }
                // Keep compilers happy about unlikely exceptions.
            } catch (IllegalAccessException e) {
                throw new ServletException(e);
            } catch (InvocationTargetException e) {
                throw new ServletException(e);
            }
        }

        /**
         * If available, return an estimate of bytes that were to be sent,
         * typically as calculated for a Content-Length output header.
         * Replaces a non-positive value with null.
         * @throws ServletException If reflection fails to notice
         * that a proposed Method is actually unavailable
         * or on an unexpected reflected return type.
         */
        protected Long getLongLength () throws ServletException {
            if (null == lengthMethod) {
                return null;
            }
            if (null == response) {
                try {
                    response = responseUnmask.doImplementation(httpResponse);
                } catch (IllegalAccessException iae) {
                    // This Exception should have been seen earlier
                    throw new ServletException(iae);
                }
            }
            try {         // Method not available before JavaEE 7.
                switch (lengthReturnType) {
                case LONG_TYPE:
                case BOXED_LONG_TYPE:
                    Long box = (Long) lengthMethod.invoke(response);
                    return null == box ? null
                        : 0L < box.longValue() ? box : null;
                case INT_TYPE:
                case INTEGER_TYPE:
                    Integer iBox = (Integer) lengthMethod.invoke(response);
                    if (null == iBox) {
                        return null;
                    }
                    long count = iBox.longValue();
                    return 0L < count ? Long.valueOf(count) : null;
                default:
                    StringBuilder sb = logStringBuilder
                        ("Unexpected return type=",
                         "FilterResponse.getLongLength()");
                    sb.append(lengthReturnType);
                    throw new ServletException(sb.toString());
                }
                // Keep compilers happy about unlikely exceptions.
            } catch (IllegalAccessException e) {
                throw new ServletException(e);
            } catch (InvocationTargetException e) {
                throw new ServletException(e);
            }
        }

        /**
         * If available, return an estimate of bytes that were to be sent,
         * typically as calculated for a Content-Length output header.
         * Replaces a non-positive value with null.
         * @throws ServletException If reflection fails to notice
         * that a proposed Method is actually unavailable
         * or on an unexpected reflected return type.
         */
        protected Integer getIntegerLength () throws ServletException {
            if (null == lengthMethod) {
                return null;
            }
            if (null == response) {
                try {
                    response = responseUnmask.doImplementation(httpResponse);
                } catch (IllegalAccessException iae) {
                    // This Exception should have been seen earlier
                    throw new ServletException(iae);
                }
            }
            try {         // Method not available before JavaEE 7.
                switch (lengthReturnType) {
                case LONG_TYPE:
                case BOXED_LONG_TYPE:
                    Long longBox = (Long) lengthMethod.invoke(response);
                    if (null == longBox) {
                        return null;
                    }
                    long l = longBox.longValue();
                    return 0L < l && Integer.MAX_VALUE < l
                        ? Integer.valueOf((int) l) : null;
                case INT_TYPE:
                case INTEGER_TYPE:
                    Integer iBox = (Integer) lengthMethod.invoke(response);
                    if (null == iBox) {
                        return null;
                    }
                    return 0 < iBox.intValue() ? iBox : null;
                default:
                    StringBuilder sb = logStringBuilder
                        ("Unexpected return type=",
                         "FilterResponse.getIntegerLength()");
                    sb.append(lengthReturnType);
                    throw new ServletException(sb.toString());
                }
                // Keep compilers happy about unlikely exceptions.
            } catch (IllegalAccessException e) {
                throw new ServletException(e);
            } catch (InvocationTargetException e) {
                throw new ServletException(e);
            }
        }

        /**
         * If available, return the 3-digit status code of the response.
         * The earlier <code>HttpServletResponse</code> interface
         * omitted this important value, which was available to Valves.
         * Replaces a non-positive value with null.
         * @throws ServletException If reflection fails to notice
         * that a proposed Method is actually unavailable
         * or on an unexpected reflected return type.
         */
        protected Integer getStatusInteger () throws ServletException {
            if (null == statusMethod) {
                return null;
            }
            if (null == response) {
                try {
                    response = responseUnmask.doImplementation(httpResponse);
                } catch (IllegalAccessException iae) {
                    // This Exception should have been seen earlier
                    throw new ServletException(iae);
                }
            }
            try {         // Method not available before JavaEE 7.
                switch (statusReturnType) {
                case LONG_TYPE:
                case BOXED_LONG_TYPE:
                    Long longBox = (Long) statusMethod.invoke(response);
                    if (null == longBox) {
                        return null;
                    }
                    long l = longBox.longValue();
                    return 0L < l && Integer.MAX_VALUE < l
                        ? Integer.valueOf((int) l) : null;
                case INT_TYPE:
                case INTEGER_TYPE:
                    Integer iBox = (Integer) statusMethod.invoke(response);
                    if (null == iBox) {
                        return null;
                    }
                    return 0 < iBox.intValue() ? iBox : null;
                default:
                    StringBuilder sb = logStringBuilder
                        ("Unexpected return type=",
                         "FilterResponse.getStatusInteger()");
                    sb.append(statusReturnType);
                    throw new ServletException(sb.toString());
                }
                // Keep compilers happy about unlikely exceptions.
            } catch (IllegalAccessException e) {
                throw new ServletException(e);
            } catch (InvocationTargetException e) {
                throw new ServletException(e);
            }
        }

        /**
         * If available,
         * return the first value assigned to a header in the response.
         * The earlier <code>HttpServletResponse</code> interface
         * omitted this important capability, which was available to Valves.
         * @throws ServletException If reflection fails to notice
         * that a proposed Method is actually unavailable.
         */
        protected String getStringHeader (String name) throws ServletException {
            if (null == headerMethod) {
                return null;
            }
            if (null == response) {
                try {
                    response = responseUnmask.doImplementation(httpResponse);
                } catch (IllegalAccessException iae) {
                    // This Exception should have been seen earlier
                    throw new ServletException(iae);
                }
            }
            try {
                return (String) headerMethod.invoke(response, name);
                // Keep compilers happy about unlikely exceptions.
            } catch (IllegalAccessException e) {
                throw new ServletException(e);
            } catch (InvocationTargetException e) {
                throw new ServletException(e);
            }
        }

        /**
         * If available,
         * return all values assigned with the same header name in the response.
         * The earlier <code>HttpServletResponse</code> interface
         * omitted this important capability, which was available to Valves.
         * @throws ServletException If reflection fails to notice
         * that a proposed Method is actually unavailable.
         */
        @SuppressWarnings("unchecked")
            protected Collection<String> getStringHeaders (String name)
            throws ServletException {

            if (null == headersMethod) {
                return null;
            }
            if (null == response) {
                try {
                    response = responseUnmask.doImplementation(httpResponse);
                } catch (IllegalAccessException iae) {
                    // This Exception should have been seen earlier
                    throw new ServletException(iae);
                }
            }
            try {
                Object obj = headersMethod.invoke(response, name);
                return null == obj ? null
                    : obj.getClass().isArray()
                    ? Arrays.asList((String[]) obj) : (Collection<String>) obj;
                // Keep compilers happy about unlikely exceptions.
            } catch (IllegalAccessException e) {
                throw new ServletException(e);
            } catch (InvocationTargetException e) {
                throw new ServletException(e);
            }
        }

        /**
         * If available, return all header names used in the response.
         * The earlier <code>HttpServletResponse</code> interface
         * omitted this important capability, which was available to Valves.
         * @throws ServletException If reflection fails to notice
         * that a proposed Method is actually unavailable.
         */
        @SuppressWarnings("unchecked")
            protected Collection<String> getStringHeaderNames ()
            throws ServletException {

            if (null == headerNamesMethod) {
                return null;
            }
            if (null == response) {
                try {
                    response = responseUnmask.doImplementation(httpResponse);
                } catch (IllegalAccessException iae) {
                    // This Exception should have been seen earlier
                    throw new ServletException(iae);
                }
            }
            try {
                Object obj = headerNamesMethod.invoke(response);
                return null == obj ? null
                    : obj.getClass().isArray()
                    ? Arrays.asList((String[]) obj) : (Collection<String>) obj;
                // Keep compilers happy about unlikely exceptions.
            } catch (IllegalAccessException e) {
                throw new ServletException(e);
            } catch (InvocationTargetException e) {
                throw new ServletException(e);
            }
        }

        /**
         * Since Timestamps are rounded in serveral database implementations,
         * provide the time in milliseconds that the host JVM supports.
         * This time estimate might be used as a database primary key,
         * which could be incremented if a collision were detected,
         * without substantially effecting the usefulness of the estimate.
         */
        protected long getCurrentTimeMillis () {
            return currentTimeMillis;
        }

        /**
         * Set Throwable thrown during processing.
         * @param throwable Anything throw, during processing.
         */
        public void setThrowable (Throwable throwable) {
            this.throwable = throwable;
        }

        /**
         * Get anything thrown thrown during processing
         * including from request attributes set on error pages.
         * If nothing thrown, returns null.
         */
        public Throwable getThrowable (HttpServletRequest request) {
            for (String attribute: throwableAttributes) {
                Object obj = request.getAttribute(attribute);
                if (null == obj) {
                    continue;
                }
                if (obj instanceof Throwable) {
                    return (Throwable) obj;
                }
                StringBuilder sb = logStringBuilder
                    ("HttpServletRequest attribute, ",
                     this.toString() + ".getThrowable(" + request + ")");
                sb.append(attribute);
                sb.append(", value for Throwable was instead ");
                sb.append(obj);
                log(sb);
            }
            return this.throwable;
        }

        /**
         * Return the Filter implementation so that Valves may access
         * the parameters associated with the current response.
         * A Valve may have several concurrent Filters,
         * each with separate ServletContexts.
         */
        protected JDBCAccessLogFilter getFilter () {
            return JDBCAccessLogFilter.this;
        }

        /**
         * If FilterConfig had a ServletContext while being called as a Filter,
         * then the ServletContext is unique; otherwise null.
         * When called as a Valve with potentially more than one container,
         * then each container can have its own ServletContext,
         * which FilterResponse discovers for each HTTP query.
         */
        protected ServletContext getServletContext () {
            return JDBCAccessLogFilter.this.servletContext;
        }

        /**
         * Log message through a ServletContext associated
         * with an HTTP request or response because Valves
         * do not automatically has a ServletContext.
         * @param message A CharSequence, which may not be null or empty, and
         * which provides warning-level message about a continuable situation.
         * @throws NullPointerException If the message parameter is null.
         * @throws IllegalArgumentException If the message parameter is empty.
         */
        protected void log (CharSequence message) {
            JDBCAccessLogFilter.this.log(getServletContext(), message);
        }

        /**
         * Log message with a throwable through a ServletContext associated
         * with an HTTP request or response because Valves
         * do not automatically has a ServletContext.
         * @param message A CharSequence, which might be null or empty,
         * to introduce the warning message.
         * @param throwable Any Throwable,
         * which will have its stack trace dumped,
         * providing more information about a continuable situation.
         * @throws NullPointerException If the throwable parameter is null.
         */
        protected void log (CharSequence message, Throwable throwable) {
            JDBCAccessLogFilter.this.log
                (getServletContext(), message, throwable);
        }

        /**
         * Convert an element of a String array into a format
         * that <code>SqlStringArray</code> may pass to the database.
         * Each database vendor may have a different approach to quoting.
         * PostgreSQL may use this version.
         * Using <code>java.lang.StringBuffer</code> in order to use
         * <code>java.util.regex.Matcher.appendReplacement()</code> and
         * <code>java.util.regex.Matcher.appendTail()</code> pair with
         * earlier Java versions that did not support
         * <code>java.lang.StringBuilder</code>,
         * which does not do unneeded sychronization.
         * @param s A String element of an array or collection
         * that will be quoted for use with <code>SqlStringArray</code>.
         * @param sb A StringBuffer that will compose the quoted element
         * into an array literal for
         * <code>PreparedStatement.setArray(int, java.sql.Array)</code>
         * or <code>PreparedStatement.setClob(int, String)</code>.
         */
        protected void stringArrayElement (String s, StringBuffer sb) {
            if (0 == s.length()) {
                sb.append("\"\"");
                return;
            }
            Matcher matcher = ARRAY_ELEMENT_QUOTE_PATTERN.matcher(s);
            while (matcher.find()) {
                matcher.appendReplacement(sb, "\\\\$0");
            }
            matcher.appendTail(sb);
        }

        protected Array createArray (Connection connection,
                                     String stringElementSqlType,
                                     Collection<String> collection)
            throws IllegalAccessException, InvocationTargetException,
                   SQLException {
            if (null == cachedArrayConnection) {
                cachedArrayConnection = connection;
                for (OpenArrayStep step : arraySteps) {
                    cachedArrayConnection =
                        step.getNextConnection(cachedArrayConnection);
                }
            }
            return (Array) createArrayMethod.invoke(cachedArrayConnection,
                                                    stringElementSqlType,
                                                    collection.toArray());
        }
    }

    /**
     * Create new FilterResponse in a way that may be overridden.
     * The static needMethods should be true if reflection is expected.
     * @param httpResponse The HttpServletResponse that will be wrapped.
     */
    public FilterResponse createFilterResponse
        (HttpServletResponse httpResponse) {

        if (debugEnabled) {
            debug("JDBCAccessLogFilter.FilterResponse " + this
                  + ".createFilterResponse(" + httpResponse + ")");
        }
        return new FilterResponse(httpResponse);
    }

    /**
     * Action to setup an initialization that includes a parameter.
     */
    protected interface ParamSetup {
        /**
         * Add a named setup that includes a parameter value.
         * @param name Non-empty String name of parameter to receive value.
         * @param value A parameter String to set up named parameter.
         * @throws ServletException if the name field cannot be transformed
         * into a database field (column) name.
         */
        public void setup (String name, String value) throws ServletException;
    }

    private ServletContext servletContext = null;
    private Method createArrayMethod = null;
    private boolean internalStringArray = false;
    private String ellipsisString = ELLIPSIS_STRING_DEFAULT;
    private String arrayElementNull = ARRAY_ELEMENT_NULL_DEFAULT;
    private String stringElementSqlType = STRING_ELEMENT_SQL_TYPE_DEFAULT;
    private String lineSeparator = System.getProperty("line.separator");
    private String arrayStart = ARRAY_BEGIN_DEFAULT;
    private String arrayEnd = ARRAY_END_DEFAULT;
    private byte uniqueViolationRetries = UNIQUE_VIOLATION_RETRIES_DEFAULT;
    private int incrementIndex = 0;
    private int[] retryIncrements = {1, 3, 2, 5};
    private String uniqueViolation = UNIQUE_VIOLATION_SQLSTATE_DEFAULT;
    private Map<String, List<String[]>> columnGroups
        = new TreeMap<String, List<String[]>>();
    private List<String> columnNameList = new LinkedList<String>();
    private Map<String, Long> columnSizes = new HashMap<String, Long>();

    private String filterName;
    private FilterConfig filterConfig;
    private DataSource ds = null;
    private String dsName = null;
    /**
     * Delays looking for connections until absolutely needed
     * to produce a row for the first access.
     * Sometimes DataSources may not be available during initialization.
     */
    private boolean notConnected = true;
    private String connectionURL = null;
    private String userName = null;
    private String password = null;
    private String driverClassName = null;
    private String tableName = null;
    private Connection conn = null;
    private List<String> throwableAttributes = new LinkedList<String>();

    private Map<BitSet, String> sqlInsertCache = new HashMap<BitSet, String>();
    private Map<Connection,
        Map<String, PreparedStatement>> preparedStatementCache
        = new IdentityHashMap<Connection, Map<String, PreparedStatement>>();
    private boolean stackPrinted = false;
    private final Map<String, ParamSetup> parameterSetups
        = new HashMap<String, ParamSetup>();
    private boolean debugEnabled = false;

    /**
     * Use <code>org.apache.catalina.Valve</code> implementation
     * to output warnings in a standard way when a ServletContext
     * is unavailable, such as during Valve configuration,
     * without having to use <code>java.lang.System.err</code>,
     * which is the output technique of last resort.
     */
    public interface Warner {

        /**
         * Output warning text message.
         * @param message Non-null, non-empty CharSequence describing warning.
         */
        public void warn (CharSequence message);

        /**
         * Output optional warning text message and stack of throwable.
         * @param message Optional CharSequence describing warning.
         * @param throwable Non-null Throwable whose stack should be output.
         */
        public void warn (CharSequence message, Throwable throwable);

        /**
         * Output debug text message, when debugging is enabled.
         * @param message Non-null, non-empty CharSequence describing
         * debugging information such as start and stop events.
         */
        public void debug (CharSequence message);
    }

    private Warner warner = null;

    /**
     * Return the current implementation of the Warner interface,
     * which defaults to null.
     */
    public Warner getWarner () {
        return warner;
    }

    /**
     * Sets the current implementation of the Warner interface,
     * which defaults to null,
     * but <code>org.apache.catalina.Valve</code> implementations
     * may establish.
     */
    public void setWarner (Warner warner) {
        this.warner = warner;
    }

    /**
     * Helper function to create log messages.
     * Declare public to give Valves access to a common format.
     * @param message Message, which may be null,
     * which follow an identification header.
     * @param routine Name of the routine called that wants the StringBuilder.
     */
    public StringBuilder logStringBuilder
        (CharSequence message, String routine) {

        StringBuilder sb
            = new StringBuilder(this.getClass().getSimpleName());
        if (null != routine) {
            sb.append('.');
            sb.append(routine);
        }
        if (null != filterName) {
            sb.append(" named ");
            sb.append(filterName);
        }
        if (null != message) {
            sb.append(": ");
            sb.append(message);
        }
        return sb;
    }

    /**
     * Outputs a warning level message using one of the standard techniques,
     * if available.
     * If not, defaults to <code>java.lang.System.err.println()</code>,
     * which is the output technique of last resort
     * since it does not support typical event logging.
     * Warnings allow processing to continue as opposed to
     * throwing an Exception, which stops processing.
     * @param message A CharSequence, which may not be null or empty, and
     * which provides warning-level message about a continuable situation.
     * @throws NullPointerException If the message parameter is null.
     * @throws IllegalArgumentException If the message parameter is empty.
     */
    public void log (CharSequence message) {
        log(this.servletContext, message);
    }

    private void log (ServletContext servletContext, CharSequence message) {
        if (null == message) {
            throw new NullPointerException
                ("No message provided for a warning log.");
        }
        if (0 == message.length()) {
            throw new IllegalArgumentException
                ("Empty message provided for a warning log.");
        }
        if (null != servletContext) {
            servletContext.log(message.toString());
        } else if (null != warner) {
            warner.warn(message);
        } else {
            System.err.println("Not event logged: " + message);
        }
    }

    /**
     * Outputs a warning level message using one of the standard techniques,
     * if available, and outputs a stack trace.
     * If not, defaults to <code>java.lang.System.err.println()</Code>,
     * which is the output technique of last resort
     * since it does not support typical event logging.
     * Warnings allow processing to continue as opposed to
     * throwing an Exception, which stops processing.
     * @param message A CharSequence, which might be null or empty,
     * to introduce the warning message.
     * @param throwable Any Throwable, which will have its stack trace dumped,
     * providing more information about a continuable situation.
     * @throws NullPointerException If the throwable parameter is null.
     */
    public void log (CharSequence message, Throwable throwable) {
        log(this.servletContext, message, throwable);
    }

    private void log (ServletContext servletContext,
                      CharSequence message,
                      Throwable throwable) {
        if (null == throwable) {
            throw new NullPointerException("Null log throwable");
        }
        if (null != servletContext) {
            servletContext.log(null == message ? "" : message.toString(),
                               throwable);
            return;
        }
        if (null != warner) {
            warner.warn(message, throwable);
            return;
        }
        if (null == message || 0 == message.length()) {
            System.err.println("Not event logged:");
        } else {
            System.err.println("Not event logged:" + message);
        }
        throwable.printStackTrace(System.err);
    }
    /**
     * Outputs a debug level message using one of the standard techniques,
     * if available.
     * If not, defaults to <code>java.lang.System.out.println()</code>,
     * which is the output technique of last resort.
     * @param message A CharSequence, which may not be null or empty, and
     * which provides warning-level message about a continuable situation.
     * @throws NullPointerException If the message parameter is null.
     * @throws IllegalArgumentException If the message parameter is empty.
     */
    public void debug (CharSequence message) {
        if (null == message) {
            throw new NullPointerException
                ("No message provided for a debug log.");
        }
        if (0 == message.length()) {
            throw new IllegalArgumentException
                ("Empty message provided for a debug log.");
        }
        if (! debugEnabled) {
            return;
        }
        if (null != warner) {
            warner.debug(message);
        } else  {
            System.out.println(message);
        }
    }

    /**
     * True if debugging is enabled.
     * Used in conditionals that wrap debugging statements
     * to avoid unnecessary formatting.
     */
    public boolean isDebugEnabled () {
        return debugEnabled;
    }

    /**
     * Sets debugging status to avoid unnecessary formatting.
     * @param debugEnabled Set to true if debugging enabled.
     */
    public void setDebugEnabled (boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    /**
     * Returns true if the Java stack has been printed
     * after a processing warning to avoid obscuring the event log.
     */
    public boolean isStackPrinted () {
        return stackPrinted;
    }

    /**
     * Sets the printed status of the Java stack after a processing warning.
     */
    public void setStackPrinted (boolean stackPrinted) {
        this.stackPrinted = stackPrinted;
    }

    /**
     * Helper adds trace information, similar to printStackTrace,
     * which includes each throwable in the cause chain
     * and their trace elements, in an ordered Collection.
     * @param trace Ordered String Collection to receive throwable
     * and trace items.
     * @param throwable The top Throwable of a cause chain.
     */
    private static void traceThrowable (Collection<String> trace,
                                        Throwable throwable) {
        while (null != throwable) {
            trace.add(throwable.toString());
            for (StackTraceElement element: throwable.getStackTrace()) {
                trace.add(element.toString());
            }
            throwable = throwable.getCause();
        }
    }

    /**
     * Helper finds root cause Throwable of input Throwable,
     * which may be the same as the input Throwable.
     * @param throwable The top Throwable of a cause chain.
     */
    private static Throwable rootCause (Throwable throwable) {
        for (Throwable cause = throwable.getCause();
             null != cause; cause = throwable.getCause()) {
            throwable = cause;
        }
        return throwable;
    }

    /**
     * Abstract class converts request and response values
     * into column values for insertion via JDBC into an access log.
     * Extended with abstract classes to insert specific parameter types
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    public static abstract class DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoValue () {
        }

        /**
         * Obtain a specific value from the HTTP query.
         * @param request Incoming request values available
         * to the HttpServletRequest interface.
         * @param response Augmented <code>FilterResponse</code> values,
         * which may not be directly visible on the current wrapped
         * <code>HttpServletResponse</code> interface.
         * @throws ServletException when the data cannot be obtained
         * because of an unexpected situation, typically a programming error.
         * @throws IllegalAccessException when access permissions
         * block reflection. Usually then retries without reflection,
         * sometimes giving up on obtaining values by returning null.
         * @throws RuntimeException when reflection finds the unexpected.
         */
        public abstract Object getObject (HttpServletRequest request,
                                          FilterResponse response)
            throws ServletException;

        /**
         * Install a value in a PreparedStatement to be sent to the database.
         * Returns true if the proposed value cannot be handled.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj A purportedly non-null Object
         * that will be transformed for insertion into the PreparedStatement.
         * @param size A restriction on how much is sent.
         * Positive values remove from the end, negative from the beginning.
         * Boolean and Number types currently ignore the size parameter.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         * @throws RuntimeException when reflection finds the unexpected.
         */
        public abstract boolean setObject (FilterResponse response,
                                           String sqlInsertString,
                                           PreparedStatement ps,
                                           int parameterIndex,
                                           Object obj, long size)
            throws SQLException;

        /** Indicates that a value used as a key, such as a timestamp,
         * may receive a small increment to resolve unique value collisions,
         * usually on the database primary key.
         */
        public boolean incrementable () {
            return false;
        }

        /**
         * Increment the value of a database key field
         * usually on the database primary key,
         * by a small increment to resolve unique value collisions.
         * Returns a new incremented value for reinsertion
         * in the PreparedStatement being resent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj The Object that will be transformed for insertion
         * into the PreparedStatement.
         * @param size A restriction on how much is sent.
         * Positive values remove from the end, negative from the beginning.
         * @param inc The amount to increment, which pseudorandomly varies
         * to avoid yet another collision among more than one incremented value.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         * @throws UnsupportedOperationException when attempting to
         * increment a type that does not explicitly support incrementing.
         */
        public Object increment (PreparedStatement ps, int parameterIndex,
                                 Object obj, long size, int inc)
            throws SQLException {

            throw new UnsupportedOperationException();
        }
    }

    /**
     * Abstract class converts request and response values
     * into Boolean column values for insertion via JDBC into an access log.
     * Extended with abstract classes to insert Boolean Object
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    protected static abstract class DoBoolean extends DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoBoolean () {
        }

        /**
         * Install a Boolean value in a PreparedStatement
         * to be sent to the database.
         * Returns false since the proposed value can always be handled.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj A Boolean Object to be inserted
         * into the PreparedStatement.
         * @param size A restriction on how much is sent, which is ignored.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         */
        @Override public boolean setObject (FilterResponse response,
                                            String sqlInsertString,
                                            PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size)
            throws SQLException {

            ps.setBoolean(parameterIndex, ((Boolean) obj).booleanValue());
            return false;
        }
    }

    /**
     * Abstract class converts request and response values
     * into Short column values for insertion via JDBC into an access log.
     * Extended with abstract classes to insert Short Object
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    protected static abstract class DoShort extends DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoShort () {
        }

        /**
         * Install a Short value in a PreparedStatement
         * to be sent to the database.
         * Returns false since the proposed value can always be handled.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj A Short Object to be inserted into the PreparedStatement.
         * @param size A restriction on how much is sent, which is ignored.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         */
        @Override public boolean setObject (FilterResponse response,
                                            String sqlInsertString,
                                            PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size)
            throws SQLException {

            ps.setShort(parameterIndex, ((Short) obj).shortValue());
            return false;
        }
    }

    /**
     * Abstract class converts request and response values
     * into Integer column values for insertion via JDBC into an access log.
     * Extended with abstract classes to insert Integer Object
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    protected static abstract class DoInteger extends DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoInteger () {
        }

        /**
         * Install an Integer value in a PreparedStatement
         * to be sent to the database.
         * Returns false since the proposed value can always be handled.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj An Integer Object to be inserted into
         * the PreparedStatement.
         * @param size A restriction on how much is sent, which is ignored.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         */
        @Override public boolean setObject (FilterResponse response,
                                            String sqlInsertString,
                                            PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size)
            throws SQLException {

            ps.setInt(parameterIndex, ((Integer) obj).intValue());
            return false;
        }
    }

    /**
     * Abstract class converts request and response values
     * into Long column values for insertion via JDBC into an access log.
     * Extended with abstract classes to insert Long Object
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    protected static abstract class DoLong extends DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoLong () {
        }

        /**
         * Install a Long value in a PreparedStatement
         * to be sent to the database.
         * Returns false since the proposed value can always be handled.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj A Long Object to be inserted into the PreparedStatement.
         * @param size A restriction on how much is sent, which is ignored.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         */
        @Override public boolean setObject (FilterResponse response,
                                            String sqlInsertString,
                                            PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size)
            throws SQLException {

            ps.setLong(parameterIndex, ((Long) obj).longValue());
            return false;
        }
    }

    /**
     * Abstract class converts request and response values
     * into Timestamp column values for insertion via JDBC into an access log.
     * Extended with abstract classes to insert Timestamp Object
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    protected static abstract class DoTimestamp extends DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoTimestamp () {
        }

        /**
         * Install a Timestamp value in a PreparedStatement
         * to be sent to the database.
         * Returns false since the proposed value can always be handled.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj A Timestamp Object to be inserted into
         * the PreparedStatement.
         * @param size A restriction on how much is sent, which is ignored.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         */
        @Override public boolean setObject (FilterResponse response,
                                            String sqlInsertString,
                                            PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size)
            throws SQLException {

            ps.setTimestamp(parameterIndex, (Timestamp) obj);
            return false;
        }
    }

    /**
     * Abstract class converts request and response values into
     * possibly truncated String column values
     * for insertion via JDBC into an access log.
     * Extended with abstract classes to insert String Object
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    protected static abstract class DoString extends DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoString () {
        }

        /**
         * Install a possibly truncated String value in a PreparedStatement
         * to be sent to the database.
         * Returns false since the proposed value can always be handled.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj A String Object to be inserted into the PreparedStatement.
         * @param size A restriction on how much is sent.
         * Positive values remove from the end, negative from the beginning.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         */
        @Override public boolean setObject (FilterResponse response,
                                            String sqlInsertString,
                                            PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size)
            throws SQLException {

            if (0L == size) {
                ps.setString(parameterIndex, (String) obj);
                return false;
            }
            long absSize = 0 < size ? size : - size;
            if (((String) obj).length() < absSize
                || Integer.MAX_VALUE < absSize) {
                ps.setString(parameterIndex, (String) obj);
                return false;
            }
            if (0 < size) {
                ps.setString(parameterIndex,
                             ((String) obj).substring(0, (int) size));
            } else {
                ps.setString(parameterIndex,
                             ((String) obj).substring(((String) obj).length()
                                                      + (int) size));
            }
            return false;
        }
    }

    /**
     * Abstract class converts request and response values into
     * possibly truncated CLOB column values
     * for insertion via JDBC into an access log.
     * Extended with abstract classes to insert the CLOB Object
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    protected static abstract class DoStringClob extends DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoStringClob () {
        }

        /**
         * Install a possibly truncated CLOB value in a PreparedStatement
         * to be sent to the database.
         * Returns false except if the length of the String after truncation
         * exceeds <code>Integer.MAX_VALUE</code>,
         * which is the maximum size of a byte array.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj A String Object to be inserted as a CLOB
         * into the PreparedStatement.
         * @param size A restriction on how much is sent.
         * Positive values remove from the end, negative from the beginning.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         */
        @Override public boolean setObject (FilterResponse response,
                                            String sqlInsertString,
                                            PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size)
            throws SQLException {

            if (Integer.MAX_VALUE < ((String) obj).length()) {
                StringBuilder sb = response.getFilter().logStringBuilder
                    ("for CLOB parameter index ", "DoStringClob.setObject()");
                sb.append(parameterIndex);
                sb.append(" of SQL statement \"");
                sb.append(sqlInsertString);
                sb.append("\" CLOB has size ");
                sb.append(((String) obj).length());
                sb.append(", which is too big to process using a char[].");
                response.log(sb);
                return true;
            }
            if (0L == size) {
                ps.setClob(parameterIndex,
                           new SerialClob(((String) obj).toCharArray()));
                return false;
            }
            long absSize = 0 < size ? size : - size;
            if (((String) obj).length() <= absSize) {
                ps.setClob(parameterIndex,
                           new SerialClob(((String) obj).toCharArray()));
                return false;
            }
            char[] chars = new char[((int) absSize)];
            if (0 < size) {
                ((String) obj).getChars(0, (int) absSize, chars, 0);
                ps.setClob(parameterIndex, new SerialClob(chars));
            } else {
                ((String) obj).getChars(((String) obj).length()
                                        + (int) size, (int) absSize, chars, 0);
                ps.setClob(parameterIndex, new SerialClob(chars));
            }
            return false;
        }
    }

    /**
     * Abstract class converts request and response values into,
     * possibly truncated with an ellipsis String, String column values
     * for insertion via JDBC into an access log.
     * Abstract class converts request and response values
     * into String column values for insertion via JDBC into an access log.
     * Extended with abstract classes to insert a potentially truncated String
     * with a potential configurable ellipsis character String
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    protected static abstract class DoText extends DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoText () {
        }

        /**
         * Install a String, possibly truncated with an ellipsis String,
         * value in a PreparedStatement to be sent to the database.
         * Returns false since the proposed value can always be handled.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj A String Object to be inserted into the PreparedStatement.
         * @param size A restriction on how much is sent.
         * Positive values remove from the end, negative from the beginning.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         */
        @Override public boolean setObject (FilterResponse response,
                                            String sqlInsertString,
                                            PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size)
            throws SQLException {

            if (0L == size) {
                ps.setString(parameterIndex, (String) obj);
                return false;
            }
            long absSize = 0 < size ? size : - size;
            if (((String) obj).length() < absSize
                || Integer.MAX_VALUE < absSize) {
                ps.setString(parameterIndex, (String) obj);
                return false;
            }
            String ellipsisString = response.getFilter().ellipsisString;
            if (0 < size) {
                ps.setString(parameterIndex,
                             ((String) obj).substring(0, (int) size)
                             + ellipsisString);
            } else {
                ps.setString(parameterIndex, ellipsisString
                             + ((String) obj).substring(((String) obj).length()
                                                        + (int) size));
            }
            return false;
        }
    }

    /**
     * Abstract class converts request and response values into,
     * possibly truncated with an ellipsis String, CLOB column values
     * for insertion via JDBC into an access log.
     * Extended with abstract classes to insert the CLOB
     * with a potential configurable ellipsis character String
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    protected static abstract class DoTextClob extends DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoTextClob () {
        }

        /**
         * Install a String, possibly truncated with an ellipsis String,
         * as a CLOB value in a PreparedStatement to be sent to the database.
         * Returns false except if the length of the String after truncation
         * exceeds <code>Integer.MAX_VALUE</code>,
         * which is the maximum size of a byte array.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj A String Object to be inserted into the PreparedStatement.
         * @param size A restriction on how much is sent.
         * Positive values remove from the end, negative from the beginning.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         */
        @Override public boolean setObject (FilterResponse response,
                                            String sqlInsertString,
                                            PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size)
            throws SQLException {

            JDBCAccessLogFilter filter = response.getFilter();
            if (Integer.MAX_VALUE < ((String) obj).length()) {
                StringBuilder sb = filter.logStringBuilder
                    ("for CLOB parameter index ", "DoTextClob.setObject()");
                sb.append(parameterIndex);
                sb.append(" of SQL statement \"");
                sb.append(sqlInsertString);
                sb.append("\" CLOB has size ");
                sb.append(((String) obj).length());
                sb.append(", which is too big to process using a char[].");
                response.log(sb);
                return true;
            }
            if (0L == size) {
                ps.setClob(parameterIndex,
                           new SerialClob(((String) obj).toCharArray()));
                return false;
            }
            long absSize = 0 < size ? size : - size;
            if (((String) obj).length() <= absSize) {
                ps.setClob(parameterIndex,
                           new SerialClob(((String) obj).toCharArray()));
                return false;
            }
            String ellipsisString = filter.ellipsisString;
            int ellipsisStringLength = ellipsisString.length();
            if (Integer.MAX_VALUE < absSize + ellipsisStringLength) {

                StringBuilder sb = filter.logStringBuilder
                    ("for CLOB parameter index ", "DoTextClob.setObject()");
                sb.append(parameterIndex);
                sb.append(" of SQL statement \"");
                sb.append(sqlInsertString);
                sb.append("\" CLOB has size ");
                sb.append(absSize);
                sb.append(", which is too big to process using a char[]"
                          + " with ellipsis ");
                if (1 == ellipsisStringLength) {
                    sb.append(" character '");
                    sb.append(ellipsisString);
                    sb.append("'.");
                } else {
                    sb.append("string \"");
                    sb.append(ellipsisString);
                    sb.append("\".");
                }
                response.log(sb);
                return true;
            }
            if (0 < size) {
                char[] chars
                    = new char[((int) size) + ellipsisStringLength];
                ((String) obj).getChars(0, (int) absSize, chars, 0);
                ellipsisString.getChars
                    (0, ellipsisStringLength, chars, (int) size);
                ps.setClob(parameterIndex, new SerialClob(chars));
            } else {
                char[] chars
                    = new char[ellipsisStringLength + ((int) size)];
                ellipsisString.getChars(0, ellipsisStringLength, chars, 0);
                ((String) obj).getChars(((String) obj).length()
                                        + (int) size, (int) absSize, chars, 
                                        ellipsisStringLength);
                ps.setClob(parameterIndex, new SerialClob(chars));
            }
            return false;
        }
    }

    /**
     * Members of a list of steps to uncover a <code>java.sql.Connection</code>
     * that can create a <code>java.sql.Array</code> of Strings.
     */
    private abstract class ArrayStep {
        private final Connection connection;
        private final String methodName;
        protected final Class<?>[] parameterClasses;
        protected Method method = null;

        public ArrayStep (Connection connection,
                          String methodName,
                          Class<?>[] parameterClasses) {
            this.connection = connection;
            this.methodName = methodName;
            this.parameterClasses = parameterClasses;
        }

        /**
         * Return the <code>java.sql.Connection</code> that created this step.
         * This value is check for identity equality to avoid cycles
         * during creation.
         */
        public final Connection getConnection () {
            return this.connection;
        }

        public final String getMethodName () {
            return this.methodName;
        }

        public final Class<?>[] getParameterClasses () {
            return this.parameterClasses;
        }
        
        public Method getMethod () {
            Class<?> conClass = getConnection().getClass();
            while (null != conClass) {
                if (debugEnabled) {
                    StringBuilder sb = logStringBuilder
                        ("Examining Class ",
                         this + ".getMethod(" + getConnection() + ", "
                         + Arrays.toString(getParameterClasses()) + ")");
                    sb.append(conClass);
                    debug(sb);
                }
                try {
                    method = conClass.getDeclaredMethod(getMethodName(),
                                                        getParameterClasses());
                    if (debugEnabled) {
                        StringBuilder sb = logStringBuilder
                            ("Proposing Method ",
                             this + ".getMethod(" + getConnection() + ", "
                             + Arrays.toString(getParameterClasses()) + ")");
                        sb.append(method);
                        sb.append(" isAccessible=");
                        sb.append(method.isAccessible());
                        debug(sb);
                    }
                    if (! method.isAccessible()) {
                        try {
                            method.setAccessible(true);
                        } catch (SecurityException se) {
                            method = null;
                        }
                    }
                    break;
                } catch (NoSuchMethodException nsme) {
                    conClass = conClass.getSuperclass();
                }
            }
            if (null == method) {
                return null;
            }
            if (2 != getParameterClasses().length) {
                return method;
            }
            try {
                Object o = method.invoke(getConnection(),
                                         stringElementSqlType,
                                         EMPTY_OBJECTS);
                if (null != o && ! (o instanceof Array)) {
                    if (debugEnabled) {
                        StringBuilder sb = logStringBuilder
                            ("Method ",
                             this + ".getMethod(" + getConnection() + ", "
                             + Arrays.toString(getParameterClasses()) + ")");
                        sb.append(method);
                        sb.append(" returned non-java.sql.Array ");
                        sb.append(o);
                        debug(sb);
                    }
                    method = null;
                }
            } catch (IllegalAccessException iae) {
                if (debugEnabled) {
                    StringBuilder sb = logStringBuilder
                    ("Method ",
                     this + ".getMethod(" + getConnection() + ", "
                     + Arrays.toString(getParameterClasses()) + ")");
                    sb.append(method);
                    sb.append
                    (" does not have permission for Connection ");
                    sb.append(getConnection());
                    sb.append(" with ");
                    sb.append(iae);
                    debug(sb);
                }
                method = null;
            } catch (InvocationTargetException ite) {
                if (debugEnabled) {
                    StringBuilder sb = logStringBuilder
                    ("Method ",
                     this + ".getMethod(" + getConnection() + ", "
                     + Arrays.toString(getParameterClasses()) + ")");
                    sb.append(method);
                    sb.append(" failed with ");
                    sb.append(ite);
                    debug(sb);
                }
                method = null;
            }
            return method;
        }

        public int getPriority() {
            return 0;
        }

        @Override public String toString () {
            return this.getClass().getName()
            + "[\"" + getMethodName() + "\"]";
        }
    }

    private class CreateArrayStep extends ArrayStep {
        public CreateArrayStep (Connection connection,
                                String methodName,
                                Class<?>[] parameterClasses) {
            super(connection, methodName, parameterClasses);
        }
    }

    private class StandardArrayStep extends CreateArrayStep {
        public StandardArrayStep (Connection connection) {
            super(connection, "createArrayOf", STRING_OBJECTS_CLASSES);
        }
    }

    private class AlternateArrayStep extends CreateArrayStep {
        public AlternateArrayStep (Connection connection) {
            super(connection, "createArrayOf", STRING_OBJECT_CLASSES);
        }
    }
    /*
    private class OracleArrayStep extends CreateArrayStep {
        public OracleArrayStep (Connection connection) {
            super(connection, "createARRAY", STRING_OBJECT_CLASSES);
        }
    }
    */

    private class OpenArrayStep extends ArrayStep {

        public OpenArrayStep (Connection connection,
                              String methodName) {
            super(connection, methodName, EMPTY_CLASSES);
        }
    
        /**
         * Return the <code>java.sql.Connection</code>
         * that comes from this step.
         */
        public Connection tryNextConnection () {
            try {
                Object o = method.invoke(getConnection(), EMPTY_OBJECTS);
                if (! (o instanceof Connection)) {
                    return null;
                }
                for (ArrayStep step : arraySteps) {
                    if (step.getConnection() == o) {
                        StringBuilder sb = logStringBuilder
                        ("Method ", "ArrayStep.tryNextConnection()");
                        sb.append(method);
                        sb.append(" cycles for ");
                        sb.append(o);
                        sb.append(" on step ");
                        sb.append(step);
                        log(sb);
                        return null;
                    }
                }
                if (debugEnabled) {
                    debug(this + ".tryNextConnection()"
                          + " found Method " + method
                          + " from Connection " + getConnection()
                          + " to Connection " + o);
                }
                return (Connection) o;
            } catch (IllegalAccessException iae) {
                StringBuilder sb = logStringBuilder
                ("Method ", this + ".tryNextConnection()");
                sb.append(method);
                sb.append
                (" does not have permission to try next for Connection ");
                sb.append(getConnection());
                sb.append(". No further invoking.");
                log(sb, iae);
                createArrayMethod = null;
                return null;
            } catch (InvocationTargetException ite) {
                StringBuilder sb = logStringBuilder
                ("Method ", this + ".tryNextConnection()");
                sb.append(method);
                sb.append
                (" cannot get next for Connection ");
                sb.append(getConnection());
                sb.append(". No further invoking.");
                log(sb, ite);
                createArrayMethod = null;
                return null;
            } catch (RuntimeException rte) {
                StringBuilder sb = logStringBuilder
                ("Method ", this + ".tryNextConnection()");
                sb.append(method);
                sb.append
                (" fails to get next for Connection ");
                sb.append(getConnection());
                sb.append(". No further invoking.");
                log(sb, rte);
                createArrayMethod = null;
                return null;
            }
        }

        /**
         * Return the <code>java.sql.Connection</code>
         * that comes from this step.
         */
        public Connection getNextConnection (Connection conn)
            throws SQLException {

            try {
                Object o = method.invoke(conn, EMPTY_OBJECTS);
                if (o instanceof Connection) {
                    return (Connection) o;
                }
                StringBuilder sb = logStringBuilder
                ("Method ", this + ".getNextConnection()");
                sb.append(method);
                sb.append(" got ");
                sb.append(o);
                sb.append(" next for Connection ");
                sb.append(conn);
                sb.append(". No further invoking.");
                createArrayMethod = null;
                throw new SQLException(sb.toString());
            } catch (IllegalAccessException iae) {
                StringBuilder sb = logStringBuilder
                ("Method ", this + ".getNextConnection()");
                sb.append(method);
                sb.append
                (" does not have permission to get next for Connection ");
                sb.append(conn);
                sb.append(". No further invoking.");
                createArrayMethod = null;
                throw new SQLException(sb.toString(), iae);
            } catch (InvocationTargetException ite) {
                StringBuilder sb = logStringBuilder
                ("Method ",
                 this + ".getNextConnection()");
                sb.append(method);
                sb.append
                (" cannot get next for Connection ");
                sb.append(conn);
                sb.append(". No further invoking.");
                createArrayMethod = null;
                throw new SQLException(sb.toString(), ite);
            }
        }
    }

    private class ConnectionArrayStep extends OpenArrayStep {
        public static final int PRIORITY = 1;

        public ConnectionArrayStep (Connection connection) {
            super(connection, "getConnection");
        }

        public int getPriority () {
            return PRIORITY;
        }
    }

    private class WrappedArrayStep extends OpenArrayStep {
        public static final int PRIORITY = 2;

        public WrappedArrayStep (Connection connection) {
            super(connection, "getWrappedObject");
        }

        public int getPriority () {
            return PRIORITY;
        }
    }

    private class UnderlyingArrayStep extends OpenArrayStep {
        public static final int PRIORITY = 3;

        public UnderlyingArrayStep (Connection connection) {
            super(connection, "getUnderlyingConnection");
        }

        public int getPriority () {
            return PRIORITY;
        }
    }

    private LinkedList<OpenArrayStep> arraySteps
        = new LinkedList<OpenArrayStep>();

    private void findCreateArrayMethod (Connection conn) {
        if (internalStringArray) {
            return;
        }
        int priority = 0;
        OpenArrayStep openStep = null;
        while (null == createArrayMethod) {
            if (null == openStep) {
                CreateArrayStep createStep = new StandardArrayStep(conn);
                createArrayMethod = createStep.getMethod();
                if (null != createArrayMethod) {
                    return;
                }
                createStep = new AlternateArrayStep(conn);
                createArrayMethod = createStep.getMethod();
                if (null != createArrayMethod) {
                    return;
                }
                /*
                createStep = new OracleArrayStep(conn);
                createArrayMethod = createStep.getMethod();
                if (null != createArrayMethod) {
                    return;
                }
                */
            }

            if (ConnectionArrayStep.PRIORITY > priority) {
                openStep = new ConnectionArrayStep(conn);
                if (null != openStep.getMethod()) {
                    Connection c = openStep.tryNextConnection();
                    if (null != c) {
                        conn = c;
                        arraySteps.add(openStep);
                        openStep = null;
                        continue;
                    }
                }
            }
            if (WrappedArrayStep.PRIORITY > priority) {
                openStep = new WrappedArrayStep(conn);
                if (null != openStep.getMethod()) {
                    Connection c = openStep.tryNextConnection();
                    if (null != c) {
                        conn = c;
                        arraySteps.add(openStep);
                        openStep = null;
                        continue;
                    }
                }
            }
            if (UnderlyingArrayStep.PRIORITY > priority) {
                openStep = new UnderlyingArrayStep(conn);
                if (null != openStep.getMethod()) {
                    Connection c = openStep.tryNextConnection();
                    if (null != c) {
                        conn = c;
                        arraySteps.add(openStep);
                        openStep = null;
                        continue;
                    }
                }
            }
            if (! arraySteps.isEmpty()) {
                openStep = arraySteps.removeLast();
                priority = openStep.getPriority();
                conn = openStep.getConnection();
                if (debugEnabled) {
                    debug("Restarting at priority " + priority
                          + " from " + openStep
                          + " with connection " + conn);
                }
                continue;
            }
            break;
        }
        if (debugEnabled) {
            debug("No createArrayMethod found. Using " + SqlStringArray.class
                  + " for connection " + conn);
        }
    }

    /**
     * Abstract class converts request and response values into an array of
     * String column values for insertion via JDBC into an access log.
     * Extended with abstract classes to insert a potentially truncated array
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    protected static abstract class DoArrayString extends DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoArrayString () {
        }

        /**
         * Install a possibly truncated array of String values
         * in a PreparedStatement to be sent to the database.
         * Returns false since the proposed value can always be handled.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj A String Collection to be inserted into
         * the PreparedStatement.
         * @param size A restriction on how many array elements are sent.
         * Positive values remove from the end, negative from the beginning.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         * @throws RuntimeException when reflection finds the unexpected.
         */
        @Override public boolean setObject (FilterResponse response,
                                            String sqlInsertString,
                                            PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size)
            throws SQLException {

            @SuppressWarnings("unchecked") Collection<String> collection
                = (Collection<String>) obj;
            if (0L < size) {
                if (collection.size() <= size) {
                    size = 0L;
                }
            } else if (0L > size) {
                if (collection.size() <= - size) {
                    size = 0L;
                } else {
                    size = - (size + collection.size());
                }
            }
            // Trim collection for String array according to size parameter.
            if (0 != size && 0 < (collection.size()
                     - (size > 0L ? (int) size : (int) - size))) {
                List<String> list = new LinkedList<String>();
                for (String s: collection) {
                    if (0L > size) {
                        size++;
                        continue;
                    }
                    list.add(s);
                    if (0L < size) {
                        if (0L >= --size) {
                            break;
                        }
                    }
                }
                collection = list;
            }
            JDBCAccessLogFilter filter = response.getFilter();
            if (null != filter.createArrayMethod) {
                // c.createArrayOf("character varying",
                //                 collection.toArray();
                // c.createARRAY("character varying",
                //               collection.toArray());
                try {
                    ps.setArray
                        (parameterIndex,
                         response.createArray(ps.getConnection(),
                                              filter.stringElementSqlType,
                                              collection));
                    return false;
                } catch (IllegalAccessException iae) {
                    // The method on the connection should be public.
                    StringBuilder sb = filter.logStringBuilder
                        (filter.createArrayMethod.toString(),
                         "DoArrayString.setObject()");
                    sb.append(" not public. Using built-in instead.");
                    response.log(sb, iae);
                    // Retry with SqlStringArray
                    filter.createArrayMethod = null;
                } catch (InvocationTargetException ite) {
                    // Unwrap the cause that reflection sent.
                    Throwable throwable = ite.getCause();
                    if (throwable instanceof SQLException) {
                        throw (SQLException) throwable;
                    }
                    if (throwable instanceof RuntimeException) {
                        throw (RuntimeException) throwable;
                    }
                    // Something really unusual was thrown.
                    StringBuilder sb = filter.logStringBuilder
                        (null == filter.createArrayMethod ? "createArrayMethod"
                         : filter.createArrayMethod.toString(),
                         "DoArrayString.setObject()");
                    sb.append(" failed for ");
                    sb.append(ps);
                    sb.append(".getConnection() ");
                    sb.append(ps.getConnection());
                    sb.append(". Using internal SqlStringArray.");
                    response.log(sb.toString(), ite);
                    filter.createArrayMethod = null;
                }
            }
            boolean afterComma = false;
            StringBuffer sb = new StringBuffer(filter.arrayStart);
            for (String s: collection) {
                if (afterComma) {
                    sb.append(' ');
                }
                if (null == s) {
                    sb.append(filter.arrayElementNull);
                } else {
                    response.stringArrayElement(s, sb);
                }
                sb.append(',');
                afterComma = true;
            }
            if (afterComma) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append(filter.arrayEnd);
            //System.out.println(sb);
            ps.setArray(parameterIndex, new SqlStringArray
                        (collection, sb, filter.stringElementSqlType));
            return false;
        }
    }

    /**
     * Abstract class converts request and response values into
     * a CLOB column values representing a arrays of Strings
     * for insertion via JDBC into an access log.
     * Extended with abstract classes to insert CLOB String representation
     * of a potentially truncated array of Strings
     * into a slot of a PreparedStatement using <code>setObject()</code>.
     * These classes are realized with classes that obtain specific
     * information about each HTTP query using <code>getObject()</code>.
     */
    protected static abstract class DoArrayStringClob extends DoValue {

        /**
         * Protected constructor does nothing special for this abstract class,
         * which is expected to be realized with an anonymous class.
         */
        protected DoArrayStringClob () {
        }

        /**
         * Install a CLOB represnetation of possibly truncated array of String
         * values in a PreparedStatement to be sent to the database.
         * Returns false since the proposed value can always be handled.
         * @param response A FilterResponse, which augments the original
         * HttpServletResponse values with other information.
         * @param sqlInsertString A String used to form error messages
         * that refer to what was to be sent to the database.
         * @param ps The PreparedStatement that will receive a processed value.
         * @param parameterIndex The index, starting at one (1),
         * which corresponds to a PreparedStatement value slot.
         * @param obj A String Collection whose String representation
         * of an array of Strings is to be inserted into the PreparedStatement.
         * @param size A restriction on how many array elements are represented.
         * Positive values remove from the end, negative from the beginning.
         * @throws SQLException rarely when the PreparedStatement
         * cannot properly interact with the database during setup.
         */
        @Override public boolean setObject (FilterResponse response,
                                            String sqlInsertString,
                                            PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size)
            throws SQLException {

            @SuppressWarnings("unchecked") Collection<String> collection
                =  (Collection<String>) obj;
            if (0L < size) {
                if (collection.size() <= size) {
                    size = 0L;
                }
            } else if (0L > size) {
                if (collection.size() <= - size) {
                    size = 0L;
                } else {
                    size = - size - collection.size();
                }
            }
            StringBuffer sb = new StringBuffer
                ((collection.size() - size > 0L
                  ? (int) size : (int) -size) * 16);
            JDBCAccessLogFilter filter = response.getFilter();
            sb.append(filter.arrayStart);
            boolean afterComma = false;
            for (String s: collection) {
                if (0L > size) {
                    size++;
                    continue;
                }
                if (afterComma) {
                    sb.append(' ');
                }
                if (null == s) {
                    sb.append(filter.arrayElementNull);
                } else {
                    response.stringArrayElement(s, sb);
                }
                sb.append(',');
                afterComma = true;
                if (0L < size) {
                    if (0L == --size) {
                        break;
                    }
                }
            }
            if (afterComma) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append(filter.arrayEnd);
            char[] chars = new char[sb.length()];
            sb.getChars(0, sb.length(), chars, 0);
            ps.setClob(parameterIndex, new SerialClob(chars));
            return false;
        }
    }

    private static final Map<String, DoValue> COLUMN_DO_VALUE_DEFAULTS;
    static {
        Map<String, DoValue> m = new HashMap<String, DoValue>();
        m.put("attributenames", new DoArrayString() {
                public List<String> getObject (HttpServletRequest request,
                                               FilterResponse response) {
                    List<String> namesList = new LinkedList<String>();
                    for (@SuppressWarnings("unchecked")
                             Enumeration<String> namesEnum
                             = request.getAttributeNames();
                         namesEnum.hasMoreElements();) {
                        namesList.add(namesEnum.nextElement());
                    }
                    return namesList;
                }});
        m.put("authtype", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getAuthType();
                }});
        m.put("buffersize", new DoInteger() {
                public Integer getObject (HttpServletRequest request,
                                          FilterResponse response) {
                    int bs = response.getBufferSize();
                    return 0 == bs ? null : Integer.valueOf(bs);
                }});
        m.put("characterencoding", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getCharacterEncoding();
                }});
        m.put("contentcountlength", new DoLong() {
                public Long getObject (HttpServletRequest request,
                                       FilterResponse response)
                    throws ServletException {
                    Long box = response.getCount();
                    if (null != box) {
                        return box;
                    } 
                    box = response.getLongLength();
                    return null == box ? null
                        : 0 <= box.longValue() ? box : null;
                }});
        m.put("contentcountlengthinteger", new DoInteger() {
                public Integer getObject (HttpServletRequest request,
                                          FilterResponse response)
                    throws ServletException {

                    Long box = response.getCount();
                    if (null != box) {
                        long l = box.longValue();
                        return 0 < l && Integer.MAX_VALUE < l ? null
                            : Integer.valueOf(box.intValue());
                    }
                    Integer len = response.getIntegerLength();
                    return null == len ? null : len;
                }});
        m.put("contentlength", new DoInteger() {
                public Integer getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    int len = request.getContentLength();
                    return 0 <= len ? Integer.valueOf(len) : null;
                }});
        m.put("contenttype", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getContentType();
                }});
        m.put("contextpath", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getContextPath();
                }});
        m.put("elapsedmilli", new DoLong() {
                public Long getObject (HttpServletRequest request,
                                       FilterResponse response) {
                    return Long.valueOf(System.currentTimeMillis()
                                        - response.getCurrentTimeMillis());
                }});
        m.put("epochmilli", new DoLong() {
                public Long getObject (HttpServletRequest request,
                                       FilterResponse response) {
                    return Long.valueOf(response.getCurrentTimeMillis());
                }});
        m.put("epochmillikey", new DoLong() {
                public Long getObject (HttpServletRequest request,
                                       FilterResponse response) {
                    return Long.valueOf(response.getCurrentTimeMillis());
                }

                public boolean incrementable () {
                    return true;
                }

                public Long increment (PreparedStatement ps, int parameterIndex,
                                       Object obj, long size, int inc)
                    throws SQLException {

                    long currentTimeMillis = inc + ((Long) obj).longValue();
                    ps.setLong(parameterIndex, currentTimeMillis);
                    return Long.valueOf(currentTimeMillis);
                }});
        m.put("headerfirsts", new DoArrayString() {
                public List<String> getObject (HttpServletRequest request,
                                               FilterResponse response) {
                    @SuppressWarnings("unchecked") Enumeration<String>
                        namesEnum = request.getHeaderNames();
                    if (null == namesEnum) {
                        return null;
                    }
                    List<String> namesList = new LinkedList<String>();
                    while (namesEnum.hasMoreElements()) {
                        namesList.add
                            (request.getHeader(namesEnum.nextElement()));
                    }
                    return namesList;
                }});
        m.put("headerfirstsclob", new DoArrayStringClob() {
                public List<String> getObject (HttpServletRequest request,
                                               FilterResponse response) {
                    @SuppressWarnings("unchecked") Enumeration<String>
                        namesEnum = request.getHeaderNames();
                    if (null == namesEnum) {
                        return null;
                    }
                    List<String> namesList = new LinkedList<String>();
                    while (namesEnum.hasMoreElements()) {
                        namesList.add
                            (request.getHeader(namesEnum.nextElement()));
                    }
                    return namesList;
                }});
        m.put("headernames", new DoArrayString() {
                public List<String> getObject (HttpServletRequest request,
                                               FilterResponse response) {
                    @SuppressWarnings("unchecked") Enumeration<String>
                        namesEnum = request.getHeaderNames();
                    if (null == namesEnum) {
                        return null;
                    }
                    List<String> namesList = new LinkedList<String>();
                    while (namesEnum.hasMoreElements()) {
                        namesList.add(namesEnum.nextElement());
                    }
                    return namesList;
                }});
        m.put("headernamesclob", new DoArrayStringClob() {
                public List<String> getObject (HttpServletRequest request,
                                               FilterResponse response) {
                    @SuppressWarnings("unchecked") Enumeration<String>
                        namesEnum = request.getHeaderNames();
                    if (null == namesEnum) {
                        return null;
                    }
                    List<String> namesList = new LinkedList<String>();
                    while (namesEnum.hasMoreElements()) {
                        namesList.add(namesEnum.nextElement());
                    }
                    return namesList;
                }});
        m.put("idvalid", new DoBoolean() {
                public Boolean getObject (HttpServletRequest request,
                                          FilterResponse response) {
                    return Boolean.valueOf(request.isRequestedSessionIdValid());
                }});
        m.put("idfromcookie", new DoBoolean() {
                public Boolean getObject (HttpServletRequest request,
                                          FilterResponse response) {
                    return Boolean.valueOf
                        (request.isRequestedSessionIdFromCookie());
                }});
        m.put("idfromurl", new DoBoolean() {
                public Boolean getObject (HttpServletRequest request,
                                          FilterResponse response) {
                    return Boolean.valueOf
                        (request.isRequestedSessionIdFromURL());
                }});
        m.put("initparameternames", new DoArrayString() {
                public List<String> getObject (HttpServletRequest request,
                                               FilterResponse response) {
                    ServletContext servletContext
                        = response.getServletContext();
                    if (null == servletContext) {
                        JDBCAccessLogFilter filter = response.getFilter();
                        StringBuilder sb = filter.logStringBuilder
                            ("could not determine ServletContext for ",
                             "ServletContext.getInitParameterNames()");
                        sb.append(response);
                        response.log(sb);
                        return null;
                    }
                    List<String> namesList = new LinkedList<String>();
                    for (@SuppressWarnings("unchecked")
                             Enumeration<String> namesEnum
                             = servletContext.getInitParameterNames();
                         namesEnum.hasMoreElements();) {
                        namesList.add(namesEnum.nextElement());
                    }
                    return namesList;
                }});
        m.put("localaddr", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getLocalAddr();
                }});
        m.put("locale", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    Locale locale = response.getLocale();
                    return null == locale ? null : locale.toString();
                }});
        m.put("localedisplayname", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    Locale locale = response.getLocale();
                    return null == locale ? null : locale.getDisplayName();
                }});
        m.put("localname", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getLocalName();
                }});
        m.put("localport", new DoInteger() {
                public Integer getObject (HttpServletRequest request,
                                          FilterResponse response) {
                    return Integer.valueOf(request.getLocalPort());
                }});
        m.put("maxinactiveinterval", new DoInteger() {
                public Integer getObject (HttpServletRequest request,
                                          FilterResponse response) {
                    HttpSession session = request.getSession(false);
                    return null == session ? null
                        : Integer.valueOf(session.getMaxInactiveInterval());
                }});
        // "method" was a non-reserved word in SQL:1999
        // but reserved in SQL:2003 and SQL:2008.
        m.put("\"method\"", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getMethod();
                }});
        m.put("parameternames", new DoArrayString() {
                public List<String> getObject (HttpServletRequest request,
                                               FilterResponse response) {
                    List<String> namesList = new LinkedList<String>();
                    for (@SuppressWarnings("unchecked")
                             Enumeration<String> namesEnum
                             = request.getParameterNames();
                         namesEnum.hasMoreElements();) {
                        namesList.add(namesEnum.nextElement());
                    }
                    return namesList;
                }});
        // "path" was a reserved word in SQL:1999
        // and non-reserved in SQL:2003 and SQL:2008.
        m.put("pathinfo", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getPathInfo();
                }});
        m.put("pathtranslated", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getPathTranslated();
                }});
        m.put("principal", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    Principal principal = request.getUserPrincipal();
                    return null == principal ? null : principal.toString();
                }});
        m.put("protocol", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getProtocol();
                }});
        m.put("querystring", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getQueryString();
                }});
        m.put("querystringclob", new DoStringClob() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getQueryString();
                }});
        m.put("querytext", new DoText() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getQueryString();
                }});
        m.put("querytextclob", new DoTextClob() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getQueryString();
                }});
        m.put("referer", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getHeader("referer");
                }});
        m.put("refererclob", new DoStringClob() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getHeader("referer");
                }});
        m.put("referertext", new DoText() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getHeader("referer");
                }});
        m.put("referertextclob", new DoTextClob() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getHeader("referer");
                }});
        m.put("remoteaddr", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getRemoteAddr();
                }});
        m.put("remotehost", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getRemoteHost();
                }});
        m.put("remotehosttext", new DoText() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getRemoteHost();
                }});
        m.put("remoteport", new DoInteger() {
                public Integer getObject (HttpServletRequest request,
                                          FilterResponse response) {
                    return Integer.valueOf(request.getRemotePort());
                }});
        m.put("remoteuser", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getRemoteUser();
                }});
        m.put("requestedsessionid", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getRequestedSessionId();
                }});
        m.put("requesturi", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getRequestURI();
                }});
        m.put("requesturl", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    StringBuffer url = request.getRequestURL();
                    return null == url ? null : url.toString();
                }});
        m.put("responsecharacterencoding", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return response.getCharacterEncoding();
                }});
        m.put("responsecontentcount", new DoLong() {
                public Long getObject (HttpServletRequest request,
                                       FilterResponse response)
                    throws ServletException {

                    return response.getCount();
                }});
        m.put("responsecontentcountinteger", new DoInteger() {
                public Integer getObject (HttpServletRequest request,
                                          FilterResponse response)
                    throws ServletException {

                    Long box = response.getCount();
                    if (null == box) {
                        return null;
                    } 
                    long l = box.longValue();
                    return 0L <= l && Integer.MAX_VALUE < l
                        ? Integer.valueOf((int) l) : null;
                }});
        m.put("responsecontentlength", new DoLong() {
                public Long getObject (HttpServletRequest request,
                                       FilterResponse response)
                    throws ServletException {

                    return response.getLongLength();
                }});
        m.put("responsecontentlengthinteger", new DoInteger() {
                public Integer getObject (HttpServletRequest request,
                                          FilterResponse response)
                    throws ServletException {

                    return response.getIntegerLength();
                }});
        m.put("responsecontenttype", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return response.getContentType();
                }});
        m.put("responseheaderfirsts", new DoArrayString() {
                public List<String> getObject (HttpServletRequest request,
                                               FilterResponse response)
                    throws ServletException {

                    Collection<String> c = response.getStringHeaderNames();
                    if (null == c) {
                        return null;
                    }
                    List<String> namesList = new LinkedList<String>();
                    Iterator<String> iter = c.iterator();
                    while (iter.hasNext()) {
                        namesList.add(response.getStringHeader(iter.next()));
                    }
                    return namesList;
                }});
        m.put("responseheaderfirstsclob", new DoArrayStringClob() {
                public List<String> getObject (HttpServletRequest request,
                                               FilterResponse response)
                    throws ServletException {

                    Collection<String> c = response.getStringHeaderNames();
                    if (null == c) {
                        return null;
                    }
                    List<String> namesList = new LinkedList<String>();
                    Iterator<String> iter = c.iterator();
                    while (iter.hasNext()) {
                        namesList.add(response.getStringHeader(iter.next()));
                    }
                    return namesList;
                }});
        m.put("responseheadernames", new DoArrayString() {
                public Collection<String> getObject (HttpServletRequest request,
                                                     FilterResponse response)
                    throws ServletException {

                    return response.getStringHeaderNames();
                }});
        m.put("responseheadernamesclob", new DoArrayStringClob() {
                public Collection<String> getObject (HttpServletRequest request,
                                                     FilterResponse response)
                    throws ServletException {

                    return response.getStringHeaderNames();
                }});
        m.put("rootcause", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    Throwable throwable = response.getThrowable(request);
                    if (null == throwable) {
                        return null;
                    }
                    return rootCause(throwable).toString();
                }});
        m.put("rootcausetext", new DoText() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    Throwable throwable = response.getThrowable(request);
                    if (null == throwable) {
                        return null;
                    }
                    return rootCause(throwable).toString();
                }});
        m.put("rootcausetrace", new DoArrayString() {
                public Collection<String> getObject (HttpServletRequest request,
                                                     FilterResponse response) {
                    Throwable throwable = response.getThrowable(request);
                    if (null == throwable) {
                        return null;
                    }
                    List<String> trace = new LinkedList<String>();
                    traceThrowable(trace, rootCause(throwable));
                    return trace;
                }});
        m.put("rootcausetraceclob", new DoArrayStringClob() {
                public Collection<String> getObject (HttpServletRequest request,
                                                     FilterResponse response) {
                    Throwable throwable = response.getThrowable(request);
                    if (null == throwable) {
                        return null;
                    }
                    List<String> trace = new LinkedList<String>();
                    traceThrowable(trace, rootCause(throwable));
                    return trace;
                }});
        m.put("scheme", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getScheme();
                }});
        m.put("secure", new DoBoolean() {
                public Boolean getObject (HttpServletRequest request,
                                          FilterResponse response) {
                    return Boolean.valueOf(request.isSecure());
                }});
        m.put("serverinfo", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    ServletContext servletContext
                        = response.getServletContext();
                    if (null == servletContext) {
                        JDBCAccessLogFilter filter = response.getFilter();
                        StringBuilder sb = filter.logStringBuilder
                            ("could not determine ServletContext for ",
                             "ServletContext.getServerInfo()");
                        sb.append(response);
                        response.log(sb);
                        return null;
                    }
                    return servletContext.getServerInfo();
                }});
        m.put("serverinfotext", new DoText() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    ServletContext servletContext
                        = response.getServletContext();
                    if (null == servletContext) {
                        JDBCAccessLogFilter filter = response.getFilter();
                        StringBuilder sb = filter.logStringBuilder
                            ("could not determine ServletContext for Text of ",
                             "ServletContext.getServerInfo()");
                        sb.append(response);
                        response.log(sb);
                        return null;
                    }
                    return servletContext.getServerInfo();
                }});
        m.put("servername", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getServerName();
                }});
        m.put("serverport", new DoInteger() {
                public Integer getObject (HttpServletRequest request,
                                          FilterResponse response) {
                    int port = request.getServerPort();
                    return 0 < port ? Integer.valueOf(port) : null;
                }});
        m.put("servletattributenames", new DoArrayString() {
                public List<String> getObject (HttpServletRequest request,
                                               FilterResponse response) {
                    ServletContext servletContext
                        = response.getServletContext();
                    if (null == servletContext) {
                        JDBCAccessLogFilter filter = response.getFilter();
                        StringBuilder sb = filter.logStringBuilder
                            ("could not determine ServletContext for ",
                             "ServletContext.getAttributeNames()");
                        sb.append(response);
                        response.log(sb);
                        return null;
                    }
                    List<String> namesList = new LinkedList<String>();
                    for (@SuppressWarnings("unchecked")
                             Enumeration<String> namesEnum
                             = servletContext.getAttributeNames();
                         namesEnum.hasMoreElements();) {
                        namesList.add(namesEnum.nextElement());
                    }
                    return namesList;
                }});
        m.put("servletcontextname", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    ServletContext servletContext
                        = response.getServletContext();
                    if (null == servletContext) {
                        JDBCAccessLogFilter filter = response.getFilter();
                        StringBuilder sb = filter.logStringBuilder
                            ("could not determine ServletContext for ",
                             "ServletContext.getServletContextName()");
                        sb.append(response);
                        response.log(sb);
                        return null;
                    }
                    return servletContext.getServletContextName();
                }});
        m.put("servletcontextpath", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    ServletContext servletContext
                        = response.getServletContext();
                    if (null == servletContext) {
                        JDBCAccessLogFilter filter = response.getFilter();
                        StringBuilder sb = filter.logStringBuilder
                            ("could not determine ServletContext for ",
                             "ServletContext.getContextPath()");
                        sb.append(response);
                        response.log(sb);
                        return null;
                    }
                    return servletContext.getContextPath();
                }});
        m.put("servletcontextpathtext", new DoText() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    ServletContext servletContext
                        = response.getServletContext();
                    if (null == servletContext) {
                        JDBCAccessLogFilter filter = response.getFilter();
                        StringBuilder sb = filter.logStringBuilder
                            ("could not determine ServletContext for Text of ",
                             "ServletContext.getContextPath()");
                        sb.append(response);
                        response.log(sb);
                        return null;
                    }
                    return servletContext.getContextPath();
                }});
        m.put("servletmajorversion", new DoShort() {
                public Short getObject (HttpServletRequest request,
                                        FilterResponse response) {
                    ServletContext servletContext
                        = response.getServletContext();
                    if (null == servletContext) {
                        JDBCAccessLogFilter filter = response.getFilter();
                        StringBuilder sb = filter.logStringBuilder
                            ("could not determine ServletContext for ",
                             "ServletContext.getMajorVersion()");
                        sb.append(response);
                        response.log(sb);
                        return null;
                    }
                    return Short.valueOf
                        ((short) servletContext.getMajorVersion());
                }});
        m.put("servletminorversion", new DoShort() {
                public Short getObject (HttpServletRequest request,
                                        FilterResponse response) {
                    ServletContext servletContext
                        = response.getServletContext();
                    if (null == servletContext) {
                        JDBCAccessLogFilter filter = response.getFilter();
                        StringBuilder sb = filter.logStringBuilder
                            ("could not determine ServletContext for ",
                             "ServletContext.getMinorVersion()");
                        sb.append(response);
                        response.log(sb);
                        return null;
                    }
                    return Short.valueOf
                        ((short) servletContext.getMinorVersion());
                }});
        m.put("servletpath", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getServletPath();
                }});
        m.put("sessionattributenames", new DoArrayString() {
                public List<String> getObject (HttpServletRequest request,
                                               FilterResponse response) {
                    HttpSession session = request.getSession(false);
                    if (null == session) {
                        return null;
                    }
                    List<String> namesList = new LinkedList<String>();
                    for (@SuppressWarnings("unchecked")
                             Enumeration<String> namesEnum
                             = session.getAttributeNames();
                         namesEnum.hasMoreElements();) {
                        namesList.add(namesEnum.nextElement());
                    }
                    return namesList;
                }});
        m.put("sessioncreationtime", new DoTimestamp() {
                public Timestamp getObject (HttpServletRequest request,
                                            FilterResponse response) {
                    HttpSession session = request.getSession(false);
                    return null == session ? null
                        : new Timestamp(session.getCreationTime());
                }});
        m.put("sessionid", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    HttpSession session = request.getSession(false);
                    return null == session ? null : session.getId();
                }});
        m.put("sessionisnew", new DoBoolean() {
                public Boolean getObject (HttpServletRequest request,
                                          FilterResponse response) {
                    HttpSession session = request.getSession(false);
                    return null == session ? null
                        : Boolean.valueOf(session.isNew());
                }});
        m.put("status", new DoInteger() {
                public Integer getObject (HttpServletRequest request,
                                          FilterResponse response)
                    throws ServletException {

                    return response.getStatusInteger();
                }});
        m.put("throwable", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    Throwable throwable = response.getThrowable(request);
                    return null == throwable ? null : throwable.toString();
                }});
        m.put("throwabletext", new DoText() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    Throwable throwable = response.getThrowable(request);
                    return null == throwable ? null : throwable.toString();
                }});
        m.put("throwabletrace", new DoArrayString() {
                public Collection<String> getObject (HttpServletRequest request,
                                                     FilterResponse response) {
                    Throwable throwable = response.getThrowable(request);
                    if (null == throwable) {
                        return null;
                    }
                    List<String> trace = new LinkedList<String>();
                    traceThrowable(trace, throwable);
                    return trace;
                }});
        m.put("throwabletraceclob", new DoArrayStringClob() {
                public Collection<String> getObject (HttpServletRequest request,
                                                     FilterResponse response) {
                    Throwable throwable = response.getThrowable(request);
                    if (null == throwable) {
                        return null;
                    }
                    List<String> trace = new LinkedList<String>();
                    traceThrowable(trace, throwable);
                    return trace;
                }});
        m.put("\"timestamp\"", new DoTimestamp() {
                public Timestamp getObject (HttpServletRequest request,
                                            FilterResponse response) {
                    return new Timestamp(response.getCurrentTimeMillis());
                }});
        m.put("timestampkey", new DoTimestamp() {
                public Timestamp getObject (HttpServletRequest request,
                                            FilterResponse response) {
                    return new Timestamp(response.getCurrentTimeMillis());
                }

                public boolean incrementable () {
                    return true;
                }

                public Timestamp increment (PreparedStatement ps,
                                            int parameterIndex,
                                            Object obj, long size, int inc)
                    throws SQLException {

                    Timestamp currentTimeMillis = (Timestamp) obj;
                    currentTimeMillis
                        = new Timestamp(inc + currentTimeMillis.getTime());
                    ps.setTimestamp(parameterIndex, currentTimeMillis);
                    return currentTimeMillis;
                }});
        m.put("useragent", new DoString() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getHeader("user-agent");
                }});
        m.put("useragentclob", new DoStringClob() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getHeader("user-agent");
                }});
        m.put("useragenttext", new DoText() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getHeader("user-agent");
                }});
        m.put("useragenttextclob", new DoTextClob() {
                public String getObject (HttpServletRequest request,
                                         FilterResponse response) {
                    return request.getHeader("user-agent");
                }});
        COLUMN_DO_VALUE_DEFAULTS = Collections.unmodifiableMap(m);
    }

    private final Map<String, DoValue> columnDoValues
        = new HashMap<String, DoValue>(COLUMN_DO_VALUE_DEFAULTS);

    private static String canonize (String s) {
        if (null == s) {
            return null;
        }
        s = s.trim().toLowerCase();
        Matcher matcher = INIT_PARAM_NAME_START_PATTERN.matcher(s);
        if (! matcher.lookingAt()) {
            return null;
        }
        StringBuilder result = new StringBuilder(matcher.group(1));
        int last = matcher.end(1);
        int end = s.length();
        matcher.usePattern(INIT_PARAM_NAME_CONTINUATION_PATTERN);
        while (last < end && matcher.region(last, end).lookingAt()) {
            result.append(matcher.group(1));
            last = matcher.end(1);
        }
        return last != end ? null : result.toString();
    }

    private String sqlName (String s) {
        if (null == s) {
            return null;
        }
        s = s.trim().toLowerCase();
        Matcher matcher = SQL_NAME_START_PATTERN.matcher(s);
        if (! matcher.lookingAt()) {
            return null;
        }
        StringBuilder result = new StringBuilder(matcher.group(1));
        int last = matcher.end(1);
        int end = s.length();
        matcher.usePattern(SQL_NAME_CONTINUATION_PATTERN);
        while (last < end && matcher.region(last, end).lookingAt()) {
            result.append(matcher.group(1));
            last = matcher.end(1);
        }
        return last != end ? null : result.toString();
    }

    /**
     * Return a SQL acceptable column (field) name string from provided name.
     * @param name A name String to be converted into a SQL column name.
     * @param description A description of the String,
     * which will be included in any ServletException.
     * @throws ServletException When the name string <code>name</code>
     * does not match SQL column (field) name syntax.
     */
    protected String sqlName (String name, String description)
        throws ServletException {

        String column = sqlName(name);
        if (null == column) {
            StringBuilder sb = logStringBuilder(description, "sqlName()");
            sb.append(" column name \"");
            sb.append(name);
            sb.append("\" malformed as SQL field name.");
            throw new ServletException(sb.toString());
        }
        return column;
    }

    /**
     * Put a <code>DoValue</code> action for a column <code>name</code>
     * on the <code>columnDoValues</code> map.
     * The method is public to allow Filter and, maybe, Valve extensions,
     * which must be installed before initializing database columns.
     * @param name A lowercase string naming a column action.
     * @param doValue A DoValue instance that will do actions for a column,
     * which does not require further parameter handling.
     * @throws NullPointerException if either parameter is null.
     * @throws IllegalArgumentException if name is empty except for whitespace.
     */
    public void putColumnDoValues (String name, DoValue doValue) {
        if (null == name) {
            throw new NullPointerException
                ("putColumnDoValues name is null with doValue=" + doValue);
        }
        if (null == doValue) {
            throw new NullPointerException
                ("putColumnDoValues doValue is null with name=" + name);
        }
        name = name.trim();
        if (0 == name.length()) {
            throw new IllegalArgumentException
                ("putColumnDoValues name is empty with doValue=" + doValue);
        }
        columnDoValues.put(name, doValue);
    }

    private boolean allowCSVs = false;
    private final Set<String> noCSVs = new HashSet<String>();

    /**
     * Put a <code>ParamSetup</code> for an action <code>name</code>
     * on the <code>parameterSetups</code> map
     * and unconditionally disallow comma separated values (CSVs) for the name.
     * Such <code>name</code>s should be initialized at most once.
     * The method is public to allow Filter and Valve extensions,
     * which must be installed before initialization,
     * typically during construction.
     * @param name A lowercase string naming a setup action.
     * @param paramSetup A ParamSetup instance that will do actions
     * when the parameter is initialized.
     * @throws NullPointerException if either parameter is null.
     * @throws IllegalArgumentException if name is empty except for whitespace.
     */
    public void putNoCSVs (String name, ParamSetup paramSetup) {
        if (null == name) {
            throw new NullPointerException
                ("putNoCSVs name is null with paramSetup=" + paramSetup);
        }
        if (null == paramSetup) {
            throw new NullPointerException
                ("putNoCSVs paramSetup is null with name=" + name);
        }
        name = name.trim();
        if (0 == name.length()) {
            throw new IllegalArgumentException
                ("putNoCSVs name is empty with doValue=" + paramSetup);
        }
        noCSVs.add(name);
        parameterSetups.put(name, paramSetup);
    }

    /**
     * Put a <code>ParamSetup</code> for an action <code>name</code>
     * on the <code>parameterSetups</code> map
     * and conditionally allow comma separated values (CSVs) for the name
     * if the <code>allowCSVs</code> constructor parameter was true,
     * which may be the case for Valves but not for pure Filters.
     * Such <code>name</code>s should be initialized at most once.
     * The method is public to allow Filter and Valve extensions,
     * which must be installed before initialization,
     * typically during construction.
     * @param name A lowercase string naming a setup action.
     * @param paramSetup A ParamSetup instance that will do actions
     * when the parameter is initialized.
     * @throws NullPointerException if either parameter is null.
     * @throws IllegalArgumentException if name is empty except for whitespace.
     */
    public void putNofilterCSVs (String name, ParamSetup paramSetup) {
        if (null == name) {
            throw new NullPointerException
                ("putNofilterCSVs name is null with paramSetup="
                 + paramSetup);
        }
        if (null == paramSetup) {
            throw new NullPointerException
                ("putNofilterCSVs paramSetup is null with name=" + name);
        }
        name = name.trim();
        if (0 == name.length()) {
            throw new IllegalArgumentException
                ("putNofilterCSVs name is empty with doValue=" + paramSetup);
        }
        if (! allowCSVs) {
            noCSVs.add(name);
        }
        parameterSetups.put(name, paramSetup);
    }

    private final Set<String> noIndexing = new HashSet<String>();

    /**
     * Put a <code>ParamSetup</code> for an action <code>name</code>
     * on the <code>parameterSetups</code> map.
     * Do not add an index suffix to names that appear more than once.
     * Such names, like "columns", are combined into a single list
     * of comma separated values (CSVs).
     * The method is public to allow Filter and Valve extensions,
     * which must be installed before initialization,
     * typically during construction.
     * @param name A lowercase string naming a setup action.
     * @param paramSetup A ParamSetup instance that will do actions
     * when the parameter is initialized.
     * @throws NullPointerException if either parameter is null.
     * @throws IllegalArgumentException if name is empty except for whitespace.
     */
    public void putNoIndexing (String name, ParamSetup paramSetup) {
        if (null == name) {
            throw new NullPointerException
                ("putNoIndexing name is null with paramSetup=" + paramSetup);
        }
        if (null == paramSetup) {
            throw new NullPointerException
                ("putNoIndexing paramSetup is null with name=" + name);
        }
        name = name.trim();
        if (0 == name.length()) {
            throw new IllegalArgumentException
                ("putNoIndexing name is empty with doValue=" + paramSetup);
        }
        noIndexing.add(name);
        parameterSetups.put(name, paramSetup);
    }

    private Iterator<String> parseCSVs
        (String canonized,
         final String paramName,
         final String s,
         final ServletException[] exceptionHolder)
        throws ServletException {

        if (null == s) {
            return Collections.<String>emptyList().iterator();
        }
        if (noCSVs.contains(canonized)) {
            return Collections.<String>singletonList(s.trim()).iterator();
        }
        return new Iterator<String>() {
            boolean checkNext = true;
            Matcher matcher = CSV_PATTERN.matcher(s);
            int last = 0;
            int end = s.length();
            String nextMatch = null;

            public boolean hasNext () {
                if (checkNext) {
                    checkNext = false;
                    nextMatch = last < end
                        && matcher.region(last, end).lookingAt()
                        ? matcher.group(1) : null;
                }
                if (null == nextMatch || 0 == nextMatch.length()) {
                    if (last != end) {
                        StringBuilder sb = logStringBuilder
                            ("failed to parse last part \"", "parseCSVs()");
                        sb.append(s.substring(last, end));
                        sb.append("\" of init-value \"");
                        sb.append(s);
                        sb.append("\" of init-param ");
                        sb.append(paramName);
                        exceptionHolder[0]
                            = new ServletException(sb.toString());
                    }
                    return false;
                }
                return true;
            }

            public String next () {
                if (! hasNext()) {
                    throw new NoSuchElementException
                        ("parseCSVs Iterator next() without hasNext()"
                        + " failed on init-param " + paramName);
                }
                last = matcher.end();
                checkNext = true;
                return nextMatch;
            }

            public void remove () {
                throw new UnsupportedOperationException
                    ("parseCSVs Iterator cannot remove.");
            }
        };
    }

    /**
     * Default Filter constructor for stand-alone Filter using reflection
     * and not accepting comma separated values except for column assignments
     * and throwableattribute names.
     */
    public JDBCAccessLogFilter () {
        this(false, true);
    }

    /**
     * Constructor for Valves and overriding Filters.
     * @param allowCSVs Flag for handling init-param values,
     * which might accept more than one instance,
     * to parse comma-separated values, which may also have whitespace.
     * @param needMethods Flag to have the first FilterResponse instance
     * use reflection to find unadvertised Methods.
     */
    public JDBCAccessLogFilter (boolean allowCSVs, boolean needMethods) {
        this.allowCSVs = allowCSVs;
        this.needMethods = needMethods;
        // Connection setup parameters
        putNoCSVs("datasource", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    value = value.trim();
                    if (0 < value.length()) {
                        dsName = value;
                    }
                }});
        putNoCSVs("connectionurl", new ParamSetup() {
                public void setup (String name, String value) {
                    connectionURL = value.trim();
                    if (0 == connectionURL.length()) {
                        connectionURL = null;
                    }
                }});
        putNoCSVs("username", new ParamSetup() {
                public void setup (String name, String value) {
                    userName = value.trim();
                    if (0 == userName.length()) {
                        userName = null;
                    }
                }});
        putNoCSVs("password", new ParamSetup() {
                public void setup (String name, String value) {
                    password = value.trim();
                    if (0 == password.length()) {
                        password = null;
                    }
                }});
        putNoCSVs("driverclass", new ParamSetup() {
                public void setup (String name, String value) {
                    driverClassName = value.trim();
                    if (0 == driverClassName.length()) {
                        driverClassName = null;
                    } else {
                    }
                }});
        // Processing parameters
        putNoCSVs("arraybegindelimiter", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    if (null == value) {
                        return;
                    }
                    arrayStart = value.trim();
                }});
        putNoCSVs("arrayenddelimiter", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    if (null == value) {
                        return;
                    }
                    arrayEnd = value.trim();
                }});
        putNoCSVs("arrayelementnull", new ParamSetup() {
                public void setup (String name, String value) {
                    if (null == value) {
                        return;
                    }
                    arrayElementNull = value.trim();
                }});
        putNoCSVs("ellipsisstring", new ParamSetup() {
                public void setup (String name, String value) {
                    if (null == value) {
                        return;
                    }
                    ellipsisString = value.trim();
                }});
        putNoCSVs("internalstringarray", new ParamSetup() {
                public void setup (String name, String value) {
                    if (null != value && FALSE_STRINGS.contains
                        (value.trim().toLowerCase())) {
                        internalStringArray = false;
                        return;
                    }
                    internalStringArray = true;
                }});
        putNoCSVs("lineseparator", new ParamSetup() {
                public void setup (String name, String value) {
                    if (null == value) {
                        return;
                    }
                    String val = value.trim();
                    if (0 == val.length()) {
                        return;
                    }
                    lineSeparator = val;
                }});
        putNoCSVs("stringelementsqltype", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    if (null != value) {
                        value = value.trim();
                    }
                    if (null == value || 0 == value.length()) {
                        StringBuilder sb = logStringBuilder
                            (" element SQL type parameter"
                             + " of String array or Collection ", "init()");
                        sb.append("may not be null or emtpy");
                        throw new ServletException(sb.toString());
                    }
                    stringElementSqlType = value;
                }});
        putNoCSVs("tablename", new ParamSetup() {
                public void setup (String name, String value) {
                    if (null == value) {
                        return;
                    }
                    String tableName = value.trim();
                    if (0 < tableName.length()) {
                        JDBCAccessLogFilter.this.tableName = tableName;
                    }
                }});
        parameterSetups.put("throwableattribute", new ParamSetup() {
                public void setup (String name, String value) {
                    if (null == value) {
                        return;
                    }
                    value = value.trim();
                    if (0 == value.length()) {
                        return;
                    }
                    throwableAttributes.add(value);
                }});
        putNoCSVs("uniqueviolationsqlstate", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    if (null == value) {
                        return;
                    }
                    String val = value.trim();
                    if (0 == val.length()) {
                        return;
                    }
                    if (5 != val.length()) {
                        StringBuilder sb = logStringBuilder
                            ("unique violation SQLState parameter ", "init()");
                        sb.append(value);
                        sb.append(" not 5 characters and ignored.");
                        throw new ServletException(sb.toString());
                    }
                    uniqueViolation = val;
                }});
        putNoCSVs("uniqueviolationretries", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    if (null == value) {
                        return;
                    }
                    String val = value.trim();
                    if (0 == val.length()) {
                        return;
                    }
                    try {
                        uniqueViolationRetries = Byte.parseByte(val);
                    } catch (NumberFormatException nfe) {
                        StringBuilder sb = logStringBuilder
                            ("unique violation retries parameter byte ",
                            "init()");
                        sb.append(value);
                        sb.append(" malformed.");
                        throw new ServletException(sb.toString(), nfe);
                    }
                }});
        // Parameterized SQL column producers
        putNofilterCSVs("attributearraystring", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "request array attribute");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoArrayString() {
                            @SuppressWarnings("unchecked")
                                public List<String> getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                Object obj = request.getAttribute(header);
                                if (null == obj) {
                                    return null;
                                }
                                if (obj.getClass().isArray()) {
                                    List<String> namesList
                                        = new LinkedList<String>();
                                    for (Object member: (Object[]) obj) {
                                        namesList.add(null == member ? null
                                                      : member.toString());
                                    }
                                    return namesList;
                                } else if (obj instanceof Iterable) {
                                    List<String> namesList
                                        = new LinkedList<String>();
                                    for (Object member:
                                             (Iterable<Object>) obj) {
                                        namesList.add(null == member ? null
                                                      : member.toString());
                                    }
                                    return namesList;
                                } else if (obj instanceof Enumeration) {
                                    List<String> namesList
                                        = new LinkedList<String>();
                                    for (@SuppressWarnings("rawtypes")
                                             Enumeration namesEnum
                                             = (Enumeration) obj;
                                         namesEnum.hasMoreElements();) {
                                        Object member
                                            = namesEnum.nextElement();
                                        namesList.add
                                            (null == member ? null
                                             : member.toString());
                                    }
                                    return namesList;
                                }
                                return null;
                            }});
                }});
        putNofilterCSVs("attributearraystringclob", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column
                        = sqlName(name, "request array CLOB attribute");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoArrayStringClob() {
                            @SuppressWarnings("unchecked")
                                public List<String> getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                Object obj = request.getAttribute(header);
                                if (null == obj) {
                                    return null;
                                }
                                if (obj.getClass().isArray()) {
                                    List<String> namesList
                                        = new LinkedList<String>();
                                    for (Object member: (Object[]) obj) {
                                        namesList.add(null == member ? null
                                                      : member.toString());
                                    }
                                    return namesList;
                                } else if (obj instanceof Iterable) {
                                    List<String> namesList
                                        = new LinkedList<String>();
                                    for (Object member:
                                             (Iterable<Object>) obj) {
                                        namesList.add(null == member ? null
                                                      : member.toString());
                                    }
                                    return namesList;
                                } else if (obj instanceof Enumeration) {
                                    List<String> namesList
                                        = new LinkedList<String>();
                                    for (@SuppressWarnings("rawtypes")
                                             Enumeration namesEnum
                                             = (Enumeration) obj;
                                         namesEnum.hasMoreElements();) {
                                    	Object member = namesEnum.nextElement();
                                        namesList.add
                                            (null == member ? null
                                             : member.toString());
                                    }
                                    return namesList;
                                }
                                return null;
                            }});
                }});
        putNofilterCSVs("attributestring", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "request attribute");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoString() {
                            public String getObject (HttpServletRequest request,
                                                     FilterResponse response) {
                                Object obj = request.getAttribute(header);
                                if (null == obj) {
                                    return null;
                                }
                                return obj.toString();
                            }});
                }});
        putNofilterCSVs("attributestringclob", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "CLOB request");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoStringClob() {
                            public String getObject (HttpServletRequest request,
                                                     FilterResponse response) {
                                Object obj = request.getAttribute(header);
                                if (null == obj) {
                                    return null;
                                }
                                return obj.toString();
                            }});
                }});
        putNofilterCSVs("attributetext", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "request text attribute");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoText() {
                            public String getObject (HttpServletRequest request,
                                                     FilterResponse response) {
                                Object obj = request.getAttribute(header);
                                if (null == obj) {
                                    return null;
                                }
                                return obj.toString();
                            }});
                }});
        putNofilterCSVs("attributetextclob", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column
                        = sqlName(name, "request text CLOB attribute");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoTextClob() {
                            public String getObject (HttpServletRequest request,
                                                     FilterResponse response) {
                                Object obj = request.getAttribute(header);
                                if (null == obj) {
                                    return null;
                                }
                                return obj.toString();
                            }});
                }});
        putNofilterCSVs("dateheader", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "date header attribute");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoLong() {
                            public Long getObject (HttpServletRequest request,
                                                   FilterResponse response) {
                                try {
                                    // Unlike most unknown value returns,
                                    // may return -1L to distinguish
                                    // the return value from null,
                                    // which signifies a bad format.
                                    return request.getDateHeader(header);
                                } catch (IllegalArgumentException iae) {
                                    return null;
                                }
                            }});
                }});
        putNofilterCSVs("dateheadertimestamp", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "date header attribute");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoTimestamp() {
                            public Timestamp getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                try {
                                    // Unlike most unknown value returns,
                                    // may return -1L to distinguish
                                    // the return value from null,
                                    // which signifies a bad format.
                                    return new Timestamp
                                        (request.getDateHeader(header));
                                } catch (IllegalArgumentException iae) {
                                    return null;
                                }
                            }});
                }});
        putNofilterCSVs("header", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "header request");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoString() {
                            public String getObject (HttpServletRequest request,
                                                     FilterResponse response) {
                                return request.getHeader(header);
                            }});
                }});
        putNofilterCSVs("headers", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "headers request");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoArrayString() {
                            public List<String> getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                List<String> namesList
                                    = new LinkedList<String>();
                                for (@SuppressWarnings("unchecked")
                                         Enumeration<String> namesEnum
                                         = request.getHeaders(header);
                                     namesEnum.hasMoreElements();) {
                                    namesList.add(namesEnum.nextElement());
                                }
                                return namesList;
                            }});
                }});
        putNofilterCSVs("headersclob", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "headers CLOB request");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoArrayStringClob() {
                            public List<String> getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                List<String> namesList
                                    = new LinkedList<String>();
                                for (@SuppressWarnings("unchecked")
                                         Enumeration<String> namesEnum
                                         = request.getHeaders(header);
                                     namesEnum.hasMoreElements();) {
                                    namesList.add(namesEnum.nextElement());
                                }
                                return namesList;
                            }});
                }});
        putNofilterCSVs("initparameter", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column
                        = sqlName(name, "ServletContext init parameter");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoString() {
                            public String getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                ServletContext servletContext
                                    = response.getServletContext();
                                if (null == servletContext) {
                                    JDBCAccessLogFilter filter
                                        = response.getFilter();
                                    StringBuilder sb = filter.logStringBuilder
                                        ("could not determine ServletContext ",
                                         "ServletContext.getInitParameter()");
                                    sb.append(response);
                                    response.log(sb);
                                    return null;
                                }
                                return servletContext.getInitParameter(header);
                            }});
                }});
        putNofilterCSVs("initparametertext", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column
                        = sqlName(name, "ServletContext init parameter");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoText() {
                            public String getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                ServletContext servletContext
                                    = response.getServletContext();
                                if (null == servletContext) {
                                    JDBCAccessLogFilter filter
                                        = response.getFilter();
                                    StringBuilder sb = filter.logStringBuilder
                                        ("could not determine ServletContext"
                                         + " for Text of",
                                         "ServletContext.getInitParameter()");
                                    sb.append(response);
                                    response.log(sb);
                                    return null;
                                }
                                return servletContext.getInitParameter(header);
                            }});
                }});
        putNofilterCSVs("intheader", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "date header attribute");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoInteger() {
                            public Integer getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                try {
                                    return Integer.valueOf
                                        (request.getIntHeader(header));
                                } catch (NumberFormatException nfe) {
                                    return null;
                                }
                            }});
                }});
        putNofilterCSVs("literal", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "literal");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoString() {
                            public String getObject (HttpServletRequest request,
                                                     FilterResponse response) {
                                return header;
                            }});
                }});
        putNofilterCSVs("literalboolean", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "literal boolean");
                    if (null == value) {
                        return;
                    }
                    value = value.trim();
                    if (0 == value.length()) {
                        return;
                    }
                    String v = value.toLowerCase();
                    final Boolean header = TRUE_STRINGS.contains(v)
                        ? Boolean.TRUE : FALSE_STRINGS.contains(v)
                        ? Boolean.FALSE : null;
                    if (null != header) {
                        putColumnDoValues(column, new DoBoolean() {
                                public Boolean getObject
                                    (HttpServletRequest request,
                                     FilterResponse response) {
                                    return header;
                                }});
                    } else {
                        StringBuilder sb = logStringBuilder
                            ("Boolean literal value ", "init()");
                        sb.append(value);
                        sb.append(" malformed.");
                        throw new ServletException(sb.toString());
                    }
                }});
        putNofilterCSVs("literalshort", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "literal short");
                    if (null == value) {
                        return;
                    }
                    value = value.trim();
                    if (0 == value.length()) {
                        return;
                    }
                    try {
                        final Short header = Short.valueOf(value);
                        putColumnDoValues(column, new DoShort() {
                                public Short getObject
                                    (HttpServletRequest request,
                                     FilterResponse response) {
                                    return header;
                                }});
                    } catch (NumberFormatException nfe) {
                        StringBuilder sb = logStringBuilder
                            ("Short literal value ", "init()");
                        sb.append(value);
                        sb.append(" malformed.");
                        throw new ServletException(sb.toString(), nfe);
                    }
                }});
        putNofilterCSVs("literalinteger", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "literal integer");
                    if (null == value) {
                        return;
                    }
                    value = value.trim();
                    if (0 == value.length()) {
                        return;
                    }
                    try {
                        final Integer header = Integer.valueOf(value);
                        putColumnDoValues(column, new DoInteger() {
                                public Integer getObject
                                    (HttpServletRequest request,
                                     FilterResponse response) {
                                    return header;
                                }});
                    } catch (NumberFormatException nfe) {
                        StringBuilder sb = logStringBuilder
                            ("Integer literal value ", "init()");
                        sb.append(value);
                        sb.append(" malformed.");
                        throw new ServletException(sb.toString(), nfe);
                    }
                }});
        putNofilterCSVs("literallong", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "literal long");
                    if (null == value) {
                        return;
                    }
                    value = value.trim();
                    if (0 == value.length()) {
                        return;
                    }
                    try {
                        final Long header = Long.valueOf(value);
                        putColumnDoValues(column, new DoLong() {
                                public Long getObject
                                    (HttpServletRequest request,
                                     FilterResponse response) {
                                    return header;
                                }});
                    } catch (NumberFormatException nfe) {
                        StringBuilder sb = logStringBuilder
                            ("Long literal value ", "init()");
                        sb.append(value);
                        sb.append(" malformed.");
                        throw new ServletException(sb.toString(), nfe);
                    }
                }});
        putNofilterCSVs("parameter", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column
                        = sqlName(name, "ServletContext init parameter");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoString() {
                            public String getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                return request.getParameter(header);
                            }});
                }});
        putNofilterCSVs("parametertext", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column
                        = sqlName(name, "ServletContext init parameter");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoText() {
                            public String getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                return request.getParameter(header);
                            }});
                }});
        putNofilterCSVs("parametervalues", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "headers request");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoArrayString() {
                            public List<String> getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                String[] vals
                                    = request.getParameterValues(header);
                                return null == vals ? null
                                    : Arrays.asList(vals);
                            }});
                }});
        putNofilterCSVs("parametervaluesclob", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "headers request");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoArrayStringClob() {
                            public List<String> getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                String[] vals
                                    = request.getParameterValues(header);
                                return null == vals ? null
                                    : Arrays.asList(vals);
                            }});
                }});
        putNofilterCSVs("responseheader", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "header response");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoString() {
                            public String getObject (HttpServletRequest request,
                                                     FilterResponse response)
                                throws ServletException {

                                return response.getStringHeader(header);
                            }});
                }});
        putNofilterCSVs("responseheaders", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "headers response");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoArrayString() {
                            public Collection<String> getObject
                                (HttpServletRequest request,
                                 FilterResponse response)
                                throws ServletException {

                                return response.getStringHeaders(header);
                            }});
                }});
        putNofilterCSVs("responseheadersclob", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "headers CLOB response");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoArrayStringClob() {
                            public Collection<String> getObject
                                (HttpServletRequest request,
                                 FilterResponse response)
                                throws ServletException {

                                return response.getStringHeaders(header);
                            }});
                }});
        putNofilterCSVs("servletattributestring", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "servlet attribute");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoString() {
                            public String getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                ServletContext servletContext
                                    = response.getServletContext();
                                if (null == servletContext) {
                                    JDBCAccessLogFilter filter
                                        = response.getFilter();
                                    StringBuilder sb = filter.logStringBuilder
                                        ("could not determine ServletContext",
                                         "ServletContext.getAttribute()");
                                    sb.append(response);
                                    response.log(sb);
                                    return null;
                                }
                                Object obj
                                    = servletContext.getAttribute(header);
                                if (null == obj) {
                                    return null;
                                }
                                return obj.toString();
                            }});
                }});
        putNofilterCSVs("sessionattributestring", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "session attribute");
                    if (null == value) {
                        return;
                    }
                    final String header = value.trim();
                    if (0 == header.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoString() {
                            public String getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                HttpSession session
                                    = request.getSession(false);
                                if (null == session) {
                                    return null;
                                }
                                Object obj = session.getAttribute(header);
                                if (null == obj) {
                                    return null;
                                }
                                return obj.toString();
                            }});
                }});
        putNofilterCSVs("userinrole", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "init parameter");
                    if (null == value) {
                        return;
                    }
                    final String role = value.trim();
                    if (0 == role.length()) {
                        return;
                    }
                    putColumnDoValues(column, new DoBoolean() {
                            public Boolean getObject
                                (HttpServletRequest request,
                                 FilterResponse response) {
                                try {
                                    return Boolean.valueOf
                                        (request.isUserInRole(role));
                                } catch (Exception e) {
                                    // A database might fail 
                                    response.log("request.isUserInRole("
                                                 + role + ")", e);
                                    return Boolean.FALSE;
                                }
                            }});
                }});
        // Column assignments from access values
        putNoIndexing("col", new ParamSetup() {
                public void setup (String name, String value)
                    throws ServletException {

                    String column = sqlName(name, "column group");
                    if (null == value) {
                        return;
                    }
                    final String groupings = value.trim();
                    if (0 == groupings.length()) {
                        return;
                    };
                    List<String[]> list = columnGroups.get(column);
                    if (null == list) {
                        list = new LinkedList<String[]>();
                        columnGroups.put(column, list);
                    }
                    Matcher matcher = ASSIGN_PATTERN.matcher(value);
                    if (! matcher.matches()) {
                        StringBuilder sb
                            = new StringBuilder("column group name \"");
                        sb.append(name);
                        sb.append("\" assignment ");
                        sb.append(1 + list.size());
                        sb.append(" string \"");
                        sb.append(value);
                        sb.append("\" cannot be parsed. Skipped.");
                        log(sb);
                        return;
                    }
                    String[] parts = {
                        matcher.group(1), // &quot;
                        matcher.group(2), // [a-zA-Z0-9_]
                        matcher.group(3), // &quot;
                        matcher.group(4), // ([{<
                        matcher.group(5), // [-0-9]
                        matcher.group(6), // >}])
                        matcher.group(7), // =
                        matcher.group(8), // &quot;
                        matcher.group(9), // [a-zA-Z0-9_]
                        matcher.group(10), // &quot;
                        matcher.group(11), // ([{<
                        matcher.group(12), // [-0-9]
                        matcher.group(13),  // >}])
                    };
                    for (int i = 0; i < parts.length; i++) {
                        if (null == parts[i]) {
                            parts[i] = "";
                        }
                    }
                    list.add(parts);
                }});
        putNoIndexing("cols", parameterSetups.get("col"));
        putNoIndexing("column", parameterSetups.get("col"));
        putNoIndexing("columns", parameterSetups.get("col"));
        // Debugging
        putNoCSVs("debug", new ParamSetup() {
                public void setup (String name, String value) {
                    if (null != value && FALSE_STRINGS.contains
                        (value.trim().toLowerCase())) {
                        setDebugEnabled(false);
                        return;
                    }
                    setDebugEnabled(true);
                }});
    }

    private void configStringBuilder (StringBuilder sb) {
        for (@SuppressWarnings("unchecked") Enumeration<String> paramNameEnum
                 = filterConfig.getInitParameterNames();
             paramNameEnum.hasMoreElements();) {
            String paramName = paramNameEnum.nextElement();
            String canonized = canonize(paramName);
            if (null != canonized && (noIndexing.contains(canonized)
                                      || ! noCSVs.contains(canonized))) {
                continue;
            }
            sb.append(lineSeparator);
            sb.append(paramName);
            sb.append('=');
            sb.append(filterConfig.getInitParameter(paramName));
        }
    }

    private void connect () throws ServletException {
        if (null != dsName) {
            InitialContext ctx = null;
            try {
                ctx = new InitialContext();
                ds = (DataSource) ctx.lookup(dsName);
                conn = ds.getConnection();
            } catch (javax.naming.NamingException e) {
                StringBuilder sb = logStringBuilder
                ("could not lookup DataSource specified as \"", "connect()");
                sb.append(dsName);
                sb.append("\"");
                sb.append(" from Context ");
                sb.append(ctx);
                if (null != ctx) {
                    sb.append(lineSeparator);
                    sb.append("InitialContext Environment ");
                    try {
                        Hashtable<?,?> ht = ctx.getEnvironment();
                        if (ht.isEmpty()) {
                            sb.append("is empty.");
                        } else {
                            sb.append("property pairs:");
                            for (Object k: ht.keySet()) {
                                sb.append(lineSeparator);
                                sb.append(k);
                                sb.append("=");
                                sb.append(ht.get(k));
                            }
                        }
                        sb.append(lineSeparator);
                        sb.append("InitialContext Bindings:");
                        NamingEnumeration<Binding> bs = ctx.listBindings("");
                        while (bs.hasMore()) {
                            Binding b = bs.next();
                            sb.append(lineSeparator);
                            sb.append(b);
                        }
                        sb.append(lineSeparator);
                    } catch (javax.naming.NamingException ne) {
                        sb.append(lineSeparator);
                        sb.append("InitialContext debugging Exception: ");
                        sb.append(ne);
                        sb.append(lineSeparator);
                    }
                }
                log(sb, e);
            } catch (SQLException e) {
                ds = null;
                StringBuilder sb
                    = logStringBuilder("Datasource named ", "connect()");
                sb.append(dsName);
                sb.append(" provided no Connection ");
                log(sb, e);
            }
        }
        if (null == conn && null != connectionURL) {
            Properties connectionProperties
                = new Properties(System.getProperties());
            if (null != userName) {
                connectionProperties.setProperty("user", userName);
            }
            if (null != password) {
                connectionProperties.setProperty("password", password);
            }
            if (null != driverClassName) {
                try {
                    Class.forName(driverClassName);
                } catch (ClassNotFoundException e) {
                    StringBuilder sb = logStringBuilder
                        (" failed to find JDBC Driver class for ", "connect()");
                    sb.append(driverClassName);
                    throw new ServletException(sb.toString(), e);
                }
            }
            try {
                conn = DriverManager.getConnection(connectionURL,
                                                   connectionProperties);
            } catch (SQLException e) {
                StringBuilder sb = logStringBuilder
                    (" failed to get connection from DriverManager",
                     "connect()");
                if (null != driverClassName) {
                    sb.append(" with Driver named \"");
                    sb.append(driverClassName);
                    sb.append('"');
                }
                if (null != userName) {
                    sb.append(" for user ");
                    sb.append(userName);
                }
                sb.append('.');
                UnavailableException ue
                    = new UnavailableException(sb.toString());
                ue.initCause(e);
                throw ue;
            }
        }
        if (null == conn) {
            StringBuilder sb = logStringBuilder
                ("got no JDBC connection with parameters:", "connect()");
            configStringBuilder(sb);
            throw new UnavailableException(sb.toString());
        }
        findCreateArrayMethod(conn);
        if (null != ds) {
            try {
                conn.close();
            } catch (SQLException e) {
                ds = null;
                StringBuilder sb
                    = logStringBuilder("Datasource named ", "connect()");
                sb.append(dsName);
                sb.append(" provided no connection ");
                UnavailableException ue
                    = new UnavailableException(sb.toString());
                ue.initCause(e);
                throw ue;
            } finally {
                conn = null;
            }
        }
    }

    private boolean matchQuotes (String first, String second,
                                 String columnGroupName, int i)
        throws ServletException {

        boolean noFirst = 0 == first.length();
        boolean noSecond = 0 == second.length();
        if (noFirst && noSecond) {
            return true;
        }
        if (noFirst || noSecond) {
            StringBuilder sb
                = logStringBuilder("column group name \"", "matchQuotes()");
            sb.append(columnGroupName);
            sb.append("\", assignment ");
            sb.append(i);
            sb.append(", missing quote character for '");
            sb.append(first);
            sb.append(second);
            sb.append("'.");
            throw new ServletException(sb.toString());
        }
        if (first.equals(second)) {
            return true;
        }
        StringBuilder sb = logStringBuilder("column group name \"",
                                            "matchQuotes()");
        sb.append(columnGroupName);
        sb.append("\", assignment ");
        sb.append(i);
        sb.append(", first quote character '");
        sb.append(first);
        sb.append("' does not match '");
        sb.append(second);
        sb.append("' quote character.");
        throw new ServletException(sb.toString());
    }

    private boolean matchGroupers (String first, String second,
                                   String columnGroupName, int i)
        throws ServletException {

        boolean noFirst = 0 == first.length();
        boolean noSecond = 0 == second.length();
        if (noFirst && noSecond) {
            return true;
        }
        if (noFirst || noSecond) {
            StringBuilder sb
                = logStringBuilder("column group name \"", "matchGroupers()");
            sb.append(columnGroupName);
            sb.append("\", assignment ");
            sb.append(i);
            sb.append(", missing enclosing character for '");
            sb.append(first);
            sb.append(second);
            sb.append("'.");
            throw new ServletException(sb.toString());
        }
        int firstIndex = first.indexOf("([{<");
        int secondIndex = second.indexOf(">}])");
        if (firstIndex == secondIndex) {
            return true;
        }
        StringBuilder sb
            = logStringBuilder("column group name \"", "matchGroupers()");
        sb.append(columnGroupName);
        sb.append("\", assignment ");
        sb.append(i);
        sb.append(", first group beginning character '");
        sb.append(first);
        sb.append("' does not match second ending grouping character'");
        sb.append(second);
        sb.append("'.");
        throw new ServletException(sb.toString());
    }

    private Long parseSize (String sizeString, String delimitor,
                            String columnGroupName, int i)
        throws ServletException {

        if (null == delimitor || 0 == delimitor.length()) {
            if (null == sizeString || 0 == sizeString.length()
                || "-".equals(sizeString)) {
                return null;
            }
            StringBuilder sb
                = logStringBuilder("column group name \"", "parseSize()");
            sb.append(columnGroupName);
            sb.append("\", assignment ");
            sb.append(i);
            sb.append(", size string \"");
            sb.append(sizeString);
            sb.append("\" not delimited. Parsing size string anyway.");
            log(sb);
        }
        if (null != sizeString && 0 != sizeString.length()) {
            if ("-".equals(sizeString)) {
                return null;
            }
            try {
                return Long.valueOf(sizeString);
            } catch (NumberFormatException nfe) {
                StringBuilder sb
                    = logStringBuilder("column group name \"", "parseSize()");
                sb.append(columnGroupName);
                sb.append("\", assignment ");
                sb.append(i);
                sb.append(", size value string \"");
                sb.append(sizeString);
                sb.append("\" cannot be parsed as long. Not using it.");
                throw new ServletException(sb.toString(), nfe);
            }
        }
        return Long.valueOf(0L);
    }

    private StringBuilder initStringBuilder (String columnGroupName, int i,
                                             String target) {
        StringBuilder sb = logStringBuilder("column group name \"", "init()");
        sb.append(columnGroupName);
        sb.append(", assignment ");
        sb.append(i);
        sb.append(", to potential database column ");
        sb.append(target);
        return sb.append(": ");
    }

    /**
     * @see javax.servlet.Filter#init(FilterConfig)
     */
    public void init (FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        filterName = filterConfig.getFilterName();
        ServletContext servletContext = filterConfig.getServletContext();
        if (null != servletContext) {
            this.servletContext = servletContext;
        }
        for (@SuppressWarnings("unchecked") Enumeration<String> paramNameEnum
                 = filterConfig.getInitParameterNames();
             paramNameEnum.hasMoreElements();) {
            String paramName = paramNameEnum.nextElement();
            String canonized = canonize(paramName);
            String paramValue = filterConfig.getInitParameter(paramName);
            if (0 == paramName.length()) {
                if (null == paramValue || 0 == paramValue.trim().length()) {
                    continue;
                }
                StringBuilder sb
                    = logStringBuilder("init parameter without a name \""
                                        + " but with non-empty value '",
                                       "init()");
                sb.append(paramValue);
                sb.append("' ignored.");
                log(sb);
                continue;
            }
            if (null == canonized) {
                StringBuilder sb
                    = logStringBuilder("init parameter name \"", "init()");
                sb.append(paramName);
                sb.append("\" malformed and ignored.");
                log(sb);
                continue;
            }
            ParamSetup paramSetup = parameterSetups.get(canonized);
            if (null == paramSetup) {
                StringBuilder sb
                    = logStringBuilder("init parameter name \"", "init()");
                sb.append(paramName);
                sb.append("\" is not recognized.");
                log(sb);
                continue;
            }
            final ServletException[] exceptionHolder = { null };
            int index = 0;
            Iterator<String> iter = parseCSVs(canonized, paramName, paramValue,
                                              exceptionHolder);
            while (iter.hasNext()) {
                paramSetup.setup(0 < index ? paramName + index : paramName,
                                 iter.next());
                if (! noIndexing.contains(canonized)) {
                    index++;
                }
            }
            if (null != exceptionHolder[0]) {
                throw exceptionHolder[0];
            }
        }
        for (String columnGroupName: columnGroups.keySet()) {
            int i = 0;
            for (String[] a: columnGroups.get(columnGroupName)) {
                i++;
                // a[0] &quot;
                // a[1] [a-zA-Z0-9_]
                // a[2] &quot;
                // a[3] ([{<
                // a[4] [-0-9]
                // a[5] >}])
                // a[6] =
                // a[7] &quot;
                // a[8] [a-zA-Z0-9_]
                // a[9] &quot;
                // a[10] ([{<
                // a[11] [-0-9]
                // a[12] >}])
                if (! matchQuotes(a[0], a[2], columnGroupName, i)) {
                    continue;
                }
                if (! matchGroupers(a[3], a[5], columnGroupName, i)) {
                    continue;
                }
                Long firstSize = parseSize(a[4], a[3], columnGroupName, i);
                if (! matchQuotes(a[7], a[9], columnGroupName, i)) {
                    continue;
                }
                if (! matchGroupers(a[10], a[12], columnGroupName, i)) {
                    continue;
                }
                String first = a[0] + a[1] + a[2];
                DoValue firstDoValue = columnDoValues.get(first);
                String second = a[7] + a[8] + a[9];
                DoValue secondDoValue = columnDoValues.get(second);
                if (null == secondDoValue) {
                    if (0 != second.length()) {
                        StringBuilder sb
                            = initStringBuilder(columnGroupName, i, first);
                        sb.append ("column action was not found for the second,"
                                   + " source column name ");
                        sb.append(second);
                        if (null == firstDoValue) {
                            sb.append(" or for the first,"
                                      + "target column assignment name ");
                            sb.append(first);
                        }
                        sb.append('.');
                        throw new ServletException(sb.toString());
                    }
                } else {
                    firstDoValue = secondDoValue;
                    putColumnDoValues(first, firstDoValue);
                }
                if (null == firstDoValue) {
                    StringBuilder sb
                        = initStringBuilder(columnGroupName, i, first);
                    sb.append("column action was not assigned");
                    if (0 != second.length()) {
                        sb.append(" from second, source column ");
                        sb.append(second);
                    }
                    sb.append('.');
                    throw new ServletException(sb.toString());
                } else if (0 != a[0].length() || '?' != a[1].charAt(0)) {
                    columnNameList.add(first);
                }
                Long secondSize = parseSize(a[11], a[10], columnGroupName, i);
                Long currentSize = columnSizes.get(a[1]);
                if (null != secondSize) {
                    currentSize = secondSize;
                    if (null != firstSize) {
                        if (! firstSize.equals(secondSize)) {
                            StringBuilder sb
                                = initStringBuilder(columnGroupName, i, first);
                            sb.append("first, target column size");
                            sb.append(firstSize);
                            sb.append(" does not match second column size ");
                            sb.append(secondSize);
                            sb.append(" for second, source column name ");
                            sb.append(second);
                            sb.append('.');
                            throw new ServletException(sb.toString());
                        }
                    }
                }
                if (null != firstSize) {
                    currentSize = firstSize;
                }
                if (null != currentSize) {
                    columnSizes.put(a[1], currentSize);
                }
            }
        }
        if (null == tableName || 0 == tableName.length()) {
            StringBuilder sb = logStringBuilder
                ("No database table named for access log.", "init()");
            throw new ServletException(sb.toString());
        }
        if (columnNameList.isEmpty()) {
            StringBuilder sb = logStringBuilder
                ("No database columns named for access log.", "init()");
            throw new ServletException(sb.toString());
            /*
            // Default column names and sizes can mask misconfiguration!
            log("No database columns named. Using default columns.");
            // Alternate column names for DoValue classes.
            for (String[] alternateName: ALTERNAME_NAME_DEFAULTS) {
                DoValue doValue = columnDoValues.get(alternateName[1]);
                if (null == doValue) {
                    StringBuilder sb = logStringBuilder
                        ("an alternate column action for ", "init()");
                    sb.append(alternateName[0]);
                    sb.append(" was not found for default ");
                    sb.append(alternateName[1]);
                    sb.append('.');
                    throw new ServletException(sb.toString());
                }
                putColumnDoValues(alternateName[0], doValue);
            }
            // Default column names and sizes
            for (String[] columnNameSize: COLUMN_NAME_SIZE_DEFAULTS) {
                columnNameList.add(columnNameSize[0]);
                if (1 < columnNameSize.length) {
                    try {
                        columnSizes.put(columnNameSize[0],
                                        Long.valueOf(columnNameSize[1]));
                    } catch (NumberFormatException nfe) {
                        StringBuilder sb = logStringBuilder
                            ("default column size ", "init()");
                        sb.append(columnNameSize[1]);
                        sb.append(" for default column name ");
                        sb.append(columnNameSize[0]);
                        sb.append(" cannot be parsed as long.");
                        throw new ServletException(sb.toString(), nfe);
                    }
                }
            }
            */
        }
        if (debugEnabled) {
            Set<DoValue> inUse = new HashSet<DoValue>();
            StringBuilder sb = logStringBuilder(lineSeparator, "init()");
            sb.append("Columns in use: ");
            for (String col: columnNameList) {
                inUse.add(columnDoValues.get(col));
                sb.append(' ');
                sb.append(col);
                sb.append(',');
            }
            if (',' == sb.charAt(sb.length() - 1)) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append(lineSeparator);
            sb.append("Actions not in use: ");
            int i = 0;
            for (String col:
                     new TreeSet<String>(columnDoValues.keySet())) {
                if (! inUse.contains(columnDoValues.get(col))) {
                    if (8 <= ++i) {
                        sb.append(lineSeparator);
                        i = 0;
                    }
                    sb.append(' ');
                    sb.append(col);
                    sb.append(',');
                }
            }
            if (',' == sb.charAt(sb.length() - 1)) {
                sb.deleteCharAt(sb.length() - 1);
            }
            log(sb);
        }
    }

    private StringBuilder destroyStringBuilder (String sqlInsertString) {
        StringBuilder sb = logStringBuilder(" PreparedStatement(", "destroy()");
        sb.append(sqlInsertString.replaceAll("\"", "\\\""));
        return sb.append(") ");
    }

    /**
     * Release cached <code>PreparedStatement</code>s and
     * JDBC connections that are not from a <code>DataSource</code>.
     * 
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy () {
        for (Map<String, PreparedStatement> psMap:
                 preparedStatementCache.values()) {
            // Cannot use enhanced for loop,
            // which does not expose iterator for remove.
            for(Iterator<String> iter = psMap.keySet().iterator();
                iter.hasNext();) {
                String sqlInsertString = iter.next();
                PreparedStatement ps = psMap.get(sqlInsertString);
                iter.remove();
                try {
                    SQLWarning warns = ps.getWarnings();
                    if  (null != warns) {
                        StringBuilder sb
                            = destroyStringBuilder(sqlInsertString);
                        while (null != warns) {
                            sb.append(" SQLWarning(");
                            sb.append(warns.getMessage());
                            sb.append(", ");
                            sb.append(warns.getSQLState());
                            sb.append(", ");
                            sb.append(warns.getErrorCode());
                            sb.append(")");
                            warns = warns.getNextWarning();
                        }
                        log(sb);
                        // If warnings, delay closing PreparedStatement
                        // until connection close automatically closes it.
                    } else {
                        ps.close();
                    }
                } catch (SQLException e) {
                    StringBuilder sb = destroyStringBuilder(sqlInsertString);
                    Throwable cause = e.getCause();
                    if (null == cause) {
                        sb.append(": ");
                        sb.append(e.toString());
                        log(sb);
                    } else {
                        log(sb, e);
                    }
                }
            }
        }
        if (null != conn) {
            try {
                conn.close();
            } catch (SQLException e) {
                StringBuilder sb = logStringBuilder
                    ("cannot close JDBC connection.", "destroy()");
                log(sb.toString(), e);
            }
            conn = null;
        }
    }

    private String sqlInsert (Object[] valueObjs) {
        BitSet nullIndexes = new BitSet();
        for (int i = 0; i < valueObjs.length; i++) {
            if (null == valueObjs[i]) {
                nullIndexes.set(i);
            }
        }
        synchronized (sqlInsertCache) {
            String result = sqlInsertCache.get(nullIndexes);
            if (null != result) {
                return result;
            }
            if (nullIndexes.cardinality() == columnNameList.size()) {
                result = "INSERT INTO " + tableName + " DEFAULT VALUES;";
                sqlInsertCache.put(nullIndexes, result);
                return result;
            }
            Iterator<String> colNameIter = columnNameList.iterator();
            StringBuilder head = new StringBuilder("INSERT INTO ")
                .append(tableName).append(" (");
            StringBuilder tail = new StringBuilder(") VALUES (");
            for (int i = 0; i < valueObjs.length; i++) {
                String colName = colNameIter.next();
                if (null != valueObjs[i]) {
                    head.append(colName).append(',');
                    tail.append("?,");
                } else if (! nullIndexes.get(i)) {
                    throw new IllegalStateException
                        (tableName + '.' + colName
                         + " implementation inconsistent about default null");
                }
            }
            head.deleteCharAt(head.length() - 1);
            tail.deleteCharAt(tail.length() - 1);
            head.append(tail).append(");").trimToSize();
            result = head.toString();
            sqlInsertCache.put(nullIndexes, result);
            return result;
        }
    }

    private PreparedStatement getPreparedStatement (Connection conn,
                                                    String sqlInsertString)
        throws SQLException {

        Map<String, PreparedStatement> psMap = preparedStatementCache.get(conn);
        if (null == psMap) {
            psMap = new HashMap<String, PreparedStatement>();
            preparedStatementCache.put(conn, psMap);
            PreparedStatement result = conn.prepareStatement(sqlInsertString);
            psMap.put(sqlInsertString, result);
            return result;
        }
        PreparedStatement result = psMap.get(sqlInsertString);
        if (null == result) {
            result = conn.prepareStatement(sqlInsertString);
            psMap.put(sqlInsertString, result);
        }
        return result;
    }

    private void processConnection (Connection conn,
                                    FilterResponse response,
                                    Object[] loggedValues)
        throws SQLException, ServletException {

        boolean newDefault;
        int parameterIndex = 0;
        String sqlInsertString = sqlInsert(loggedValues);
        PreparedStatement ps;
        do {
            newDefault = false;
            int i = 0;
            sqlInsertString = sqlInsert(loggedValues);
            ps = getPreparedStatement(conn, sqlInsertString);
            for (String columnName: columnNameList) {
                Object val = loggedValues[i++];
                if (null != val) {
                    Long paramSize = columnSizes.get(columnName);
                    long size = null == paramSize ? 0 : paramSize.longValue();
                    ++parameterIndex;
                    if (debugEnabled) {
                        debug("Before setObject: i=" + i
                              + " parameterIndex=" + parameterIndex
                              + " columnName=" + columnName
                              + " size=" + size + " val=" + val);
                    }
                    // Returns true if the proposed value cannot be handled.
                    newDefault = columnDoValues.get(columnName).setObject
                        (response, sqlInsertString, ps, parameterIndex,
                         val, size);
                    if (newDefault) {
                        loggedValues[--i] = null;
                        parameterIndex = 0;
                        break;
                    }
                }
            }
        } while (newDefault);
        int remains = uniqueViolationRetries;
        do {
            try {
                conn.setAutoCommit(true);
                int cnt = ps.executeUpdate();
                if (1 != cnt) {
                    StringBuilder sb = logStringBuilder
                        (" PreparedStatement(\"", "process()");
                    sb.append(sqlInsertString.replaceAll("\"", "\\\""));
                    sb.append("\") returned ");
                    sb.append(cnt);
                    sb.append(" instead of 1 for");
                    for (Object val: loggedValues) {
                        if (null != val) {
                            sb.append(' ');
                            sb.append(val);
                        }
                        sb.append(',');
                    }
                    if (',' == sb.charAt(sb.length() - 1)) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    response.log(sb);
                }
            } catch (SQLException e) {
                String sqlState = e.getSQLState();
                if (debugEnabled) {
                    debug("processConnection ps.executeUpdate parameterIndex="
                          + parameterIndex + " SQLState=" + sqlState
                          + " caught=" + e);
                }
                if (uniqueViolation.equalsIgnoreCase(sqlState)) {
                    int i = 0;
                    parameterIndex = 0;
                    boolean incremented = false;
                    for (String columnName: columnNameList) {
                        Object val = loggedValues[i];
                        if (null != val) {
                            ++parameterIndex;
                            if (columnDoValues.get
                                (columnName).incrementable()) {
                                Long paramSize = columnSizes.get(columnName);
                                long size = null == paramSize ? 0L
                                    : paramSize.longValue();
                                loggedValues[i] =
                                    columnDoValues.get(columnName).increment
                                    (ps, parameterIndex, val, size,
                                     retryIncrements[incrementIndex]);
                                incremented = true;
                            }
                        }
                        i++;
                    }
                    if (incremented) {
                        if(retryIncrements.length <= ++incrementIndex) {
                            incrementIndex = 0;
                        }
                        continue;
                    }
                }
                throw e;
            }
            break;
        } while (0 < remains--);
    }

    /**
     * Does the access logging for <code>Filter.doFilter</code>
     * or <code>Valve.invoke</code> in four (4) phases.
     * <ul>
     * <ol>Creates an array of potential Objects to pass new values
     * for the database fields.</ol>
     * <ol>Determines which of those values are non-null
     * in order to set up a <code>java.sql.PreparedStatement</code>,
     * which passes non-null values and
     * uses database defaults for null ones.</ol>
     * <ol>Fills in the <code>java.sql.PreparedStatement</code>
     * with appropriate setters.</ol>
     * </ol>Executes the <code>java.sql.PreparedStatement</code>
     * to pass values to the database.</ol></ul>
     * <p>
     * Synchronizes on all of the <code>java.sql.Connection</code> objects
     * even though Datasource pools would provide enough synchronization.
     * Guards against a DataSource using a basic implementation that might issue
     * a <code>java.sql.Connection</code> twice.
     * @param request The HttpServletRequest passed with the HTTP query.
     * @param response A FilterResponse wrapping the HTTP query response.
     * @throws SQLException rarely when the PreparedStatement
     * cannot properly interact with the database during setup.
     * @throws ServletException when the data cannot be obtained
     * because of an unexpected situation, typically a programming error.
     */
    public void process (HttpServletRequest request, FilterResponse response)
        throws IOException, SQLException, ServletException {

        if (debugEnabled) {
            debug("Entering " + this + " process on " + response);
        }
        if (notConnected) {
            // If an Exception is thrown, will not try again.
            notConnected = false;
            connect();
        }
        Object[] loggedValues = new Object[columnNameList.size()];
        int i = 0;
        for (String columnName: columnNameList) {
            loggedValues[i++]
                = columnDoValues.get(columnName).getObject(request, response);
        }
        if (null != this.conn) {
            synchronized (conn) { // Avoid corruption of PreparedStatement
                processConnection(conn, response, loggedValues);
            }
        } else if (null != ds) {
            Connection c = ds.getConnection();
            synchronized (c) {
                try {
                    processConnection(c, response, loggedValues);
                } finally {
                    c.close();     // return Connection to DataSource pool
                }
            }
        } else {
            StringBuilder sb = logStringBuilder
                ("No connection among parameters:", "process()");
            configStringBuilder(sb);
            throw new IOException(sb.toString());
        }
        if (debugEnabled) {
            debug("Leaving " + this + " process on " + response);
        }
    }

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     * @exception IOException Could be thrown by next stacked Filter.
     * @exception ServletException Database SQLException is wrapped 
     * in a ServletException. Could be thrown by next stacked Filter too.
     * @exception NullPointerException If any parameter is null.
     */
    public void doFilter (ServletRequest request,
                          ServletResponse servletResponse,
                          FilterChain chain)
        throws IOException, ServletException {

        if (debugEnabled) {
            debug("Entering doFilter of " + this + " on " + servletResponse);
        }
        if (! (servletResponse instanceof HttpServletResponse
               && request instanceof HttpServletRequest)) {
            chain.doFilter(request, servletResponse);
            // Not logged
            return;
        }
        FilterResponse response
            = createFilterResponse((HttpServletResponse) servletResponse);
        // pass the request along the filter chain
        try {
            chain.doFilter(request, servletResponse);
        } catch (IOException e) {
            response.setThrowable(e);
            throw e;
        } catch (ServletException e) {
            response.setThrowable(e);
            throw e;
        } catch (RuntimeException e) {
            response.setThrowable(e);
            throw e;
        } catch (Error e) {
            response.setThrowable(e);
            throw e;
        } finally {
            try {
                process((HttpServletRequest) request, response);
            } catch (SQLException e) {
                if (debugEnabled) {
                    debug("Catching SQLException");
                }
                // After the first SQLException prints its stack,
                // then just print the SQLException to avoid
                // overwhelming the log if access logging is unavailable.
                if (isStackPrinted()) {
                    StringBuilder sb
                        = logStringBuilder(e.toString(), "doFilter()");
                    response.log(sb);
                } else {
                    setStackPrinted(true);
                    StringBuilder sb = logStringBuilder(null, "doFilter()");
                    response.log(sb, e);
                }
            }
        }
    }
}
