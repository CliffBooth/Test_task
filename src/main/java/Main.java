import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Укажите тестовый файл");
            return;
        }
        String fileName = args[0];
        long start = System.currentTimeMillis();
        Set<String> uniqueLines;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName));) {
            uniqueLines = br.lines()
                    .filter(Main::checkLineFormat)
                    .collect(Collectors.toSet());
        }

        List<Set<String>> groups = getGroups(uniqueLines);
        groups.sort(Comparator.comparingInt((Set<String> l) -> l.size()).reversed());
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(Paths.get("result.txt")), StandardCharsets.UTF_8)
        )) {
            writer.write("" + (int) groups.stream().filter(g -> g.size() > 1).count());
            writer.newLine();
            for (int i = 0; i < groups.size(); i++) {
                writer.write("Группа " + (i + 1));
                writer.newLine();
                for (String line : groups.get(i)) {
                    writer.write(line);
                    writer.newLine();
                }
                writer.flush();
            }
        }

        long end = System.currentTimeMillis();
        double time = (end - start) / 1000f;
        System.out.printf("program finished in %.2f seconds\n", time);
    }

    private static class NewWord {
        public String word;
        public int position;

        public NewWord(String word, int position) {
            this.word = word;
            this.position = position;
        }
    }

    private static List<Set<String>> getGroups(Collection<String> lines) {
        List<Set<String>> result = new ArrayList<>(); // resulting list of groups
        List<Map<String, Integer>> wordToGroupList = new ArrayList<>(); // word position to word
        Map<Integer, Integer> mergedGroupToResultingGroup = new HashMap<>(); // history of groups merging into another group

        for (String line : lines) {
            String[] words = line.split(";");
            TreeSet<Integer> possibleGroups = new TreeSet<>(); // contains possible group numbers for this line
            List<NewWord> newWordList = new ArrayList<>(); // list of words, that were never at position i before

            for (int i = 0; i < words.length; i++) {
                String word = words[i];

                if (wordToGroupList.size() == i) {
                    wordToGroupList.add(new HashMap<>());
                }

                if (word.equals("\"\""))
                    continue;

                if (wordToGroupList.get(i).containsKey(word)) {
                    int group = wordToGroupList.get(i).get(word);
                    while (mergedGroupToResultingGroup.containsKey(group)) {
                        group = mergedGroupToResultingGroup.get(group); // getting the last group
                    }
                    possibleGroups.add(group);
                } else {
                    newWordList.add(new NewWord(word, i));
                }
            }

            int groupNumber = result.size();
            if (possibleGroups.isEmpty()) {
                // if none of words can be in any group, we create a new group
                result.add(new HashSet<>());
            } else {
                // otherwise we take first of possible groups
                groupNumber = possibleGroups.first();
            }
            for (NewWord newWord : newWordList) {
                wordToGroupList.get(newWord.position).put(newWord.word, groupNumber);
            }
            for (int groupToMerge : possibleGroups) {
                if (groupToMerge != groupNumber) {
                    // merge lines from all other possible groups into the resulting one
                    mergedGroupToResultingGroup.put(groupToMerge, groupNumber);
                    result.get(groupNumber).addAll(result.get(groupToMerge));
                    result.set(groupToMerge, null);
                }
            }
            result.get(groupNumber).add(line);
        }
        while (result.remove(null)); // remove all nulls from the resulting list
        return result;
    }

    private static boolean checkLineFormat(String line) {
        return line.matches("(\"\\d*\";)*(\"\"|\"\\d*\")");
    }
}