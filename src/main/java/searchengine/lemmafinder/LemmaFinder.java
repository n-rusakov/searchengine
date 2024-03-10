package searchengine.lemmafinder;

import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.misc.Array2DHashSet;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class LemmaFinder {

    private final LuceneMorphology luceneMorphology;

    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};

    public Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }

        return lemmas;
    }

    public String getNormalForm(String word) {
        if (word.isBlank()) {
            return "";
        }

        List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
        if (anyWordBaseBelongToParticle(wordBaseForms)) {
            return "";
        }

        List<String> normalForms = luceneMorphology.getNormalForms(word);
        if (normalForms.isEmpty()) {
            return "";
        }

        return normalForms.get(0);
    }


    public Set<String> getLemmaSet(String text) {
        String[] textArray = arrayContainsRussianWords(text);
        Set<String> lemmaSet = new HashSet<>();
        for (String word : textArray) {
            if (!word.isEmpty() && isCorrectWordForm(word)) {
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }

                List<String> normalForms = luceneMorphology.getNormalForms(word);
                if (normalForms.isEmpty()) {
                    continue;
                }

                lemmaSet.add(normalForms.get(0));
            }
        }
        return lemmaSet;
    }


    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    public String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яё\\s])", " ")
                .trim()
                .split("\\s+");
    }

    public String[] arrayContainsRussianWordsOriginal(String text) {
        return text.replaceAll("([^а-яёА-ЯЁ\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean isCorrectWordForm(String word) {
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
        for (String morphInfo : wordInfo) {
            if (morphInfo.matches(WORD_TYPE_REGEX)) {
                return false;
            }
        }
        return true;
    }
}
