-- Create the new schema
CREATE SCHEMA IF NOT EXISTS property;

-- Create temporary table and populate it with all the properties from ab2d
CREATE TEMPORARY TABLE properties_tmp (like public.properties);
INSERT INTO properties_tmp (SELECT * FROM public.properties);

-- Create new table if it doesn't already exist
CREATE TABLE IF NOT EXISTS property.properties (like public.properties including all);

-- Create a sequence if it doesn't already exist
CREATE SEQUENCE IF NOT EXISTS property.property_sequence
    AS integer OWNED BY property.properties.id;

-- Insert any missing values into the properties table. If this is run a second time, it will not duplicate the values
INSERT INTO property.properties (id, key, value, created, modified)
    SELECT t.id, t.key, t.value, t.created, t.modified
    FROM properties_tmp t
        LEFT JOIN property.properties p ON p.key = t.key WHERE p.key is null;

-- Update the sequence value to the greatest value of the existing properties
SELECT setval('property.property_sequence', max(id)) FROM property.properties;

-- Drop the temp table - clean up
DROP TABLE properties_tmp;

-- Let Quicksight query this table if it ever wants to
GRANT SELECT ON property.properties TO ab2d_analyst;
