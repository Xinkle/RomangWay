package database

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction

fun findCommand(commandName: String): CommandTeaching? = transaction {
    CommandTeaching.find(CommandTeachingTable.name eq commandName).firstOrNull()
}

fun findSimilarCommands(commandName: String): List<CommandTeaching> = transaction {
    val likeContentQuery = "%${commandName.drop(1)}%"

    CommandTeaching.find(CommandTeachingTable.name like likeContentQuery).toList()
}