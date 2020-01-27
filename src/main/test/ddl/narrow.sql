-- Test example valve.war javax.servlet.filter with the PostgreSQL columns
-- in the comments of info.bobkirby.valve.JDBCAccessLogFilter.java.
-- Replace *TBD* userName and password in WEB-INF/web.xml
-- or add an <init-param> "datasource" with value "DefaultDS"
-- for a defined DataSource.
--
-- A matching "columns" XML value could be:
-- &quot;timestamp&quot;=timestampkey, remotehost(15)=remoteaddr,
-- query(2048)=requesturi, &quot;method&quot;(8), status,
-- username(16)=remoteuser,bytes=contentcountlengthinteger,
-- virtualhost=servername(64), serverport, referer(1028)=referertext,
-- useragent(512)=useragenttext,rootcausetrace(3)
--
-- PostgreSQL specific command to temporarily set schema
SET search_path TO public;

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
COMMENT ON TABLE access IS
'Access log test target for JDBCAccesstLogFilter.';
