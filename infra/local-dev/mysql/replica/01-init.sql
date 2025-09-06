-- Create local wallet user first (not replicated)
SET SQL_LOG_BIN = 0;
CREATE USER IF NOT EXISTS 'wallet'@'%' IDENTIFIED BY 'wallet';
GRANT SELECT ON wallet.* TO 'wallet'@'%';
FLUSH PRIVILEGES;
SET SQL_LOG_BIN = 1;

-- Configure replica (using new MySQL 8.0 syntax)
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-primary',
  SOURCE_USER='repl',
  SOURCE_PASSWORD='repl',
  SOURCE_PORT=3306,
  SOURCE_AUTO_POSITION=1;

-- Configure replica settings (server_id and log_bin set via command line)
SET GLOBAL binlog_format = 'ROW';
SET GLOBAL binlog_row_image = 'FULL';

-- Start replica (using new MySQL 8.0 syntax)
START REPLICA;
