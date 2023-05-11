package test

import org.junit.Test
import vfunny.shortvideovfunnyapp.Post.model.Language.Companion.getAllLanguages

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