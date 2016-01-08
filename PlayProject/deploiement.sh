#!/bin/bash
./activator dist
ssh root@$1 -i conf/server_key rm -rf playproject-1.0-SNAPSHOT
ssh root@$1 -i conf/server_key rm playproject-1.0-SNAPSHOT.zip
scp target/universal/playproject-1.0-SNAPSHOT.zip root@$1:/root
ssh root@$1 -i conf/server_key unzip playproject-1.0-SNAPSHOT.zip
ssh root@$1 -i conf/server_key killall java
ssh root@$1 -i conf/server_key "cd playproject-1.0-SNAPSHOT && ./bin/playproject -Dhttp.port=80" &

scp startup.sh root@$1:/root/play*/
ssh root@$1 -i conf/server_key chmod +x startup.sh

scp playproject-init root@$1:/etc/init.d/
ssh root@$1 -i conf/server_key chmod +x /etc/init.d/playproject-init
