package actors

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.actor.PoisonPill
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.collection.immutable.Map


case class Port(in : Int, protocol : String, out : Int)
case class CreateContainer(image : String, ports : Seq[Port])

object ContainersActor {
  def props(out: ActorRef, ws: WSClient) = Props(new ContainersActor(out, ws))
}

class ContainersActor(out: ActorRef, ws: WSClient) extends Actor {
  def receive = {
    case msg: String =>
      {
        println(msg);
        out ! ("""{ "info" : "I received your message: """ + msg +"\"}")
        self ! PoisonPill
      }
    case CreateContainer(image, ports) => {
      out ! Json.toJson(
                Map(
                  "info" -> Json.toJson("Creating container for image "+image)
                )
              )
      println("CreateContainer Ok");
      println(ports);
      println(image);
      val params = Json.parse("""
      {
  	    "AttachStdin": false,
  	    "AttachStdout": true,
  	    "AttachStderr": true,
  	    "Tty": false,
  	    "OpenStdin": false,
  	    "StdinOnce": false,
  	    "Image": """"+image+"""",
  	    "ExposedPorts": {"""+
  	         ports.map(port => s""" "${port.in}/${port.protocol}" : {} """).mkString("",",","")  
  	         //   "80/tcp": {}
  	    +"""},
  	    "StopSignal": "SIGTERM",
  	    "HostConfig": {
  	      "PortBindings": { """+
  	        ports.map(port => s""" "${port.in}/${port.protocol}" : [{ "HostPort" : "${port.out}"}] """).mkString("",",","")  
  	     // "80/tcp": [{ "HostPort": "8081" }] 
  	    +"""},
  	      "PublishAllPorts": false
  	      }
  	      
  	   }
      """)
 
  println(params);
        ws.url("https://192.168.30.53:8080/containers/create").post(params).map(response =>
          {
            val res = 
            if(response.status==201){
              Json.toJson(
                Map(
                  "success" -> Json.toJson("The container has been created with image "+image)
                )
              )
            }
            else
            {
              Json.toJson(
                Map(
                  "error" -> Json.toJson(response.body)
                )
              )
            }
            println("Requete Ok "+image)
            out ! res.toString()
            self ! PoisonPill

          })
    }
  }
}