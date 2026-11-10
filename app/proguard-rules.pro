# R8 (AGP 9.4.1's 9.4.x, still present in 9.4.34 and 9.5.29) can repackage a public part of the Kotlin
# stdlib's multifile facade kotlin.text.StringsKt into the default package while its superclass stays
# in kotlin.text. The stdlib declares these parts package-private (`class StringsKt__IndentKt`, no
# `public`), so ART refuses to link the subclass at startup:
#   IllegalAccessError: Class kotlin.text.k extended by class ha2 is inaccessible
# Pinning the names keeps the whole chain in kotlin.text, where package-private access is legal.
# Shrinking still removes unused parts; only renaming/repackaging/merging them is prevented.
-keepnames class kotlin.text.StringsKt__**
