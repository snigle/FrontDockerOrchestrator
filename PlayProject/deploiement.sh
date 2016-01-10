#!/bin/bash

dist="playproject-1.0-SNAPSHOT"

./activator dist
ssh root@$1 -i conf/server_key rm -rf $dist
ssh root@$1 -i conf/server_key rm $dist.zip
scp -i conf/server_key target/universal/$dist.zip root@$1:/root
ssh root@$1 -i conf/server_key unzip playproject-1.0-SNAPSHOT.zip

scp -i conf/server_key startup.sh root@$1:/root/$dist/
ssh root@$1 -i conf/server_key chmod +x /root/$dist/startup.sh

scp -i conf/server_key playproject-init root@$1:/etc/init.d/
ssh root@$1 -i conf/server_key chmod +x /etc/init.d/playproject-init


#create link
ssh root@$1 -i conf/server_key rm -rf play
ssh root@$1 -i conf/server_key ln -s $dist play

#Put absolute path
data=`cat conf/application.conf `
echo "${data//'conf/'/'/root/play/conf/'}" > toto.tmp
scp -i conf/server_key toto.tmp root@$1:~/play/conf/application.conf
rm toto.tmp

ssh root@$1 -i conf/server_key killall java
ssh root@$1 -i conf/server_key "service playproject-init" &
