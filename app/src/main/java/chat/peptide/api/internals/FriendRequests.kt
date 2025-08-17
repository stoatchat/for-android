package chat.peptide.api.internals

import chat.peptide.api.PeptideAPI
import chat.peptide.api.schemas.User

object FriendRequests {
    fun getIncoming(): List<User> {
        return PeptideAPI.userCache.values.filter { user ->
            user.relationship == "Incoming"
        }
    }

    fun getOutgoing(): List<User> {
        return PeptideAPI.userCache.values.filter { user ->
            user.relationship == "Outgoing"
        }
    }

    fun getBlocked(): List<User> {
        return PeptideAPI.userCache.values.filter { user ->
            user.relationship == "Blocked"
        }
    }

    fun getOnlineFriends(): List<User> {
        return PeptideAPI.userCache.values.filter { user ->
            user.relationship == "Friend" && user.online == true
        }
    }

    fun getFriends(excludeOnline: Boolean = false): List<User> {
        return PeptideAPI.userCache.values.filter { user ->
            user.relationship == "Friend" && if (excludeOnline) user.online == false else true
        }
    }
}