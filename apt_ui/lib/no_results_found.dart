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
    List<String> replacements = data!['_replacements'];
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
          const Text(" No Results Found ",
              style: TextStyle(
                  color: Colors.blueAccent,
                  fontWeight: FontWeight.bold,
                  fontSize: 20)),
          Expanded(
            child: ListView.separated(
                padding: const EdgeInsets.all(8),
                itemCount: replacements.length,
                separatorBuilder: (BuildContext context, int index) =>
                    const Divider(),
                itemBuilder: (BuildContext context, int index) {
                  return ElevatedButton(
                    child: Text(replacements[index]),
                    onPressed: () {
                      /// Close it if still Open
                      String t = replacements[index].trim();
                      //BackendService.addToSuggestions(t!);
                      Navigator.pushNamed(context, '/load_page', arguments: {
                        '_wordSearched': t,
                      });
                    },
                    style: ElevatedButton.styleFrom(
                        primary: const Color.fromARGB(255, 16, 211, 255),
                        fixedSize: const Size(200, 40),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(50))),
                  );
                }),
          ),
        ],
      ),
    );
  }
}
