package chat.peptide.api.internals

import chat.peptide.api.schemas.Member

class Members {
    // memberCache (mapping of serverId to userId to member)
    private val memberCache = mutableMapOf<String, MutableMap<String, Member>>()

    fun getMember(serverId: String, userId: String): Member? {
        return memberCache[serverId]?.get(userId)
    }

    fun hasMember(serverId: String, userId: String): Boolean {
        return memberCache[serverId]?.containsKey(userId) ?: false
    }

    fun setMember(serverId: String, member: Member) {
        if (!memberCache.containsKey(serverId)) {
            memberCache[serverId] = mutableMapOf()
        }

        memberCache[serverId]?.set(member.id!!.user, member)
    }

    fun removeMember(serverId: String, userId: String) {
        memberCache[serverId]?.remove(userId)
    }

    fun clear() {
        memberCache.clear()
    }

    /**
     * Returns a Map of userId to server-nickname for the given serverId.
     */
    fun markdownMemberMapFor(serverId: String): Map<String, String> {
        return memberCache[serverId]?.mapNotNull { (userId, member) ->
            member.nickname?.let { userId to member.nickname }
        }?.toMap() ?: emptyMap()
    }

    fun filterNamesFor(serverId: String, query: String): List<Member> {
        return memberCache[serverId]?.values?.filter { member ->
            member.nickname?.contains(query, ignoreCase = true) ?: false
        } ?: emptyList()
    }
}
