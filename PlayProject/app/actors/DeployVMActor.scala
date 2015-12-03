package actors

import akka.actor.{Actor, ActorLogging, PoisonPill}
import akka.event.LoggingReceive
import models.Vapp
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

/**
 * Created by eisti on 12/2/15.
 */



case class VMDeployed(name: String)
case class Message(s : String)




  class DeployActor(func: Future[WSResponse]) extends Actor with ActorLogging {



    def receive = LoggingReceive {
      case VMDeployed(name) => {
        val req = func
           req.map(response => {
             val vapp = new Vapp(response.xml)
             val test_vapp_deployed = vapp.vms filter (x => x.name == name)
             vapp.vms.map(x => println(x.name))
             //          println(vapp)
             if (test_vapp_deployed.size != 1) {
               println("Virtual machine is being deployed, please wait")
               Thread sleep (10000)
               self ! VMDeployed(name)
             } else {
               println("Virtual machine sucessfully deployed")
               self ! PoisonPill
             }

           })





      }


    }

}


