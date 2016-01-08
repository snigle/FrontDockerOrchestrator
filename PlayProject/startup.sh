nomVM="WaitingConfiguration"
user="user1"
org="ICC-02"
pass="eisti0002"

# Génération du loginCookie à utiliser pour la suite
echo ">> 1 - Obtention du login cookie et des divers HREF"
curl -s -k -c loginCookie.txt -H "Accept: application/*+xml;version=5.1" --user $user@$org:$pass https://vcloud-director-http-2.ccr.eisti.fr/api/login > /dev/null

#curl -s -k -b loginCookie.txt -H "Accept:application/*+xml;version=1.5" -X GET "https://vcloud-director-http-2.ccr.eisti.fr/api/query?type=vm&fields=name&filter=(status==POWERED_ON;isVAppTemplate==false)"
# Get id of the mh-keystore
reqVM=`curl -s -k -b loginCookie.txt -H "Accept:application/*+xml;version=1.5" -X GET "https://vcloud-director-http-2.ccr.eisti.fr/api/query?type=vm&fields=name&filter=(status==POWERED_ON;isVAppTemplate==false)" | grep $nomVM | cut -d '"' -f4`

echo "Find VM : $reqVM"

if [ -n "$reqVM" ]; then
#Get Vapp id
#curl -s -k -b loginCookie.txt -H "Accept:application/*+xml;version=1.5" -X GET $reqVM
vappid=`curl -s -k -b loginCookie.txt -H "Accept:application/*+xml;version=1.5" -X GET $reqVM | grep rel=\"up\" | cut -d '"' -f6 | cut -d '/' -f6`

head -n -1 conf/application.conf > temp.txt ; mv temp.txt conf/application.conf
echo "vapp.id=\"${vappid:5}\"" >> conf/application.conf;

#Rename the machine


data=`curl -s -k -b loginCookie.txt -H "Accept:application/*+xml;version=1.5" -X GET $reqVM`
      
echo ${data/$nomVM/"mh-keystore"} > conf/temp.xml

curl -s -k -b loginCookie.txt -H "Accept:application/*+xml;version=1.5" -H "Content-Type:application/vnd.vmware.vcloud.vm+xml" -X PUT "$reqVM" --data @conf/temp.xml

rm conf/temp.xml
fi
