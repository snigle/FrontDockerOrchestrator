package actors

import akka.actor.{ActorLogging, ActorRef, Props}
import models.{Task, TaskFactory}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

trait DeleteMessage

/*
* Classes that represent tasks sent to VM through vcloud api
*/
case class Delete(vm_id: String, override val task: Task) extends DeployMessageType(task)
case class PowerOff(vm_id: String, override val task: Task) extends DeployMessageType(task)
case class Init(vm_id: String) extends DeleteMessage

/*
* Object used by websockets
*/
object DeleteActor {
  def props(out: ActorRef, ws: WSClient, func: () => Future[WSResponse], cookie: () => String) = Props(new DeleteActor(out, ws, func, cookie))
}


/*
* Actor used to delete VM
*/
class DeleteActor(override val out: ActorRef, override val ws: WSClient, func: () => Future[WSResponse], override val cookie: () => String) extends VappActor(out, ws, func, cookie) with ActorLogging {

  //Send a delete request to vcloud api
  def reqDelete(vm_id: String) {
    out ! response_json("info", "Deleting VM")
    ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-" + vm_id).withHeaders(
      "Cookie" -> cookie(),
      "Accept" -> "application/*+xml;version=1.5").delete.map(response =>
        {
          val task = TaskFactory(response.xml)
          self ! Delete(vm_id, task)
        })
  }

  def receive = {

    //communicate with websockets by showing informations of the deleting procedure
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

      //To power off a VM before deleting it
    case PowerOff(vm_id, task) => {
      waitTask(PowerOff(vm_id, updateTask(task)), "PowerOff the VM", () => {
        reqDelete(vm_id)
      })
    }

      // Tells when VM is deleted
    case Delete(vm_id, task) => {
      waitTask(Delete(vm_id, updateTask(task)), "Deleting the VM", () => {
        out ! response_json("success", "Machine has deleted")
      })
    }


  }

}