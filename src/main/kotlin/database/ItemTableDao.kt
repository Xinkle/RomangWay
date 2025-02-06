package database

import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object ItemTableDao {
    fun getItemIdByName(name: String): Int? = transaction {
        // Query for the item with the given name and return its itemId
        Item.find { ItemTable.name eq name }
            .singleOrNull() // Ensures only one result is fetched, or null if not found
            ?.itemId
    }

    fun insertOrReplaceItem(name: String, itemId: Int) = transaction {
        // Perform insert or replace operation
        ItemTable.insertIgnore {
            it[ItemTable.name] = name
            it[ItemTable.itemId] = itemId
        }.also { result ->
            // If no row is inserted (conflict happened), replace the row
            if (result.insertedCount == 0) {
                ItemTable.update({ ItemTable.name eq name }) {
                    it[ItemTable.itemId] = itemId
                }
            }
        }
    }
}
