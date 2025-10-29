@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package tty

import kotlinx.cinterop.*
import platform.posix.*

object Tty {
    fun isattyStdout(): Boolean = (isatty(STDOUT_FILENO) == 1)
    fun isattyStdin(): Boolean = (isatty(STDIN_FILENO) == 1)

    class RawToken internal constructor(val old: termios)

    fun enableRaw(fd: Int = STDIN_FILENO): RawToken? {
        // Allocate persistent copies so we can restore even if we leave scope
        val orig = nativeHeap.alloc<termios>()
        if (tcgetattr(fd, orig.ptr) != 0) {
            nativeHeap.free(orig)
            return null
        }
        val raw = nativeHeap.alloc<termios>()
        // copy orig -> raw
        platform.posix.memcpy(raw.ptr, orig.ptr, sizeOf<termios>().convert())
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
        val ok = tcsetattr(fd, TCSANOW, raw.ptr) == 0
        nativeHeap.free(raw)
        return if (ok) RawToken(orig) else { nativeHeap.free(orig); null }
    }

    fun restoreRaw(token: RawToken, fd: Int = STDIN_FILENO) {
        tcsetattr(fd, TCSANOW, token.old.ptr)
        nativeHeap.free(token.old)
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
        fun query(fd: Int): TermSize? = memScoped {
            val ws = alloc<winsize>()
            if (ioctl(fd, TIOCGWINSZ.toULong(), ws.ptr) == 0) {
                TermSize(ws.ws_row.toInt(), ws.ws_col.toInt())
            } else null
        }
        // Try stdout, then stdin, then /dev/tty, then env, else fallback
        query(fd)?.let { return it }
        query(STDIN_FILENO)?.let { return it }
        memScoped {
            val dev = fopen("/dev/tty", "r")
            if (dev != null) {
                val ttyFd = fileno(dev)
                val ts = query(ttyFd)
                fclose(dev)
                if (ts != null) return ts
            }
        }
        // Env vars COLUMNS and LINES
        val colsEnv = getenv("COLUMNS")?.toKString()?.toIntOrNull()
        val rowsEnv = getenv("LINES")?.toKString()?.toIntOrNull()
        if (colsEnv != null && rowsEnv != null) return TermSize(rowsEnv, colsEnv)
        return TermSize(24, 80)
    }
}
