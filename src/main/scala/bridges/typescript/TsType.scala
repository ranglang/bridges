package bridges.typescript

import bridges.core._
import bridges.typescript.syntax._

sealed abstract class TsType extends Product with Serializable {
  import TsType._

  def |(that: TsType): TsType =
    Union(List(this, that))

  def &(that: TsType): TsType =
    Inter(List(this, that))
}

object TsType {
  final case class Ref(id: String, params: List[TsType] = Nil) extends TsType
  final case object Str                                        extends TsType
  final case object Chr                                        extends TsType
  final case object Intr                                       extends TsType
  final case object Real                                       extends TsType
  final case object Bool                                       extends TsType
  final case object Null                                       extends TsType
  final case class StrLit(value: String)                       extends TsType
  final case class ChrLit(value: Char)                         extends TsType
  final case class IntrLit(value: Int)                         extends TsType
  final case class RealLit(value: Double)                      extends TsType
  final case class BoolLit(value: Boolean)                     extends TsType
  final case class Arr(tpe: TsType)                            extends TsType
  final case class Struct(fields: List[(String, TsType)])      extends TsType
  final case class Inter(types: List[TsType])                  extends TsType
  final case class Union(types: List[TsType])                  extends TsType

  def from(tpe: Type): TsType =
    tpe match {
      case Type.Ref(id, params) => Ref(id, params.map(from))
      case Type.Str             => Str
      case Type.Chr             => Chr
      case Type.Intr            => Intr
      case Type.Real            => Real
      case Type.Bool            => Bool
      case Type.Opt(tpe)        => from(tpe) | Null
      case Type.Arr(tpe)        => Arr(from(tpe))
      case Type.Prod(fields)    => translateProd(fields)
      case Type.Sum(products)   => translateSum(products)
    }

  private def translateProd(fields: List[(String, Type)]): Struct =
    Struct(fields.map { case (name, tpe) => (name, from(tpe)) })

  private def translateSum(products: List[(String, Type.Prod)]): Union =
    Union(products.map {
      case (name, tpe) =>
        Struct(("type" -> StrLit(name)) +: translateProd(tpe.fields).fields)
    })

  implicit val rename: Rename[TsType] =
    Rename.pure { (value, from, to) =>
      def renameId(id: String): String =
        if (id == from) to else id

      value match {
        case Ref(id, params) => Ref(renameId(id), params.map(_.rename(from, to)))
        case tpe @ Str       => tpe
        case tpe @ Chr       => tpe
        case tpe @ Intr      => tpe
        case tpe @ Real      => tpe
        case tpe @ Bool      => tpe
        case tpe @ Null      => tpe
        case tpe: StrLit     => tpe
        case tpe: ChrLit     => tpe
        case tpe: IntrLit    => tpe
        case tpe: RealLit    => tpe
        case tpe: BoolLit    => tpe
        case Arr(tpe)        => Arr(tpe.rename(from, to))
        case Struct(fields)  => Struct(fields.map(_.rename(from, to)))
        case Inter(types)    => Inter(types.map(_.rename(from, to)))
        case Union(types)    => Union(types.map(_.rename(from, to)))
      }
    }
}
