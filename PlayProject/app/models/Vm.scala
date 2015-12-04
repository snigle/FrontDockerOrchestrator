package models

import scala.xml.Node
import play.api.libs.json._

case class Vm (id : String, name : String, ipLocal : String, ipExternal : String, active : Boolean = false, containers : Seq[Container] = Nil) {
  
}

object VmFactory{
  
  def apply(xml : Node, json : Option[JsValue]) : Vm = {
    
    //Create a list of all containers
    val containers = json match{
      case None => Nil
      case Some(json) => {
        val names = (json \\ "Names").map(_.head.asOpt[String].getOrElse(""))
        val images = (json \\ "Image").map(_.asOpt[String].getOrElse(""))
        names.indices.map(i => Container("",names(i),images(i),Nil))
      }
    }

    val vmName = xml.attribute("name").get.text
    Vm(xml.attribute("id").get.text.split(":").last,
      vmName,  
      (xml \ "NetworkConnectionSection" \ "NetworkConnection" \ "IpAddress").text,
      (xml \ "NetworkConnectionSection" \ "NetworkConnection" \ "ExternalIpAddress").text,
      xml.attribute("deployed").get.text.toBoolean,
      containers.filter { container => container.name.split("/")(1) == vmName }
    )
  }
}