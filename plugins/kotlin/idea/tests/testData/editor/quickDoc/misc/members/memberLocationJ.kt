class LocationContainerE {
    var memberLocationC = 0
    fun memberLocationD() {}

    class LocationContainerF {
        var memberLocationE = 0
        fun memberLocationF() {}
    }

    inner class LocationContainerG {
        var memberLocationG = 0
        fun memberLocationH() {}
    }

    companion object {
        var memberLocationI = 0
        fun <caret>memberLocationJ() {}
    }
}
//INFO: <div class='definition'><pre><span style="color:#000080;font-weight:bold;">public</span> <span style="color:#000080;font-weight:bold;">final</span> <span style="color:#000080;font-weight:bold;">fun</span> <span style="color:#000000;">memberLocationJ</span>()<span style="">: </span><span style="color:#000000;"><a href="psi_element://kotlin.Unit">Unit</a></span></pre></div><div class='bottom'><icon src="KotlinBaseResourcesIcons.ClassKotlin"/>&nbsp;<a href="psi_element://LocationContainerE.Companion"><code><span style="color:#000000;">LocationContainerE.Companion</span></code></a><br/></div>
