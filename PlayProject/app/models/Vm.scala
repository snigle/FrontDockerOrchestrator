package models

import scala.xml.Node

case class Vm (id : String, name : String, ipLocal : String, ipExternal : String, active : Boolean = false) {
  def this(xml : Node) = {
    this(xml.attribute("id").get.text.split(":").last,
      xml.attribute("name").get.text,  
      (xml \ "NetworkConnectionSection" \ "NetworkConnection" \ "IpAddress").text,
      (xml \ "NetworkConnectionSection" \ "NetworkConnection" \ "ExternalIpAddress").text,
      xml.attribute("deployed").get.text.toBoolean
    )
  }
}