@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package tty

import kotlinx.cinterop.*
import platform.posix.*

object Tty {
    fun isattyStdout(): Boolean = (isatty(STDOUT_FILENO) == 1)
    fun isattyStdin(): Boolean = (isatty(STDIN_FILENO) == 1)

    class RawToken internal constructor(val old: termios)

    fun enableRaw(fd: Int = STDIN_FILENO): RawToken? {
        memScoped {
            val orig = alloc<termios>()
            if (tcgetattr(fd, orig.ptr) != 0) return null
            val raw = orig
            // cfmakeraw equivalent
            val iflagsToClear: UInt = (IGNBRK or BRKINT or PARMRK or ISTRIP or INLCR or IGNCR or ICRNL or IXON).toUInt()
            val oflagsToClear: UInt = OPOST.toUInt()
            val lflagsToClear: UInt = (ECHO or ECHONL or ICANON or ISIG or IEXTEN).toUInt()
            raw.c_iflag = raw.c_iflag and iflagsToClear.inv()
            raw.c_oflag = raw.c_oflag and oflagsToClear.inv()
            raw.c_lflag = raw.c_lflag and lflagsToClear.inv()
            raw.c_cflag = raw.c_cflag or (CS8.toUInt())
            raw.c_cc[VMIN] = 0.toUByte()   // non-blocking read
            raw.c_cc[VTIME] = 1.toUByte() // 100ms timeout
            if (tcsetattr(fd, TCSANOW, raw.ptr) != 0) return null
            return RawToken(orig)
        }
    }

    fun restoreRaw(token: RawToken, fd: Int = STDIN_FILENO) {
        memScoped {
            tcsetattr(fd, TCSANOW, token.old.ptr)
        }
    }

    inline fun <R> withRawMode(fd: Int = STDIN_FILENO, body: () -> R): R {
        val token = enableRaw(fd)
        try {
            return body()
        } finally {
            if (token != null) restoreRaw(token, fd)
        }
    }

    fun readByte(fd: Int = STDIN_FILENO): Int = memScoped {
        val buf = allocArray<ByteVar>(1)
        val n = read(fd, buf, 1.convert())
        if (n <= 0) -1 else (buf[0].toInt() and 0xFF)
    }

    data class TermSize(val rows: Int, val cols: Int)

    fun getTermSize(fd: Int = STDOUT_FILENO): TermSize {
        memScoped {
            val ws = alloc<winsize>()
            if (ioctl(fd, TIOCGWINSZ.toULong(), ws.ptr) == 0) {
                return TermSize(ws.ws_row.toInt(), ws.ws_col.toInt())
            }
        }
        return TermSize(24, 80)
    }
}
