package actors

import akka.actor.{Actor, ActorLogging, PoisonPill}
import akka.event.LoggingReceive
import models.Vapp
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.Future

/**
 * Created by eisti on 12/2/15.
 */



case class VMDeployed(name: String, i : Int = 1000)
case class Message(s : String)



class DeployActor(func: () => Future[WSResponse]) extends Actor with ActorLogging {

println("testt")

    def receive = LoggingReceive {
      case VMDeployed(name,cpt) => {
        println("VMDeployed "+name);
           func().map(response => {
             val vapp = new Vapp(response.xml)
             val test_vapp_deployed = vapp.vms filter (x => x.name == name)
             vapp.vms.map(x => println(x.name))
             //          println(vapp)
             
             if(cpt<=0){
               println("Creation VM Time out");
             }
             else if (test_vapp_deployed.size != 1 ) {
               println("Virtual machine is being deployed, please wait")
               
               context.system.scheduler.scheduleOnce(10 seconds, self ,VMDeployed(name,cpt-1))
             } else {
               println("Virtual machine sucessfully deployed")
               //self ! PoisonPill
             }

           })





      }


    }

}


