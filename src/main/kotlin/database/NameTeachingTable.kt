package database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object NameTeachingTable : IntIdTable() {
    val name = text("name")
    val description = text("description")
    val writer = text("writer")
    val isOverridable = bool("is_overridable")
    val createDate = long("create_date")
}

class NameTeaching(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<NameTeaching>(NameTeachingTable)

    var name by NameTeachingTable.name
    var description by NameTeachingTable.description
    var writer by NameTeachingTable.writer
    var isOverridable by NameTeachingTable.isOverridable
    var createDate by NameTeachingTable.createDate
}