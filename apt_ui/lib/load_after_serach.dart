import 'api.dart';
import 'package:flutter/material.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:language_tool/language_tool.dart';

class LoadAfterLogin extends StatefulWidget {
  const LoadAfterLogin({Key? key}) : super(key: key);

  @override
  _LoadAfterLoginState createState() => _LoadAfterLoginState();
}

class _LoadAfterLoginState extends State<LoadAfterLogin> {
  Map data = {};
  Future<void> getData() async {
    await Future.delayed(const Duration(seconds: 1), () async {
      String searchQ = data["_wordSearched"];

      /// Call The search Query Here and Get Results as a List of Sites and Their paragraph
      List<String> resultsUrls = [];
      List<String> resultsParagraphs = [];
      List<String> headers = [];
      List<List<dynamic>> tokens = [];
      dynamic result = await DBManager.executeQuery(searchQ);
      for (var e in result) {
        resultsUrls.add(e["url"]);
        headers.add(e["header"]);
        tokens.add(e["tokens"]);
        resultsParagraphs.add(e["paragraph"]);
      }
      ///////////////////////////////////////////////

      if (resultsUrls.isEmpty) {
        var tool = LanguageTool(language: "en-US");
        List<WritingMistake> result = await tool.check(searchQ);
        List<String> replacements = [];
        for (WritingMistake e in result) {
          List<String?>? ret = e.replacements;
          if (ret == null) {
            break;
          }
          for (String? s in ret) {
            replacements.add(s!);
          }
        }

        Navigator.popAndPushNamed(context, '/no_results', arguments: {
          '_wordSearched': searchQ,
          '_replacements': replacements
        });
        return;
      }

      Navigator.popAndPushNamed(context, '/results_page', arguments: {
        '_wordSearched': searchQ,
        '_urls': resultsUrls,
        '_headers': headers,
        '_tokens': tokens,
        '_paragraphs': resultsParagraphs,
        '_id': 0
      });
    });
  }

  @override
  void initState() {
    super.initState();
    getData();
  }

  @override
  Widget build(BuildContext context) {
    // if data is empty, initialize it
    data = data.isNotEmpty
        ? data
        : ModalRoute.of(context)!.settings.arguments as Map;

    return Scaffold(
      backgroundColor: Colors.blue[900],
      body: const Center(
        child: SpinKitDoubleBounce(
          color: Colors.white,
          size: 80.0,
        ),
      ),
    );
  }
}
