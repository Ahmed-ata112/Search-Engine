import 'package:flutter/material.dart';

class NoResultsFound extends StatelessWidget {
  NoResultsFound({Key? key}) : super(key: key);
  Map? data = {};

  @override
  Widget build(BuildContext context) {
    data = data!.isNotEmpty
        ? data
        : ModalRoute.of(context)!.settings.arguments as Map?;
    String wordSearched = data!['_wordSearched'];

    return Scaffold(
      appBar: AppBar(
        title: Text("Results for '" + wordSearched + '\''),
        leading: BackButton(
          onPressed: () {
            Navigator.popUntil(context, ModalRoute.withName("/search_home"));
          },
        ),
      ),
      body: const Center(
        child: Text(" No Results Found ",
            style: TextStyle(
                color: Colors.blueAccent,
                fontWeight: FontWeight.bold,
                fontSize: 20)),
      ),
    );
  }
}
