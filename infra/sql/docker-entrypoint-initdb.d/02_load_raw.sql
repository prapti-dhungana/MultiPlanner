COPY naptan_raw
FROM '/data/naptan/Stops.csv'
WITH (FORMAT csv, HEADER true);
