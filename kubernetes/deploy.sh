#!/bin/bash

red_err()(set -o pipefail;"$@" 2> >(sed $'s,.*,\e[31m&\e[m,'>&2))
export -f red_err

set -a
source .secrets
source .config
set +a

AKS_NAME=aks-tukano-60045-60174
ACR_NAME=acrtukano6004560174
RESOURCE_GROUP=aks-test-60174-rg
DNS_LABEL=tukano6004560174

TUKANO_IMAGE_NAME=tukano-tomcat

add_dns() {
  LB_RG=$(az aks show --name $AKS_NAME --resource-group $RESOURCE_GROUP --query "nodeResourceGroup" -o tsv)
  FRONTEND_IP_NAME=$(az network lb show -n kubernetes -g $LB_RG \
    --query "loadBalancingRules[?frontendPort==\`80\`].frontendIPConfiguration.id" -o tsv 2> /dev/null | awk -F'/' '{print $NF}')
  IP_NAME=$(az network lb frontend-ip show --lb-name kubernetes  --name $FRONTEND_IP_NAME \
    --resource-group $LB_RG --query publicIPAddress.id -o tsv 2> /dev/null | awk -F'/' '{print $NF}')
  az network public-ip update -g $LB_RG -n $IP_NAME --dns-name $DNS_LABEL &> /dev/null
  if [ $? -eq 0 ]; then
    echo "Successfully added DNS"
  else
    echo "Error adding DNS"
  fi
}

export DOCKER_BUILDKIT=1
cd ..
docker build -t $ACR_NAME.azurecr.io/$TUKANO_IMAGE_NAME -f kubernetes/Dockerfile --build-arg DEPLOY_APPLICATION_CLASS=TukanoApplication . 1> /dev/null
if [ $? -ne 0 ]; then
  exit $?
fi
cd - 1> /dev/null || exit

az group create --name $RESOURCE_GROUP --location "$AZURE_REGION" 1> /dev/null

az acr show -n $ACR_NAME -g $RESOURCE_GROUP &> /dev/null
if [ $? -ne 0 ]; then
  az acr create \
    --resource-group $RESOURCE_GROUP \
    --name $ACR_NAME \
    --sku Basic \
    --admin-enabled true 1> /dev/null
fi
az acr login -n $ACR_NAME 1> /dev/null

docker push $ACR_NAME.azurecr.io/$TUKANO_IMAGE_NAME 1> /dev/null

AKS_SHOW=$(az aks show -n $AKS_NAME -g $RESOURCE_GROUP) &> /dev/null
if [ $? -ne 0 ]; then
  az aks create \
    --resource-group $RESOURCE_GROUP \
    --name $AKS_NAME \
    --node-count 1 \
    --generate-ssh-keys \
    --attach-acr $ACR_NAME
  az aks approuting enable -n $AKS_NAME -g $RESOURCE_GROUP 
else
  echo $AKS_SHOW | grep '"powerState": { "code": "Running" }' &> /dev/null
  if [ $? -ne 0 ]; then
    az aks start -n $AKS_NAME -g $RESOURCE_GROUP 1> /dev/null
    az aks approuting enable -n $AKS_NAME -g $RESOURCE_GROUP 
  fi
fi
az aks get-credentials --resource-group $RESOURCE_GROUP --name $AKS_NAME

kubectl delete all --all

kubectl delete secrets postgres-secret
kubectl create secret generic postgres-secret \
 --from-literal=POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
 --from-literal=POSTGRES_USER=$POSTGRES_USER \
 --from-literal=POSTGRES_DB=$POSTGRES_DB

kubectl delete secrets tukano-secrets
kubectl create secret generic tukano-secrets --from-env-file=.secrets


kubectl delete configmap tukano-config

tmpconfig=$(mktemp /tmp/.config.XXXXX)
cat .config >> $tmpconfig && echo "" >> $tmpconfig
echo EXTERNAL_ENDPOINT="http://${DNS_LABEL}.${AZURE_REGION}.cloudapp.azure.com/rest" >> $tmpconfig
kubectl create configmap tukano-config --from-env-file=$tmpconfig

if $CACHE_ENABLED; then
  red_err kubectl apply -f redis-service.yaml
fi
red_err kubectl apply -f postgres-service.yaml
red_err kubectl apply -f tukano-service.yaml

add_dns

echo "Attaching to Tukano..."

attach_tukano() {
  while :
  do
   kubectl attach $(kubectl get pods --output name | grep tukano) 
  done
}

attach_blobs() {
  while :
  do
   kubectl attach $(kubectl get pods --output name | grep blobs) 
  done
}

attach_tukano
