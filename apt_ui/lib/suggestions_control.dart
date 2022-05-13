import 'api.dart';

class BackendService {
  static final Set<String> allWords = {};

  static Future<void> initWords() async {
    List result = await DBManager.getSuggestionsList();

    for (String word in result) {
      allWords.add(word);
    }
  }

  static addToSuggestions(String word) {
    allWords.add(word);
  }

  static getSuggestions(String pattern) {
    Set<String> subToReturn = {};

    for (String ele in allWords) {
      if (ele.startsWith(pattern) && pattern.isNotEmpty) {
        subToReturn.add(ele);
      }
    }
    return subToReturn;
  }
}
