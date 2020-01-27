-- Test Tomcat Valve with a wide variety of PostgreSQL columns
-- Replace *TBD* userName and password below
-- or add a datasource ="DefaultDS" attribute
-- for a DataSource defined at the top level of the "deploy" directory.
/*
 <Valve className="info.bobkirby.valve.ResponseJDBCAccessLogValve"
  driverClass="org.postgresql.Driver"
  connectionURL="jdbc:postgresql://postgres:5432/postgres"
  userName="*TBD*" password="*TBD*"
  tableName="public.wide"
  header="Origin,Via"
  sessionattributestring="display-type"
  literalInteger="1"
  columns='"timestamp"=timestampkey, remotehost(15)=remoteaddr,
    query(2048)=requesturi, "method"(8), status, username(16)=remoteuser,
    bytes=contentcountlength, virtualhost=servername(64), serverport,
    referer(1028)=referertext, useragent(512)=useragenttext,
    origin=header,via=header1,inheads=headernames,infirsts=headerfirsts,
    outheads=responseheadernames,outfirsts=responseheaderfirsts,
    sessionattributenames,sessionattr=sessionattributestring,
    sessionid,sessionisnew,sessioncreationtime,
    contextpath,servletcontextpath,
    lit=literalinteger,elapsedmilli,serverinfo(-10),rootcausetrace'
 />
 */
-- PostgreSQL specific command to temporarily set schema
SET search_path TO public;

CREATE TABLE wide (
 "timestamp" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
 remoteHost CHAR(15) NOT NULL,
 query TEXT NOT NULL,
 "method" CHARACTER VARYING(8) NOT NULL,
 status SMALLINT NOT NULL,
 username CHAR(16),
 bytes BIGINT NOT NULL DEFAULT (-1),
 virtualHost CHARACTER VARYING(64) NOT NULL,
 serverPort INTEGER,
 referer TEXT,
 useragent TEXT,
 origin TEXT,
 via TEXT,
 inheads CHARACTER VARYING[] NOT NULL,
 infirsts TEXT[] NOT NULL,
 outheads CHARACTER VARYING[],
 outfirsts TEXT[],
 sessionattributenames CHARACTER VARYING[],
 sessionattr TEXT,
 sessionid TEXT,
 sessionisnew BOOLEAN,
 sessioncreationtime TIMESTAMP WITH TIME ZONE,
 contextpath TEXT,
 servletcontextpath TEXT,
 lit INTEGER NOT NULL,
 elapsedmilli BIGINT NOT NULL,
 serverinfo CHARACTER VARYING(11) NOT NULL, -- 10+1 for ellipse character
 rootcausetrace CHARACTER VARYING[],
 CONSTRAINT access_timestamp_pkey PRIMARY KEY ("timestamp")
 ) WITH (OIDS=FALSE);
COMMENT ON TABLE wide IS
'Access log test target for JDBCAccesstLogValve or JDBCAccesstLogFilter.';
