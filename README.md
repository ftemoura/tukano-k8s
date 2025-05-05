# Tukano - K8s version 

Cloud-native short videos platform proof of concept, deployed in Kubernetes (AKS). 

Developed as part of the Cloud Computing course 2024/25 @ NOVA SST.

## Deployment Stack

<p align="center">
<img style="padding: 50px; width:80%; height:auto;" src="./architecture.svg">
</p>

- Webserver 
	- Tomcat (`tukano` and `blobs`)
- Cache 
	- Redis Cache
- Database 
	- PostgreSQL
- Storage
	- Persistent Volume
- Routing
	- NGINX Ingress, routing between the `tukano` webserver (`/rest/`)  and `blobs` webserver (`/rest/blobs`)

## Developers

- [Francisco Moura](https://github.com/ftemoura)
- [Guilherme Fernandes](https://github.com/Gui28F)
