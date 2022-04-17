import 'dart:math';
import 'package:flutter/material.dart';
import 'package:getwidget/getwidget.dart';
import 'package:url_launcher/url_launcher.dart';

Widget resultCard(ResultTile art, context) {
  return GFCard(
    boxFit: BoxFit.cover,
    titlePosition: GFPosition.start,
    showOverlayImage: false,
    title: GFListTile(
      // avatar: GFAvatar(),
      titleText: art.url,
    ),
    content: Column(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        const SizedBox(
          height: 15,
        ),
        Text(
          art.para,
          maxLines: 4,
        ),
      ],
    ),
    buttonBar: GFButtonBar(
      children: <Widget>[
        GFButton(
          onPressed: () {
            //Go to the URLs
            //TODO: Change this to the Url
            _launchURL("https://www.google.com");
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
  ResultTile(this.url, this.para);
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
    print(id);
    List<String> _urls = data!['_urls'];
    List<String> _paragraphs = data!['_paragraphs'];

    for (int i = 0; i < _urls.length; i++) {
      allTiles.add(ResultTile(_urls[i], _paragraphs[i]));
    }
    int allCount = allTiles.length;

    return Scaffold(
      appBar: AppBar(
        title: Text("Results for '" + wordSearched + '\''),
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
