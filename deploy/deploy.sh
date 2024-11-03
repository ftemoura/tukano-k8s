#!/bin/bash
set -x #echo on

#set -o allexport
#source ./azure.config
#set +o allexport

SECRETS_FILE_LOCATION="../src/java/resources/application.secrets"

add_secret() {
  local secret_key="$1"
  local secret_value="$2"
  local secrets_files="$3"

  for secrets_file in $secrets_files 
  do
    if ! grep -q "$secret_key" "$secrets_file"; then
          echo "$secret_key=$secret_value" >> "$secrets_file"
    else
      sed -i "/$secret_key/c\\$secret_key=$secret_value" $secrets_file
    fi
  done
}

export AZURE_RESOURCE_GROUP=rg-Tukano-60045-60174
export AZURE_RESOURCE_GROUP_LOCATION=francecentral
export AZURE_APP_NAME_BASE=astukano6004560174
export AZURE_REDIS_NAME_BASE=redistukano6004560174
export AZURE_COSMOSDB_NAME_BASE=cosmostukano6004560174
export AZURE_COSMOSDB_POSTGRESQL_NAME_BASE=sqltukano6004560174
export AZURE_BLOB_STORAGE_ACCOUNT_NAME_BASE=stk60045
export AZURE_REGIONS="francecentral canadacentral"
export SECRET_FILES="./francecentral.secrets ./canadacentral.secrets"
export AZURE_FUNCTIONS_NAME_BASE=funtukano6004560174

regions=($AZURE_REGIONS)
secret_files=($SECRET_FILES)

DEPLOY_BLOBS=true
DEPLOY_COSMOSDB_POSTGRESQL=true
DEPLOY_COSMOSDB=false
DEPLOY_REDIS=true
DEPLOY_FUNCTIONS=false
DEPLOY_APP=false


az group create -l $AZURE_RESOURCE_GROUP_LOCATION -n $AZURE_RESOURCE_GROUP

AZURE_SUBSCRIPTION=$(az account show --query "id" | tr -d '"')

# COSMOS DB DEPLOY
if $DEPLOY_COSMOSDB; then
  # geo - verify regions len
  az cosmosdb create \
  --name $AZURE_COSMOSDB_NAME_BASE \
  --resource-group $AZURE_RESOURCE_GROUP \
  --locations regionName=${regions[0]} failoverPriority=0 isZoneRedundant=False \
  --locations regionName=${regions[1]} failoverPriority=1 isZoneRedundant=True \
  --enable-multiple-write-locations

  AZURE_COSMOSDB_PRIMARY_KEY=$(az cosmosdb keys list --name $AZURE_COSMOSDB_NAME_BASE --resource-group $AZURE_RESOURCE_GROUP --type keys --query "primaryMasterKey" | tr -d '"')
  AZURE_COSMOSDB_SECONDARY_KEY=$(az cosmosdb keys list --name $AZURE_COSMOSDB_NAME_BASE --resource-group $AZURE_RESOURCE_GROUP --type keys --query "secondaryMasterKey" | tr -d '"')
  AZURE_COSMOSDB_PRIMARY_ENDPOINT=$(az cosmosdb show --name $AZURE_COSMOSDB_NAME_BASE --resource-group $AZURE_RESOURCE_GROUP --query "writeLocations[0].documentEndpoint" | tr -d '"')
  AZURE_COSMOSDB_SECONDARY_ENDPOINT=$(az cosmosdb show --name $AZURE_COSMOSDB_NAME_BASE --resource-group $AZURE_RESOURCE_GROUP --query "writeLocations[1].documentEndpoint" | tr -d '"')
  AZURE_COSMOSDB_PRIMARY_NAME=$(az cosmosdb show --name $AZURE_COSMOSDB_NAME_BASE --resource-group $AZURE_RESOURCE_GROUP --query "writeLocations[0].id" | tr -d '"')
  AZURE_COSMOSDB_SECONDARY_NAME=$(az cosmosdb show --name $AZURE_COSMOSDB_NAME_BASE --resource-group $AZURE_RESOURCE_GROUP --query "writeLocations[1].id" | tr -d '"')

  # write the secrets to the appropriate region secret file

  add_secret AZURE_COSMOSDB_KEY $AZURE_COSMOSDB_PRIMARY_KEY "${secret_files[0]}"
  add_secret AZURE_COSMOSDB_KEY $AZURE_COSMOSDB_SECONDARY_KEY "${secret_files[1]}"

  add_secret AZURE_COSMOSDB_URL $AZURE_COSMOSDB_PRIMARY_ENDPOINT "${secret_files[0]}"
  add_secret AZURE_COSMOSDB_URL $AZURE_COSMOSDB_SECONDARY_ENDPOINT "${secret_files[1]}"

  add_secret AZURE_COSMOSDB_NAME $AZURE_COSMOSDB_PRIMARY_NAME "${secret_files[0]}"
  add_secret AZURE_COSMOSDB_NAME $AZURE_COSMOSDB_SECONDARY_NAME "${secret_files[1]}"
fi

# https://learn.microsoft.com/en-us/cli/azure/cosmosdb/postgres/cluster?view=azure-cli-latest#az-cosmosdb-postgres-cluster-create
# COSMOS DB POSTGRESQL DEPLOY
if $DEPLOY_COSMOSDB_POSTGRESQL; then

  AZURE_COSMOSDB_POSTGRESQL_PWD=$(pwgen -N 1 -n 100)

  az cosmosdb postgres cluster create \
  --name $AZURE_COSMOSDB_POSTGRESQL_NAME_BASE${regions[0]} \
  --resource-group $AZURE_RESOURCE_GROUP \
  --subscription $AZURE_SUBSCRIPTION \
  --enable-ha false \
  --coordinator-v-cores 4 \
  --coordinator-server-edition "GeneralPurpose" \
  --coordinator-storage 131072 \
  --enable-shards-on-coord true \
  --node-count 0 \
  --administrator-login-password $AZURE_COSMOSDB_POSTGRESQL_PWD

  az cosmosdb postgres cluster create \
  --name $AZURE_COSMOSDB_POSTGRESQL_NAME_BASE${regions[1]} \
  --resource-group $AZURE_RESOURCE_GROUP \
  --subscription $AZURE_SUBSCRIPTION \
  --source-location ${regions[0]} \
  --source-resource-id $(az cosmosdb postgres cluster show -n $AZURE_COSMOSDB_POSTGRESQL_NAME_BASE${regions[0]} -g $AZURE_RESOURCE_GROUP --subscription $AZURE_SUBSCRIPTION --query "id" | tr -d '"')

  AZURE_POSTGRES_USER=$(az cosmosdb postgres cluster show -n $AZURE_COSMOSDB_POSTGRESQL_NAME_BASE${regions[0]} -g $AZURE_RESOURCE_GROUP --subscription $AZURE_SUBSCRIPTION --query "administratorLogin" | tr -d '"')
  AZURE_POSTGRES_URI=$(az cosmosdb postgres cluster show -g rg-Tukano-60045-60174 -n sqltukano6004560174 --query "serverNames[0].fullyQualifiedDomainName" | tr -d '"')
  AZURE_POSTGRES_JDBC="jdbc:postgresql://$AZURE_POSTGRES_URI:5432/citus?sslmode=require"

  add_secret HIBERNATE_USERNAME $AZURE_POSTGRES_USER "$SECRET_FILES"
  add_secret HIBERNATE_PWD $AZURE_COSMOSDB_POSTGRESQL_PWD "$SECRET_FILES"
  add_secret HIBERNATE_JDBC_URL $AZURE_POSTGRES_JDBC "$SECRET_FILES"
fi

# BLOB STORAGE DEPLOY
if $DEPLOY_BLOBS; then
  az storage account create \
    --name $AZURE_BLOB_STORAGE_ACCOUNT_NAME_BASE \
    --resource-group $AZURE_RESOURCE_GROUP \
    --location ${regions[0]} \
    --kind StorageV2 \
    --sku "Standard_RAGRS"
  AZURE_BLOB_STORE_CONNECTION=$(az storage account show-connection-string --name $AZURE_BLOB_STORAGE_ACCOUNT_NAME_BASE --resource-group $AZURE_RESOURCE_GROUP --query "connectionString" | tr -d '"')
  add_secret AZURE_BLOB_STORE_CONNECTION $AZURE_BLOB_STORE_CONNECTION "$SECRET_FILES"
fi

INDEX=0
for region in $AZURE_REGIONS
do
  if $DEPLOY_REDIS; then
    az redis create --name $AZURE_REDIS_NAME_BASE$region --resource-group $AZURE_RESOURCE_GROUP --location "$region" --sku Basic --vm-size c0
    redis=($(az redis show --name "$redis_name" --resource-group $resourceGroup --query [hostName,enableNonSslPort,port,sslPort] --output tsv))
    keys=($(az redis list-keys --name "$redis_name" --resource-group $resourceGroup --query [primaryKey,secondaryKey] --output tsv))
    add_secret REDIS_URL ${redis[0]} "${secret_files[$INDEX]}"
    add_secret REDIS_KEY ${keys[0]} "${secret_files[$INDEX]}"
    add_secret REDIS_PORT ${redis[2]} "${secret_files[$INDEX]}"
  fi

  export AZURE_REGION=$region # used in POM
  if $DEPLOY_APP; then
    export AZURE_JAVA_PACKAGING="war"
    cp $region.secrets SECRETS_FILE_LOCATION
    envsubst "$(printf '${%s} ' ${!AZURE*})" < ../pom.xml > ../_pom.xml && \
    mvn -f ../_pom.xml clean compile package azure-webapp:deploy
    rm ../_pom.xml
  fi

  if $DEPLOY_FUNCTIONS; then
    export AZURE_JAVA_PACKAGING="jar"
    cp $region.secrets SECRETS_FILE_LOCATION
    envsubst "$(printf '${%s} ' ${!AZURE*})" < ../pom.xml > ../_pom.xml && \
    mvn -f ../_pom.xml clean compile package azure-functions:deploy
    rm ../_pom.xml
  fi

  let INDEX=${INDEX}+1
done



# Variable block


# Delete a redis cache
#echo "Deleting $cache"
#az redis delete --name "$cache" --resource-group $resourceGroup -y

# echo "Deleting all resources"
#az group delete --resource-group $resourceGroup -y