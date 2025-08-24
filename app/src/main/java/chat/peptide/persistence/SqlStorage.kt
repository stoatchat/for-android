package chat.peptide.persistence

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import chat.peptide.PeptideApplication

object SqlStorage {
    val driver: SqlDriver = AndroidSqliteDriver(
        Database.Schema,
        PeptideApplication.instance.applicationContext,
        "peptide.db"
    )
}