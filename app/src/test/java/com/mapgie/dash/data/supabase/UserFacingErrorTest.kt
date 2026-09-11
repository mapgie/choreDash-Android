package com.mapgie.dash.data.supabase

import org.junit.Assert.assertEquals
import org.junit.Test

/** What reaches the screen when a Supabase request fails: the reason, never the request. */
class UserFacingErrorTest {

    private val restDump = "permission denied for table tags\n" +
        "URL: https://example.supabase.co/rest/v1/tags?tag_id=eq.laundry-towels\n" +
        "Headers: [Authorization=[Bearer sb_publishable_abc], apikey=[sb_publishable_abc]]\n" +
        "Http Method: PATCH"

    @Test
    fun `a REST failure shows its first line and drops the URL, headers and key`() {
        assertEquals("permission denied for table tags", userFacingMessage(restDump))
    }

    @Test
    fun `a plain message is shown as it is`() {
        assertEquals("Could not reach the server", userFacingMessage("Could not reach the server"))
        assertEquals("Could not reach the server", RuntimeException("Could not reach the server").userFacingMessage())
    }

    @Test
    fun `an exception with no message names its type instead of showing nothing`() {
        assertEquals("IllegalStateException", IllegalStateException().userFacingMessage())
        assertEquals("Request failed", userFacingMessage("URL: https://x"))
    }
}
