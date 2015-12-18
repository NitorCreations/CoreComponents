#!/bin/bash

FILE=$3
BUCKET=$2
ROLE=$1
if [ -z "$4" ]; then
  OUT=$(basename ${FILE})
else
  OUT=$4
fi
CONTENT_TYPE="application/octet-stream"
DATE=$(date -R)
RESOURCE="/${BUCKET}/${FILE}"
TMP=$(mktemp)
curl -s http://169.254.169.254/latest/meta-data/iam/security-credentials/${ROLE} | egrep ^[[:space:]]*\" | sed 's/[^\"]*\"\([^\"]*\)\".:.\"\([^\"]*\).*/\1=\2/g' > $TMP
source $TMP
rm -f $TMP
SIGNSTR="GET\n\n${CONTENT_TYPE}\n${DATE}\nx-amz-security-token:${Token}\n${RESOURCE}"
SIGNATURE=$(echo -en ${SIGNSTR} | openssl sha1 -hmac ${SecretAccessKey} -binary | base64)
exec curl -s -o $OUT  -X GET -H "Host: ${BUCKET}.s3.amazonaws.com" -H "Date: ${DATE}" -H "Content-Type: ${CONTENT_TYPE}" -H "Authorization: AWS ${AccessKeyId}:${SIGNATURE}" -H "x-amz-security-token: ${Token}" https://${BUCKET}.s3.amazonaws.com/${FILE}
