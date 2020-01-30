JDBC and DataSource Access Logs from Filters and Valves
=======================================================
http://bobkirby.info/

JDBCAccessLogFilter is a highly configurable Filter,
which produces database rows for each HTTP access.
By itself, JDBCAccessLogFilter works with dynamic web servers
and application servers back to Java 1.5 (AKA Java 5)
using reflection to possibly access hidden information is early Java versions.
Extensions for different environments can avoid inefficient reflection
and provide more reliable, possibly expanded, HTTP access.

JDBCAccessLogFilter supports access to over sixty HTTP accessors
without parameters and more than a dozen with a single parameter.
Each accessor may populate a column (field) in table of a SQL database
supporting JDBC like PostgreSQL, one row per HTTP access.

Use cases
---------
When data resides in a relational database many capabilities become available
over what could be easily obtained from text files.
Basic capabilities include search and filtering using SQL,
which is a declarative language expressing logic statements.
Typically "joins" allow tables to be combined into more expressive information,
which may be custom formatted and ordered for analysis.
For example, joining on IP addresses in the SQL access log with
IP address ranges with locations of a site like
http://software77.net/geo-ip/ may be very useful.
Many SQL databases also include producing alerts, typically with triggers,
which can automatically lead to other actions.

These capabilities support many use cases.
* __Business Intelligence__
  Businesses can track where activity originates,
  whether activity lead to conversion,
  when activities occur in terms of the calendar and time of day,
  and the steps between initial and final activities.
  Campaigns can be correlated with results,
  particularly when access logs join with other business records.
  Database tools can provide many varieties of analysis.
* __Configuration__
  In addition to monitoring current activity,
  historical trends may be available.
  Enough equipment can be deployed in advance of actual loads.
  Loads may be balanced among servers.
  With more accessible information,
  resources can be allocated more efficiently and timely.
* __Microservices__
  When parts of loads are distributed across many servers,
  their access logs can be combined into a unified audit trail.
  A unified audit trail can identify bottlenecks and overloads,
  which can then be corrected.
* __Upgrading__
  During upgrades,
  access logs that span old and new resources
  can confirm that changes occur as expected or warn about the unanticipated.
  Queries on access logs can efficiently provide immediate feedback.
* __Security__
  Access logs can help identify the unusual,
  such as queries for unadvertised pages.
  But malefactors more regularly try to gain improper access.
  Unlike legitimate spiders, malefactors are apt not to check robots.txt,
  possible come from places that are not wanted,
  have signatures such as a dubious user-agent,
  get many status 403 "page not found" results,
  and otherwise place an unwanted burden on a web site,
  such as denial of service attacks.
  Queries of access logs can identify bad behavior,
  perhaps even triggering alerts.
* __Learning__
  Servers may have many details that go unrecognized or not understood.
  Access logs, particularly those that include many details,
  even details whose purpose may not be readily understood,
  can increase understanding of interactions.

Distribution
------------
[Filter and Valve JavaDoc](example.war/doc/index.html)

When used as a Filter,
the archive presented to the server may include the files.
For example,
a WAR might include the files and WEB-INF/web.xml Filter configuration.
When used as a Valve,
the files may reside in their own JAR, like valve.jar,
typically in a global library, like a "lib" or a "deploy" directory.
For a Tomcat Valve, whose effects are global,
or its embedding in earlier JBoss implementations,
Valve configuration may be in a server.xml file
of a jbossweb.sar of a selected deploy subdirectory.

The files support a range of Java environments
whose implementations are not always compatible with each other.

* __JDBCAccessLogFilter__
  The base works with all of the environments, using reflection,
  but omits features that were unsupported in the earlier environments.
  The base must be included in later environments.

* __FacadeJDBCAccessLogFilter__
  The Facade version, a Filter, supplies status and byte count features,
  which were omitted from the Java EE5 HttpServletResponse interface.
  Later environments should not use The Facade version.
  Alternatively, the Tomcat Valve versions supply the features
  by accessing Tomcat implementation details.

* __ResponseFacade__
  In order get those features in the Tomcat environment,
  which the JBoss 5.1 software uses,
  the file org.apache.catalina.connector.ResponseFacade has been extended.
  In JBoss 5.1, ResponseFacade resides in jbossweb.jar,
  which is in the jbossweb.sar directory.

  One of several approaches may install a compiled version of ResponseFacade,
  with its extensions, after backups (very important) have been completed.
  - The compiled class files (usually 4) might be placed in a new JAR file
    that classloaders would use before the original versions. Tricky.
  - The jar utility can insert a compiled version of ResponseFacade
    into the compressed jbossweb.jar file, replacing the original.
  - The jbossweb.jar file can be exploded into a directory heirarchy
    using renaming and an unzip utility.
    A compiled, newer version of ResponseFacade can then replace the original.

* __JDBCAccessLogValve__
  The base Valve version uses the base Filter in Tomcat environments.
  Because a Java EE 5 Valve gets an implementation Response object
  rather than the limited ResponseFacade implementation of HttpServletResponse,
  a Valve can access most useful features.

* __ResponseJDBCAccessLogValve__
  The Response version extends the base Tomcat Valve
  with a more efficient implementation that avoids Java reflection.
  The Response version should be preferred over the base Tomcat Valve alone.
  
* __EE7JDBCAccessLogFilter__
  The Java EE 7 version uses implementations of HttpServletResponse
  without the limitations of earlier versions of the interface.
  However, the Java EE 7 version will not compile in earlier environments.
  
* __EE8JDBCAccessLogFilter__
  Further extensions should be straightforward.

Building
--------
The software was primarily built with a Mac OS X version of
Eclipse Java EE IDE for Web Developers, Kepler Service Release 2,
which suppresses the details of the compilation classpath.
Since multiple versions were being created,
appropriate files for each version were moved in or out of a project
with defaults mostly set for Java 1.5 (5.0),
which allowed compiled classes to be captured.
A Maven POM.xml might be created to handle many versions
but was not attempted.

* __Java EE7 and beyond__
  Support for later Java and JavaEE versions were similarly compiled
  with a Mac OS X version of Eclipse Java EE IDE, 2019-09R.

* __JavaDoc__
  The Mac OS X 10.6.8 version of javadoc (standard Doclet 1.6.0_65)
  with Eclipse provided arguments:
  ```
  javadoc -source 1.5 \
   -windowtitle "JDBC Access Log Filter and Valve files" \
   -tag copyright
  ```

* __Facade__
  The JBoss 5.1GA Tomcat modifications were hand-crafted as an experiment,
  which attempted to get around reflection security restrictions.
  Such modifications could risk production systems.
  After exploding `jbossweb.sar/jbossweb.jar`,
  basic compilation was done with
  ```
  javac -source 1.5 -Xlint:unchecked
  ```
  on jbossweb.sar/jbossweb.jar/org.apache.catalina.connector.ResponseFacade
  in the "deploy" directory.
  The classpath included `common/lib/servlet-api.jar`,
  which included just enough of the Java EE support, and `jbossweb.jar`.

Testing
--------
Only off-line integrated testing was done: no unit testing or test frameworks.
The data definition language (DDL) [`wide.sql`](src/main/test/ddl/wide.sql)
began a wide range of field (column) tests
for the PostgreSQL environment.
The DDL [`narrow.sql`](src/main/test/ddl/narrow.sql)
sets up a smaller day-to-day environment.
Modifications should be straightfoward for other relational databases.

Relational databases that do not support arrays may use
character large objects (CLOBs) instead with either standard large object
or JavaScript Object Notation (JSON) field types.
If so, JDBCAccessLogFilter field (column) configuration for arrays
may need to be modified with the "clob" suffix specified in the
[JavaDoc](example.war/doc/index.html).

The test web archive (WAR) `example.war` requires
a connection to an appropriate database table,
which, by default is named "access" without a specified schema
in the WAR configuration file [`web.xml`](example.war/WEB-INF/web.xml).
Either of the DDL files [`narrow.sql`](src/main/test/ddl/narrow.sql)
or [`wide.sql`](src/main/test/ddl/wide.sql)
could create a table with fields (columns)
that would satisfy the default JDBCAccessLogFilter configuration.

The configuration file [`web.xml`](example.war/WEB-INF/web.xml)
names "java:/DefaultDS" as the first choice for making a database connection.
If this DataSource is not available,
then the configured database URL, username, and password
will need editing for appropriate values.
Either database connection will need an accessible database engine
and a JDBC driver JAR installed for that engine.

After `example.war` is deployed in running container software,
such as an application server, like Tomcat, JBoss, or Wildfly,
a browser displays its web pages with a URL like
[`http://127.0.0.1:8080/example/`](http://127.0.0.1:8080/example/),
where typically only the localhost "127.0.0.1" is initially reachable,
a non-priviledged port like 8080 is configured initially, and, by default,
since [`web.xml`](example.war/WEB-INF/web.xml) configures no `<context-root>`,
the WAR exploded directory name "example" is used.

The directory `example.war` includes a Java server page (JSP)
[`testit.jsp`](example.war/testit.jsp),
which generates an error with a distinct root cause,
which forwards to the [`error.jsp`](example.war/error.jsp)
error-handling JSP servlet.
In addition to filter configuration, [`web.xml`](example.war/WEB-INF/web.xml)
includes a "throwableattribute" `<context-param>` to support the few errors
that may actually forward to the error page.

Dependencies
------------
This software was developed to operate in a wide range of environments
with the following minimums:
* Java Development Kit (JDK) 5 (1.5) or newer - check `java -version`
* Java Enterprise Edition (EE) that runs on the JDK
* JDBC for a relevant relational database management system (RDBMS)

Issues
------
The octet length of output may only be reliable with Tomcat-based software,
such as in the earlier JBoss versions, which counts output bytes.
The JavaEE interface `javax.servlet.http.HttpServletResponse`
does not include counts and containers do not uniformly include
a "Content-Length" HTTP header.

Copyright and License
---------------------
Copyright © 2020 Robert L. Kirby. Some rights reserved.  
kirby dot bob separated with an "at" sign from gmail dot com.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0
http://opensource.org/licenses/eclipse-1.0.php
which can be found in the file epl-v10.html.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.
You must not remove this notice, or any other, from this software.

The intent of the use of this license is to keep this software
as open source with the author identity intact,
without contaminating other software with the license of this software,
even if used internally or provided to others for commercial purposes.
Software is provided as-is with user assumption of all risks.
