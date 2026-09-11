package com.mapgie.dash.data.model

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the app-to-database contract: every enum value the app can write to
 * Supabase must be permitted by the matching CHECK constraint in
 * `supabase/schema.sql`. This is exactly the drift that broke saving a task due
 * "Eventually" (the app sent a `due_period` the constraint didn't allow, and
 * Supabase rejected the insert). Adding a value to [DuePeriod] or [TaskPriority]
 * without widening the schema now fails here instead of in the field.
 *
 * It proves only that the app agrees with the checked-in schema, not that a live
 * database has had that schema applied. The optional REST contract check
 * (`.github/workflows/schema-contract.yml`) covers the live-database side.
 */
class SchemaSyncTest {

    private val schema: String by lazy { readSchema() }

    @Test
    fun `every due_period the app can write is allowed by the schema`() {
        val allowed = allowedValues("due_period")
        val emitted = DuePeriod.keys.toSet()
        assertTrue(
            "supabase/schema.sql allows due_period $allowed, but the app can write " +
                "${emitted - allowed} (see DuePeriod). Widen the CHECK and add the ALTER migration.",
            allowed.containsAll(emitted),
        )
    }

    @Test
    fun `every priority the app can write is allowed by the schema`() {
        val allowed = allowedValues("priority")
        val emitted = TaskPriority.entries.map { it.wire }.toSet()
        assertTrue(
            "supabase/schema.sql allows priority $allowed, but the app can write " +
                "${emitted - allowed} (see TaskPriority). Widen the CHECK and add the ALTER migration.",
            allowed.containsAll(emitted),
        )
    }

    @Test
    fun `every table is granted to the anon role for the Data API`() {
        // From 2026 Supabase stops auto-exposing public tables to the Data API, so
        // a table the app reaches through the anon key must carry an explicit GRANT
        // to anon (see supabase/schema.sql). This catches adding a CREATE TABLE
        // without the matching grant.
        // Anchored to the start of a line so a comment that merely says
        // "CREATE TABLE IF NOT EXISTS never alters..." is not read as a table.
        val tables = Regex("""(?m)^[ \t]*CREATE TABLE IF NOT EXISTS\s+(\w+)""", RegexOption.IGNORE_CASE)
            .findAll(schema).map { it.groupValues[1] }.toList()
        assertTrue("No CREATE TABLE statements found in supabase/schema.sql", tables.isNotEmpty())
        val ungranted = tables.filterNot { table ->
            Regex("""GRANT[^;]*\bON\s+$table\b[^;]*\banon\b""", RegexOption.IGNORE_CASE).containsMatchIn(schema)
        }
        assertTrue(
            "supabase/schema.sql creates $ungranted but never GRANTs them to anon; the app " +
                "can't reach them through the Data API. Add a GRANT ... ON <table> TO anon.",
            ungranted.isEmpty(),
        )
    }

    /** The quoted values inside the first `CHECK (<column> IN ('a', 'b', ...))` for [column]. */
    private fun allowedValues(column: String): Set<String> {
        val inList = Regex("""$column\s+IN\s*\(([^)]*)\)""", RegexOption.IGNORE_CASE)
            .find(schema)?.groupValues?.get(1)
            ?: error("No `CHECK ($column IN (...))` found in supabase/schema.sql")
        return Regex("'([^']*)'").findAll(inList).map { it.groupValues[1] }.toSet()
    }

    /** Walks up from the test working directory (the app module) to the repo's schema file. */
    private fun readSchema(): String {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        repeat(6) {
            val candidate = File(dir, "supabase/schema.sql")
            if (candidate.isFile) return candidate.readText()
            dir = dir?.parentFile
        }
        error("supabase/schema.sql not found above ${System.getProperty("user.dir")}")
    }
}
