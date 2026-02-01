CREATE EXTENSION IF NOT EXISTS pg_trgm;

DROP TABLE IF EXISTS stations;
DROP TABLE IF EXISTS naptan_raw;

-- Must match Stops.csv columns (same order)
CREATE TABLE naptan_raw (
  "ATCOCode" TEXT,
  "NaptanCode" TEXT,
  "PlateCode" TEXT,
  "CleardownCode" TEXT,
  "CommonName" TEXT,
  "CommonNameLang" TEXT,
  "ShortCommonName" TEXT,
  "ShortCommonNameLang" TEXT,
  "Landmark" TEXT,
  "LandmarkLang" TEXT,
  "Street" TEXT,
  "StreetLang" TEXT,
  "Crossing" TEXT,
  "CrossingLang" TEXT,
  "Indicator" TEXT,
  "IndicatorLang" TEXT,
  "Bearing" TEXT,
  "NptgLocalityCode" TEXT,
  "LocalityName" TEXT,
  "ParentLocalityName" TEXT,
  "GrandParentLocalityName" TEXT,
  "Town" TEXT,
  "TownLang" TEXT,
  "Suburb" TEXT,
  "SuburbLang" TEXT,
  "LocalityCentre" TEXT,
  "GridType" TEXT,
  "Easting" TEXT,
  "Northing" TEXT,
  "Longitude" TEXT,
  "Latitude" TEXT,
  "StopType" TEXT,
  "BusStopType" TEXT,
  "TimingStatus" TEXT,
  "DefaultWaitTime" TEXT,
  "Notes" TEXT,
  "NotesLang" TEXT,
  "AdministrativeAreaCode" TEXT,
  "CreationDateTime" TEXT,
  "ModificationDateTime" TEXT,
  "RevisionNumber" TEXT,
  "Modification" TEXT,
  "Status" TEXT
);

CREATE TABLE stations (
  atco_code TEXT PRIMARY KEY,
  naptan_code TEXT,
  name TEXT NOT NULL,
  locality TEXT,
  town TEXT,
  stop_type TEXT,
  lat DOUBLE PRECISION,
  lon DOUBLE PRECISION
);

-- Autocomplete / similarity
CREATE INDEX stations_name_trgm_idx ON stations USING gin (name gin_trgm_ops);
CREATE INDEX stations_locality_trgm_idx ON stations USING gin (locality gin_trgm_ops);
CREATE INDEX stations_town_trgm_idx ON stations USING gin (town gin_trgm_ops);
