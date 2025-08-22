#!/bin/bash

# Secure mode
db_url="jdbc:postgresql://localhost:26257/ledger?ssl=true&sslmode=require"
db_user=root
db_password=cockroach
spring_profiles="default"

java -jar ledger.jar \
--spring.datasource.url="${db_url}" \
--spring.datasource.username=${db_user} \
--spring.datasource.password=${db_password} \
--spring.profiles.active="${spring_profiles}" @*
