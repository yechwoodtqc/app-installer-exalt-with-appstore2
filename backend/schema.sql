CREATE TABLE IF NOT EXISTS authorization_codes (
  code_hash TEXT PRIMARY KEY,
  expires_at INTEGER NOT NULL,
  used_at INTEGER,
  created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS devices (
  device_id TEXT PRIMARY KEY,
  token_hash TEXT NOT NULL UNIQUE,
  created_at INTEGER NOT NULL,
  revoked_at INTEGER
);
CREATE TABLE IF NOT EXISTS apps (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  version TEXT NOT NULL,
  size_bytes INTEGER NOT NULL DEFAULT 0,
  package_name TEXT NOT NULL,
  apk_url TEXT NOT NULL,
  icon_url TEXT NOT NULL DEFAULT '',
  enabled INTEGER NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS install_reports (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  device_id TEXT NOT NULL,
  package_name TEXT NOT NULL,
  version TEXT NOT NULL,
  status TEXT NOT NULL,
  reported_at INTEGER NOT NULL
);
