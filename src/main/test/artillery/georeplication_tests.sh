#!/bin/bash
set -x #echo on

LOCATION_RG=francecentral
REGIONS="northeurope francecentral eastasia australiacentral westus southafricanorth brazilsouth israelcentral"
RESOURCE_GROUP=containers-test-60174
ACR_NAME=acrcontainerstest60174
CONTAINER_NAME=containers-test-60174
IMAGE_NAME=artillery
SERVER_URI=https://tukano-60045-60174-as.azurewebsites.net/rest

az group create --name $RESOURCE_GROUP --location "$LOCATION_RG"
az acr create --resource-group $RESOURCE_GROUP --name $ACR_NAME --sku Basic --admin-enabled true
az acr login -n $ACR_NAME
ACR_USER=$(az acr credential show -n $ACR_NAME --query username | tr -d '"')
ACR_PWD=$(az acr credential show -n $ACR_NAME --query 'passwords[0].value' | tr -d '"')
echo $ACR_USER
echo $ACR_PWD
docker build -t $ACR_NAME.azurecr.io/$IMAGE_NAME .
docker push $ACR_NAME.azurecr.io/$IMAGE_NAME

for region in $REGIONS
do 
    az container delete --resource-group $RESOURCE_GROUP --name $ACR_NAME$region --yes
    az container create --resource-group $RESOURCE_GROUP \
        --location "$region" \
        --name $ACR_NAME$region \
        --image $ACR_NAME.azurecr.io/$IMAGE_NAME \
        --environment-variables SERVER_URI=$SERVER_URI \
        --registry-password $ACR_PWD \
        --registry-username $ACR_USER \
        --restart-policy "Never"
    az container exec --resource-group $RESOURCE_GROUP --name $ACR_NAME$region --exec-command "run run --output test-run-report.json /scripts/efficiency.yaml"
    az container exec --resource-group $RESOURCE_GROUP --name $ACR_NAME$region --exec-command "cat test-run-report.json" > ./reports/report_$region.json
    az container delete --resource-group $RESOURCE_GROUP --name $ACR_NAME$region --yes
done
az container delete --resource-group $RESOURCE_GROUP --name $ACR_NAME --yes