#!/bin/bash

echo Starting backup $(date)
cd /var/atlassian
tar -czf - application-data | aws s3 cp - s3://nitor-confluence-backup/confluence-files.tar.gz &
mysqldump --single-transaction -u confluence -psalasana confluence | gzip | aws s3 cp - s3://nitor-confluence-backup/confluence-mysql.sql.gz &
wait
echo Backup finished $(date)

