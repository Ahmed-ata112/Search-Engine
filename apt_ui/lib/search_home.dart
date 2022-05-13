import 'package:apt_ui/suggestions_control.dart';
import 'package:flutter/material.dart';
import 'package:flutter_typeahead/flutter_typeahead.dart';
import 'package:speech_to_text/speech_to_text.dart';
import 'package:speech_to_text/speech_recognition_result.dart';

import 'api.dart';

class SearchHome extends StatefulWidget {
  const SearchHome({Key? key}) : super(key: key);

  @override
  State<SearchHome> createState() => _SearchHomeState();
}

class _SearchHomeState extends State<SearchHome> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _typeAheadController = TextEditingController();
  late String? _wordSearched;
  final SpeechToText _speechToText = SpeechToText();
  bool _speechEnabled = false;
  String _lastWords = '';

  @override
  void initState() {
    super.initState();
    BackendService.initWords();
    _initSpeech();
  }

  /// This has to happen only once per app
  void _initSpeech() async {
    _speechEnabled = await _speechToText.initialize();
    setState(() {});
  }

  /// Each time to start a speech recognition session
  void _startListening() async {
    await _speechToText.listen(onResult: _onSpeechResult);
    setState(() {});
  }

  /// Manually stop the active speech recognition session
  /// Note that there are also timeouts that each platform enforces
  /// and the SpeechToText plugin supports setting timeouts on the
  /// listen method.
  void _stopListening() async {
    await _speechToText.stop();
    setState(() {});
  }

  /// This is the callback that the SpeechToText plugin calls when
  /// the platform returns recognized words.
  void _onSpeechResult(SpeechRecognitionResult result) {
    setState(() {
      _lastWords = result.recognizedWords;
      _wordSearched = _lastWords;
      _typeAheadController.text = _lastWords;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white70,
      body: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Form(
            key: _formKey,
            child: Padding(
              padding: const EdgeInsets.all(32.0),
              child: Column(
                children: <Widget>[
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Image.asset(
                        "assets/search.png",
                        height: 100,
                        width: 100,
                      ),
                      const SizedBox(
                        width: 20,
                      ),
                      const Text(
                        "Googlio",
                        style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 30,
                            color: Color.fromARGB(255, 0, 193, 255)),
                      )
                    ],
                  ),
                  TypeAheadFormField(
                    textFieldConfiguration: TextFieldConfiguration(
                        onSubmitted: (input) {
                          if (_formKey.currentState!.validate()) {
                            /// Close it if still Open
                            _stopListening();
                            _formKey.currentState!.save();
                            String? t = _wordSearched?.trim();
                            BackendService.addToSuggestions(t!);
                            Navigator.pushNamed(context, '/load_page',
                                arguments: {
                                  '_wordSearched': t,
                                });
                          }
                        },
                        autofocus: true,
                        controller: _typeAheadController,
                        style: const TextStyle(color: Colors.black),
                        decoration: const InputDecoration(
                          labelText: 'search',
                        ),
                        cursorColor: Colors.cyan),
                    suggestionsCallback: (pattern) {
                      return BackendService.getSuggestions(pattern);
                    },
                    itemBuilder: (context, suggestion) {
                      return ListTile(
                        title: Text(suggestion as String),
                      );
                    },
                    noItemsFoundBuilder: (value) {
                      return const SizedBox(
                        height: 0.0,
                      );
                    },
                    transitionBuilder: (context, suggestionsBox, controller) {
                      return suggestionsBox;
                    },
                    onSuggestionSelected: (suggestion) {
                      _typeAheadController.text = suggestion as String;
                    },
                    validator: (value) {
                      if (value!.isEmpty) {
                        return 'Please,Enter something to search for';
                      }
                      return null;
                    },
                    onSaved: (value) => _wordSearched = value!,
                  ),
                  const SizedBox(
                    height: 10.0,
                  ),
                  ElevatedButton(
                    child: const Text('Search'),
                    onPressed: () {
                      if (_formKey.currentState!.validate()) {
                        /// Close it if still Open
                        _stopListening();
                        _formKey.currentState!.save();
                        String? t = _wordSearched?.trim();

                        BackendService.addToSuggestions(t!);
                        Navigator.pushNamed(context, '/load_page', arguments: {
                          '_wordSearched': t,
                        });
                      }
                    },
                    style: ElevatedButton.styleFrom(
                        primary: const Color.fromARGB(255, 16, 211, 255),
                        fixedSize: const Size(200, 40),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(50))),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed:
            // If not yet listening for speech start, otherwise stop
            _speechToText.isNotListening ? _startListening : _stopListening,
        tooltip: _speechToText.isNotListening ? 'Listen' : "Stop",
        child: Icon(_speechToText.isNotListening ? Icons.mic_off : Icons.mic),
      ),
    );
  }
}
