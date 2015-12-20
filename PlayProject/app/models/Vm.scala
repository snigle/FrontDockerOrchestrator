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
        val ids = (json \\ "Id").map(_.asOpt[String].getOrElse(""))
        val ports = (json \\ "Ports").map(e => {
          val publics = (e \\ "PublicPort").map(_.asOpt[Int].getOrElse(0))
          val privates = (e \\ "PrivatePort").map(_.asOpt[Int].getOrElse(0))
          publics.indices.map(i => (privates(i),publics(i))).toMap
        })
        val actives = (json \\ "Status").map(_.asOpt[String].getOrElse("")).toList.map(act => act.contains("Up"))
        names.indices.map(i => Container(ids(i),names(i),images(i),ports(i),actives(i)))
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