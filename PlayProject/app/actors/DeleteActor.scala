package actors

import akka.actor.Actor
import play.api.libs.ws.WSClient
import javax.inject.Inject
import scala.concurrent.Future
import play.api.libs.ws.WSResponse
import scala.concurrent.ExecutionContext.Implicits._
import models.VappFactory
import scala.concurrent.duration._
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ActorLogging
import models.TaskFactory
import models.Task

trait DeleteMessage
case class Delete(vm_id: String, override val task: Task) extends DeployMessageType(task)
case class PowerOff(vm_id: String, override val task: Task) extends DeployMessageType(task)
case class Init(vm_id: String) extends DeleteMessage

object DeleteActor {
  def props(out: ActorRef, ws: WSClient, func: () => Future[WSResponse], cookie: () => String) = Props(new DeleteActor(out, ws, func, cookie))
}

class DeleteActor(override val out: ActorRef, override val ws: WSClient, func: () => Future[WSResponse], override val cookie: () => String) extends VappActor(out, ws, func, cookie) with ActorLogging {

  def reqDelete(vm_id: String) {
    out ! response_json("info", "Deleting VM")
    ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-" + vm_id).withHeaders(
      "Cookie" -> cookie(),
      "Accept" -> "application/*+xml;version=1.5").delete.map(response =>
        {
          println("reqDelete ok")
          println(response.xml)
          val task = TaskFactory(response.xml)
          self ! Delete(vm_id, task)
        })
  }

  def receive = {

    case Init(vm_id) => {
      out ! response_json("info", "Deleting VM")
      val vm = getVapp.vms.filter(vm => vm.id == vm_id).head
      if (vm.active) {
        //PowerOff
        val data = <UndeployVAppParams xmlns="http://www.vmware.com/vcloud/v1.5">
                     <UndeployPowerAction>powerOff</UndeployPowerAction>
                   </UndeployVAppParams>

        ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-" + vm_id + "/action/undeploy").withHeaders(
          "Cookie" -> cookie(),
          "Accept" -> "application/*+xml;version=1.5",
          "Content-Type" -> "application/vnd.vmware.vcloud.undeployVAppParams+xml").post(data).map(response => {
            val task = TaskFactory(response.xml)
            self ! PowerOff(vm_id, task)
          })
      } else {
        reqDelete(vm_id);
      }
    }

    case PowerOff(vm_id, task) => {
      waitTask(PowerOff(vm_id, updateTask(task)), "PowerOff the VM", () => {
        reqDelete(vm_id)
      })
    }

    case Delete(vm_id, task) => {
      waitTask(Delete(vm_id, updateTask(task)), "Deleting the Vapp", () => {
        out ! response_json("success", "Machine has deleted")
      })
    }


  }

}