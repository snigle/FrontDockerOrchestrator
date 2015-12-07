package actors

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import models.Vapp
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Play.current

import models.VappFactory


/**
 * Created by eisti on 12/2/15.
 */



case class VMDeployed(name: String, i : Int = 1000)
case class VMPoweredOn(id_vm : String, i : Int = 1000)
case class Message(s : String)



class DeployActor(func: () => Future[WSResponse],cookie : String) extends Actor with ActorLogging {

//println("testt")

    def receive = LoggingReceive {
      case VMDeployed(name,cpt) => {
//        println("VMDeployed "+name);
           func().map(response => {
             val vapp = VappFactory(response.xml)
             val test_vapp_deployed = vapp.vms filter (x => x.name == name)
//             vapp.vms.map(x => println(x.name))

             //          println(vapp)

             if(cpt<=0){
               println("Creation VM Time out");
             }
             else if (test_vapp_deployed.size != 1 ) {
               println("Virtual machine is being deployed, please wait")
               
               context.system.scheduler.scheduleOnce(10 seconds, self ,VMDeployed(name,cpt-1))
             } else {
               println("Virtual machine sucessfully deployed")
               val vm_created = test_vapp_deployed(0)
               WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-" + vm_created.id +"/power/action/powerOn").withHeaders(
                                "Cookie" -> cookie,
                                "Accept" -> "application/*+xml;version=1.5"
                              ).post("")
               println("Virtual machine powered on")
               context.system.scheduler.scheduleOnce(25 seconds, self ,VMPoweredOn(vm_created.id))


             }



           })





      }

      case VMPoweredOn(id_vm,cpt) => {
        func().map(response => {
          val vapp = VappFactory(response.xml)
          val test_vapp_deployed = vapp.vms filter (x => x.id == id_vm)
          val vm_created = test_vapp_deployed(0)

          if (!(vm_created.active)) {
            println("Virtual machine is starting")
//            context.system.scheduler.scheduleOnce(10 seconds, self ,VMPoweredOn(id_vm,cpt-1))
          } else {
//            val updateip_xml = <NetworkConnectionSection
//                             xmlns="http://www.vmware.com/vcloud/v1.5"
//                             xmlns:ovf="http://schemas.dmtf.org/ovf/envelope/1">
//                               <ovf:Info>Specifies the available VM network connections</ovf:Info>
//                               <PrimaryNetworkConnectionIndex>0</PrimaryNetworkConnectionIndex>
//                               <NetworkConnection network="NAT" needsCustomization="true">
//                                 <NetworkConnectionIndex>0</NetworkConnectionIndex>
//                                 <IsConnected>true</IsConnected>
//                                 <IpAddressAllocationMode>POOL</IpAddressAllocationMode>
//                               </NetworkConnection>
//                             </NetworkConnectionSection>
//
//
//                           WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-"+ vm_created.id +"/networkConnectionSection").withHeaders(
//                             "Cookie" -> cookie,
//                             "Accept" -> "application/*+xml;version=1.5",
//                             "Content-Type" -> "application/vnd.vmware.vcloud.networkConnectionSection+xml"
//                           ).post(updateip_xml)

                           println("Ip updated")
          }

        })

      }


    }

}


