package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LemmatizatorTest {
    public static Map<String, Integer> collectLemmas(String text) {
        HashMap<String, Integer> result = new HashMap<>();
        String[] words = arrayContainsRussianWords(text);

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }


        }

        return result;
    }

    public static void main(String[] args) {

        System.out.println(String.format("%02d", 199));
        /*try {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();

            String[] words = arrayContainsRussianWords("кирпич карамультук ёжик ть фыавыаф а б в г д е Ё" +
                    " ж з и й к л м н о п р с т у ф х ц ч ш щ ъ ы ь э ю я");
            //List<String> wordBaseForms = luceneMorph.getMorphInfo("кирпич");
            //System.out.println(isCorrectWordForm("", luceneMorph));
            for (String word : words) {
                System.out.println(word + " : " + luceneMorph.getMorphInfo(word).get(0) +
                        " : " + isCorrectWordForm(word, luceneMorph));
            }

        } catch (IOException e) {
            System.out.println("exception");
        } */

    }

    private static boolean isCorrectWordForm(String word, LuceneMorphology l) {
        String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
        List<String> wordInfo = l.getMorphInfo(word);
        for (String morphInfo : wordInfo) {
            if (morphInfo.matches(WORD_TYPE_REGEX)) {
                return false;
            }
        }
        return true;
    }


    private static String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("ё", "е")
                .replaceAll("([^а-яё\\s])", " ")
                .trim()
                .split("\\s+");
    }
}

