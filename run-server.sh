#!/bin/bash

db_url="jdbc:postgresql://localhost:26257/ledger?sslmode=disable"
db_user=root
db_password=
#spring_profile="default"
spring_profile="test"

######################################################

pid=$(ps -ef | grep "java" | grep "ledger.jar" | awk '{print $2}')
if [ ! -x ${pid} ]; then
   echo -e "Existing process found (${pid}) - is it running?"
   exit 1
fi

app_jarfile=target/ledger.jar

if [ ! -f "$app_jarfile" ]; then
    echo -e "Building jar.."
    ./mvnw clean install
fi

java -jar $app_jarfile \
--spring.datasource.url="${db_url}" \
--spring.datasource.username=${db_user} \
--spring.datasource.password=${db_password} \
--spring.profiles.active="${spring_profile}" \
$*
