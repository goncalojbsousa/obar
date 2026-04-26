-- Driver approval workflow
-- Adds an optional approval_note column to users for admin feedback on driver approval/rejection.
ALTER TABLE users ADD COLUMN IF NOT EXISTS approval_note VARCHAR;
