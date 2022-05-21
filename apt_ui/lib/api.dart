import 'dart:convert';
import 'package:http/http.dart' as http;
import 'dart:async';

class DBManager {
  //This stores the url that we'll deal with the restful api through
  //for web-based applications this will be http://localhost:8080/
  //for mobile-based applications (emulators) this will be http://10.0.2.2:8080
  //http://localhost:8080
  static const String baseUrl = 'http://localhost:8081';

  static Future<dynamic> executeQuery(String searchQ) async {
    var response = await http.get(Uri.parse('$baseUrl/Query/$searchQ'));
    if (response.statusCode == 200) {
      List ret =
          json.decode(utf8.decode(response.bodyBytes)); // list<list<dynamic>>
      return ret; // List of JsonMaps

    } else {
      print(response.statusCode);
      return null;
    }
  }

  static Future<List> getSuggestionsList() async {
    var response = await http.get(Uri.parse('$baseUrl/suggests'));
    if (response.statusCode == 200) {
      List ret =
          json.decode(utf8.decode(response.bodyBytes)); // list<list<dynamic>>
      print(ret);
      return ret; // List of JsonMaps
    } else {
      print(response.statusCode);
      return [];
    }
  }
}
