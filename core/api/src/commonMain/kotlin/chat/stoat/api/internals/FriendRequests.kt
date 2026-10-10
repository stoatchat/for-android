package chat.stoat.api.internals

import chat.stoat.api.StoatAPI
import chat.stoat.core.model.schemas.User

object FriendRequests {
    fun getIncoming(): List<User> {
        return StoatAPI.userCache.values.filter { user ->
            user.relationship == "Incoming"
        }
    }

    fun getOutgoing(): List<User> {
        return StoatAPI.userCache.values.filter { user ->
            user.relationship == "Outgoing"
        }
    }

    fun getBlocked(): List<User> {
        return StoatAPI.userCache.values.filter { user ->
            user.relationship == "Blocked"
        }
    }

    fun getOnlineFriends(): List<User> {
        return StoatAPI.userCache.values.filter { user ->
            user.relationship == "Friend" && user.online == true
        }
    }

    fun getFriends(excludeOnline: Boolean = false): List<User> {
        return StoatAPI.userCache.values.filter { user ->
            user.relationship == "Friend" && if (excludeOnline) user.online == false else true
        }
    }
}