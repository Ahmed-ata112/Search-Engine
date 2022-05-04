import 'package:apt_ui/results_page.dart';
import 'package:apt_ui/search_home.dart';
import 'package:flutter/material.dart';
import 'api.dart';
import 'load_after_serach.dart';
import 'no_results_found.dart';

void try_api() async {
  List<String> resultsUrls = [];
  List<String> resultsParagraphs = [];

  List result = await DBManager.getSuggestionsList();
  print(result);
}

void main() {
  runApp(const MyApp());
  // try_api();
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      initialRoute: '/search_home',
      routes: {
        '/search_home': (context) => const SearchHome(),
        '/load_page': (context) => const LoadAfterLogin(),
        '/results_page': (context) => ResultsPage(),
        '/no_results': (context) => NoResultsFound(),
      },
    );
  }
}
