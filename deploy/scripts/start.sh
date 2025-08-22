#!/bin/bash

db_url="jdbc:postgresql://localhost:26257/ledger?ssl=true&sslmode=require"
db_user=root
db_password=cockroach
spring_profile="default"

nohup java -jar ledger.jar \
--spring.datasource.url="${db_url}" \
--spring.datasource.username=${db_user} \
--spring.datasource.password=${db_password} \
--spring.profiles.active="${spring_profile}" \
> ledger-stdout.log 2>&1 &
