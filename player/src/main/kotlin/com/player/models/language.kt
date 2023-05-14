package vfunny.shortvideovfunnyapp.Post.model

enum class Language(val code: String) {
    WORLDWIDE("ww"), ENGLISH("en"), HINDI("hi"), OTHERS("ot");

    companion object {
        private const val TAG = "Language"
        fun getAllLanguages(): List<Language> {
            return enumValues<Language>().toList()
        }

        fun getAllLanguagesString(): Array<String> {
            return enumValues<Language>().map { it.code }.toTypedArray()
        }

    }
}
