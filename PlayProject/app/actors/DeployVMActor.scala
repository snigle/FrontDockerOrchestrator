package actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.event.LoggingReceive
import models.{Task, TaskFactory, VappFactory, Vm}
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSClient, WSResponse}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Created by eisti on 12/2/15.
 */

sealed trait DeployType

case class IPChanged(vm: Vm, override val task: Task) extends DeployMessageType(task)
case class ChangeHostname(vm: Vm, override val task: Task) extends DeployMessageType(task)
case class VMDeployed(name: String, override val task: Task) extends DeployMessageType(task)
case class PowerOn(vm: Vm, override val task: Task) extends DeployMessageType(task)
case class Message(s: String) extends DeployType

object DeployVMActor {
  def props(out: ActorRef, ws: WSClient, func: () => Future[WSResponse], cookie: () => String) = Props(new DeployVMActor(out, ws, func, cookie))
}


class DeployVMActor(override val out: ActorRef, override val ws: WSClient, override val func: () => Future[WSResponse], override val cookie: () => String) extends VappActor(out,ws,func,cookie) with ActorLogging {

  def reqCopieVm(cookie: String, source_vm: String, name_new_vm: String) = {
    val vm_copy_xml = <RecomposeVAppParams xmlns="http://www.vmware.com/vcloud/v1.5" xmlns:ns2="http://schemas.dmtf.org/ovf/envelope/1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ovf="http://schemas.dmtf.org/ovf/envelope/1" xmlns:environment_1="http://schemas.dmtf.org/ovf/environment/1">
                        <Description> "api deployed vm to ol-vapp-04" </Description>
                        <SourcedItem sourceDelete="false">
                          <Source name={ name_new_vm } href={ source_vm }/>
                        </SourcedItem>
                        <AllEULAsAccepted>true</AllEULAsAccepted>
                      </RecomposeVAppParams>

    WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-9dd013e3-3f51-4cde-a19c-f96b4ad2e350/action/recomposeVApp").withHeaders(
      "Cookie" -> cookie,
      "Accept" -> "application/*+xml;version=1.5",
      "Content-Type" -> "application/vnd.vmware.vcloud.recomposeVAppParams+xml").post(vm_copy_xml).map(response => {
        println("Req creation ok")
        //println(response.xml)
        val task = TaskFactory(response.xml)
        self ! VMDeployed(name_new_vm, task)
      })

  }

  def reqUpdateIp(vm_created: Vm) = {
    val updateip_xml = <NetworkConnectionSection xmlns="http://www.vmware.com/vcloud/v1.5" xmlns:ovf="http://schemas.dmtf.org/ovf/envelope/1">
                         <ovf:Info>Specifies the available VM network connections</ovf:Info>
                         <PrimaryNetworkConnectionIndex>0</PrimaryNetworkConnectionIndex>
                         <NetworkConnection network="NAT" needsCustomization="true">
                           <NetworkConnectionIndex>0</NetworkConnectionIndex>
                           <IsConnected>true</IsConnected>
                           <IpAddressAllocationMode>POOL</IpAddressAllocationMode>
                         </NetworkConnection>
                       </NetworkConnectionSection>

    WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-" + vm_created.id + "/networkConnectionSection").withHeaders(
      "Cookie" -> cookie(),
      "Accept" -> "application/*+xml;version=1.5",
      "Content-Type" -> "application/vnd.vmware.vcloud.networkConnectionSection+xml").put(updateip_xml).map(response =>
        {
          println("reqUpdateIP ok")
          println(response.xml)
          val task = TaskFactory(response.xml)
          self ! IPChanged(vm_created, task)
        })
  }

  def reqChangeHostname(vm: Vm) = {
    val changeHostname_xml =
      <GuestCustomizationSection xmlns="http://www.vmware.com/vcloud/v1.5" xmlns:ovf="http://schemas.dmtf.org/ovf/envelope/1" ovf:required="false">
        <ovf:Info>Specifies Guest OS Customization Settings</ovf:Info>
        <Enabled>true</Enabled>
        <ComputerName>{ vm.name }</ComputerName>
      </GuestCustomizationSection>

    WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-" + vm.id + "/guestCustomizationSection").withHeaders(
      "Cookie" -> cookie(),
      "Accept" -> "application/*+xml;version=1.5",
      "Content-Type" -> "application/vnd.vmware.vcloud.guestCustomizationSection+xml").put(changeHostname_xml).map(response =>
        {
          println("reqUpdateIP ok")
          val task = TaskFactory(response.xml)
          self ! ChangeHostname(vm, task)
        })
  }

  def reqPowerOn(vm: Vm) = {
    WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-" + vm.id + "/power/action/powerOn").withHeaders(
      "Cookie" -> cookie(),
      "Accept" -> "application/*+xml;version=1.5").post("").map(response => {
        self ! PowerOn(vm,TaskFactory(response.xml))
      })
  }

  
  

  def receive = LoggingReceive {
    case "\"start\"" => {
      out ! response_json("info", "Creating new VM")
      func().map(response => {
        val vapp = VappFactory(response.xml)
        reqCopieVm(cookie(), "https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-bb168665-8203-4edc-9ff8-dab64e754620", "swarm-agent-" + (vapp.indice + 1))
        out ! response_json("info", "Creating new VM")
      })
    }
    

    case ChangeHostname(vm, task) => {
      waitTask(ChangeHostname(vm, updateTask(task)), "Changing hostname of the Virtual machine, please wait", () => {
        //Power On and install
        reqPowerOn(vm)
      })
    }
    case IPChanged(vm, task) => {
      waitTask(IPChanged(vm, updateTask(task)), "Assigning IP to the Virtual machine, please wait", () => {
        println("ip ok")
        reqChangeHostname(vm)
      })
    }
    case PowerOn(vm, task) => {
      waitTask(PowerOn(vm, updateTask(task)), "Starting the Virtual machine, please wait", () => {
        out ! response_json("success", "Machine has been created")
      })
    }
    case VMDeployed(name, task) => {

      waitTask(VMDeployed(name, updateTask(task)), "Virtual machine is being deployed, please wait", () => {
        val vapp = getVapp
        val vm = vapp.vms.filter(x => x.name == name).head
        println("Virtual machine sucessfully deployed")
        reqUpdateIp(vm)
      })

    }
    

  }

  


  

}
  


