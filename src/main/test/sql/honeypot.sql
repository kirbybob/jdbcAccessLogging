-- PostgreSQL 8.4.5
WITH p AS
-- Parameters
(SELECT 60 AS dys, -- Days in time interval
 -- Useragent SIMILAR TO regular expression for bots, crawlers, and slurpers
 '(%[Bb][Oo][Tt]%)|(%[Cc]raw%)|(%[Ss]lurp%)'::text AS bots,
 -- Query SIMILAR TO regular expression for unadvertised pages
 '/(((dev|format|KRPrimitives|matching|testit|welcome).htm)'
  '|(files/%))'::text AS unad
), tmp AS
-- Temporary table of acceptable rows in time range
(SELECT a.status, a.remotehost, a.username, a."query", a.method,
 a.virtualhost, a.serverport, a.useragent,
 k.code_3, k.divert, k.country_name
 FROM p, "access" AS a
 LEFT OUTER JOIN ip_country AS i ON i.range_first =
 (SELECT MAX(j.range_first) FROM ip_country AS j
  WHERE j.range_first <= CAST(CAST
   -- Parse IPv4 text into INTEGER to find range floor
   ((CAST(SUBSTRING
     (a.remotehost FROM E'^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.(\\d{1,3})$')
     AS BIGINT) + 256 *CAST(SUBSTRING
     (a.remotehost FROM E'^\\d{1,3}\\.\\d{1,3}\\.(\\d{1,3})\\.\\d{1,3}$')
     AS BIGINT) + 256 * 256 * CAST(SUBSTRING
     (a.remotehost FROM E'^\\d{1,3}\\.(\\d{1,3})\\.\\d{1,3}\\.\\d{1,3}$')
     AS BIGINT) + 256 * 256 * 256 * CAST(SUBSTRING
     (a.remotehost FROM E'^(\\d{1,3})\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$')
     AS BIGINT))
  AS BIT(32)) AS INTEGER)
 ) AND CAST(CAST
   -- Parse IPv4 text into INTEGER between inclusive range bounds
   ((CAST(SUBSTRING
     (a.remotehost FROM E'^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.(\\d{1,3})$')
     AS BIGINT) + 256 *CAST(SUBSTRING
     (a.remotehost FROM E'^\\d{1,3}\\.\\d{1,3}\\.(\\d{1,3})\\.\\d{1,3}$')
     AS BIGINT) + 256 * 256 * CAST(SUBSTRING
     (a.remotehost FROM E'^\\d{1,3}\\.(\\d{1,3})\\.\\d{1,3}\\.\\d{1,3}$')
     AS BIGINT) + 256 * 256 * 256 * CAST(SUBSTRING
     (a.remotehost FROM E'^(\\d{1,3})\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$')
     AS BIGINT))
  AS BIT(32)) AS INTEGER)
  BETWEEN SYMMETRIC i.range_first AND i.range_last
 LEFT OUTER JOIN country_code AS k ON k.code_2 = i.code_2
 WHERE a."timestamp" > CURRENT_TIMESTAMP - (p.dys || 'day')::INTERVAL
  -- Exclude loopback and LAN IPv4 addresses
  AND a.remotehost NOT SIMILAR TO '(127.0.0.1%)|(66.92.28.246%)|(192.168.%)'
-- ORDER BY k.code_3, a.status, a.serverport
), iso AS
-- Count of all distinct country codes
(SELECT COALESCE(count(COALESCE(k.code_3, 'unknown')), 0) AS isocount
 FROM (SELECT DISTINCT t.code_3 FROM tmp AS t) AS k)
-- Outer SELECT ORDER BY "ordering" first to preserve subselect groupings
SELECT "Statistic", "count", "ISO3", "status", "serverport", "information"
FROM (
(SELECT p.dys || ' day grand totals' AS "Statistic",
 -- Uses a Scalar select to insure output even with no rows.
 COALESCE((SELECT count(*) FROM tmp AS t), 0) AS "count",
 '=' || (SELECT iso.isocount FROM iso) AS "ISO3",
 COALESCE((SELECT count(*) FROM (SELECT DISTINCT t.status
  FROM tmp AS t) AS k), 0)::SMALLINT AS "status",
 COALESCE((SELECT count(*) FROM (SELECT DISTINCT t.serverport
  FROM tmp AS t) AS k), 0)::SMALLINT AS "serverport",
 CURRENT_TIMESTAMP::text AS "information",
 1 AS "ordering"
 FROM p
)
UNION
(SELECT 'status serverport' AS "Statistic", count(*) AS "count",
 '' AS "ISO3", t.status AS "status", t.serverport AS "serverport",
 '' AS "information",
 3 AS "ordering"
 FROM tmp AS t
 GROUP BY t.status, t.serverport
)
UNION
(SELECT 'port method' AS "Statistic", count(*) AS "count",
 '' AS "ISO3", null AS "status", t.serverport AS "serverport",
 t.method AS "information",
 5 AS "ordering"
 FROM tmp AS t
 GROUP BY t.serverport, t.method
)
UNION
(SELECT 'Total method' AS "Statistic",
 COALESCE(count(*), 0) AS "count",
 '' AS "ISO3", null AS "status", null AS "serverport",
 '' AS "information",
 6 AS "ordering"
 FROM (SELECT DISTINCT k.method FROM tmp AS k) AS t
)
UNION
(SELECT 'countries status port method' AS "Statistic",
 count(*) AS "count",
 CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
 THEN COALESCE(t.code_3, 'ZZZ')
 WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END AS "ISO3",
 t.status AS "status", t.serverport AS "serverport",
 t.method AS "information",
 7 AS "ordering"
 FROM tmp AS t
 GROUP BY CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
  THEN COALESCE(t.code_3, 'ZZZ')
  WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END,
 t.status, t.serverport, t.method
)
UNION
(SELECT 'Total serverport method' AS "Statistic",
 COALESCE(count(*), 0) AS "count",
 '' AS "ISO3", null AS "status", null AS "serverport", '' AS "information",
 8 AS "ordering"
 FROM (SELECT DISTINCT COALESCE(k.serverport, -1),
       COALESCE(k.method, 'unknown') FROM tmp AS k) AS t
)
UNION
(SELECT 'countries robots sitemap query' AS "Statistic",
 count(*) AS "count",
 CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
 THEN COALESCE(t.code_3, 'ZZZ')
 WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END AS "ISO3",
 t.status AS "status", t.serverport AS "serverport",
 t."query" AS "information",
 9 AS "ordering"
 FROM tmp AS t
 WHERE t."query" SIMILAR TO '(/robots%)|(/sitemap%)'
 GROUP BY CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
  THEN COALESCE(t.code_3, 'ZZZ')
  WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END,
 t.status, t.serverport, t."query"
)
UNION
(SELECT 'Total robots query' AS "Statistic",
 count(*) AS "count",
 '=' || COALESCE((SELECT count(k.code_3) FROM
  (SELECT DISTINCT COALESCE(t.code_3, '') AS code_3
   FROM tmp AS t WHERE t."query" SIMILAR TO '/robots%') AS k), 0) AS "ISO3",
 null AS "status", null AS "serverport", '/robot' AS "information",
 10 AS "ordering"
 FROM tmp AS t
 WHERE t."query" SIMILAR TO '/robots%'
)
UNION
(SELECT 'Total sitemap query' AS "Statistic",
 count(*) AS "count",
 '=' || COALESCE((SELECT count(k.code_3) FROM
  (SELECT DISTINCT COALESCE(t.code_3, '') AS code_3
   FROM tmp AS t WHERE t."query" SIMILAR TO '/sitemap%') AS k), 0) AS "ISO3",
 null AS "status", null AS "serverport", '/sitemap' AS "information",
 11 AS "ordering"
 FROM tmp AS t
 WHERE t."query" SIMILAR TO '/sitemap%'
)
UNION
(SELECT 'Total robots sitemap query' AS "Statistic",
 count(*) AS "count",
 '=' || COALESCE((SELECT count(k.code_3) FROM
  (SELECT DISTINCT COALESCE(t.code_3, '') AS code_3
   FROM tmp AS t WHERE t."query" SIMILAR TO '(/robots%)|(/sitemap%)') AS k),
  0) AS "ISO3",
 null AS "status", null AS "serverport",
 '=' || COALESCE((SELECT count(k."query") FROM
  (SELECT DISTINCT t."query" AS "query" FROM tmp AS t
   WHERE t."query" SIMILAR TO '(/robots%)|(/sitemap%)') AS k), 0)
  AS "information",
 12 AS "ordering"
 FROM tmp AS t
 WHERE t."query" SIMILAR TO '(/robots%)|(/sitemap%)'
)
UNION
(SELECT 'countries miscoded semicolon' AS "Statistic",
 count(*) AS "count",
 CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
 THEN COALESCE(t.code_3, 'ZZZ')
 WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END AS "ISO3",
 null AS "status", null AS "serverport",
 COALESCE(SUBSTRING(t."query" FROM E'^(/.+\.htm)%.*'),
  SUBSTRING(t."query" FROM E'^(/.*)[%?].*'), t."query") AS "information",
 13 AS "ordering"
 FROM tmp AS t
 WHERE t."query" ILIKE E'%\%3B%'
 GROUP BY CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
  THEN COALESCE(t.code_3, 'ZZZ')
  WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END,
  COALESCE(SUBSTRING(t."query" FROM E'^(/.+\.htm)%.*'),
   SUBSTRING(t."query" FROM E'^(/.*)[%?].*'), t."query")
)
UNION
(SELECT 'Total miscoded semicolon' AS "Statistic",
 count(*) AS "count",
 '=' || COALESCE((SELECT count(k.code_3) FROM
  (SELECT DISTINCT COALESCE(t.code_3, '') AS code_3
   FROM tmp AS t WHERE t."query" ILIKE E'%\%3B%') AS k), 0) AS "ISO3",
 null AS "status", null AS "serverport", E'%3Bjsessionid=' AS "information",
 14 AS "ordering"
 FROM tmp AS t
 WHERE t."query" ILIKE E'%\%3B%'
)
UNION
(SELECT 'countries bot useragent' AS "Statistic", count(*) AS "count",
 CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
 THEN COALESCE(t.code_3, 'ZZZ')
 WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END AS "ISO3",
 t.status AS "status", t.serverport AS "serverport",
 COALESCE(trim(both E' \t' from t.useragent), '') AS "information",
 15 AS "ordering"
 FROM tmp AS t, p
 WHERE t.useragent SIMILAR TO p.bots
 GROUP BY CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
  THEN COALESCE(t.code_3, 'ZZZ')
  WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END,
 t.status, t.serverport, COALESCE(trim(both E' \t' from t.useragent), '')
)
UNION
(SELECT 'Total bot useragent' AS "Statistic",
 count(*) AS "count",
 '=' || COALESCE((SELECT count(k.code_3) FROM
  (SELECT DISTINCT COALESCE(t.code_3, '') AS code_3
   FROM tmp AS t, p WHERE t.useragent SIMILAR TO p.bots) AS k),
  0) AS "ISO3",
 (select count(*) FROM
  (select distinct COALESCE(trim(both E' \t' from t.useragent), '')
   FROM tmp AS t, p
   WHERE t.useragent SIMILAR TO p.bots) AS k)::INTEGER AS "status",
 null AS "serverport", p.bots AS "information",
 16 AS "ordering"
 FROM tmp AS t, p
 WHERE t.useragent SIMILAR TO p.bots
 GROUP BY p.bots
)
UNION
(SELECT 'status useragent' AS "Statistic", count(*) AS "count",
 '' AS "ISO3", t.status AS "status", null AS "serverport",
 COALESCE(trim(both E' \t' from t.useragent), '') AS "information",
 17 AS "ordering"
 FROM tmp AS t
 GROUP BY t.status, COALESCE(trim(both E' \t' from t.useragent), '')
)
UNION
(SELECT 'Total useragent' AS "Statistic",
 count(*) AS "count",
 '=' || (select iso.isocount FROM iso) AS "ISO3",
 null AS "status", null AS "serverport",
 null AS "information",
 18 AS "ordering"
 FROM (SELECT DISTINCT COALESCE(trim(both E' \t' from t.useragent), '')
       FROM tmp AS t) AS k
)
UNION
(SELECT 'countries status unadvertised' AS "Statistic",
 count(*) AS "count",
 CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
 THEN COALESCE(t.code_3, 'ZZZ')
 WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END AS "ISO3",
 t.status AS "status", null AS "serverport",
 t."query" AS "information",
 19 AS "ordering"
 FROM tmp AS t, p
 WHERE t."query" SIMILAR TO p.unad AND COALESCE(t.username, '') = ''
 GROUP BY CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
  THEN COALESCE(t.code_3, 'ZZZ')
  WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END,
  t.status, t."query"
)
UNION
(SELECT 'Total unadvertised' AS "Statistic",
 count(*) AS "count",
 '=' || COALESCE((SELECT count(k.code_3) FROM
  (SELECT DISTINCT COALESCE(t.code_3, '') AS code_3
   FROM tmp AS t, p
   WHERE t."query" SIMILAR TO p.unad AND COALESCE(t.username, '') = '')
  AS k), 0) AS "ISO3",
 COALESCE((select count(*) FROM
  (select distinct t."query" FROM tmp AS t, p
   WHERE t."query" SIMILAR TO p.unad AND COALESCE(t.username, '') = '')
  AS k)::INTEGER, 0) AS "status",
 null AS "serverport", p.unad AS "information",
 20 AS "ordering"
 FROM tmp AS t, p
 WHERE t."query" SIMILAR TO p.unad AND COALESCE(t.username, '') = ''
 GROUP BY p.unad
)
UNION
(SELECT 'countries status virtualhost' AS "Statistic",
 count(*) AS "count",
 CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
 THEN COALESCE(t.code_3, 'ZZZ')
 WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END AS "ISO3",
 t.status AS "status", null AS "serverport",
 COALESCE(trim(both E' \t' from t.virtualhost), '') AS "information",
 21 AS "ordering"
 FROM tmp AS t
 GROUP BY CASE WHEN COALESCE(t.code_3, 'ZZZ') IN ('USA', 'CHN', 'ZZZ')
  THEN COALESCE(t.code_3, 'ZZZ')
  WHEN t.divert IS NULL THEN 'deny' ELSE 'permit' END,
 t.status, COALESCE(trim(both E' \t' from t.virtualhost), '')
)
UNION
(SELECT 'Total virtualhost' AS "Statistic",
 count(*) AS "count",
 '=' || (select iso.isocount FROM iso) AS "ISO3",
 null AS "status", null AS "serverport", null AS "information",
 22 AS "ordering"
 FROM (SELECT DISTINCT COALESCE(trim(both E' \t' from t.virtualhost), '')
       FROM tmp AS t) AS k
)
UNION
(SELECT 'country bad status' AS "Statistic", count(*) AS "count",
 t.code_3 AS "ISO3", COALESCE(b.badcount, 0)::INTEGER AS "status",
 null AS "serverport",
  CASE WHEN t.divert IS NULL THEN 'deny' WHEN t.divert IS TRUE THEN 'permit'
   ELSE 'allow' END || ' ' || COALESCE(t.country_name, 'unknown')
   AS "information",
 23 AS "ordering"
 FROM tmp AS t
 LEFT OUTER JOIN
 (SELECT count(*) AS badcount, c.code_3 AS code_3
  FROM tmp AS c
  WHERE c.status / 100 NOT BETWEEN 2 AND 3
  GROUP BY c.code_3) AS b
  ON COALESCE(t.code_3, 'unknown') = COALESCE(b.code_3, 'unknown')
 GROUP BY t.code_3, b.badcount,
  CASE WHEN t.divert IS NULL THEN 'deny' WHEN t.divert IS TRUE THEN 'permit'
   ELSE 'allow' END || ' ' || COALESCE(t.country_name, 'unknown')
)
UNION
-- Uses scalar SELECTs
(SELECT 'Total bad status' AS "Statistic",
 (SELECT count(*) FROM tmp AS t) AS "count",
 '=' || (select iso.isocount FROM iso) AS "ISO3",
 (SELECT count(*) FROM tmp AS t
  WHERE t.status / 100 NOT BETWEEN 2 AND 3)::INTEGER AS "status",
 null::INTEGER AS "serverport",
 'during ' || p.dys || ' day' || COALESCE(CASE p.dys WHEN 1 THEN '' END, 's')
  AS "information",
 24 AS "ordering"
 FROM p
)) AS Z ORDER BY "ordering", "ISO3", "status", "serverport", "information";
