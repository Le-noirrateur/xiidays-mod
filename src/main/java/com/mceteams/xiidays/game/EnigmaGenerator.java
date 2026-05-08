package com.mceteams.xiidays.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Générateur d'énigmes variées pour le Core Maze
 * Types: logique, maths, patterns, codes
 */
public class EnigmaGenerator {

    private static final Random random = new Random();

    public enum EnigmaType {
        MATH,       // Calculs mathématiques
        SEQUENCE,   // Suites logiques
        LOGIC,      // Questions de logique
        CODE,       // Décodage
        WORD        // Jeux de mots/anagrammes
    }

    public record Enigma(
            EnigmaType type,
            String question,
            String answer,
            String hint
    ) {}

    /**
     * Génère une liste d'énigmes aléatoires uniques
     * @param count Nombre d'énigmes à générer
     * @return Liste d'énigmes
     */
    public static List<Enigma> generateEnigmas(int count) {
        List<Enigma> allEnigmas = new ArrayList<>();
        allEnigmas.addAll(generateMathEnigmas());
        allEnigmas.addAll(generateSequenceEnigmas());
        allEnigmas.addAll(generateLogicEnigmas());
        allEnigmas.addAll(generateCodeEnigmas());
        allEnigmas.addAll(generateWordEnigmas());

        // Mélanger et prendre les N premières
        Collections.shuffle(allEnigmas);
        return allEnigmas.subList(0, Math.min(count, allEnigmas.size()));
    }

    /**
     * Génère une seule énigme aléatoire
     */
    public static Enigma generateRandomEnigma() {
        return generateEnigmas(1).get(0);
    }

    // ===== ENIGMES MATHEMATIQUES =====
    private static List<Enigma> generateMathEnigmas() {
        List<Enigma> enigmas = new ArrayList<>();

        // Additions/Soustractions
        int a = random.nextInt(50) + 10;
        int b = random.nextInt(50) + 10;
        enigmas.add(new Enigma(EnigmaType.MATH,
                String.format("Combien font %d + %d ?", a, b),
                String.valueOf(a + b),
                "Addition simple"));

        int c = random.nextInt(100) + 50;
        int d = random.nextInt(50) + 10;
        enigmas.add(new Enigma(EnigmaType.MATH,
                String.format("Combien font %d - %d ?", c, d),
                String.valueOf(c - d),
                "Soustraction simple"));

        // Multiplications
        int e = random.nextInt(12) + 2;
        int f = random.nextInt(12) + 2;
        enigmas.add(new Enigma(EnigmaType.MATH,
                String.format("Combien font %d x %d ?", e, f),
                String.valueOf(e * f),
                "Table de multiplication"));

        // Divisions
        int divisor = random.nextInt(10) + 2;
        int quotient = random.nextInt(10) + 5;
        int dividend = divisor * quotient;
        enigmas.add(new Enigma(EnigmaType.MATH,
                String.format("Combien font %d / %d ?", dividend, divisor),
                String.valueOf(quotient),
                "Division simple"));

        // Puissances
        enigmas.add(new Enigma(EnigmaType.MATH,
                "Combien font 2^8 ?",
                "256",
                "Puissance de 2"));

        enigmas.add(new Enigma(EnigmaType.MATH,
                "Combien font 3^4 ?",
                "81",
                "3 x 3 x 3 x 3"));

        // Racines carrées
        enigmas.add(new Enigma(EnigmaType.MATH,
                "Quelle est la racine carrée de 144 ?",
                "12",
                "12 x 12 = ?"));

        enigmas.add(new Enigma(EnigmaType.MATH,
                "Quelle est la racine carrée de 256 ?",
                "16",
                "16 x 16 = ?"));

        return enigmas;
    }

    // ===== ENIGMES DE SEQUENCES =====
    private static List<Enigma> generateSequenceEnigmas() {
        List<Enigma> enigmas = new ArrayList<>();

        // Suite arithmétique
        enigmas.add(new Enigma(EnigmaType.SEQUENCE,
                "Quel est le prochain nombre ? 2, 4, 6, 8, ?",
                "10",
                "+2 à chaque fois"));

        enigmas.add(new Enigma(EnigmaType.SEQUENCE,
                "Quel est le prochain nombre ? 3, 6, 9, 12, ?",
                "15",
                "Table de 3"));

        enigmas.add(new Enigma(EnigmaType.SEQUENCE,
                "Quel est le prochain nombre ? 5, 10, 15, 20, ?",
                "25",
                "+5 à chaque fois"));

        // Suite géométrique
        enigmas.add(new Enigma(EnigmaType.SEQUENCE,
                "Quel est le prochain nombre ? 2, 4, 8, 16, ?",
                "32",
                "x2 à chaque fois"));

        enigmas.add(new Enigma(EnigmaType.SEQUENCE,
                "Quel est le prochain nombre ? 3, 9, 27, 81, ?",
                "243",
                "x3 à chaque fois"));

        // Fibonacci
        enigmas.add(new Enigma(EnigmaType.SEQUENCE,
                "Quel est le prochain nombre ? 1, 1, 2, 3, 5, 8, ?",
                "13",
                "Chaque nombre = somme des 2 précédents"));

        // Suites décroissantes
        enigmas.add(new Enigma(EnigmaType.SEQUENCE,
                "Quel est le prochain nombre ? 100, 90, 80, 70, ?",
                "60",
                "-10 à chaque fois"));

        // Suites alternées
        enigmas.add(new Enigma(EnigmaType.SEQUENCE,
                "Quel est le prochain nombre ? 1, 2, 4, 7, 11, ?",
                "16",
                "+1, +2, +3, +4, +5..."));

        // Carrés
        enigmas.add(new Enigma(EnigmaType.SEQUENCE,
                "Quel est le prochain nombre ? 1, 4, 9, 16, 25, ?",
                "36",
                "Carrés parfaits"));

        return enigmas;
    }

    // ===== ENIGMES DE LOGIQUE =====
    private static List<Enigma> generateLogicEnigmas() {
        List<Enigma> enigmas = new ArrayList<>();

        enigmas.add(new Enigma(EnigmaType.LOGIC,
                "Un fermier a 17 moutons. Tous sauf 9 meurent. Combien en reste-t-il ?",
                "9",
                "Lisez bien la question"));

        enigmas.add(new Enigma(EnigmaType.LOGIC,
                "Je suis le double de la moitié de 10. Qui suis-je ?",
                "10",
                "Double de moitié = nombre original"));

        enigmas.add(new Enigma(EnigmaType.LOGIC,
                "Si 5 machines font 5 objets en 5 minutes, combien de temps mettent 100 machines pour faire 100 objets ?",
                "5",
                "Chaque machine = 1 objet en 5 min"));

        enigmas.add(new Enigma(EnigmaType.LOGIC,
                "Un père et son fils ont ensemble 36 ans. Le père a 30 ans de plus que le fils. Quel âge a le fils ?",
                "3",
                "x + (x+30) = 36"));

        enigmas.add(new Enigma(EnigmaType.LOGIC,
                "Combien de mois ont 28 jours ?",
                "12",
                "Tous les mois ont AU MOINS 28 jours"));

        enigmas.add(new Enigma(EnigmaType.LOGIC,
                "Si tu me dépasses, tu es à quelle place ?",
                "2",
                "Tu prends sa place"));

        enigmas.add(new Enigma(EnigmaType.LOGIC,
                "Un escargot grimpe 3m par jour et glisse de 2m chaque nuit. En combien de jours atteint-il 10m ?",
                "8",
                "Jour 8: 7m + 3m = 10m"));

        enigmas.add(new Enigma(EnigmaType.LOGIC,
                "Combien de fois peut-on soustraire 5 de 25 ?",
                "1",
                "Après la 1ère fois, ce n'est plus 25"));

        return enigmas;
    }

    // ===== ENIGMES DE CODE =====
    private static List<Enigma> generateCodeEnigmas() {
        List<Enigma> enigmas = new ArrayList<>();

        // César décalage +1
        enigmas.add(new Enigma(EnigmaType.CODE,
                "Décoder (A=B, B=C...): NJOF",
                "MINE",
                "Décaler chaque lettre de -1"));

        enigmas.add(new Enigma(EnigmaType.CODE,
                "Décoder (A=B, B=C...): EJBNPOE",
                "DIAMOND",
                "Décaler chaque lettre de -1"));

        // Lettres = position dans l'alphabet
        enigmas.add(new Enigma(EnigmaType.CODE,
                "Si A=1, B=2... Que signifie 7-15-12-4 ?",
                "GOLD",
                "G=7, O=15, L=12, D=4"));

        enigmas.add(new Enigma(EnigmaType.CODE,
                "Si A=1, B=2... Que signifie 9-18-15-14 ?",
                "IRON",
                "I=9, R=18, O=15, N=14"));

        // Inversé
        enigmas.add(new Enigma(EnigmaType.CODE,
                "Décoder ce mot inversé: ENOTS",
                "STONE",
                "Lire à l'envers"));

        enigmas.add(new Enigma(EnigmaType.CODE,
                "Décoder ce mot inversé: TFARC",
                "CRAFT",
                "Lire à l'envers"));

        // Minecraft-related
        enigmas.add(new Enigma(EnigmaType.CODE,
                "Décoder (A=Z, B=Y...): XIVVKVI",
                "CREEPER",
                "Alphabet inversé"));

        enigmas.add(new Enigma(EnigmaType.CODE,
                "Combien de lettres dans ENDERMAN ?",
                "8",
                "Comptez simplement"));

        return enigmas;
    }

    // ===== ENIGMES DE MOTS =====
    private static List<Enigma> generateWordEnigmas() {
        List<Enigma> enigmas = new ArrayList<>();

        // Anagrammes Minecraft
        enigmas.add(new Enigma(EnigmaType.WORD,
                "Anagramme de VESTE: métal précieux dans Minecraft",
                "STEVE",
                "Le personnage principal"));

        enigmas.add(new Enigma(EnigmaType.WORD,
                "Anagramme de BLOCS: objet pour frapper",
                "BLOCK",
                "Élément de base de Minecraft"));

        // Devinettes
        enigmas.add(new Enigma(EnigmaType.WORD,
                "Je brille la nuit mais je disparais le jour. Qui suis-je dans Minecraft ?",
                "LUNE",
                "Astre nocturne"));

        enigmas.add(new Enigma(EnigmaType.WORD,
                "Vert, hostile et explosif. Qui suis-je ?",
                "CREEPER",
                "Mob emblématique"));

        enigmas.add(new Enigma(EnigmaType.WORD,
                "Je suis noir, carré et téléporte. Qui suis-je ?",
                "ENDERMAN",
                "Mob de l'End"));

        enigmas.add(new Enigma(EnigmaType.WORD,
                "On me mine au niveau Y -59, je suis le plus solide. Qui suis-je ?",
                "BEDROCK",
                "Bloc indestructible"));

        enigmas.add(new Enigma(EnigmaType.WORD,
                "Je suis rouge, dure et viens du Nether. Qui suis-je ?",
                "NETHERITE",
                "Minerai ultime"));

        enigmas.add(new Enigma(EnigmaType.WORD,
                "Combien de slots dans un double coffre ?",
                "54",
                "27 x 2"));

        return enigmas;
    }

    /**
     * Vérifie si une réponse est correcte (insensible à la casse et aux espaces)
     */
    public static boolean checkAnswer(Enigma enigma, String userAnswer) {
        if (userAnswer == null || userAnswer.isEmpty()) return false;

        String expected = enigma.answer().trim().toLowerCase();
        String given = userAnswer.trim().toLowerCase();

        return expected.equals(given);
    }
}
