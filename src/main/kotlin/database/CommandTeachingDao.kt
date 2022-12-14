package database

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction

fun findCommand(commandName: String): CommandTeaching? = transaction {
    CommandTeaching.find(CommandTeachingTable.name eq commandName)
        .orderBy(CommandTeachingTable.createDate to SortOrder.DESC)
        .firstOrNull()
}

fun findSimilarCommands(commandName: String): List<CommandTeaching> = transaction {
    val likeContentQuery = "%${commandName.trimStart('!')}%"

    CommandTeaching.find(CommandTeachingTable.name like likeContentQuery).toList()
}