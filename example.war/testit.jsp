<%-- Copyright © 2019 Robert L. Kirby
--%><%@ page language="java"
contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
%><?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" >
<head>
 <link rel="canonical" href="/testit.htm" />
 <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
 <meta name="Title" content="Show Test Results" />
 <meta name="Author" content="Robert (Bob) L. Kirby" />
 <meta name="Description" content="Show test results" />
<title>Show Test Results</title>
</head>
<body>
<h2 style="clear: left">Test Results</h2>
<%
String attrName = config.getInitParameter("throwableattribute");
if (null == attrName) {
%><h4>No init attribute for logging throwable found. Trying default.</h4>
<%
  attrName = "Throwable";
}
if (null == request.getAttribute(attrName)) {
  // Testing root cause
  throw new RuntimeException("Testit exception",
                             new IllegalArgumentException("Testit arg"));
}
%>
<h4>Request attribute <%= attrName
%> already has a Throwable. Not throwing.</h4>
</body>
</html>
