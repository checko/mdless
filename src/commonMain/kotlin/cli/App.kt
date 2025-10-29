package cli

import pager.PagerState

object App {
    fun handleKey(pager: PagerState, key: KeyCmd) : Boolean {
        when (key) {
            KeyCmd.Down -> pager.scrollLines(1)
            KeyCmd.Up -> pager.scrollLines(-1)
            KeyCmd.PageDown -> pager.scrollPages(1)
            KeyCmd.PageUp -> pager.scrollPages(-1)
            KeyCmd.Top -> pager.jumpToTop()
            KeyCmd.Bottom -> pager.jumpToBottom()
            KeyCmd.None -> {}
            KeyCmd.SearchForward -> {}
            KeyCmd.SearchBackward -> {}
            KeyCmd.SearchNext -> {}
            KeyCmd.SearchPrev -> {}
            KeyCmd.Quit -> return false
        }
        return true
    }
}
