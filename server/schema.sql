CREATE TABLE IF NOT EXISTS daily_budget (
  day TEXT PRIMARY KEY, reserved INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS image_requests (
  id TEXT PRIMARY KEY, day TEXT NOT NULL, cost INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending'
);
CREATE TRIGGER IF NOT EXISTS reserve_image_budget BEFORE INSERT ON image_requests
BEGIN
  SELECT CASE WHEN COALESCE((SELECT reserved FROM daily_budget WHERE day=NEW.day),0)+NEW.cost>5000
    THEN RAISE(ABORT,'DAILY_IMAGE_LIMIT') END;
  INSERT INTO daily_budget(day,reserved) VALUES(NEW.day,NEW.cost)
    ON CONFLICT(day) DO UPDATE SET reserved=reserved+NEW.cost;
END;
