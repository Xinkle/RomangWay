package database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object CommandTeachingTable : IntIdTable() {
    val name = text("name")
    val description = text("description")
    val writer = text("writer")
    val isOverridable = bool("is_overridable")
    val createDate = long("create_date")
}

class CommandTeaching(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CommandTeaching>(CommandTeachingTable)

    var name by CommandTeachingTable.name
    var description by CommandTeachingTable.description
    var writer by CommandTeachingTable.writer
    var isOverridable by CommandTeachingTable.isOverridable
    var createDate by CommandTeachingTable.createDate
}