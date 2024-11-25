# Bulk Import Tutorial

Execute the following SQL to import a default account plan:

```sql
IMPORT INTO account (id,city,name,balance,currency,allow_negative,type,updated_at)
    CSV DATA (
    'http://localhost:9090/api/import/account.csv/us-east-1',
    'http://localhost:9090/api/import/account.csv/us-east-2',
    'http://localhost:9090/api/import/account.csv/us-west-1',
    'http://localhost:9090/api/import/account.csv/us-west-2',
    'http://localhost:9090/api/import/account.csv/us-central-1',
    'http://localhost:9090/api/import/account.csv/eu-central-1',
    'http://localhost:9090/api/import/account.csv/eu-central-2',
    'http://localhost:9090/api/import/account.csv/eu-west-1',
    'http://localhost:9090/api/import/account.csv/eu-west-2',
    'http://localhost:9090/api/import/account.csv/eu-west-3',
    'http://localhost:9090/api/import/account.csv/eu-south-1',
    'http://localhost:9090/api/import/account.csv/eu-south-2',
    'http://localhost:9090/api/import/account.csv/eu-north-1',
    'http://localhost:9090/api/import/account.csv/ap-northeast-1',
    'http://localhost:9090/api/import/account.csv/ap-southeast-2',
    'http://localhost:9090/api/import/account.csv/me-south-1',
    'http://localhost:9090/api/import/account.csv/ca-central-1',
    'http://localhost:9090/api/import/account.csv/sa-east-1',
    'http://localhost:9090/api/import/account.csv/af-south-1'
    ) WITH delimiter = ',', skip = '1';
```

Alternatively, put the import statement into a SQL file and execute:    

    cockroach sql --insecure --database ledger < import.sql
