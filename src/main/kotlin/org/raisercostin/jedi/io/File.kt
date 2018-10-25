package org.raisercostin.jedi

import java.io.InputStream
import java.net.URL
import java.nio.file.Path

interface File {
}

class Locations{
    companion object {
        @JvmStatic
        fun current(): String {
            return "";
        }
    }
}
