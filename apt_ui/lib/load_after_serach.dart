import 'api.dart';
import 'package:flutter/material.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';

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
      dynamic result = await DBManager.executeQuery(searchQ);

      for (var e in result) {
        resultsUrls.add(e["url"]);
        resultsParagraphs.add(e["paragraph"]);
      }
      ///////////////////////////////////////////////
      Navigator.popAndPushNamed(context, '/results_page', arguments: {
        '_wordSearched': searchQ,
        '_urls': resultsUrls,
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
