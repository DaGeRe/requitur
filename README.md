Requitur
===================

This enables using Run-Length-Encoding and Sequitur in Java projects. The current implementation of Sequitur (http://www.sequitur.info/) unfortunately only supports `String` elements. This implementation should be usable in different context (e.g. trace analysis, where trace elements might also contain the call depth etc.), therefore, any content can be compressed.

Use the interface of `Sequitur`. To support own content, create a subclass of `Content`.

# License

This project is dual licensed under MIT and AGPL license. You might use it under any of those licenses. If you choose to change anything, you might choose between those licenses for publishing your changes.
