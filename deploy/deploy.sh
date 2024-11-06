#!/bin/bash


set -o allexport
source ./azure.secrets
set +o allexport

SECRETS_FILE_LOCATION="../src/main/resources/application.secrets"

trap ctrl_c INT

function ctrl_c() {
  jobs="$(jobs -p)"; [ -n "$jobs" ] && kill $jobs
}

add_secret() {
  local secret_key="$1"
  local secret_value="$2"
  local secrets_files="$3"

  for secrets_file in $secrets_files 
  do
    if ! grep -q "$secret_key" "$secrets_file"; then
      while true;
      do
        (flock -n 9 
          echo "$secret_key=$secret_value" >> "$secrets_file"
        ) 9> /var/tmp/lock
        if [ $? -eq 0 ]; then
          break
        fi
      done
    else
      while true;
      do
        (flock -n 9 
          sed -i "/$secret_key/c\\$secret_key=$secret_value" $secrets_file
        ) 9> /var/tmp/lock
      if [ $? -eq 0 ]; then
          break
      fi
      done
    fi
  done
}


export AZURE_REGIONS="francecentral canadacentral"
export SECRET_FILES="./francecentral.secrets ./canadacentral.secrets"
regions=($AZURE_REGIONS)
secret_files=($SECRET_FILES)

export USED_DB_TYPE=COSMOS
export AZURE_RESOURCE_GROUP=rg-Tukano-60045-60174
export AZURE_RESOURCE_GROUP_LOCATION=${regions[0]}
export AZURE_APP_NAME_BASE=astukano6004560174
export AZURE_SERVICE_PLAN_BASE=asptukano6004560174
export AZURE_REDIS_NAME_BASE=redistukano6004560174
export AZURE_COSMOSDB_NAME_BASE=cosmostukano6004560174
export AZURE_COSMOSDB_DATABASE_NAME=db-$AZURE_COSMOSDB_NAME_BASE
export AZURE_COSMOSDB_POSTGRESQL_NAME_BASE=sqltukano6004560174
export AZURE_BLOB_STORAGE_ACCOUNT_NAME_BASE=stk60045
export AZURE_FUNCTIONS_NAME_BASE=funtukano6004560174
export AZURE_FUNCTIONS_STORAGE_ACCOUNT_NAME=sfuntk6004560174
export AZURE_TRAFFIC_MANAGER_NAME_BASE=tm
export AZURE_TRAFFIC_MANAGER_SUFFIX=.trafficmanager.net


DEPLOY_BLOBS=false
DEPLOY_COSMOSDB_POSTGRESQL=false
DEPLOY_COSMOSDB=false
DEPLOY_REDIS=false
DEPLOY_FUNCTIONS=false
DEPLOY_APP=false
DEPLOY_FUNCTIONS_TRAFFIC_MANAGER=false
DEPLOY_APP_TRAFFIC_MANAGER=false
DO_CERTIFICATE=false
LOCAL_TOMCAT=false

main() {
  add_secret AZURE_REGION ${regions[0]} ${secret_files[0]}
  add_secret AZURE_REGION ${regions[1]} ${secret_files[1]}
  add_secret TOKEN_SECRET $TOKEN_SECRET "$SECRET_FILES"
  add_secret USED_DB_TYPE $USED_DB_TYPE "$SECRET_FILES"
  add_secret AZURE_FUNCTIONS_URL http://$AZURE_FUNCTIONS_NAME_BASE$AZURE_TRAFFIC_MANAGER_SUFFIX/api  "$SECRET_FILES"
  az group create -l $AZURE_RESOURCE_GROUP_LOCATION -n $AZURE_RESOURCE_GROUP 1> /dev/null
  export AZURE_SUBSCRIPTION=$(az account show --query "id" | tr -d '"')
  if $DEPLOY_COSMOSDB; then
    (deploy_cosmosdb) &
  fi
  if $DEPLOY_COSMOSDB_POSTGRESQL; then
    (deploy_postgresql) &
  fi
  if $DEPLOY_BLOBS; then
    (deploy_blobs) &
  fi
  if $DEPLOY_REDIS; then
    (deploy_redis) &
  fi
  if $DEPLOY_APP; then
    (deploy_app_1) &
  fi
  wait
  if $DEPLOY_APP; then
    deploy_app_2
  fi
  if $DEPLOY_FUNCTIONS; then
    deploy_functions
  fi
  if $DEPLOY_FUNCTIONS_TRAFFIC_MANAGER; then
      deploy_traffic_manager $AZURE_FUNCTIONS_NAME_BASE
  fi
  if $DEPLOY_APP_TRAFFIC_MANAGER; then
    deploy_traffic_manager $AZURE_APP_NAME_BASE
  fi
}

deploy_traffic_manager() {
  local service="$1"

  az network traffic-manager profile create \
    --name $AZURE_TRAFFIC_MANAGER_NAME_BASE$service \
    --resource-group $AZURE_RESOURCE_GROUP \
    --routing-method Performance \
    --path '/' \
    --protocol "HTTPS" \
    --unique-dns-name $service  \
    --ttl 30 \
    --port 443 1> /dev/null \
  && echo "Created Traffic Manager"

  INDEX=0
  for region in $AZURE_REGIONS;
  do
    let priority=${INDEX}+1
    az network traffic-manager endpoint create \
        --name $service${regions[$INDEX]} \
        --resource-group $AZURE_RESOURCE_GROUP \
        --profile-name $AZURE_TRAFFIC_MANAGER_NAME_BASE$service \
        --type azureEndpoints \
        --target-resource-id  $(az webapp show --name $service${regions[$INDEX]} --resource-group $AZURE_RESOURCE_GROUP --query id --output tsv) \
        --endpoint-status Enabled 1> /dev/null

    if $DO_CERTIFICATE; then
      FQDN=$(az network traffic-manager profile show --name $AZURE_TRAFFIC_MANAGER_NAME_BASE$service --resource-group $AZURE_RESOURCE_GROUP --query dnsConfig.fqdn --output tsv)
      az webapp config ssl create --resource-group $AZURE_RESOURCE_GROUP --certificate-name 1testscc --name $service${regions[$INDEX]} --hostname $FQDN 1> /dev/null
      if [ $? -ne 0 ]; then
          echo "Can't create"
          return
      fi
      while true;
      do
        THUMBPRINT=$(az webapp config ssl show -g rg-Tukano-60045-60174 --certificate-name $FQDN --query "thumbprint" --output tsv)
        if [ $? -eq 0 ]; then
          break
        fi
        sleep 10
      done
      az webapp config ssl bind --certificate-thumbprint $THUMBPRINT --name $service${regions[INDEX]} --resource-group $AZURE_RESOURCE_GROUP --ssl-type SNI 1> /dev/null    
    fi
    let INDEX=${INDEX}+1
  done
  wait
}

deploy_cosmosdb() {
  while true;
  do
    if [ ${#regions[@]} -eq 1 ]; then
      az cosmosdb create \
      --name $AZURE_COSMOSDB_NAME_BASE \
      --resource-group $AZURE_RESOURCE_GROUP 1> /dev/null \
      && echo "Created CosmosDB for ${region[0]}"
    else
      az cosmosdb create \
      --name $AZURE_COSMOSDB_NAME_BASE \
      --resource-group $AZURE_RESOURCE_GROUP \
      --locations regionName=${regions[0]} failoverPriority=0 isZoneRedundant=False \
      --locations regionName=${regions[1]} failoverPriority=1 isZoneRedundant=True \
      --enable-multiple-write-locations 1> /dev/null \
      && echo "Created CosmosDB for ${region[0]} and ${region[1]}"
    fi
    if [ $? -eq 0 ]; then
      break
    fi
    echo "CosmosDB deployment failed for regions $AZURE_REGIONS, deleting and retrying in 20 seconds."
    sleep 10
    az cosmosdb delete --name $AZURE_COSMOSDB_NAME_BASE --resource-group $AZURE_RESOURCE_GROUP --yes
    sleep 10
  done
  az cosmosdb sql database create -a $AZURE_COSMOSDB_NAME_BASE --name $AZURE_COSMOSDB_DATABASE_NAME --resource-group $AZURE_RESOURCE_GROUP --throughput "400" 1> /dev/null 
  az cosmosdb sql container create -g $AZURE_RESOURCE_GROUP -a $AZURE_COSMOSDB_NAME_BASE -d $AZURE_COSMOSDB_DATABASE_NAME -n "shorts" --partition-key-path "/id" 1> /dev/null 
  az cosmosdb sql container create -g $AZURE_RESOURCE_GROUP -a $AZURE_COSMOSDB_NAME_BASE -d $AZURE_COSMOSDB_DATABASE_NAME -n "likes" --partition-key-path "/shortId" 1> /dev/null 
  az cosmosdb sql container create -g $AZURE_RESOURCE_GROUP -a $AZURE_COSMOSDB_NAME_BASE -d $AZURE_COSMOSDB_DATABASE_NAME -n "follows" --partition-key-path "/followee" 1> /dev/null 
  az cosmosdb sql container create -g $AZURE_RESOURCE_GROUP -a $AZURE_COSMOSDB_NAME_BASE -d $AZURE_COSMOSDB_DATABASE_NAME -n "users" --partition-key-path "/id" 1> /dev/null 

  AZURE_COSMOSDB_PRIMARY_KEY=$(az cosmosdb keys list --name $AZURE_COSMOSDB_NAME_BASE --resource-group $AZURE_RESOURCE_GROUP --type keys --query "primaryMasterKey" | tr -d '"')
  AZURE_COSMOSDB_PRIMARY_ENDPOINT=$(az cosmosdb show --name $AZURE_COSMOSDB_NAME_BASE --resource-group $AZURE_RESOURCE_GROUP --query "documentEndpoint" | tr -d '"')
  
  # write the secrets to the appropriate region secret file
  add_secret AZURE_COSMOSDB_KEY $AZURE_COSMOSDB_PRIMARY_KEY "$SECRET_FILES" 
  add_secret AZURE_COSMOSDB_ENDPOINT $AZURE_COSMOSDB_PRIMARY_ENDPOINT "$SECRET_FILES"
  add_secret AZURE_COSMOSDB_NAME $AZURE_COSMOSDB_DATABASE_NAME "$SECRET_FILES"
}

deploy_postgresql() {
  AZURE_COSMOSDB_POSTGRESQL_PWD=$(pwgen -N 1 -n 100)

  az cosmosdb postgres cluster create \
  --name $AZURE_COSMOSDB_POSTGRESQL_NAME_BASE${regions[0]} \
  --resource-group $AZURE_RESOURCE_GROUP \
  --subscription $AZURE_SUBSCRIPTION \
  --enable-ha false \
  --coordinator-v-cores 2 \
  --coordinator-server-edition "GeneralPurpose" \
  --coordinator-storage 131072 \
  --enable-shards-on-coord true \
  --coord-public-ip-access true \
  --node-enable-public-ip-access true \
  --node-count 0 \
  --administrator-login-password $AZURE_COSMOSDB_POSTGRESQL_PWD 1> /dev/null \
  && echo "Created Postgres Cluster #1"

  az cosmosdb postgres cluster create \
  --name $AZURE_COSMOSDB_POSTGRESQL_NAME_BASE${regions[1]} \
  --resource-group $AZURE_RESOURCE_GROUP \
  --subscription $AZURE_SUBSCRIPTION \
  --source-location ${regions[0]} \
  --source-resource-id $(az cosmosdb postgres cluster show -n $AZURE_COSMOSDB_POSTGRESQL_NAME_BASE${regions[0]} -g $AZURE_RESOURCE_GROUP --subscription $AZURE_SUBSCRIPTION --query "id" | tr -d '"') 1> /dev/null \
  && echo "Created Postgres Cluster #2"

  AZURE_POSTGRES_USER=$(az cosmosdb postgres cluster show -n $AZURE_COSMOSDB_POSTGRESQL_NAME_BASE${regions[0]} -g $AZURE_RESOURCE_GROUP --subscription $AZURE_SUBSCRIPTION --query "administratorLogin" | tr -d '"')
  AZURE_POSTGRES_URI=$(az cosmosdb postgres cluster show -g $AZURE_RESOURCE_GROUP -n $AZURE_COSMOSDB_POSTGRESQL_NAME_BASE${regions[0]} --query "serverNames[0].fullyQualifiedDomainName" | tr -d '"')
  AZURE_POSTGRES_JDBC="jdbc:postgresql://$AZURE_POSTGRES_URI:5432/citus?sslmode=require"

  add_secret HIBERNATE_USERNAME $AZURE_POSTGRES_USER "$SECRET_FILES"
  add_secret HIBERNATE_PWD $AZURE_COSMOSDB_POSTGRESQL_PWD "$SECRET_FILES"
  add_secret HIBERNATE_JDBC_URL $AZURE_POSTGRES_JDBC "$SECRET_FILES"
}

deploy_blobs() {
  az storage account create \
    --name $AZURE_BLOB_STORAGE_ACCOUNT_NAME_BASE \
    --resource-group $AZURE_RESOURCE_GROUP \
    --location ${regions[0]} \
    --kind StorageV2 \
    --sku "Standard_RAGRS" 1> /dev/null &&\
  echo "Created"

  az storage container create \
        --name "shorts" \
        --account-name $AZURE_BLOB_STORAGE_ACCOUNT_NAME_BASE \
        --auth-mode login 1> /dev/null

  AZURE_BLOB_STORE_CONNECTION=$(az storage account show-connection-string --name $AZURE_BLOB_STORAGE_ACCOUNT_NAME_BASE --resource-group $AZURE_RESOURCE_GROUP --query "connectionString" | tr -d '"')
  add_secret AZURE_BLOB_STORE_CONNECTION $AZURE_BLOB_STORE_CONNECTION "$SECRET_FILES"
}

deploy_redis() {
  INDEX=0
  for region in $AZURE_REGIONS
  do
      az redis create --name $AZURE_REDIS_NAME_BASE$region --resource-group $AZURE_RESOURCE_GROUP --location "$region" --sku Basic --vm-size c0 1> /dev/null
      redis=($(az redis show --name $AZURE_REDIS_NAME_BASE$region --resource-group $AZURE_RESOURCE_GROUP --query [hostName,enableNonSslPort,port,sslPort] --output tsv))
      keys=($(az redis list-keys --name $AZURE_REDIS_NAME_BASE$region --resource-group $AZURE_RESOURCE_GROUP --query [primaryKey,secondaryKey] --output tsv))
      add_secret REDIS_URL ${redis[0]} "${secret_files[$INDEX]}"
      add_secret REDIS_KEY ${keys[0]} "${secret_files[$INDEX]}"
      add_secret REDIS_PORT ${redis[3]} "${secret_files[$INDEX]}"
    
    let INDEX=${INDEX}+1
  done
  wait
}

deploy_app_1() {
  for region in $AZURE_REGIONS
  do 
    az appservice plan create \
    --resource-group $AZURE_RESOURCE_GROUP \
    --name $AZURE_SERVICE_PLAN_BASE$region \
    --is-linux \
    --number-of-workers 1 \
    --location $region \
    --sku S1 1> /dev/null &&
    echo "Deployed App Service Plan"

    az webapp create \
    --resource-group $AZURE_RESOURCE_GROUP \
    --plan $AZURE_SERVICE_PLAN_BASE$region \
    --name $AZURE_APP_NAME_BASE$region \
    --runtime "TOMCAT:10.0-java17" 1> /dev/null && 
    echo "Deployed App"
  done
}

deploy_app_2() {
  INDEX=0
  for region in $AZURE_REGIONS
  do 
    export AZURE_REGION=$region # used in POM
    export AZURE_JAVA_PACKAGING="war"
    cp $region.secrets $SECRETS_FILE_LOCATION
    if $LOCAL_TOMCAT; then
      envsubst "$(printf '${%s} ' ${!AZURE*})" < ../pom.xml > ../_pom.xml && \
      mvn -f ../_pom.xml clean compile package tomcat7:redeploy
    else
      envsubst "$(printf '${%s} ' ${!AZURE*})" < ../pom.xml > ../_pom.xml && \
      mvn -f ../_pom.xml clean compile package azure-webapp:deploy
    fi
      rm ../_pom.xml
  done
}

deploy_functions() {
  INDEX=0
  for region in $AZURE_REGIONS
  do
    export AZURE_REGION=$region
    export AZURE_JAVA_PACKAGING="jar"
    cp $region.secrets $SECRETS_FILE_LOCATION
    envsubst "$(printf '${%s} ' ${!AZURE*})" < ../pom.xml > ../_pom.xml && \
    mvn -f ../_pom.xml clean compile package azure-functions:deploy
    rm ../_pom.xml
    let INDEX=${INDEX}+1
  done
}
main