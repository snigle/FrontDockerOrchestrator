package actors

import akka.actor.{ Actor, ActorLogging }
import akka.event.LoggingReceive
import models.Vapp
import play.api.libs.ws.{ WS, WSResponse }
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Play.current
import models.VappFactory
import akka.actor.ActorRef
import play.api.libs.ws.WSClient
import akka.actor.Props
import models.Vm
import akka.actor.PoisonPill
import scala.concurrent.Await
import scala.util.Success
import scala.util.Failure
import models.TaskFactory
import models.Task
import play.api.libs.json.Json

/**
 * Created by eisti on 12/2/15.
 */

sealed trait DeployType
class DeployMessageType(val task: Task)
case class IPChanged(vm: Vm, override val task: Task) extends DeployMessageType(task)
case class ChangeHostname(vm: Vm, override val task: Task) extends DeployMessageType(task)
case class VMDeployed(name: String, override val task: Task) extends DeployMessageType(task)
case class PowerOn(vm: Vm, override val task: Task) extends DeployMessageType(task)
case class Message(s: String) extends DeployType

object DeployVMActor {
  def props(out: ActorRef, ws: WSClient, func: () => Future[WSResponse], cookie: () => String) = Props(new DeployVMActor(out, ws, func, cookie))
}

class DeployVMActor(out: ActorRef, ws: WSClient, func: () => Future[WSResponse], cookie: () => String) extends Actor with ActorLogging {

  //println("testt")

  def response_json(status: String, message: String) = {
    Json.toJson(Map(status -> message)).toString()
  }
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
        println(task)
        println("ok task toto")
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
          println(task)
          println("ok task")
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
          println(response.xml)
          val task = TaskFactory(response.xml)
          println(task)
          println("ok task")
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

  def waitTask(m: DeployMessageType, waitingMessage: String, success: () => Unit) = {
    if (m.task.status == "queued" || m.task.status == "running") {
      out ! response_json("info", waitingMessage)
      context.system.scheduler.scheduleOnce(5 seconds, self, m)
    } else if (m.task.status == "success") {
      success()
    } else {
      println("Close socket")
      out ! response_json("error", m.task.message)
      self ! PoisonPill
    }
  }

  def receive = LoggingReceive {
    case "start" => {
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

  def updateTask(task: Task) = {

    if (task.id != "undefined") {
      val req =
        WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/task/" + task.id).withHeaders(
          "Cookie" -> cookie(),
          "Accept" -> "application/*+xml;version=1.5").get.map(response =>
            {
              TaskFactory(response.xml)
            })
      Await.ready(req, 10 seconds).value.get match {
        case Success(x) => x
        case Failure(x) => throw new Exception("Can't parse the task")
      }
    } else {
      task
    }
  }

  def getVapp = {
    val req =
      func().map(response =>
        {
          VappFactory(response.xml)
        })
    Await.ready(req, 10 seconds).value.get match {
      case Success(x) => x
      case Failure(x) => throw new Exception("Can't parse the VApp")
    }
  }

}
  


