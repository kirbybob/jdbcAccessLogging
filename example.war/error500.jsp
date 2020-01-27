<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    isErrorPage="true"
%><%-- Copyright © 2019 Robert L. Kirby
Some earlier servers do not set the status to 500 until after Filter processing.
--%><html xmlns="http://www.w3.org/1999/xhtml" lang="en" >
<head>
 <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
 <meta name="robots" content="noindex, nofollow" />
<title>Internal Error 500</title>
</head>
<body>
<h2>Internal Error 500</h2>
<% // Save Throwable for access logging.
String attrName = config.getInitParameter("throwableattribute");
if (null == attrName) {
  attrName = application.getInitParameter("throwableattribute");
}
if (null == attrName) {
  attrName = "Throwable";
}
Object obj = request.getAttribute(attrName);
if ((null == obj || ! (obj instanceof Throwable)) && null != exception) {
  request.setAttribute(attrName, exception);
} else if (null != exception) {
  System.err.print(obj + " not setting attribute=" + attrName
                   + " for " + exception);
}
%><p>Caught internal error: <pre><%= exception %></pre><%
for (Throwable cause = null == exception ? null : exception.getCause();
     null != cause; cause = cause.getCause()) {
%><br>
with cause: <%= cause %><%
}
%></p><%
String referer = request.getHeader("referer");
if (null != referer && 0 < referer.length()) {
%><p>Referring page: <%= referer %></p>
<%
}
%><p>Use the browser back button to try again.</p>
</body>
</html>
