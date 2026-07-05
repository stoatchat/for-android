package chat.stoat.activities

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ShareTargetActivityTest {
    @Test
    fun fileUriPointingAtPrivateStorageIsRejectedDuringOnCreate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext

        val privateDatabase = File(targetContext.applicationInfo.dataDir, "databases/revolt.db")
        val intent = Intent(targetContext, ShareTargetActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(privateDatabase))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val activity = instrumentation.startActivitySync(intent)
        try {
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            assertTrue(
                "ShareTargetActivity must reject file:// URIs before rendering the share flow",
                activity.isFinishing && content.childCount == 0
            )
        } finally {
            activity.finish()
        }
    }
}
