#!/bin/bash
./activator dist
ssh root@$1 -i conf/server_key rm -rf playproject-1.0-SNAPSHOT
ssh root@$1 -i conf/server_key rm playproject-1.0-SNAPSHOT.zip
scp target/universal/playproject-1.0-SNAPSHOT.zip root@$1:/root
ssh root@$1 -i conf/server_key unzip playproject-1.0-SNAPSHOT.zip
ssh root@$1 -i conf/server_key killall java
ssh root@$1 -i conf/server_key "cd playproject-1.0-SNAPSHOT && ./bin/playproject -Dhttp.proxyHost=192.168.254.10 -Dhttp.proxyPort=3128 -Dhttp.nonProxyHosts='localhost|127.0.0.1|192.168.2.100|192.168.2.101|192.168.2.2|192.168.2.103|192.168.2.104|192.168.2.105|192.168.2.106|192.168.2.107|192.168.2.108|192.168.2.194|192.168.2.110' -Dhttp.port=80" &



