package org.bharatai.ps5.metalinspection

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal data class InspectionRecord(
    val id: Long,
    val label: String,
    val inspectedAt: Long,
    val durationMs: Long,
    val severity: String,
)

internal class InspectionHistoryStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "inspection_history.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE inspections (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                label TEXT NOT NULL,
                inspected_at INTEGER NOT NULL,
                duration_ms INTEGER NOT NULL,
                severity TEXT NOT NULL DEFAULT 'unreviewed'
            )""".trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE inspections ADD COLUMN severity TEXT NOT NULL DEFAULT 'unreviewed'")
    }

    fun add(record: InspectionRecord): Long {
        val values = ContentValues().apply {
            put("label", record.label)
            put("inspected_at", record.inspectedAt)
            put("duration_ms", record.durationMs)
            put("severity", record.severity)
        }
        return writableDatabase.insertOrThrow("inspections", null, values)
    }

    fun setSeverity(id: Long, severity: String) {
        writableDatabase.update("inspections", ContentValues().apply { put("severity", severity) }, "id = ?", arrayOf(id.toString()))
    }

    fun recent(limit: Int = 20): List<InspectionRecord> {
        val records = mutableListOf<InspectionRecord>()
        readableDatabase.query(
            "inspections",
            arrayOf("id", "label", "inspected_at", "duration_ms", "severity"),
            null,
            null,
            null,
            null,
            "inspected_at DESC",
            limit.toString(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                records += InspectionRecord(
                    id = cursor.getLong(0),
                    label = cursor.getString(1),
                    inspectedAt = cursor.getLong(2),
                    durationMs = cursor.getLong(3),
                    severity = cursor.getString(4),
                )
            }
        }
        return records
    }
}
