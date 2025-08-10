package chat.peptide.persistence

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import chat.peptide.RevoltApplication

object SqlStorage {
    val driver: SqlDriver = AndroidSqliteDriver(
        Database.Schema,
        RevoltApplication.instance.applicationContext,
        "revolt.db"
    )
}