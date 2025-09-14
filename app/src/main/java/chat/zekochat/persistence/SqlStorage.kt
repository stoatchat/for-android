package chat.zekochat.persistence

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import chat.zekochat.PeptideApplication
import com.zekochat.persistence.Database

object SqlStorage {
    val driver: SqlDriver = AndroidSqliteDriver(
        Database.Schema,
        PeptideApplication.instance.applicationContext,
        "peptide.db"
    )
}