INSERT INTO stations (
  atco_code,
  naptan_code,
  name,
  locality,
  stop_type,
  lat,
  lon
)
SELECT
  "ATCOCode",
  "NaptanCode",
  "CommonName",
  "LocalityName",
  "StopType",
  NULLIF("Latitude",'')::double precision,
  NULLIF("Longitude",'')::double precision
FROM naptan_raw
WHERE "StopType" IN ('RLY', 'RSE')
  AND (
    LEFT("ATCOCode", 3) = '490'
    OR "AdministrativeAreaCode" = '82'
  );
