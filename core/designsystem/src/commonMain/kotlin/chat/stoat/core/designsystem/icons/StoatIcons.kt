package chat.stoat.core.designsystem.icons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import chat.stoat.core.designsystem.resources.Res
import chat.stoat.core.designsystem.resources.ic_account_box_24dp
import chat.stoat.core.designsystem.resources.ic_account_circle_24dp
import chat.stoat.core.designsystem.resources.ic_action_key_24dp
import chat.stoat.core.designsystem.resources.ic_add_24dp
import chat.stoat.core.designsystem.resources.ic_add_reaction_24dp
import chat.stoat.core.designsystem.resources.ic_arrow_back_24dp
import chat.stoat.core.designsystem.resources.ic_arrow_forward_24dp
import chat.stoat.core.designsystem.resources.ic_attach_file_24dp
import chat.stoat.core.designsystem.resources.ic_attach_file_off_24dp
import chat.stoat.core.designsystem.resources.ic_badge_24dp
import chat.stoat.core.designsystem.resources.ic_bedtime_24dp
import chat.stoat.core.designsystem.resources.ic_block_24dp
import chat.stoat.core.designsystem.resources.ic_brightness_alert_24dp
import chat.stoat.core.designsystem.resources.ic_bus_railway_24dp
import chat.stoat.core.designsystem.resources.ic_call_24dp__fill
import chat.stoat.core.designsystem.resources.ic_call_end_24dp__fill
import chat.stoat.core.designsystem.resources.ic_camera_24dp
import chat.stoat.core.designsystem.resources.ic_cameraswitch_24dp
import chat.stoat.core.designsystem.resources.ic_campaign_24dp
import chat.stoat.core.designsystem.resources.ic_chat_24dp
import chat.stoat.core.designsystem.resources.ic_check_24dp
import chat.stoat.core.designsystem.resources.ic_chevron_forward_24dp
import chat.stoat.core.designsystem.resources.ic_circle_24dp
import chat.stoat.core.designsystem.resources.ic_circle_24dp__fill
import chat.stoat.core.designsystem.resources.ic_circle_circle_24dp
import chat.stoat.core.designsystem.resources.ic_close_24dp
import chat.stoat.core.designsystem.resources.ic_cloud_24dp
import chat.stoat.core.designsystem.resources.ic_content_copy_24dp
import chat.stoat.core.designsystem.resources.ic_contract_edit_24dp
import chat.stoat.core.designsystem.resources.ic_create_new_folder_24dp
import chat.stoat.core.designsystem.resources.ic_delete_24dp
import chat.stoat.core.designsystem.resources.ic_devices_24dp
import chat.stoat.core.designsystem.resources.ic_do_not_disturb_on_24dp
import chat.stoat.core.designsystem.resources.ic_door_open_24dp
import chat.stoat.core.designsystem.resources.ic_download_24dp
import chat.stoat.core.designsystem.resources.ic_drag_handle_24dp
import chat.stoat.core.designsystem.resources.ic_edit_24dp
import chat.stoat.core.designsystem.resources.ic_emoji_nature_24dp
import chat.stoat.core.designsystem.resources.ic_emoji_objects_24dp
import chat.stoat.core.designsystem.resources.ic_emoji_people_24dp
import chat.stoat.core.designsystem.resources.ic_emoji_symbols_24dp
import chat.stoat.core.designsystem.resources.ic_error_24dp
import chat.stoat.core.designsystem.resources.ic_explore_24dp
import chat.stoat.core.designsystem.resources.ic_feedback_24dp
import chat.stoat.core.designsystem.resources.ic_file_export_24dp
import chat.stoat.core.designsystem.resources.ic_file_open_24dp
import chat.stoat.core.designsystem.resources.ic_file_present_24dp
import chat.stoat.core.designsystem.resources.ic_flag_24dp
import chat.stoat.core.designsystem.resources.ic_folder_24dp
import chat.stoat.core.designsystem.resources.ic_folder_off_24dp
import chat.stoat.core.designsystem.resources.ic_folder_open_24dp
import chat.stoat.core.designsystem.resources.ic_forum_24dp
import chat.stoat.core.designsystem.resources.ic_forum_24dp__fill
import chat.stoat.core.designsystem.resources.ic_forward_5_24dp
import chat.stoat.core.designsystem.resources.ic_front_hand_24dp
import chat.stoat.core.designsystem.resources.ic_fullscreen_24dp
import chat.stoat.core.designsystem.resources.ic_fullscreen_exit_24dp
import chat.stoat.core.designsystem.resources.ic_gavel_24dp
import chat.stoat.core.designsystem.resources.ic_grid_3x3_24dp
import chat.stoat.core.designsystem.resources.ic_grid_3x3_off_24dp
import chat.stoat.core.designsystem.resources.ic_group_24dp
import chat.stoat.core.designsystem.resources.ic_group_add_24dp
import chat.stoat.core.designsystem.resources.ic_group_remove_24dp
import chat.stoat.core.designsystem.resources.ic_hardware_24dp
import chat.stoat.core.designsystem.resources.ic_headset_24dp
import chat.stoat.core.designsystem.resources.ic_headset_off_24dp
import chat.stoat.core.designsystem.resources.ic_home_24dp
import chat.stoat.core.designsystem.resources.ic_id_card_24dp
import chat.stoat.core.designsystem.resources.ic_identifier_copy_24dp
import chat.stoat.core.designsystem.resources.ic_inbox_24dp
import chat.stoat.core.designsystem.resources.ic_info_24dp
import chat.stoat.core.designsystem.resources.ic_ink_highlighter_move_24dp
import chat.stoat.core.designsystem.resources.ic_ios_share_24dp
import chat.stoat.core.designsystem.resources.ic_keep_24dp
import chat.stoat.core.designsystem.resources.ic_keep_off_24dp
import chat.stoat.core.designsystem.resources.ic_key_24dp
import chat.stoat.core.designsystem.resources.ic_keyboard_arrow_right_24dp
import chat.stoat.core.designsystem.resources.ic_lab_research_24dp
import chat.stoat.core.designsystem.resources.ic_landscape_2_edit_24dp
import chat.stoat.core.designsystem.resources.ic_language_24dp
import chat.stoat.core.designsystem.resources.ic_link_24dp
import chat.stoat.core.designsystem.resources.ic_list_24dp
import chat.stoat.core.designsystem.resources.ic_lock_24dp
import chat.stoat.core.designsystem.resources.ic_logout_24dp
import chat.stoat.core.designsystem.resources.ic_mark_chat_read_24dp
import chat.stoat.core.designsystem.resources.ic_menu_24dp
import chat.stoat.core.designsystem.resources.ic_mic_24dp
import chat.stoat.core.designsystem.resources.ic_mic_off_24dp
import chat.stoat.core.designsystem.resources.ic_mobile_share_24px
import chat.stoat.core.designsystem.resources.ic_mood_24dp
import chat.stoat.core.designsystem.resources.ic_more_vert_24dp
import chat.stoat.core.designsystem.resources.ic_move_to_inbox_24dp
import chat.stoat.core.designsystem.resources.ic_note_stack_24dp
import chat.stoat.core.designsystem.resources.ic_notification_settings_24dp
import chat.stoat.core.designsystem.resources.ic_notifications_24dp
import chat.stoat.core.designsystem.resources.ic_open_in_browser_24dp
import chat.stoat.core.designsystem.resources.ic_open_in_new_24dp
import chat.stoat.core.designsystem.resources.ic_page_info_24dp
import chat.stoat.core.designsystem.resources.ic_palette_24dp
import chat.stoat.core.designsystem.resources.ic_pause_24dp__fill
import chat.stoat.core.designsystem.resources.ic_person_celebrate_24dp
import chat.stoat.core.designsystem.resources.ic_person_off_24dp
import chat.stoat.core.designsystem.resources.ic_photo_library_24dp
import chat.stoat.core.designsystem.resources.ic_pinboard_24dp
import chat.stoat.core.designsystem.resources.ic_play_arrow_24dp
import chat.stoat.core.designsystem.resources.ic_play_arrow_24dp__fill
import chat.stoat.core.designsystem.resources.ic_plumbing_24dp
import chat.stoat.core.designsystem.resources.ic_qr_code_scanner_24dp
import chat.stoat.core.designsystem.resources.ic_remove_24dp
import chat.stoat.core.designsystem.resources.ic_replay_5_24dp
import chat.stoat.core.designsystem.resources.ic_reply_24dp
import chat.stoat.core.designsystem.resources.ic_report_24dp
import chat.stoat.core.designsystem.resources.ic_screen_share_24dp
import chat.stoat.core.designsystem.resources.ic_search_24dp
import chat.stoat.core.designsystem.resources.ic_send_24dp
import chat.stoat.core.designsystem.resources.ic_settings_24dp
import chat.stoat.core.designsystem.resources.ic_shield_lock_24dp
import chat.stoat.core.designsystem.resources.ic_sign_language_24dp
import chat.stoat.core.designsystem.resources.ic_smart_toy_24dp
import chat.stoat.core.designsystem.resources.ic_south_24dp
import chat.stoat.core.designsystem.resources.ic_sports_and_outdoors_24dp
import chat.stoat.core.designsystem.resources.ic_sports_bar_24dp
import chat.stoat.core.designsystem.resources.ic_sprint_24dp
import chat.stoat.core.designsystem.resources.ic_star_shine_24dp
import chat.stoat.core.designsystem.resources.ic_star_shine_24dp__fill
import chat.stoat.core.designsystem.resources.ic_tag_24dp
import chat.stoat.core.designsystem.resources.ic_timer_24dp
import chat.stoat.core.designsystem.resources.ic_undo_24dp
import chat.stoat.core.designsystem.resources.ic_verified_24dp__fill
import chat.stoat.core.designsystem.resources.ic_videocam_24dp
import chat.stoat.core.designsystem.resources.ic_videocam_off_24dp
import chat.stoat.core.designsystem.resources.ic_visibility_24dp
import chat.stoat.core.designsystem.resources.ic_visibility_off_24dp
import chat.stoat.core.designsystem.resources.ic_voice_chat_24dp
import chat.stoat.core.designsystem.resources.ic_volume_up_24dp
import chat.stoat.core.designsystem.resources.ic_waving_hand_24dp
import chat.stoat.core.designsystem.resources.ic_webhook_24dp
import chat.stoat.core.designsystem.resources.ic_wifi_tethering_24dp
import chat.stoat.core.designsystem.resources.ic_wifi_tethering_error_24dp
import chat.stoat.core.designsystem.resources.ic_workspace_premium_24dp__fill
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Immutable
class StoatIcon internal constructor(internal val resource: DrawableResource)

@Composable
fun StoatIcon.painter(): Painter = painterResource(resource)

object StoatIcons {
    val About = StoatIcon(Res.drawable.ic_info_24dp)
    val Account = StoatIcon(Res.drawable.ic_lock_24dp)
    val ActiveSupporterBadge = StoatIcon(Res.drawable.ic_emoji_people_24dp)
    val Add = StoatIcon(Res.drawable.ic_add_24dp)
    val AddFriendByUsername = StoatIcon(Res.drawable.ic_tag_24dp)
    val AddMembers = StoatIcon(Res.drawable.ic_add_24dp)
    val AddReaction = StoatIcon(Res.drawable.ic_add_reaction_24dp)
    val AddToFolder = StoatIcon(Res.drawable.ic_folder_24dp)
    val AgeGate = StoatIcon(Res.drawable.ic_front_hand_24dp)
    val Appearance = StoatIcon(Res.drawable.ic_palette_24dp)
    val AttachFile = StoatIcon(Res.drawable.ic_attach_file_24dp)
    val AudioOutput = StoatIcon(Res.drawable.ic_volume_up_24dp)
    val Authenticator = StoatIcon(Res.drawable.ic_shield_lock_24dp)
    val Back = StoatIcon(Res.drawable.ic_arrow_back_24dp)
    val Ban = StoatIcon(Res.drawable.ic_gavel_24dp)
    val Bans = StoatIcon(Res.drawable.ic_gavel_24dp)
    val Block = StoatIcon(Res.drawable.ic_block_24dp)
    val BlockedMessage = StoatIcon(Res.drawable.ic_block_24dp)
    val Bot = StoatIcon(Res.drawable.ic_smart_toy_24dp)
    val BotBadge = StoatIcon(Res.drawable.ic_smart_toy_24dp)
    val BridgedBadge = StoatIcon(Res.drawable.ic_link_24dp)
    val Call = StoatIcon(Res.drawable.ic_call_24dp__fill)
    val CallCamera = StoatIcon(Res.drawable.ic_videocam_24dp)
    val CallCameraOff = StoatIcon(Res.drawable.ic_videocam_off_24dp)
    val CallStarted = StoatIcon(Res.drawable.ic_call_24dp__fill)
    val CancelEditing = StoatIcon(Res.drawable.ic_close_24dp)
    val CatchUp = StoatIcon(Res.drawable.ic_inbox_24dp)
    val CatchUpAvailable = StoatIcon(Res.drawable.ic_move_to_inbox_24dp)
    val Changelog = StoatIcon(Res.drawable.ic_campaign_24dp)
    val Channel = StoatIcon(Res.drawable.ic_tag_24dp)
    val ChannelCategory = StoatIcon(Res.drawable.ic_list_24dp)
    val ChannelDescriptionChanged = StoatIcon(Res.drawable.ic_contract_edit_24dp)
    val ChannelIconChanged = StoatIcon(Res.drawable.ic_landscape_2_edit_24dp)
    val ChannelRenamed = StoatIcon(Res.drawable.ic_ink_highlighter_move_24dp)
    val Channels = StoatIcon(Res.drawable.ic_grid_3x3_24dp)
    val ChatSettings = StoatIcon(Res.drawable.ic_chat_24dp)
    val ClearSearch = StoatIcon(Res.drawable.ic_close_24dp)
    val CloseSkinToneMenu = StoatIcon(Res.drawable.ic_keyboard_arrow_right_24dp)
    val Confirm = StoatIcon(Res.drawable.ic_check_24dp)
    val ConfirmEdit = StoatIcon(Res.drawable.ic_edit_24dp)
    val Copy = StoatIcon(Res.drawable.ic_content_copy_24dp)
    val CopyId = StoatIcon(Res.drawable.ic_identifier_copy_24dp)
    val CopyLink = StoatIcon(Res.drawable.ic_link_24dp)
    val Deafen = StoatIcon(Res.drawable.ic_headset_24dp)
    val Deafened = StoatIcon(Res.drawable.ic_headset_off_24dp)
    val Debug = StoatIcon(Res.drawable.ic_sign_language_24dp)
    val Delete = StoatIcon(Res.drawable.ic_delete_24dp)
    val DirectMessage = StoatIcon(Res.drawable.ic_account_circle_24dp)
    val Disclosure = StoatIcon(Res.drawable.ic_chevron_forward_24dp)
    val Discover = StoatIcon(Res.drawable.ic_explore_24dp)
    val Dismiss = StoatIcon(Res.drawable.ic_close_24dp)
    val EarlyAccess = StoatIcon(Res.drawable.ic_plumbing_24dp)
    val Edit = StoatIcon(Res.drawable.ic_edit_24dp)
    val EditedIndicator = StoatIcon(Res.drawable.ic_edit_24dp)
    val Editing = StoatIcon(Res.drawable.ic_edit_24dp)
    val Emoji = StoatIcon(Res.drawable.ic_mood_24dp)
    val EmojiActivities = StoatIcon(Res.drawable.ic_sports_and_outdoors_24dp)
    val EmojiFlags = StoatIcon(Res.drawable.ic_flag_24dp)
    val EmojiFood = StoatIcon(Res.drawable.ic_sports_bar_24dp)
    val EmojiNature = StoatIcon(Res.drawable.ic_emoji_nature_24dp)
    val EmojiObjects = StoatIcon(Res.drawable.ic_emoji_objects_24dp)
    val EmojiPeople = StoatIcon(Res.drawable.ic_emoji_people_24dp)
    val EmojiSmileys = StoatIcon(Res.drawable.ic_mood_24dp)
    val EmojiSymbols = StoatIcon(Res.drawable.ic_emoji_symbols_24dp)
    val EmojiTravel = StoatIcon(Res.drawable.ic_bus_railway_24dp)
    val Emojis = StoatIcon(Res.drawable.ic_mood_24dp)
    val EnterFullscreen = StoatIcon(Res.drawable.ic_fullscreen_24dp)
    val Error = StoatIcon(Res.drawable.ic_error_24dp)
    val ExitFullscreen = StoatIcon(Res.drawable.ic_fullscreen_exit_24dp)
    val ExpandToolbar = StoatIcon(Res.drawable.ic_chevron_forward_24dp)
    val Experiments = StoatIcon(Res.drawable.ic_lab_research_24dp)
    val ExportTheme = StoatIcon(Res.drawable.ic_file_export_24dp)
    val Feedback = StoatIcon(Res.drawable.ic_feedback_24dp)
    val FileAttachment = StoatIcon(Res.drawable.ic_file_present_24dp)
    val Folder = StoatIcon(Res.drawable.ic_folder_open_24dp)
    val FolderColour = StoatIcon(Res.drawable.ic_palette_24dp)
    val Friends = StoatIcon(Res.drawable.ic_group_24dp)
    val GroupChannel = StoatIcon(Res.drawable.ic_account_box_24dp)
    val HangUp = StoatIcon(Res.drawable.ic_call_end_24dp__fill)
    val HidePassword = StoatIcon(Res.drawable.ic_visibility_off_24dp)
    val ImportTheme = StoatIcon(Res.drawable.ic_file_open_24dp)
    val Invites = StoatIcon(Res.drawable.ic_link_24dp)
    val JoinLounge = StoatIcon(Res.drawable.ic_waving_hand_24dp)
    val JoinVoice = StoatIcon(Res.drawable.ic_voice_chat_24dp)
    val Kick = StoatIcon(Res.drawable.ic_sports_and_outdoors_24dp)
    val Labs = StoatIcon(Res.drawable.ic_sign_language_24dp)
    val LabsHome = StoatIcon(Res.drawable.ic_home_24dp)
    val LabsMockups = StoatIcon(Res.drawable.ic_menu_24dp)
    val LabsSandboxes = StoatIcon(Res.drawable.ic_play_arrow_24dp)
    val Language = StoatIcon(Res.drawable.ic_language_24dp)
    val Leave = StoatIcon(Res.drawable.ic_door_open_24dp)
    val Locked = StoatIcon(Res.drawable.ic_lock_24dp)
    val LogOut = StoatIcon(Res.drawable.ic_logout_24dp)
    val MarkRead = StoatIcon(Res.drawable.ic_mark_chat_read_24dp)
    val MarkUnread = StoatIcon(Res.drawable.ic_visibility_off_24dp)
    val MassMention = StoatIcon(Res.drawable.ic_campaign_24dp)
    val Members = StoatIcon(Res.drawable.ic_group_24dp)
    val MetaKey = StoatIcon(Res.drawable.ic_action_key_24dp)
    val Microphone = StoatIcon(Res.drawable.ic_mic_24dp)
    val MicrophoneMuted = StoatIcon(Res.drawable.ic_mic_off_24dp)
    val MicrophonePermission = StoatIcon(Res.drawable.ic_mic_24dp)
    val ModerationBadge = StoatIcon(Res.drawable.ic_brightness_alert_24dp)
    val MoreOptions = StoatIcon(Res.drawable.ic_more_vert_24dp)
    val NewFolder = StoatIcon(Res.drawable.ic_create_new_folder_24dp)
    val NewGroup = StoatIcon(Res.drawable.ic_group_add_24dp)
    val NotesChannel = StoatIcon(Res.drawable.ic_note_stack_24dp)
    val NotificationSettings = StoatIcon(Res.drawable.ic_notification_settings_24dp)
    val Notifications = StoatIcon(Res.drawable.ic_notifications_24dp)
    val Official = StoatIcon(Res.drawable.ic_workspace_premium_24dp__fill)
    val OpenDrawer = StoatIcon(Res.drawable.ic_menu_24dp)
    val OpenExternal = StoatIcon(Res.drawable.ic_open_in_new_24dp)
    val OpenInBrowser = StoatIcon(Res.drawable.ic_open_in_browser_24dp)
    val Overview = StoatIcon(Res.drawable.ic_info_24dp)
    val OwnershipTransferred = StoatIcon(Res.drawable.ic_key_24dp)
    val Pause = StoatIcon(Res.drawable.ic_pause_24dp__fill)
    val PermissionAllow = StoatIcon(Res.drawable.ic_check_24dp)
    val PermissionDeny = StoatIcon(Res.drawable.ic_close_24dp)
    val PermissionInherit = StoatIcon(Res.drawable.ic_remove_24dp)
    val Permissions = StoatIcon(Res.drawable.ic_page_info_24dp)
    val PhotoLibrary = StoatIcon(Res.drawable.ic_photo_library_24dp)
    val Pinned = StoatIcon(Res.drawable.ic_keep_24dp)
    val PinnedMessages = StoatIcon(Res.drawable.ic_pinboard_24dp)
    val Play = StoatIcon(Res.drawable.ic_play_arrow_24dp__fill)
    val PresenceDnd = StoatIcon(Res.drawable.ic_do_not_disturb_on_24dp)
    val PresenceFocus = StoatIcon(Res.drawable.ic_circle_circle_24dp)
    val PresenceIdle = StoatIcon(Res.drawable.ic_bedtime_24dp)
    val PresenceOffline = StoatIcon(Res.drawable.ic_circle_24dp)
    val PresenceOnline = StoatIcon(Res.drawable.ic_circle_24dp__fill)
    val Profile = StoatIcon(Res.drawable.ic_id_card_24dp)
    val Pronouns = StoatIcon(Res.drawable.ic_person_celebrate_24dp)
    val RecordVoiceMessage = StoatIcon(Res.drawable.ic_mic_24dp)
    val RecoveryCode = StoatIcon(Res.drawable.ic_key_24dp)
    val RemoveAttachment = StoatIcon(Res.drawable.ic_attach_file_off_24dp)
    val RemoveFriend = StoatIcon(Res.drawable.ic_person_off_24dp)
    val RemoveFromFolder = StoatIcon(Res.drawable.ic_folder_off_24dp)
    val RemoveReply = StoatIcon(Res.drawable.ic_close_24dp)
    val Reorder = StoatIcon(Res.drawable.ic_drag_handle_24dp)
    val Reply = StoatIcon(Res.drawable.ic_reply_24dp)
    val Report = StoatIcon(Res.drawable.ic_report_24dp)
    val ResetToDefault = StoatIcon(Res.drawable.ic_undo_24dp)
    val RestrictedChannel = StoatIcon(Res.drawable.ic_grid_3x3_off_24dp)
    val RoleUnknown = StoatIcon(Res.drawable.ic_error_24dp)
    val Roles = StoatIcon(Res.drawable.ic_flag_24dp)
    val Save = StoatIcon(Res.drawable.ic_check_24dp)
    val SaveToDevice = StoatIcon(Res.drawable.ic_download_24dp)
    val ScanQrCode = StoatIcon(Res.drawable.ic_qr_code_scanner_24dp)
    val ScreenSharing = StoatIcon(Res.drawable.ic_screen_share_24dp)
    val ScreenSharingActivity = StoatIcon(Res.drawable.ic_screen_share_24dp)
    val ScrollToBottom = StoatIcon(Res.drawable.ic_south_24dp)
    val Search = StoatIcon(Res.drawable.ic_search_24dp)
    val SeekBack = StoatIcon(Res.drawable.ic_replay_5_24dp)
    val SeekForward = StoatIcon(Res.drawable.ic_forward_5_24dp)
    val Selected = StoatIcon(Res.drawable.ic_check_24dp)
    val Send = StoatIcon(Res.drawable.ic_send_24dp)
    val ServerIdentity = StoatIcon(Res.drawable.ic_id_card_24dp)
    val Sessions = StoatIcon(Res.drawable.ic_devices_24dp)
    val Settings = StoatIcon(Res.drawable.ic_settings_24dp)
    val Share = StoatIcon(Res.drawable.ic_ios_share_24dp)
    val ShareProfile = StoatIcon(Res.drawable.ic_ios_share_24dp)
    val ShowPassword = StoatIcon(Res.drawable.ic_visibility_24dp)
    val Slowmode = StoatIcon(Res.drawable.ic_timer_24dp)
    val StartScreenShare = StoatIcon(Res.drawable.ic_mobile_share_24px)
    val Success = StoatIcon(Res.drawable.ic_check_24dp)
    val SwitchCamera = StoatIcon(Res.drawable.ic_cameraswitch_24dp)
    val SystemMessage = StoatIcon(Res.drawable.ic_info_24dp)
    val SystemSettings = StoatIcon(Res.drawable.ic_arrow_forward_24dp)
    val TabCommunities = StoatIcon(Res.drawable.ic_tag_24dp)
    val TabConversations = StoatIcon(Res.drawable.ic_forum_24dp)
    val TabConversationsSelected = StoatIcon(Res.drawable.ic_forum_24dp__fill)
    val TabOverview = StoatIcon(Res.drawable.ic_star_shine_24dp)
    val TabOverviewSelected = StoatIcon(Res.drawable.ic_star_shine_24dp__fill)
    val TakePhoto = StoatIcon(Res.drawable.ic_camera_24dp)
    val TeamMemberBadge = StoatIcon(Res.drawable.ic_hardware_24dp)
    val TextChannel = StoatIcon(Res.drawable.ic_grid_3x3_24dp)
    val Timeout = StoatIcon(Res.drawable.ic_timer_24dp)
    val Unpinned = StoatIcon(Res.drawable.ic_keep_off_24dp)
    val UserAdded = StoatIcon(Res.drawable.ic_group_add_24dp)
    val UserBanned = StoatIcon(Res.drawable.ic_gavel_24dp)
    val UserCard = StoatIcon(Res.drawable.ic_badge_24dp)
    val UserJoined = StoatIcon(Res.drawable.ic_waving_hand_24dp)
    val UserKicked = StoatIcon(Res.drawable.ic_sports_and_outdoors_24dp)
    val UserLeft = StoatIcon(Res.drawable.ic_door_open_24dp)
    val UserRemoved = StoatIcon(Res.drawable.ic_group_remove_24dp)
    val Verified = StoatIcon(Res.drawable.ic_verified_24dp__fill)
    val VoiceActivity = StoatIcon(Res.drawable.ic_volume_up_24dp)
    val VoiceChannel = StoatIcon(Res.drawable.ic_volume_up_24dp)
    val VoiceConnected = StoatIcon(Res.drawable.ic_wifi_tethering_24dp)
    val VoiceConnecting = StoatIcon(Res.drawable.ic_sprint_24dp)
    val VoiceDisconnected = StoatIcon(Res.drawable.ic_wifi_tethering_error_24dp)
    val Webhook = StoatIcon(Res.drawable.ic_webhook_24dp)
    val WebhookBadge = StoatIcon(Res.drawable.ic_cloud_24dp)
}
