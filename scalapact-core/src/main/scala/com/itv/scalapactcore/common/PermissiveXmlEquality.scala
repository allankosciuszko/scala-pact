package com.itv.scalapactcore.common

import scala.language.implicitConversions
import scala.xml.Elem

object PermissiveXmlEquality {

  implicit def toJsonEqualityWrapper(json: Elem): XmlEqualityWrapper = XmlEqualityWrapper(json)

  case class XmlEqualityWrapper(json: Elem) {
    def =~(to: Elem): Boolean = PermissiveXmlEqualityHelper.areEqual(json, to)
  }
}


object PermissiveXmlEqualityHelper {

  /***
    * Permissive equality means that the elements and fields defined in the 'expected'
    * are required to be present in the 'received', however, extra elements on the right
    * are allowed and ignored. Additionally elements are still considered equal if their
    * fields or array elements are out of order, as long as they are present since json
    * doesn't not guarantee element order.
    */
  def areEqual(expected: Elem, received: Elem): Boolean = {

    println("------")
    println(expected)

    println(expected.namespace)
    println(expected.isEmpty)
    println(expected.isAtom)
    println(expected.label)
    println(expected.child)
    println(expected.child.length)
    println(expected.attributes.asAttrMap)

    println(<foo>foo text</foo>.child)
    println(<foo>foo text</foo>.child.isEmpty)
    println(<foo/>.isAtom)

    // For each node:
    // node name is the same
    // contains all attributes
    // has at least the same number of child nodes
    // if no child nodes, should be equal



//    expected match {
//      case j: Json if j.isObject && received.isObject =>
//        compareFields(expected, received, j.objectFieldsOrEmpty)
//
//      case j: Json if j.isArray && received.isArray =>
//
//        (j.array |@| received.array) { (ja, ra) =>
//          ja.forall { jo =>
//            ra.exists(ro => areEqual(jo, ro))
//          }
//        } match {
//          case Some(matches) => matches
//          case None => false
//        }
//
//      case j: Json =>
//        expected == received
//    }
    expected == received
  }

  lazy val mapContainsMap: Map[String, String] => Map[String, String] => Boolean = e => r =>
    e.forall { ee =>
      r.exists(rr => rr._1 == ee._1 && rr._2 == ee._2)
    }

//  private def compareFields(expected: Json, received: Json, expectedFields: List[Json.JsonField]): Boolean = {
//    if(!expectedFields.forall(f => received.hasField(f))) false
//    else {
//      expectedFields.forall { field =>
//
//        (expected.field(field) |@| received.field(field)){ areEqual(_, _) } match {
//          case Some(bool) => bool
//          case None => false
//        }
//      }
//    }
//  }

}