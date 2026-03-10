#!/bin/bash

db_url="jdbc:postgresql://localhost:26257/ledger?sslmode=require&allow_unsafe_internals=true"
db_user=root
db_password=cockroach
spring_profiles="default"

nohup java -jar ledger.jar \
--spring.datasource.url="${db_url}" \
--spring.datasource.username=${db_user} \
--spring.datasource.password=${db_password} \
--spring.profiles.active="${spring_profiles}" \
> ledger-stdout.log 2>&1 &
