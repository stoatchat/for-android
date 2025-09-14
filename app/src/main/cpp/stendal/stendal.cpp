#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include <cmark.h>

#define STENDAL_ASTNODE_CONSTRUCTOR_SIGNATURE "(ILjava/lang/String;Ljava/util/List;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Boolean;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;)V"

namespace Stendal {
    jclass arrayListClass = nullptr;
    jmethodID constructArrayListMethod = nullptr;
    jmethodID addArrayListMethod = nullptr;
    jclass astNodeClass = nullptr;
    jclass integerWrapperClass = nullptr;
    jmethodID astNodeConstructor = nullptr;
    jmethodID integerWrapperConstructor = nullptr;
    jclass booleanWrapperClass = nullptr;
    jmethodID booleanWrapperConstructor = nullptr;

    inline bool string_starts_with(std::string const &value, std::string const &prefix) {
        return value.rfind(prefix, 0) == 0;
    }

    inline bool string_ends_with(std::string const &value, std::string const &suffix) {
        if (suffix.size() > value.size()) return false;
        return std::equal(suffix.rbegin(), suffix.rend(), value.rbegin());
    }

    void init(JNIEnv *env) {
        jclass localArrayListClass = env->FindClass("java/util/ArrayList");
        arrayListClass = (jclass) env->NewGlobalRef(localArrayListClass);
        constructArrayListMethod = env->GetMethodID(localArrayListClass, "<init>", "(I)V");
        addArrayListMethod = env->GetMethodID(localArrayListClass, "add", "(Ljava/lang/Object;)Z");

        jclass localAstNodeClass = env->FindClass("chat/zekochat/ndk/AstNode");
        astNodeClass = (jclass) env->NewGlobalRef(localAstNodeClass);
        astNodeConstructor = env->GetMethodID(localAstNodeClass, "<init>",
                                              STENDAL_ASTNODE_CONSTRUCTOR_SIGNATURE);

        jclass localIntegerWrapperClass = env->FindClass("java/lang/Integer");
        integerWrapperClass = (jclass) env->NewGlobalRef(localIntegerWrapperClass);
        integerWrapperConstructor = env->GetMethodID(localIntegerWrapperClass, "<init>", "(I)V");

        jclass localBooleanWrapperClass = env->FindClass("java/lang/Boolean");
        booleanWrapperClass = (jclass) env->NewGlobalRef(localBooleanWrapperClass);
        booleanWrapperConstructor = env->GetMethodID(localBooleanWrapperClass, "<init>", "(Z)V");
    }

    jobject node_instance(JNIEnv *env, cmark_node *node, jobject children) {
        jstring typeStr = env->NewStringUTF(cmark_node_get_type_string(node));
        jstring literalStr = env->NewStringUTF(cmark_node_get_literal(node));
        jstring actionStr = env->NewStringUTF(cmark_node_get_url(node));
        jobject headingLevelIntg = env->NewObject(Stendal::integerWrapperClass,
                                                  Stendal::integerWrapperConstructor,
                                                  (int) cmark_node_get_heading_level(
                                                          node));
        jobject listTypeIntg = env->NewObject(Stendal::integerWrapperClass,
                                              Stendal::integerWrapperConstructor,
                                              (int) cmark_node_get_list_type(node));
        jobject delimiterTypeIntg = env->NewObject(Stendal::integerWrapperClass,
                                                   Stendal::integerWrapperConstructor,
                                                   (int) cmark_node_get_list_delim(
                                                           node));
        jobject startIntg = env->NewObject(Stendal::integerWrapperClass,
                                           Stendal::integerWrapperConstructor,
                                           (int) cmark_node_get_list_start(node));
        jobject tightBoln = env->NewObject(Stendal::booleanWrapperClass,
                                           Stendal::booleanWrapperConstructor,
                                           (bool) cmark_node_get_list_tight(node));
        jstring fenceStr = env->NewStringUTF(cmark_node_get_fence_info(node));
        jstring titleStr = env->NewStringUTF(cmark_node_get_title(node));
        jstring onEnterStr = env->NewStringUTF(cmark_node_get_on_enter(node));
        jstring onExitStr = env->NewStringUTF(cmark_node_get_on_exit(node));
        jobject startLineIntg = env->NewObject(Stendal::integerWrapperClass,
                                               Stendal::integerWrapperConstructor,
                                               (int) cmark_node_get_start_line(node));
        jobject endLineIntg = env->NewObject(Stendal::integerWrapperClass,
                                             Stendal::integerWrapperConstructor,
                                             (int) cmark_node_get_end_line(node));
        jobject startColumnIntg = env->NewObject(Stendal::integerWrapperClass,
                                                 Stendal::integerWrapperConstructor,
                                                 (int) cmark_node_get_start_column(
                                                         node));
        jobject endColumnIntg = env->NewObject(Stendal::integerWrapperClass,
                                               Stendal::integerWrapperConstructor,
                                               (int) cmark_node_get_end_column(node));

        jobject inst = env->NewObject(Stendal::astNodeClass, Stendal::astNodeConstructor,
                                      (int) cmark_node_get_type(node),
                                      typeStr, children,
                                      literalStr, actionStr, headingLevelIntg,
                                      listTypeIntg, delimiterTypeIntg, startIntg, tightBoln,
                                      fenceStr, titleStr, onEnterStr, onExitStr, startLineIntg,
                                      endLineIntg,
                                      startColumnIntg, endColumnIntg);

        env->DeleteLocalRef(typeStr);
        env->DeleteLocalRef(literalStr);
        env->DeleteLocalRef(actionStr);
        env->DeleteLocalRef(headingLevelIntg);
        env->DeleteLocalRef(listTypeIntg);
        env->DeleteLocalRef(delimiterTypeIntg);
        env->DeleteLocalRef(startIntg);
        env->DeleteLocalRef(tightBoln);
        env->DeleteLocalRef(fenceStr);
        env->DeleteLocalRef(titleStr);
        env->DeleteLocalRef(onEnterStr);
        env->DeleteLocalRef(onExitStr);
        env->DeleteLocalRef(startLineIntg);
        env->DeleteLocalRef(endLineIntg);
        env->DeleteLocalRef(startColumnIntg);
        env->DeleteLocalRef(endColumnIntg);

        return inst;
    }

    jobject collect_nodes(JNIEnv *env, cmark_node *doc) {
        std::vector<std::pair<cmark_node *, jobject>> children;

        {
            cmark_node *child = cmark_node_first_child(doc);
            while (child) {
                children.push_back(std::pair(child, Stendal::collect_nodes(env, child)));
                child = cmark_node_next(child);
            }
        }

        jobject list = env->NewObject(Stendal::arrayListClass, Stendal::constructArrayListMethod,
                                      (int) children.size());

        for (auto child: children) {
            jobject inst = Stendal::node_instance(env, child.first, child.second);
            env->DeleteLocalRef(child.second);
            env->CallBooleanMethod(list, Stendal::addArrayListMethod, inst);
            env->DeleteLocalRef(inst);
        }

        return list;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_chat_zekochat_ndk_Stendal_init(JNIEnv *env, [[maybe_unused]] jobject thiz) {
    Stendal::init(env);
}

extern "C" JNIEXPORT jobject JNICALL
Java_chat_zekochat_ndk_Stendal_render(JNIEnv *env, [[maybe_unused]] jobject thiz, jstring input) {
    const char *inputStr = env->GetStringUTFChars(input, nullptr);
    cmark_node *doc = cmark_parse_document(inputStr, strlen(inputStr),
                                           CMARK_OPT_DEFAULT | CMARK_OPT_HARDBREAKS |
                                           CMARK_OPT_VALIDATE_UTF8);

    jobject nodes = Stendal::collect_nodes(env, doc);
    jobject inst = Stendal::node_instance(env, doc, nodes);

    cmark_node_free(doc);
    env->ReleaseStringUTFChars(input, inputStr);

    return inst;
}