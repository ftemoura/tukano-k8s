FROM artilleryio/artillery:latest

WORKDIR /scripts
RUN npm install form-data
RUN apk add bind-tools
RUN apk add traceroute

COPY . /scripts

ENTRYPOINT ["tail", "-f", "/dev/null"]