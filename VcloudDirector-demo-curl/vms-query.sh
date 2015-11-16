curl -k -b loginCookie.txt -H "Accept:application/*+xml;version=1.5" -H "Content-Type:application/vnd.vmware.vcloud.query+xml" -X GET https://vcloud-director-http-2.ccr.eisti.fr/api/vms/query
