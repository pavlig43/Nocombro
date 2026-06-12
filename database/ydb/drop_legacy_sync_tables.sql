-- Run manually only after every installed client uses mirror sync and
-- local/mirror recovery has been verified.
--
-- The application never executes this file.

DROP TABLE IF EXISTS `sync_push_log`;
DROP TABLE IF EXISTS `reminder_email_source`;
DROP TABLE IF EXISTS `experiment_reminder_email_source`;
