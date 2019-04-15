package chat.rocket.android.videoconference.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import chat.rocket.android.videoconference.presenter.JitsiVideoConferenceView
import chat.rocket.android.videoconference.presenter.VideoConferencePresenter
import dagger.android.AndroidInjection
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetView
import org.jitsi.meet.sdk.JitsiMeetViewListener
import timber.log.Timber
import java.net.URL
import javax.inject.Inject

fun Context.videoConferenceIntent(chatRoomId: String, chatRoomType: String): Intent =
    Intent(this, VideoConferenceActivity::class.java)
        .putExtra(INTENT_CHAT_ROOM_ID, chatRoomId)
        .putExtra(INTENT_CHAT_ROOM_TYPE, chatRoomType)

private const val INTENT_CHAT_ROOM_ID = "chat_room_id"
private const val INTENT_CHAT_ROOM_TYPE = "chat_room_type"

class VideoConferenceActivity : JitsiMeetActivity(), JitsiVideoConferenceView,
    JitsiMeetViewListener {
    @Inject
    lateinit var presenter: VideoConferencePresenter
    private lateinit var chatRoomId: String
    private lateinit var chatRoomType: String
    private var view: JitsiMeetView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        chatRoomId = intent.getStringExtra(INTENT_CHAT_ROOM_ID)
        requireNotNull(chatRoomId) { "no chat_room_id provided in Intent extras" }
        chatRoomType = intent.getStringExtra(INTENT_CHAT_ROOM_TYPE)
        requireNotNull(chatRoomType) { "no chat_room_type provided in Intent extras" }

        view = JitsiMeetView(this)
        view?.listener = this
        setContentView(view)

        presenter.setup(chatRoomId, chatRoomType)
        presenter.initVideoConference()
    }

    override fun onConferenceWillJoin(map: MutableMap<String, Any>?) =
        logJitsiMeetViewState("Joining video conferencing", map)

    override fun onConferenceJoined(map: MutableMap<String, Any>?) =
        logJitsiMeetViewState("Joined video conferencing", map)

    override fun onConferenceTerminated(map: MutableMap<String, Any>?) {
        if(!map.isNullOrEmpty()){
            if(map.containsKey("error")) {
                logJitsiMeetViewState("Terminated video conferencing with error", map)
            }
            else{
                logJitsiMeetViewState("Terminated video conferencing", map)
                finishJitsiVideoConference()
            }
        }
    }

    override fun startJitsiVideoConference(url: String, name: String?) {
        var options = JitsiMeetConferenceOptions.Builder()
                .setAudioMuted(true)
                .setVideoMuted(true)
                .setServerURL(URL(url))
                .setAudioOnly(false)
                .build()

        view?.join(options)
    }

    override fun finishJitsiVideoConference() {
        presenter.invalidateTimer()
        view?.dispose()
        view = null
        finish()
    }

    override fun logJitsiMeetViewState(message: String, map: MutableMap<String, Any>?) =
        Timber.i("$message:  $map")
}
