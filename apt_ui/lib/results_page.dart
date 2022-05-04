import 'dart:math';
import 'package:flutter/material.dart';
import 'package:getwidget/getwidget.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:highlight_text/highlight_text.dart';
import 'package:number_paginator/number_paginator.dart';

Widget resultCard(ResultTile art, context) {
  Map<String, HighlightedWord> words = <String, HighlightedWord>{};
  for (String word in art.tokens) {
    words[word] = HighlightedWord(
      onTap: () {
        print("highlight");
      },
      textStyle: const TextStyle(
        fontWeight: FontWeight.bold,
        fontSize: 10,
      ),
    );
  }
  return GFCard(
    boxFit: BoxFit.cover,
    titlePosition: GFPosition.start,
    showOverlayImage: false,
    title: GFListTile(
      // avatar: GFAvatar(),
      titleText: art.header,
      subTitleText: art.url,
    ),
    content: Column(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        const SizedBox(
          height: 15,
        ),
        TextHighlight(
          text: art.para, // You need to pass the string you want the highlights
          words: words, // Your dictionary words
          textStyle: const TextStyle(
            // You can set the general style, like a Text()
            fontSize: 10,
            color: Colors.black,
          ),
          textAlign: TextAlign
              .justify, // You can use any attribute of the RichText widget
        ),
      ],
    ),
    buttonBar: GFButtonBar(
      children: <Widget>[
        GFButton(
          onPressed: () {
            //Go to the URLs
            //TODO: Change this to the Url
            _launchURL(art.url);
          },
          text: "Read",
          blockButton: true,
        ),
      ],
    ),
  );
}

_launchURL(String url) async {
  if (await canLaunch(url)) {
    await launch(url);
  } else {
    throw 'Could not launch $url';
  }
}

class ResultTile {
  final String url;
  final String para;
  final String header;
  final List<dynamic> tokens;
  ResultTile(this.url, this.para, this.header, this.tokens);
}

class ResultsPage extends StatelessWidget {
  ResultsPage({Key? key}) : super(key: key);
  Map? data = {};
  static const int numResults = 10;
  List<ResultTile> allTiles = [];

  @override
  Widget build(BuildContext context) {
    data = data!.isNotEmpty
        ? data
        : ModalRoute.of(context)!.settings.arguments as Map?;
    //print(data);
    String wordSearched = data!['_wordSearched'];
    int id = data!['_id'];
    List<String> _urls = data!['_urls'];
    List<String> _paragraphs = data!['_paragraphs'];
    List<String> _headers = data!['_headers'];
    List<List<dynamic>> _tokens = data!['_tokens'];

    for (int i = 0; i < _urls.length; i++) {
      allTiles
          .add(ResultTile(_urls[i], _paragraphs[i], _headers[i], _tokens[i]));
    }
    int allCount = allTiles.length;

    return Scaffold(
      appBar: AppBar(
        title: Text("${_urls.length} Results for '" + wordSearched + '\''),
        leading: BackButton(
          onPressed: () {
            Navigator.popUntil(context, ModalRoute.withName("/search_home"));
          },
        ),
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              itemCount: min(allTiles.length - numResults * id,
                  numResults), // should be dynamic -> retrieve articles
              itemBuilder: (context, index) {
                // will show The 10 results only
                return resultCard(allTiles[id * numResults + index], context);
              },
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              Expanded(
                child: ElevatedButton(
                  onPressed: (id == 0
                      ? null
                      : () {
                          Navigator.pop(context);
                        }),
                  child: const Text('previous'),
                ),
              ),
              Expanded(
                child: ElevatedButton(
                  onPressed: (id + 1) * numResults >= allCount
                      ? null
                      : () {
                          Navigator.pushNamed(context, '/results_page',
                              arguments: {
                                '_wordSearched': wordSearched,
                                '_urls': _urls,
                                '_headers': _headers,
                                'tokens': _tokens,
                                '_paragraphs': _paragraphs,
                                '_id': id + 1
                              });
                        },
                  child: const Text('next'),
                ),
              ),
            ]),
          )
        ],
      ),
    );
  }
}
