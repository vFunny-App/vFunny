package vfunny.shortvideovfunnyapp.Post.model

enum class Language(val code: String) {
    WORLDWIDE("ww"), ENGLISH("en"), HINDI("hi"), OTHERS("ot");

    companion object {
        private const val TAG = "Language"
        fun getAllLanguages(): List<Language> {
            return enumValues<Language>().toList()
        }
        fun getAllLanguagesMappedFalse(): MutableMap<Language, Boolean> {
            val languageMap = mutableMapOf<Language, Boolean>()
            for (language in enumValues<Language>()) {
                languageMap[language] = false
            }
            return languageMap
        }
        fun getAllLanguagesMapped(selectedLanguages: List<Language>): MutableMap<Language, Boolean> {
            val allLanguage = enumValues<Language>().toList()
            val filteredSelectedLanguages = selectedLanguages.filter { it != WORLDWIDE }

            val languageMap = mutableMapOf<Language, Boolean>()
            for (language in allLanguage) {
                languageMap[language] = filteredSelectedLanguages.any { it.name == language.name }
            }
            return languageMap
        }

        fun getAllLanguagesString(): Array<String> {
            return enumValues<Language>().map { it.code }.toTypedArray()
        }

        fun getLanguageFromCode(code: String): Language? {
            return enumValues<Language>().find { it.code == code }
        }

        fun getLanguageFromName(name: String?): Language? {
            return enumValues<Language>().find { it.name == name }
        }
    }

}
