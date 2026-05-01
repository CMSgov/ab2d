INSERT INTO property.properties (id, key, value, created, modified)
SELECT (SELECT COALESCE(MAX(id),0)+1 FROM property.properties),
       'v3.idr-importer.status', 'import_not_in_progress', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'v3.idr-importer.status');
