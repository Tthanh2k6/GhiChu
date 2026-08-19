package com.ghichu

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Bai kiem thu de chac HA TANG chay duoc, truoc khi tin vao cac bai khac.
 *
 * Diem can chac: `org.json` trong `android.jar` chi la vo rong — goi vao la
 * `RuntimeException: Stub!`. Bai nay hong nghia la phai kiem tra lai
 * `testImplementation("org.json:json:...")` o build.gradle.kts.
 */
class HaTangTest {

    @Test
    fun orgJsonChayDuocTrenJvm() {
        val o = JSONObject("""{"a": 1, "b": "hai"}""")
        assertEquals(1, o.getInt("a"))
        assertEquals("hai", o.getString("b"))
    }
}
