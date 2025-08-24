package chat.peptide.ndk

import kotlinx.serialization.Serializable

object AstNodeListType {
    const val NONE = 0
    const val BULLET = 1
    const val ORDERED = 2
}

object AstNodeDelimiterType {
    const val NONE = 0
    const val PERIOD = 1
    const val PARENTHESIS = 2
}

@Serializable
data class AstNode(
    val type: Int,
    val stringType: String,
    val children: List<AstNode>?,
    val text: String?,
    val url: String?,
    val level: Int?,
    val listType: Int?,
    val delimiterType: Int?,
    val listStart: Int?,
    val listTight: Boolean?,
    val fence: String?,
    val title: String?,
    val onEnter: String?,
    val onExit: String?,
    val startLine: Int?,
    val endLine: Int?,
    val startColumn: Int?,
    val endColumn: Int?,
)

object Stendal {
    external fun init()
    external fun render(input: String): AstNode
}