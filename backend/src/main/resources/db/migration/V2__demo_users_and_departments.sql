-- DEMO CREDENTIALS - synthetic demo-only accounts for the sample dataset.
-- These are public by design (portfolio/demo project); never reuse them anywhere real.
--   username    / password           / role
--   admin       / Admin@HRD-Demo     / ADMIN       (full access incl. user administration)
--   hr.manager  / HrManager@HRD-Demo / HR_MANAGER  (data steward, approves incentives)
--   viewer      / Viewer@HRD-Demo    / VIEWER      (read-only business data)
--   auditor     / Auditor@HRD-Demo   / AUDITOR     (read-only plus audit trail)
-- Passwords are stored as bcrypt hashes (cost 10). All data in this database
-- is SYNTHETIC sample data.

INSERT INTO users (id, username, password, full_name, role, enabled, created_at) VALUES
(1, 'admin',      '$2a$10$iP3XkUbowN33lAr.3kZKr.EQqzrS3kMesCU3JRDCR6w2JfmP9SO.G', 'System Administrator', 'ADMIN',      TRUE, '2026-01-01T00:00:00Z'),
(2, 'hr.manager', '$2a$10$MH/Sh.8PAeypv4x0TK/83OR/4Zk8q7oxa2KL8Pd9PfF1e4Y/TO0BK', 'HR Manager',           'HR_MANAGER', TRUE, '2026-01-01T00:00:00Z'),
(3, 'viewer',     '$2a$10$kZHnwcbXbG/l4njKID3iyeGG1YWV0X8bDPvKipbc1PrBJ8pkx1W6W', 'Plant Viewer',         'VIEWER',     TRUE, '2026-01-01T00:00:00Z'),
(4, 'auditor',    '$2a$10$hTSOs5epsn4nHi8s1dN5nujWq/BOq/6Ps1k0lfCSmAiQc5.nKs3Xa', 'Internal Auditor',     'AUDITOR',    TRUE, '2026-01-01T00:00:00Z');

-- Department ids 1-8 are fixed; V3 (synthetic employees/production/earnings)
-- references these ids. Ids 1-5 are PRODUCTION shops, 6-8 are SUPPORT.
INSERT INTO departments (id, code, name, type) VALUES
(1, 'MELT',   'Blast Furnace',           'PRODUCTION'),
(2, 'SMS',    'Steel Melting Shop',      'PRODUCTION'),
(3, 'ROLL',   'Rolling Mill',            'PRODUCTION'),
(4, 'FAB',    'Fabrication Shop',        'PRODUCTION'),
(5, 'ENERGY', 'Power and Blowing Station', 'PRODUCTION'),
(6, 'MAINT',  'Maintenance',             'SUPPORT'),
(7, 'QUAL',   'Quality Control',         'SUPPORT'),
(8, 'LOGS',   'Logistics and Yard',      'SUPPORT');

-- Explicit-id inserts do not advance IDENTITY sequences; reseed both past
-- their seeded maxima so application inserts never collide on the PK.
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('departments', 'id'), (SELECT MAX(id) FROM departments));
