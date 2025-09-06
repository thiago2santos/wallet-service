-- Create replication user
CREATE USER 'repl'@'%' IDENTIFIED BY 'repl';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';

-- Grant read/write permissions to wallet user
GRANT ALL PRIVILEGES ON wallet.* TO 'wallet'@'%';

-- Flush privileges
FLUSH PRIVILEGES;

-- Configure replication settings (log_bin is set via command line)
SET GLOBAL server_id = 1;
SET GLOBAL binlog_format = 'ROW';
SET GLOBAL binlog_row_image = 'FULL';
