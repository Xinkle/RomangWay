package database

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

fun findCommand(commandName: String, isDeleted: Boolean = false): CommandTeaching? = transaction {
    findAllCommand(commandName, isDeleted).firstOrNull()
}

fun findAllCommand(commandName: String, isDeleted: Boolean = false): List<CommandTeaching> = transaction {
    CommandTeaching.find {
        (CommandTeachingTable.name eq commandName) and (CommandTeachingTable.isDeleted eq isDeleted)
    }.orderBy(CommandTeachingTable.createDate to SortOrder.DESC).toList()
}


fun findSimilarCommands(commandName: String): List<CommandTeaching> = transaction {
    val likeContentQuery = "%${commandName.trimStart('!')}%"

    CommandTeaching.find((CommandTeachingTable.name like likeContentQuery) and (CommandTeachingTable.isDeleted eq false))
        .toList()
}

fun createCommandTeaching(
    modifiedName: String,
    description: String,
    writer: String
): CommandTeaching = transaction {
    CommandTeaching.new {
        name = modifiedName
        this.description = description
        this.writer = writer
        isOverridable = false
        isDeleted = false
        createDate = System.currentTimeMillis()
    }
}
