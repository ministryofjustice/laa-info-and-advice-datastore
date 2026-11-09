# RDS CA bundle

`rds-eu-west-2-bundle.pem` holds the Amazon RDS root CAs for eu-west-2. The Docker image copies it to `/opt/laa-info-and-advice-datastore/rds-ca-bundle.pem`, and the Helm `DB_URL` uses it with `sslmode=verify-full` so the app verifies the RDS server certificate (CTRL-054).

These are public certificates. To refresh them:

```sh
curl -sSo certs/rds-eu-west-2-bundle.pem https://truststore.pki.rds.amazonaws.com/eu-west-2/eu-west-2-bundle.pem
```

Do this when AWS announces an RDS CA rotation, and before the instances move to a new CA.
