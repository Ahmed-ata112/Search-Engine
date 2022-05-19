import 'dart:math';
import 'package:flutter/material.dart';
import 'package:getwidget/getwidget.dart';
import 'package:substring_highlight/substring_highlight.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:highlight_text/highlight_text.dart';
import 'package:number_paginator/number_paginator.dart';

Widget resultCard(ResultTile art, context) {
  Map<String, HighlightedWord> words = <String, HighlightedWord>{};

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
        SubstringHighlight(
            caseSensitive: false,
            maxLines: 6,
            overflow: TextOverflow.ellipsis,
            terms: art.tokens,
            text: art.para,
            // text: 'ee e!',
            textAlign: TextAlign.right,
            textStyle: const TextStyle(
              // non-highlight style
              color: Colors.grey,
            ),
            words: true),
      ],
    ),
    buttonBar: GFButtonBar(
      children: <Widget>[
        GFButton(
          onPressed: () {
            //Go to the URLs
            //TODO: Change this to the Url
            _launchURL(art.url);
            //_launchURL("https://www.google.com/");
          },
          text: "Read",
          blockButton: true,
        ),
      ],
    ),
  );
}

_launchURL(String url) async {
  Uri uu = Uri.parse(url);
  if (await canLaunchUrl(uu)) {
    await launchUrl(uu);
  } else {
    throw 'Could not launch $url';
  }
}

class ResultTile {
  final String url;
  final String para;
  final String header;
  final List<String> tokens;
  ResultTile(this.url, this.para, this.header, this.tokens);
}

class ResultsPage extends StatefulWidget {
  const ResultsPage({Key? key}) : super(key: key);

  @override
  State<ResultsPage> createState() => _ResultsPageState();
}

class _ResultsPageState extends State<ResultsPage> {
  Map? data = {};
  static const int numResults = 10;
  int _currentPage = 0;

  @override
  Widget build(BuildContext context) {
    data = data!.isNotEmpty
        ? data
        : ModalRoute.of(context)!.settings.arguments as Map?;

    String wordSearched = data!['_wordSearched'];
    List<String> _urls = data!['_urls'];
    List<String> _paragraphs = data!['_paragraphs'];
    List<String> _headers = data!['_headers'];
    List<String> _tokens = data!['_tokens'];
    int _numPages = (_urls.length / 10.0).ceil();
    List<ResultTile> allTiles = [];

    for (int i = 0; i < _urls.length; i++) {
      allTiles.add(ResultTile(_urls[i], _paragraphs[i], _headers[i], _tokens));
    }
    var pages = List.generate(
      _numPages,
      (pageInd) => ListView.builder(
        itemCount: min(allTiles.length - numResults * pageInd,
            numResults), // should be dynamic -> retrieve articles
        itemBuilder: (context, index) {
          // will show The 10 results only

          return resultCard(allTiles[pageInd * numResults + index], context);
        },
      ),
    );
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
            child: Container(
              color: Theme.of(context).backgroundColor,
              child: pages[_currentPage],
            ),
          ),
          NumberPaginator(
            numberPages: _numPages,
            onPageChange: (int index) {
              setState(() {
                _currentPage = index;
              });
            },
          ),
        ],
      ),
    );
  }
}
