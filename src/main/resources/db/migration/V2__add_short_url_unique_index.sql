-- Redirect lookups filter on short_url; without an index every cache miss is a
-- full table scan. Unique because each short code maps to exactly one URL.
CREATE UNIQUE INDEX url_short_url_key ON url (short_url);
