package actors

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import models.Vapp
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Created by eisti on 12/2/15.
 */



case class VMDeployed(name: String)
case class Message(s : String)




  class DeployActor(func: Future[WSResponse]) extends Actor with ActorLogging {



    def receive = LoggingReceive {
      case VMDeployed(name) => {
        val response = func
        response.onComplete {
          case Success(response) => {
            val vapp = new Vapp(response.xml)
            val test_vapp_deployed = vapp.vms filter (x => x.name == name)
            //println(test_vapp_deployed.size)
            if (test_vapp_deployed == 1){
               sender ! "Virtual machine is created"
            }
            else {
              sender ! "Virtual machine is being deployed, please wait"
            }
          }


          case Failure(e) => println("erreur : " + e)
        }

      }

      case Message(s) => {
        println(s)
      }

    }

}


