package actors

import akka.actor.Actor
import play.api.libs.ws.WSClient
import javax.inject.Inject
import scala.concurrent.Future
import play.api.libs.ws.WSResponse
import scala.concurrent.ExecutionContext.Implicits._
import models.VappFactory


case class Delete(vm_id : String, count : Int = 10)

class DeleteActor (ws: WSClient, func: () => Future[WSResponse], cookie : () => String) extends Actor{
  
  def receive = {
    case Delete(vm_id, count) => {
      func().map(response => {
        val vapp = VappFactory(response.xml)
        if(count < 0 ){
          println("Delete Time out : VM not shutdown")
        }
        else if(vapp.vms.filter(_.id==vm_id).head.active){
          self ! Delete(vm_id,count-1)
        }
        else{
           ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-" + vm_id).withHeaders(
          "Cookie" -> cookie(),
          "Accept" -> "application/*+xml;version=1.5"
        ).delete
        println("Deleting VM")
        }
    })
    }
  }
  
}