--psql -h 192.168.1.2 -f $SRC/SQL/access_select100.sql postgres jboss
--\pset format wrapped
--\pset columns 80
--\pset pager off
SELECT CAST("timestamp" AS TIMESTAMP(0)), remotehost, query, "method", status,
 username, bytes, virtualhost, serverport, referer, useragent
 FROM "access"
 ORDER BY "timestamp" DESC
 LIMIT 500;
