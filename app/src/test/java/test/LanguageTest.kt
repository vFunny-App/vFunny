package test

import org.junit.Test
import vfunny.shortvideovfunnyapp.Login.PostsUtils.PostsManager
import vfunny.shortvideovfunnyapp.Login.data.Language
import vfunny.shortvideovfunnyapp.Login.data.Language.Companion.getAllLanguages

internal class LanguageTest {

    companion object {
        private const val TAG = "LanguageTest"
    }

    @Test
    fun getAllLanguagesTest() {
        // Code to test your method
        println("$TAG, testMyMethod")
        for (allLanguage in getAllLanguages()) {
            println("$TAG, allLanguage $allLanguage")
            println("$TAG, name ${allLanguage.name}")
        }
    }

}