#!/bin/bash

db_url="jdbc:postgresql://localhost:26257/ledger??ssl=true&sslmode=require"
db_user=root
db_password=cockroach
spring_profile="default"

######################################################

pid=$(ps -ef | grep "java" | grep "ledger.jar" | awk '{print $2}')
if [ ! -x ${pid} ]; then
   echo -e "Existing process found (${pid}) - is it running?"
   exit 1
fi

echo -e "Inspecting pom.xml version.."
pomVersion=$(echo 'VERSION=${project.version}' | ./mvnw help:evaluate | grep '^VERSION=' | sed 's/^VERSION=//g')
app_jarfile=target/ledger-${pomVersion}.jar

if [ ! -f "$app_jarfile" ]; then
    echo -e "Building jar.."
    ./mvnw clean install
fi

java -jar $app_jarfile \
#--spring.datasource.url="${db_url}" \
#--spring.datasource.username=${db_user} \
#--spring.datasource.password=${db_password} \
#--spring.profiles.active="${spring_profile}" \
$*
