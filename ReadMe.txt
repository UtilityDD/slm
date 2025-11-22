
Recommendation: Provide images where the height is around 360px to 480px.

The layout has a very large number of TextViews for the levels, which are all defined statically. This is inefficient and makes the layout file very difficult to manage. I will replace this with a more dynamic approach in MainActivity.kt later, but for now, I will clean up the XML to prepare for that change.
The lineman_character's size and positioning can be refined to better match the new vector asset.